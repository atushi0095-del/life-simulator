import type {
  ScenarioInput,
  ScenarioType,
  ScenarioResult,
  SimulationResults,
  AnnualCashFlow,
  LifePeriod,
  MilestoneBalances,
} from './types';
import {
  RETURN_RATES,
  EXPENSE_INFLATION,
  INCOME_GROWTH,
  TAKE_HOME_RATE,
  PENSION_START_AGE,
  DEFAULT_PENSION_MONTHLY,
} from './constants';
import { buildAllEvents, sumEventCosts, sumTemporaryIncomes } from './events';

// ============================================================
// Internal helpers
// ============================================================

function getReturnRate(input: ScenarioInput, scenario: ScenarioType): number {
  return RETURN_RATES[input.returnMode][scenario];
}

function getPensionAnnual(input: ScenarioInput): number {
  const monthly = input.pensionMonthly ?? DEFAULT_PENSION_MONTHLY[input.familyType];
  return monthly * 12;
}

/** Get the income multiplier for career events (role demotion / reemployment) at a given age */
function getCareerIncomeRate(input: ScenarioInput, age: number): number {
  if (!input.careerEvents || input.careerEvents.length === 0) return 1;
  // Find the most recent career event at or before this age
  let rate = 1;
  for (const event of input.careerEvents) {
    if (age >= event.age) {
      rate = event.incomeRate;
    }
  }
  return rate;
}

/** Get essential/comfort expenses, using post-retirement overrides if applicable */
function getExpenses(
  input: ScenarioInput,
  t: number,
  inflationFactor: number,
  isPostRetirement: boolean,
): { essential: number; comfort: number } {
  const essBase = isPostRetirement && input.postRetirementEssentialExpenses != null
    ? input.postRetirementEssentialExpenses
    : input.annualEssentialExpenses;
  const comBase = isPostRetirement && input.postRetirementComfortExpenses != null
    ? input.postRetirementComfortExpenses
    : input.annualComfortExpenses;
  return {
    essential: essBase * Math.pow(1 + inflationFactor, t),
    comfort:   comBase * Math.pow(1 + inflationFactor, t),
  };
}

/** Build one year's cash flow row, given the previous year's assets */
function buildRow(
  yearOffset: number,
  prevAssets: number,
  income: number,
  essentialExpenses: number,
  comfortExpenses: number,
  eventCosts: number,
  returnRate: number,
  period: LifePeriod,
  age: number,
): AnnualCashFlow {
  const netFlow = income - essentialExpenses - comfortExpenses - eventCosts;
  const assets  = prevAssets * (1 + returnRate) + netFlow;
  return {
    year: yearOffset,
    age,
    period,
    income,
    essentialExpenses,
    comfortExpenses,
    eventCosts,
    netFlow,
    assets,
  };
}

// ============================================================
// 3-period builders
// Each takes startAssets and returns the rows for that period.
// ============================================================

function buildEmploymentPeriod(
  input: ScenarioInput,
  scenario: ScenarioType,
  startAssets: number,
  events: ReturnType<typeof buildAllEvents>,
): AnnualCashFlow[] {
  const rows: AnnualCashFlow[] = [];
  const returnRate   = getReturnRate(input, scenario);
  const inflationFactor = EXPENSE_INFLATION[scenario];
  const incomeFactor    = INCOME_GROWTH[scenario];
  let prevAssets = startAssets;

  const endOffset = input.retirementAge - input.age; // exclusive

  for (let t = 0; t < endOffset; t++) {
    const age  = input.age + t;
    const careerRate = getCareerIncomeRate(input, age);
    const income = input.annualIncome * careerRate * TAKE_HOME_RATE * Math.pow(1 + incomeFactor, t)
      + sumTemporaryIncomes(t, input);
    const { essential: essentialExpenses, comfort: comfortExpenses } = getExpenses(input, t, inflationFactor, false);
    const eventCosts = sumEventCosts(t, input, events);

    const row = buildRow(t, prevAssets, income, essentialExpenses, comfortExpenses, eventCosts, returnRate, 'employment', age);
    rows.push(row);
    prevAssets = row.assets;
  }
  return rows;
}

function buildGapPeriod(
  input: ScenarioInput,
  scenario: ScenarioType,
  startAssets: number,
  events: ReturnType<typeof buildAllEvents>,
): AnnualCashFlow[] {
  // Gap period only exists if retirement is before pension start age
  if (input.retirementAge >= PENSION_START_AGE) return [];

  const rows: AnnualCashFlow[] = [];
  const returnRate      = getReturnRate(input, scenario);
  const inflationFactor = EXPENSE_INFLATION[scenario];
  let prevAssets = startAssets;

  const gapStart = input.retirementAge - input.age;  // offset from age
  const gapEnd   = PENSION_START_AGE   - input.age;  // exclusive

  for (let t = gapStart; t < gapEnd; t++) {
    const age  = input.age + t;
    const { essential: essentialExpenses, comfort: comfortExpenses } = getExpenses(input, t, inflationFactor, true);
    const eventCosts = sumEventCosts(t, input, events);

    const gapIncome = sumTemporaryIncomes(t, input);
    const row = buildRow(t, prevAssets, gapIncome, essentialExpenses, comfortExpenses, eventCosts, returnRate, 'gap', age);
    rows.push(row);
    prevAssets = row.assets;
  }
  return rows;
}

function buildPensionPeriod(
  input: ScenarioInput,
  scenario: ScenarioType,
  startAssets: number,
  events: ReturnType<typeof buildAllEvents>,
): AnnualCashFlow[] {
  const rows: AnnualCashFlow[] = [];
  const returnRate      = getReturnRate(input, scenario);
  const inflationFactor = EXPENSE_INFLATION[scenario];
  const pensionAnnual   = getPensionAnnual(input);
  let prevAssets = startAssets;

  const pensionStart = Math.max(input.retirementAge, PENSION_START_AGE) - input.age;
  const totalYears   = input.expectedLifespan - input.age; // inclusive end

  for (let t = pensionStart; t <= totalYears; t++) {
    const age  = input.age + t;
    const { essential: essentialExpenses, comfort: comfortExpenses } = getExpenses(input, t, inflationFactor, true);
    const eventCosts = sumEventCosts(t, input, events);

    const pensionIncome = pensionAnnual + sumTemporaryIncomes(t, input);
    const row = buildRow(t, prevAssets, pensionIncome, essentialExpenses, comfortExpenses, eventCosts, returnRate, 'pension', age);
    rows.push(row);
    prevAssets = row.assets;
  }
  return rows;
}

// ============================================================
// Full timeline
// ============================================================

export function buildCashFlowTimeline(
  input: ScenarioInput,
  scenario: ScenarioType,
): AnnualCashFlow[] {
  const events = buildAllEvents(input);
  const startAssets = input.financialAssets;

  const employment = buildEmploymentPeriod(input, scenario, startAssets, events);
  const retirementBonus = input.retirementBonus ?? 0;
  const lastEmploymentAssets = (employment.length > 0
    ? employment[employment.length - 1].assets
    : startAssets) + retirementBonus;

  const gap = buildGapPeriod(input, scenario, lastEmploymentAssets, events);
  const lastGapAssets = gap.length > 0
    ? gap[gap.length - 1].assets
    : lastEmploymentAssets;

  const pension = buildPensionPeriod(input, scenario, lastGapAssets, events);

  return [...employment, ...gap, ...pension];
}

// ============================================================
// Required assets: PV of post-retirement net deficits
// ============================================================

export function computeRequiredAssets(
  input: ScenarioInput,
  scenario: ScenarioType,
): number {
  const returnRate      = getReturnRate(input, scenario);
  const inflationFactor = EXPENSE_INFLATION[scenario];
  const pensionAnnual   = getPensionAnnual(input);
  const events          = buildAllEvents(input);

  let requiredAssets = 0;
  const retirementOffset = input.retirementAge - input.age;
  const totalYears       = input.expectedLifespan - input.age;

  for (let t = retirementOffset; t <= totalYears; t++) {
    const { essential: essentialExpenses, comfort: comfortExpenses } = getExpenses(input, t, inflationFactor, true);
    const eventCosts = sumEventCosts(t, input, events);

    const income = input.age + t >= PENSION_START_AGE ? pensionAnnual : 0;
    const netDeficit = essentialExpenses + comfortExpenses + eventCosts - income;

    if (netDeficit > 0) {
      const yearsAfterRetirement = t - retirementOffset;
      const discountFactor = Math.pow(1 + returnRate, yearsAfterRetirement);
      requiredAssets += netDeficit / (discountFactor > 0 ? discountFactor : 0.001);
    }
  }
  return Math.max(0, requiredAssets);
}

// ============================================================
// Milestone balances
// ============================================================

function computeMilestoneBalances(
  timeline: AnnualCashFlow[],
): MilestoneBalances {
  // Only post-retirement depletion matters for milestone reporting
  const ageAtDepletion = timeline.find(r => r.assets < 0 && r.period !== 'employment')?.age ?? null;

  function balanceAt(age: number): number | null {
    if (ageAtDepletion !== null && ageAtDepletion <= age) return null;
    return timeline.find(r => r.age === age)?.assets ?? null;
  }

  return {
    age80: balanceAt(80),
    age85: balanceAt(85),
    age90: balanceAt(90),
    age95: balanceAt(95),
  };
}

// ============================================================
// Run one scenario
// ============================================================

export function runScenario(
  input: ScenarioInput,
  scenario: ScenarioType,
): ScenarioResult {
  const timeline        = buildCashFlowTimeline(input, scenario);
  const retirementRow   = timeline.find(r => r.age === input.retirementAge);
  const projectedAssets = retirementRow?.assets ?? 0;
  const requiredAssets  = computeRequiredAssets(input, scenario);
  const gapValue        = projectedAssets - requiredAssets;
  // Only count depletion that occurs AFTER retirement — employment-period dips are
  // temporary (income keeps arriving) and should not trigger a "depletion" warning.
  const ageAtDepletion  = timeline.find(r => r.assets < 0 && r.period !== 'employment')?.age ?? null;
  const milestoneBalances = computeMilestoneBalances(timeline);

  return {
    scenarioType: scenario,
    requiredAssets,
    projectedAssets,
    gapValue,
    cashFlowTimeline: timeline,
    ageAtDepletion,
    milestoneBalances,
  };
}

// ============================================================
// Run all three scenarios
// ============================================================

export function runAllScenarios(input: ScenarioInput): SimulationResults {
  return {
    worst:  runScenario(input, 'worst'),
    main:   runScenario(input, 'main'),
    upside: runScenario(input, 'upside'),
  };
}
