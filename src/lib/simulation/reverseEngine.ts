import type {
  ScenarioInput,
  ScenarioType,
  ReverseSimulationResult,
  SpendableMargin,
  AnnualCashFlow,
} from './types';
import {
  RETURN_RATES,
  EXPENSE_INFLATION,
  INCOME_GROWTH,
  TAKE_HOME_RATE,
  PENSION_START_AGE,
  DEFAULT_PENSION_MONTHLY,
  SURPLUS_DISPLAY_FACTOR,
} from './constants';
import { buildAllEvents, sumEventCosts } from './events';

// ============================================================
// buildCashFlowTimelineWithExtraSpend
//
// Same as buildCashFlowTimeline but adds `extraAnnualSpend` each year
// (inflation-adjusted). The extra spend is applied from retirementAge onward
// (the period where the user has discretion to spend more or less).
// ============================================================

export function buildCashFlowTimelineWithExtraSpend(
  input: ScenarioInput,
  scenario: ScenarioType,
  extraAnnualSpend: number,
): AnnualCashFlow[] {
  const events        = buildAllEvents(input);
  const returnRate    = RETURN_RATES[input.returnMode][scenario];
  const inflationRate = EXPENSE_INFLATION[scenario];
  const incomeRate    = INCOME_GROWTH[scenario];
  const pensionAnnual = (input.pensionMonthly ?? DEFAULT_PENSION_MONTHLY[input.familyType]) * 12;

  const totalYears = input.expectedLifespan - input.age; // inclusive end
  const retOffset  = input.retirementAge - input.age;
  const penOffset  = Math.max(retOffset, PENSION_START_AGE - input.age);
  const rows: AnnualCashFlow[] = [];
  let prevAssets = input.financialAssets;

  for (let t = 0; t <= totalYears; t++) {
    const age = input.age + t;

    // Determine period
    let period: 'employment' | 'gap' | 'pension';
    if (t < retOffset) {
      period = 'employment';
    } else if (t < penOffset) {
      period = 'gap';
    } else {
      period = 'pension';
    }

    // Add retirement bonus at the retirement year
    if (t === retOffset && (input.retirementBonus ?? 0) > 0) {
      prevAssets += input.retirementBonus!;
    }

    // Income (with career event rate)
    let income = 0;
    if (period === 'employment') {
      let careerRate = 1;
      if (input.careerEvents) {
        for (const event of input.careerEvents) {
          if (age >= event.age) careerRate = event.incomeRate;
        }
      }
      income = input.annualIncome * careerRate * TAKE_HOME_RATE * Math.pow(1 + incomeRate, t);
    } else if (period === 'pension') {
      income = pensionAnnual;
    }

    // Expenses (use post-retirement overrides if applicable)
    const isPostRetirement = period !== 'employment';
    const essBase = isPostRetirement && input.postRetirementEssentialExpenses != null
      ? input.postRetirementEssentialExpenses : input.annualEssentialExpenses;
    const comBase = isPostRetirement && input.postRetirementComfortExpenses != null
      ? input.postRetirementComfortExpenses : input.annualComfortExpenses;
    const essentialExpenses = essBase * Math.pow(1 + inflationRate, t);
    const comfortExpenses = comBase * Math.pow(1 + inflationRate, t);
    const eventCosts = sumEventCosts(t, input, events);

    // Extra spend: applied from retirement onward, inflation-adjusted
    const extra = t >= retOffset
      ? extraAnnualSpend * Math.pow(1 + inflationRate, t - retOffset)
      : 0;

    const netFlow = income - essentialExpenses - comfortExpenses - eventCosts - extra;
    const assets  = prevAssets * (1 + returnRate) + netFlow;

    rows.push({
      year: t,
      age,
      period,
      income,
      essentialExpenses,
      comfortExpenses: comfortExpenses + extra, // fold extra into comfort for display
      eventCosts,
      netFlow,
      assets,
    });
    prevAssets = assets;
  }

  return rows;
}

// ============================================================
// Reverse simulation: binary search for max extra annual spend
// such that balance at targetAge >= targetBalance
// ============================================================

export function runReverseSimulation(
  input: ScenarioInput,
  scenario: ScenarioType,
  targetAge: number,
  targetBalance: number,
): ReverseSimulationResult {
  // Clamp targetAge to expectedLifespan (can't look beyond timeline)
  const clampedTargetAge = Math.min(targetAge, input.expectedLifespan);
  targetAge = clampedTargetAge;

  // Edge: targetAge is before or at current age
  if (targetAge <= input.age) {
    return {
      scenarioType: scenario,
      extraAnnualSpend: 0,
      extraMonthlySpend: 0,
      targetAge,
      targetBalance,
    };
  }

  function getBalanceAtTarget(extraSpend: number): number {
    const timeline = buildCashFlowTimelineWithExtraSpend(input, scenario, extraSpend);
    const row = timeline.find(r => r.age === targetAge);
    return row?.assets ?? -Infinity;
  }

  // Check baseline (no extra spend)
  const baselineBalance = getBalanceAtTarget(0);
  if (baselineBalance < targetBalance) {
    // Already insufficient — user needs to cut spending, not add
    // Return negative margin
    let lo = -5000;
    let hi = 0;
    for (let iter = 0; iter < 50; iter++) {
      const mid = (lo + hi) / 2;
      if (getBalanceAtTarget(mid) >= targetBalance) {
        lo = mid;
      } else {
        hi = mid;
      }
    }
    const extra = Math.floor(lo);
    return {
      scenarioType: scenario,
      extraAnnualSpend: extra,
      extraMonthlySpend: Math.floor((extra / 12) * 10) / 10,
      targetAge,
      targetBalance,
    };
  }

  // Binary search: find maximum extra annual spend
  let lo = 0;
  let hi = Math.max(baselineBalance - targetBalance, 1000); // upper bound estimate

  // Ensure hi is actually too high
  while (getBalanceAtTarget(hi) >= targetBalance) {
    hi *= 2;
    if (hi > 100000) break; // safety
  }

  for (let iter = 0; iter < 50; iter++) {
    const mid = (lo + hi) / 2;
    if (getBalanceAtTarget(mid) >= targetBalance) {
      lo = mid;
    } else {
      hi = mid;
    }
  }

  const extraAnnual = Math.floor(lo); // round down for safety
  return {
    scenarioType: scenario,
    extraAnnualSpend: extraAnnual,
    extraMonthlySpend: Math.floor((extraAnnual / 12) * 10) / 10,
    targetAge,
    targetBalance,
  };
}

// ============================================================
// Build timeline with EXTRA spend applied from t=0 (current age)
// rather than from retirement.  Used for "lifetime" margin calc.
// ============================================================

export function buildCashFlowTimelineWithCurrentExtraSpend(
  input: ScenarioInput,
  scenario: ScenarioType,
  extraAnnualSpend: number,
): AnnualCashFlow[] {
  const events        = buildAllEvents(input);
  const returnRate    = RETURN_RATES[input.returnMode][scenario];
  const inflationRate = EXPENSE_INFLATION[scenario];
  const incomeRate    = INCOME_GROWTH[scenario];
  const pensionAnnual = (input.pensionMonthly ?? DEFAULT_PENSION_MONTHLY[input.familyType]) * 12;

  const totalYears = input.expectedLifespan - input.age; // inclusive end
  const retOffset  = input.retirementAge - input.age;
  const penOffset  = Math.max(retOffset, PENSION_START_AGE - input.age);
  const rows: AnnualCashFlow[] = [];
  let prevAssets = input.financialAssets;

  for (let t = 0; t <= totalYears; t++) {
    const age = input.age + t;

    let period: 'employment' | 'gap' | 'pension';
    if (t < retOffset) {
      period = 'employment';
    } else if (t < penOffset) {
      period = 'gap';
    } else {
      period = 'pension';
    }

    // Retirement bonus at retirement year
    if (t === retOffset && (input.retirementBonus ?? 0) > 0) {
      prevAssets += input.retirementBonus!;
    }

    // Income with career events
    let income = 0;
    if (period === 'employment') {
      let careerRate = 1;
      if (input.careerEvents) {
        for (const event of input.careerEvents) {
          if (age >= event.age) careerRate = event.incomeRate;
        }
      }
      income = input.annualIncome * careerRate * TAKE_HOME_RATE * Math.pow(1 + incomeRate, t);
    } else if (period === 'pension') {
      income = pensionAnnual;
    }

    // Expenses (post-retirement overrides)
    const isPostRetirement = period !== 'employment';
    const essBase = isPostRetirement && input.postRetirementEssentialExpenses != null
      ? input.postRetirementEssentialExpenses : input.annualEssentialExpenses;
    const comBase = isPostRetirement && input.postRetirementComfortExpenses != null
      ? input.postRetirementComfortExpenses : input.annualComfortExpenses;
    const essentialExpenses = essBase * Math.pow(1 + inflationRate, t);
    const comfortExpenses   = comBase * Math.pow(1 + inflationRate, t);
    const eventCosts        = sumEventCosts(t, input, events);

    // Extra spend from t=0, inflation-adjusted from the start
    const extra = extraAnnualSpend * Math.pow(1 + inflationRate, t);

    const netFlow = income - essentialExpenses - comfortExpenses - eventCosts - extra;
    const assets  = prevAssets * (1 + returnRate) + netFlow;

    rows.push({
      year: t, age, period, income, essentialExpenses,
      comfortExpenses: comfortExpenses + extra,
      eventCosts, netFlow, assets,
    });
    prevAssets = assets;
  }

  return rows;
}

// ============================================================
// Sustainable monthly budget:
// "想定寿命（expectedLifespan）でちょうど0になる月間使用可能額"
// Returns the TOTAL monthly spending amount (not just extra)
// positive differenceMonthly = can spend more than current
// negative differenceMonthly = needs to cut spending
// ============================================================

export interface SustainableMonthlyBudget {
  /** 万円/年 — total annual spending that results in balance=0 at expectedLifespan */
  sustainableAnnual: number;
  /** 万円/月 */
  sustainableMonthly: number;
  /** 万円/年 — current total (essential + comfort) */
  currentAnnual: number;
  /** 万円/月 — positive = can spend more, negative = needs to cut */
  differenceMonthly: number;
}

export function computeSustainableMonthlyBudget(
  input: ScenarioInput,
  scenario: ScenarioType,
): SustainableMonthlyBudget {
  const currentAnnual = input.annualEssentialExpenses + input.annualComfortExpenses;
  const targetAge = input.expectedLifespan;

  // Use buildCashFlowTimelineWithCurrentExtraSpend so that:
  // - getBalanceAt(0) exactly matches the residual balance table
  // - post-retirement expense overrides are respected
  // - employment income / career events are handled correctly
  function getBalanceAt(extraSpend: number): number {
    const tl = buildCashFlowTimelineWithCurrentExtraSpend(input, scenario, extraSpend);
    const row = tl.find(r => r.age === targetAge);
    return row?.assets ?? -Infinity;
  }

  const baselineBalance = getBalanceAt(0);
  let extraAnnual: number;

  if (baselineBalance < 0) {
    // Already running short — need to find how much to CUT (extraAnnual will be negative)
    let lo = -currentAnnual * 2;
    let hi = 0;
    // Expand lo until it gives negative balance
    while (getBalanceAt(lo) < 0) {
      lo *= 2;
      if (Math.abs(lo) > 500000) break;
    }
    for (let iter = 0; iter < 60; iter++) {
      const mid = (lo + hi) / 2;
      if (getBalanceAt(mid) >= 0) hi = mid; else lo = mid;
    }
    extraAnnual = Math.ceil(hi); // round toward zero (less generous cut)
  } else {
    // Surplus — find how much extra can be spent (extraAnnual will be positive)
    let lo = 0;
    let hi = Math.max(baselineBalance / 10, 100);
    // Expand hi until balance goes negative
    while (getBalanceAt(hi) >= 0) {
      hi *= 2;
      if (hi > 500000) break;
    }
    for (let iter = 0; iter < 60; iter++) {
      const mid = (lo + hi) / 2;
      if (getBalanceAt(mid) >= 0) lo = mid; else hi = mid;
    }
    extraAnnual = Math.floor(lo); // round down for safety
  }

  const sustainableAnnual  = currentAnnual + extraAnnual;
  const sustainableMonthly = Math.floor((sustainableAnnual / 12) * 10) / 10;
  const differenceMonthly  = Math.floor((extraAnnual / 12) * 10) / 10;

  return { sustainableAnnual, sustainableMonthly, currentAnnual, differenceMonthly };
}

// ============================================================
// Targeted sustainable budget:
// "What can I spend right now if I target exactly `targetBalance` at `targetAge`?"
// Same mechanics as computeSustainableMonthlyBudget but with custom target.
// ============================================================

export interface TargetedSustainableBudget extends SustainableMonthlyBudget {
  /** 歳 — the target age used */
  targetAge: number;
  /** 万円 — the target balance used */
  targetBalance: number;
  /** 万円 — forward-simulation verification of balance at targetAge */
  verifiedBalance: number;
}

export function computeTargetedSustainableBudget(
  input: ScenarioInput,
  scenario: ScenarioType,
  targetAge: number,
  targetBalance: number,
): TargetedSustainableBudget {
  const currentAnnual  = input.annualEssentialExpenses + input.annualComfortExpenses;
  const clampedTarget  = Math.min(Math.max(targetAge, input.age + 1), input.expectedLifespan);

  function getBalanceAt(extraSpend: number): number {
    const tl = buildCashFlowTimelineWithCurrentExtraSpend(input, scenario, extraSpend);
    const row = tl.find(r => r.age === clampedTarget);
    return row?.assets ?? -Infinity;
  }

  const baselineBalance = getBalanceAt(0);
  let extraAnnual: number;

  if (baselineBalance <= targetBalance) {
    // Need to cut spending to reach targetBalance (or baseline already below target)
    let lo = -currentAnnual * 2;
    let hi = 0;
    while (getBalanceAt(lo) < targetBalance) {
      lo *= 2;
      if (Math.abs(lo) > 500000) break;
    }
    for (let iter = 0; iter < 60; iter++) {
      const mid = (lo + hi) / 2;
      if (getBalanceAt(mid) >= targetBalance) hi = mid; else lo = mid;
    }
    extraAnnual = Math.ceil(hi);
  } else {
    // Have surplus above target — find max extra spend
    let lo = 0;
    let hi = Math.max((baselineBalance - targetBalance) / 10, 100);
    while (getBalanceAt(hi) >= targetBalance) {
      hi *= 2;
      if (hi > 500000) break;
    }
    for (let iter = 0; iter < 60; iter++) {
      const mid = (lo + hi) / 2;
      if (getBalanceAt(mid) >= targetBalance) lo = mid; else hi = mid;
    }
    extraAnnual = Math.floor(lo);
  }

  const sustainableAnnual  = currentAnnual + extraAnnual;
  const sustainableMonthly = Math.floor((sustainableAnnual / 12) * 10) / 10;
  const differenceMonthly  = Math.floor((extraAnnual / 12) * 10) / 10;
  const verifiedBalance    = Math.round(getBalanceAt(extraAnnual));

  return {
    sustainableAnnual, sustainableMonthly, currentAnnual, differenceMonthly,
    targetAge: clampedTarget, targetBalance, verifiedBalance,
  };
}

// ============================================================
// Compute spendable margins at milestone ages
// "How much extra could I spend per year and still have ≥ 0 at age X?"
// ============================================================

export function computeSpendableMargins(
  input: ScenarioInput,
  scenario: ScenarioType,
  milestoneAges: number[] = [65, 70, 75, 80, 85, 90, 95],
): SpendableMargin[] {
  const timeline = buildCashFlowTimelineWithExtraSpend(input, scenario, 0);
  const margins: SpendableMargin[] = [];

  for (const age of milestoneAges) {
    // Only include ages AFTER retirement (employment-period margins are not meaningful)
    if (age <= input.age || age > input.expectedLifespan) continue;
    if (age <= input.retirementAge) continue;

    const row = timeline.find(r => r.age === age);
    const projectedBalance = row?.assets ?? 0;

    // Run reverse simulation targeting balance = 0 at this age
    const result = runReverseSimulation(input, scenario, age, 0);

    // Apply safety factor
    const safeAnnual  = Math.floor(result.extraAnnualSpend * SURPLUS_DISPLAY_FACTOR);
    const safeMonthly = Math.floor((safeAnnual / 12) * 10) / 10;

    margins.push({
      age,
      projectedBalance: Math.round(projectedBalance),
      annualMargin: safeAnnual,
      monthlyMargin: safeMonthly,
    });
  }

  return margins;
}
