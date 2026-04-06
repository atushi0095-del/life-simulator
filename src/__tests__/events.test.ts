import { describe, it, expect } from 'vitest';
import {
  buildEducationEvents,
  buildHousingEvent,
  buildCareEvent,
  sumEventCosts,
} from '@/lib/simulation/events';
import type { ScenarioInput } from '@/lib/simulation/types';
import { EDUCATION_TOTAL_COST, EDUCATION_DURATION_YEARS } from '@/lib/simulation/constants';

const BASE_INPUT: ScenarioInput = {
  age:                      35,
  familyType:               'family_with_children',
  financialAssets:          300,
  annualIncome:             500,
  annualEssentialExpenses:  200,
  annualComfortExpenses:    50,
  retirementAge:            65,
  expectedLifespan:         90,
  returnMode:               'moderate',
  children:                 [],
};

describe('buildEducationEvents', () => {
  it('returns empty array when no children', () => {
    const events = buildEducationEvents(BASE_INPUT);
    expect(events).toHaveLength(0);
  });

  it('returns one event per child', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      children: [
        { birthYearOffset: 0, educationPath: 'public' },
        { birthYearOffset: 3, educationPath: 'private_uni' },
      ],
    };
    const events = buildEducationEvents(input);
    expect(events).toHaveLength(2);
  });

  it('spreads education cost evenly over 22 years', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      children: [{ birthYearOffset: 0, educationPath: 'public' }],
    };
    const [ev] = buildEducationEvents(input);
    const expectedAnnual = EDUCATION_TOTAL_COST.public / EDUCATION_DURATION_YEARS;

    for (let t = 0; t < EDUCATION_DURATION_YEARS; t++) {
      expect(ev.getCostAtYearOffset(t, input)).toBeCloseTo(expectedAnnual, 1);
    }
    // After 22 years, cost is 0
    expect(ev.getCostAtYearOffset(EDUCATION_DURATION_YEARS, input)).toBe(0);
  });

  it('respects birthYearOffset for delayed birth', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      children: [{ birthYearOffset: 5, educationPath: 'public' }],
    };
    const [ev] = buildEducationEvents(input);
    // Before birth offset: 0
    expect(ev.getCostAtYearOffset(4, input)).toBe(0);
    // At birth offset: cost begins
    expect(ev.getCostAtYearOffset(5, input)).toBeGreaterThan(0);
    // After education ends (5 + 22 = 27): 0
    expect(ev.getCostAtYearOffset(27, input)).toBe(0);
  });
});

describe('buildHousingEvent', () => {
  it('returns null when no housing in input', () => {
    expect(buildHousingEvent(BASE_INPUT)).toBeNull();
  });

  it('purchase: cost only at yearsFromNow', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      housing: { type: 'purchase', yearsFromNow: 3, cost: 4000, annualRent: 0 },
    };
    const ev = buildHousingEvent(input)!;
    expect(ev.getCostAtYearOffset(2, input)).toBe(0);
    expect(ev.getCostAtYearOffset(3, input)).toBe(4000);
    expect(ev.getCostAtYearOffset(4, input)).toBe(0);
  });

  it('rent: annual cost from yearsFromNow onward', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      housing: { type: 'rent', yearsFromNow: 2, cost: 0, annualRent: 120 },
    };
    const ev = buildHousingEvent(input)!;
    expect(ev.getCostAtYearOffset(1, input)).toBe(0);
    expect(ev.getCostAtYearOffset(2, input)).toBe(120);
    expect(ev.getCostAtYearOffset(10, input)).toBe(120);
  });
});

describe('buildCareEvent', () => {
  it('returns null when no care in input', () => {
    expect(buildCareEvent(BASE_INPUT)).toBeNull();
  });

  it('applies care cost only during care period', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      care: { yearsFromNow: 20, annualCost: 150, durationYears: 3 },
    };
    const ev = buildCareEvent(input)!;
    expect(ev.getCostAtYearOffset(19, input)).toBe(0);
    expect(ev.getCostAtYearOffset(20, input)).toBe(150);
    expect(ev.getCostAtYearOffset(22, input)).toBe(150);
    expect(ev.getCostAtYearOffset(23, input)).toBe(0);
  });
});

describe('sumEventCosts', () => {
  it('sums multiple events at same year', () => {
    const input: ScenarioInput = {
      ...BASE_INPUT,
      children: [{ birthYearOffset: 0, educationPath: 'public' }],
      care: { yearsFromNow: 0, annualCost: 100, durationYears: 5 },
    };
    const events = [
      buildEducationEvents(input)[0],
      buildCareEvent(input)!,
    ];
    const totalAtYear0 = sumEventCosts(0, input, events);
    const expectedEdu = EDUCATION_TOTAL_COST.public / EDUCATION_DURATION_YEARS;
    expect(totalAtYear0).toBeCloseTo(expectedEdu + 100, 0);
  });

  it('returns 0 when no events', () => {
    expect(sumEventCosts(0, BASE_INPUT, [])).toBe(0);
  });
});
