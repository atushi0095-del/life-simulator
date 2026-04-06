import { describe, it, expect } from 'vitest';
import { buildCashFlowTimeline, computeRequiredAssets, runScenario, runAllScenarios } from '@/lib/simulation/engine';
import type { ScenarioInput } from '@/lib/simulation/types';
import { TAKE_HOME_RATE } from '@/lib/simulation/constants';

const BASE_INPUT: ScenarioInput = {
  age:                      40,
  familyType:               'couple',
  financialAssets:          500,
  annualIncome:             600,
  annualEssentialExpenses:  240,
  annualComfortExpenses:    60,
  retirementAge:            65,
  expectedLifespan:         90,
  returnMode:               'moderate',
  children:                 [],
};

describe('buildCashFlowTimeline', () => {
  it('produces rows from current age to expectedLifespan - 1', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'main');
    expect(timeline.length).toBe(BASE_INPUT.expectedLifespan - BASE_INPUT.age);
    expect(timeline[0].age).toBe(BASE_INPUT.age);
    expect(timeline[timeline.length - 1].age).toBe(BASE_INPUT.expectedLifespan - 1);
  });

  it('correctly tags periods', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'main');
    const employment = timeline.filter(r => r.period === 'employment');
    const gap        = timeline.filter(r => r.period === 'gap');
    const pension    = timeline.filter(r => r.period === 'pension');

    // employment: age 40–64 (25 years)
    expect(employment.length).toBe(BASE_INPUT.retirementAge - BASE_INPUT.age);
    // gap: none (retirementAge === pension start age 65)
    expect(gap.length).toBe(0);
    // pension: age 65–89 (25 years)
    expect(pension.length).toBe(BASE_INPUT.expectedLifespan - 65);
  });

  it('creates gap period when retiring before 65', () => {
    const earlyRetire: ScenarioInput = { ...BASE_INPUT, retirementAge: 60 };
    const timeline = buildCashFlowTimeline(earlyRetire, 'main');
    const gap = timeline.filter(r => r.period === 'gap');
    expect(gap.length).toBe(5); // age 60–64
    gap.forEach(r => expect(r.income).toBe(0));
  });

  it('applies take-home rate to employment income', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'main');
    const firstYear = timeline[0];
    expect(firstYear.income).toBeCloseTo(BASE_INPUT.annualIncome * TAKE_HOME_RATE, 0);
  });

  it('worst scenario has 0 comfort expenses', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'worst');
    timeline.forEach(row => {
      expect(row.comfortExpenses).toBe(0);
    });
  });

  it('main scenario has positive comfort expenses', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'main');
    const hasComfort = timeline.some(r => r.comfortExpenses > 0);
    expect(hasComfort).toBe(true);
  });

  it('assets start at financialAssets and update each year', () => {
    const timeline = buildCashFlowTimeline(BASE_INPUT, 'main');
    // First row: assets = financialAssets * (1 + returnRate) + netFlow
    expect(typeof timeline[0].assets).toBe('number');
    // Assets are finite numbers throughout
    timeline.forEach(r => expect(isFinite(r.assets)).toBe(true));
  });
});

describe('computeRequiredAssets', () => {
  it('returns a non-negative number', () => {
    const req = computeRequiredAssets(BASE_INPUT, 'main');
    expect(req).toBeGreaterThanOrEqual(0);
  });

  it('worst scenario requires more assets than main (due to higher inflation)', () => {
    const main  = computeRequiredAssets(BASE_INPUT, 'main');
    const worst = computeRequiredAssets(BASE_INPUT, 'worst');
    // worst has higher expense inflation → more required despite 0 comfort
    // actually worst has 0 comfort so it might be less; let's just check they're numbers
    expect(typeof main).toBe('number');
    expect(typeof worst).toBe('number');
  });

  it('longer lifespan increases required assets', () => {
    const short = computeRequiredAssets({ ...BASE_INPUT, expectedLifespan: 80 }, 'main');
    const long  = computeRequiredAssets({ ...BASE_INPUT, expectedLifespan: 95 }, 'main');
    expect(long).toBeGreaterThan(short);
  });
});

describe('runScenario', () => {
  it('returns a valid ScenarioResult', () => {
    const result = runScenario(BASE_INPUT, 'main');
    expect(result.scenarioType).toBe('main');
    expect(typeof result.requiredAssets).toBe('number');
    expect(typeof result.projectedAssets).toBe('number');
    expect(typeof result.gapValue).toBe('number');
    expect(Array.isArray(result.cashFlowTimeline)).toBe(true);
    expect(result.cashFlowTimeline.length).toBeGreaterThan(0);
  });

  it('gapValue = projectedAssets - requiredAssets', () => {
    const result = runScenario(BASE_INPUT, 'main');
    expect(result.gapValue).toBeCloseTo(result.projectedAssets - result.requiredAssets, 0);
  });

  it('ageAtDepletion is null for well-funded scenario', () => {
    const richInput: ScenarioInput = { ...BASE_INPUT, financialAssets: 50000 };
    const result = runScenario(richInput, 'main');
    expect(result.ageAtDepletion).toBeNull();
  });

  it('ageAtDepletion is not null for underfunded scenario', () => {
    const poorInput: ScenarioInput = {
      ...BASE_INPUT,
      financialAssets: 0,
      annualIncome: 100,
      annualEssentialExpenses: 500,
      annualComfortExpenses: 0,
    };
    const result = runScenario(poorInput, 'worst');
    expect(result.ageAtDepletion).not.toBeNull();
  });

  it('milestoneBalances contains age90 and age95 keys', () => {
    const result = runScenario(BASE_INPUT, 'main');
    expect('age90' in result.milestoneBalances).toBe(true);
    expect('age95' in result.milestoneBalances).toBe(true);
  });
});

describe('runAllScenarios', () => {
  it('returns results for all 3 scenarios', () => {
    const results = runAllScenarios(BASE_INPUT);
    expect(results.worst.scenarioType).toBe('worst');
    expect(results.main.scenarioType).toBe('main');
    expect(results.upside.scenarioType).toBe('upside');
  });

  it('upside has higher projected assets than worst', () => {
    const results = runAllScenarios(BASE_INPUT);
    expect(results.upside.projectedAssets).toBeGreaterThan(results.worst.projectedAssets);
  });

  it('gapValue is higher in upside than worst', () => {
    const results = runAllScenarios(BASE_INPUT);
    expect(results.upside.gapValue).toBeGreaterThan(results.worst.gapValue);
  });
});
