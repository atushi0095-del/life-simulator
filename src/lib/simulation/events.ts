import type { ScenarioInput, EducationConfig, EducationStage } from './types';
import {
  EDUCATION_TOTAL_COST,
  EDUCATION_DURATION_YEARS,
  EDUCATION_STAGE_COST,
  EDUCATION_STAGE_YEARS,
  EDUCATION_STAGE_START_AGE,
} from './constants';

// ============================================================
// Pluggable cost event interface
// Each event contributes an annual cost for a given year offset
// (yearOffset = 0 means the current year, i.e., at input.age)
// ============================================================

export interface CostEvent {
  label: string;
  getCostAtYearOffset: (yearOffset: number, input: ScenarioInput) => number;
}

// ---- Education helpers ----

const STAGE_ORDER: EducationStage[] = ['elementary', 'middle', 'high', 'university'];

/**
 * Compute per-stage education cost using the detailed EducationConfig.
 * Returns annual cost for a given year offset since child's birth.
 */
function educationCostFromConfig(config: EducationConfig, childAge: number): number {
  for (const stage of STAGE_ORDER) {
    const stageStart = EDUCATION_STAGE_START_AGE[stage];
    const stageYears = EDUCATION_STAGE_YEARS[stage];
    const stageEnd   = stageStart + stageYears;

    if (childAge >= stageStart && childAge < stageEnd) {
      const stageType  = config[stage];
      const totalCost  = EDUCATION_STAGE_COST[stage][stageType];
      return totalCost / stageYears;
    }
  }
  return 0; // before elementary or after university
}

/**
 * Compute total education cost from a per-stage EducationConfig.
 */
export function computeEducationTotalFromConfig(config: EducationConfig): number {
  return STAGE_ORDER.reduce((sum, stage) => {
    return sum + EDUCATION_STAGE_COST[stage][config[stage]];
  }, 0);
}

// ---- Education ----

export function buildEducationEvents(input: ScenarioInput): CostEvent[] {
  return input.children.map((child, i) => ({
    label: `子ども${i + 1}教育費`,
    getCostAtYearOffset: (yearOffset: number): number => {
      // If the child has a detailed per-stage config, use it
      if (child.educationConfig) {
        const childAge = yearOffset - child.birthYearOffset;
        if (childAge < 0) return 0;
        return educationCostFromConfig(child.educationConfig, childAge);
      }

      // Otherwise, use the legacy flat-spread model
      const startOffset = child.birthYearOffset;
      const endOffset   = startOffset + EDUCATION_DURATION_YEARS - 1;
      if (yearOffset < startOffset || yearOffset > endOffset) return 0;
      const total = EDUCATION_TOTAL_COST[child.educationPath];
      return total / EDUCATION_DURATION_YEARS;
    },
  }));
}

// ---- Housing ----

/**
 * For purchase: one-time cost at yearsFromNow.
 * For rent:     annual rent applied from yearsFromNow onward.
 */
export function buildHousingEvent(input: ScenarioInput): CostEvent | null {
  if (!input.housing) return null;
  const h = input.housing;
  if (h.type === 'purchase') {
    return {
      label: '住宅購入費',
      getCostAtYearOffset: (yearOffset: number): number =>
        yearOffset === h.yearsFromNow ? h.cost : 0,
    };
  }
  // rent
  return {
    label: '家賃',
    getCostAtYearOffset: (yearOffset: number): number =>
      yearOffset >= h.yearsFromNow ? h.annualRent : 0,
  };
}

// ---- Care ----

export function buildCareEvent(input: ScenarioInput): CostEvent | null {
  if (!input.care) return null;
  const c = input.care;
  return {
    label: '介護費',
    getCostAtYearOffset: (yearOffset: number): number => {
      if (yearOffset < c.yearsFromNow) return 0;
      if (yearOffset >= c.yearsFromNow + c.durationYears) return 0;
      return c.annualCost;
    },
  };
}

// ---- Car ----

/**
 * One-time car purchase, or periodic replacement if replacementEveryYears is set.
 */
export function buildCarEvent(input: ScenarioInput): CostEvent | null {
  if (!input.car) return null;
  const c = input.car;
  return {
    label: '車購入費',
    getCostAtYearOffset: (yearOffset: number): number => {
      if (yearOffset < c.yearsFromNow) return 0;
      const elapsed = yearOffset - c.yearsFromNow;
      if (!c.replacementEveryYears) {
        return elapsed === 0 ? c.cost : 0;
      }
      return elapsed % c.replacementEveryYears === 0 ? c.cost : 0;
    },
  };
}

// ---- Temporary income ----

/**
 * Sum all temporary income entries (already take-home) at a given year offset.
 * Used directly in engine.ts and montecarlo.ts income calculations.
 */
export function sumTemporaryIncomes(yearOffset: number, input: ScenarioInput): number {
  if (!input.temporaryIncomes?.length) return 0;
  return input.temporaryIncomes.reduce((sum, ti) => {
    if (yearOffset >= ti.yearsFromNow && yearOffset < ti.yearsFromNow + ti.durationYears) {
      return sum + ti.annualAmount;
    }
    return sum;
  }, 0);
}

// ---- Aggregator ----

export function buildAllEvents(input: ScenarioInput): CostEvent[] {
  const events: CostEvent[] = [...buildEducationEvents(input)];
  const housing = buildHousingEvent(input);
  if (housing) events.push(housing);
  const care = buildCareEvent(input);
  if (care) events.push(care);
  const car = buildCarEvent(input);
  if (car) events.push(car);
  return events;
}

export function sumEventCosts(
  yearOffset: number,
  input: ScenarioInput,
  events: CostEvent[],
): number {
  return events.reduce((sum, ev) => sum + ev.getCostAtYearOffset(yearOffset, input), 0);
}
