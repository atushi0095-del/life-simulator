import type { ScenarioInput, MonteCarloOutput, AgeDepletionEntry } from './types';
import {
  RETURN_RATES,
  RETURN_STDDEV,
  EXPENSE_INFLATION,
  INCOME_GROWTH,
  TAKE_HOME_RATE,
  PENSION_START_AGE,
  DEFAULT_PENSION_MONTHLY,
  MONTE_CARLO_RUNS,
} from './constants';
import { buildAllEvents, sumEventCosts, sumTemporaryIncomes } from './events';

// ============================================================
// Pseudo-random normal distribution via Box-Muller transform
// (No external dependency, deterministic enough for display use)
// ============================================================

function boxMuller(): number {
  let u = 0, v = 0;
  while (u === 0) u = Math.random();
  while (v === 0) v = Math.random();
  return Math.sqrt(-2.0 * Math.log(u)) * Math.cos(2.0 * Math.PI * v);
}

/**
 * Sample annual return from a log-normal distribution.
 * Uses the moderate scenario mean return for the given returnMode.
 */
export function sampleAnnualReturn(
  returnMode: ScenarioInput['returnMode'],
): number {
  const mean   = RETURN_RATES[returnMode].main;
  const stddev = RETURN_STDDEV[returnMode];
  // log-normal: exp(μ + σ·Z) where μ, σ are of the underlying normal
  // Approximation: shift mean to account for variance drag
  const mu    = Math.log(1 + mean) - 0.5 * stddev * stddev;
  const sigma = stddev;
  return Math.exp(mu + sigma * boxMuller()) - 1;
}

// ============================================================
// Single Monte Carlo trial
// Returns the final assets value at expectedLifespan.
// If assets go negative before the end, returns the assets at that point
// (caller determines depletion age separately).
// ============================================================

export function runSingleTrial(input: ScenarioInput): {
  finalAssets: number;
  ageAtDepletion: number | null;
} {
  const events = buildAllEvents(input);
  // Use 'main' scenario for expense inflation / income growth in MC
  // (variability comes only from returns)
  const inflationFactor = EXPENSE_INFLATION.main;
  const incomeFactor    = INCOME_GROWTH.main;
  const pensionAnnual   =
    (input.pensionMonthly ?? DEFAULT_PENSION_MONTHLY[input.familyType]) * 12;

  let assets = input.financialAssets;
  let ageAtDepletion: number | null = null;
  const totalYears = input.expectedLifespan - input.age;

  for (let t = 0; t <= totalYears; t++) {
    const currentAge = input.age + t;
    const returnRate = sampleAnnualReturn(input.returnMode);

    // Retirement bonus at retirement year
    if (currentAge === input.retirementAge && (input.retirementBonus ?? 0) > 0) {
      assets += input.retirementBonus!;
    }

    // Income (with career event rate)
    let income = 0;
    if (currentAge < input.retirementAge) {
      let careerRate = 1;
      if (input.careerEvents) {
        for (const event of input.careerEvents) {
          if (currentAge >= event.age) careerRate = event.incomeRate;
        }
      }
      income = input.annualIncome * careerRate * TAKE_HOME_RATE * Math.pow(1 + incomeFactor, t);
    } else if (currentAge >= PENSION_START_AGE) {
      income = pensionAnnual;
    }
    // Add temporary incomes (養育費・副業・賃貸収入など) for any period
    income += sumTemporaryIncomes(t, input);

    // Expenses (use post-retirement overrides if applicable)
    const isPostRetirement = currentAge >= input.retirementAge;
    const essBase = isPostRetirement && input.postRetirementEssentialExpenses != null
      ? input.postRetirementEssentialExpenses : input.annualEssentialExpenses;
    const comBase = isPostRetirement && input.postRetirementComfortExpenses != null
      ? input.postRetirementComfortExpenses : input.annualComfortExpenses;
    const essentialExpenses = essBase * Math.pow(1 + inflationFactor, t);
    const comfortExpenses   = comBase * Math.pow(1 + inflationFactor, t);
    const eventCosts        = sumEventCosts(t, input, events);

    const netFlow = income - essentialExpenses - comfortExpenses - eventCosts;
    assets = assets * (1 + returnRate) + netFlow;

    // Only flag depletion post-retirement (consistent with engine.ts).
    // Employment-period dips from large one-time costs (housing, education) are not "depletion".
    if (assets < 0 && ageAtDepletion === null && currentAge >= input.retirementAge) {
      ageAtDepletion = currentAge;
    }
  }

  return { finalAssets: assets, ageAtDepletion };
}

// ============================================================
// Percentile helper
// ============================================================

function percentile(sorted: number[], p: number): number {
  if (sorted.length === 0) return 0;
  const idx = Math.min(
    Math.floor((p / 100) * sorted.length),
    sorted.length - 1,
  );
  return sorted[idx];
}

// ============================================================
// Full Monte Carlo run
// ============================================================

export function runMonteCarlo(input: ScenarioInput): MonteCarloOutput {
  const finalAssetsList: number[] = [];
  const ageAtDepletionMap: Map<number, number> = new Map();
  let successCount = 0;

  for (let i = 0; i < MONTE_CARLO_RUNS; i++) {
    const { finalAssets, ageAtDepletion } = runSingleTrial(input);
    finalAssetsList.push(finalAssets);

    if (ageAtDepletion === null) {
      successCount++;
    } else {
      const count = ageAtDepletionMap.get(ageAtDepletion) ?? 0;
      ageAtDepletionMap.set(ageAtDepletion, count + 1);
    }
  }

  const sorted = [...finalAssetsList].sort((a, b) => a - b);
  const successRate = successCount / MONTE_CARLO_RUNS;

  // Build depletion histogram
  const ageAtDepletionDistribution: AgeDepletionEntry[] = Array.from(
    ageAtDepletionMap.entries(),
  )
    .map(([age, count]) => ({ age, count }))
    .sort((a, b) => a.age - b.age);

  return {
    trials: MONTE_CARLO_RUNS,
    successRate,
    percentiles: {
      p10: percentile(sorted, 10),
      p25: percentile(sorted, 25),
      p50: percentile(sorted, 50),
      p75: percentile(sorted, 75),
      p90: percentile(sorted, 90),
    },
    ageAtDepletionDistribution,
  };
}
