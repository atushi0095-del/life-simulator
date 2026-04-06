import { describe, it, expect } from 'vitest';
import { runMonteCarlo, sampleAnnualReturn } from '@/lib/simulation/montecarlo';
import type { ScenarioInput } from '@/lib/simulation/types';

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

describe('sampleAnnualReturn', () => {
  it('returns a finite number', () => {
    const r = sampleAnnualReturn('moderate');
    expect(isFinite(r)).toBe(true);
  });

  it('returns values in a reasonable range over 100 samples', () => {
    const samples = Array.from({ length: 100 }, () => sampleAnnualReturn('moderate'));
    const outOfRange = samples.filter(v => v < -0.5 || v > 1.0);
    // Expect < 5% of samples to be extreme outliers
    expect(outOfRange.length).toBeLessThan(5);
  });
});

describe('runMonteCarlo', () => {
  it('runs 1000 trials', () => {
    const output = runMonteCarlo(BASE_INPUT);
    expect(output.trials).toBe(1000);
  });

  it('successRate is between 0 and 1', () => {
    const output = runMonteCarlo(BASE_INPUT);
    expect(output.successRate).toBeGreaterThanOrEqual(0);
    expect(output.successRate).toBeLessThanOrEqual(1);
  });

  it('percentiles are ordered p10 <= p25 <= p50 <= p75 <= p90', () => {
    const { percentiles } = runMonteCarlo(BASE_INPUT);
    expect(percentiles.p10).toBeLessThanOrEqual(percentiles.p25);
    expect(percentiles.p25).toBeLessThanOrEqual(percentiles.p50);
    expect(percentiles.p50).toBeLessThanOrEqual(percentiles.p75);
    expect(percentiles.p75).toBeLessThanOrEqual(percentiles.p90);
  });

  it('very rich input has high success rate', () => {
    const richInput: ScenarioInput = { ...BASE_INPUT, financialAssets: 100000 };
    const output = runMonteCarlo(richInput);
    expect(output.successRate).toBeGreaterThan(0.8);
  });

  it('very poor input has low success rate', () => {
    const poorInput: ScenarioInput = {
      ...BASE_INPUT,
      financialAssets:          0,
      annualIncome:             100,
      annualEssentialExpenses:  600,
      annualComfortExpenses:    0,
    };
    const output = runMonteCarlo(poorInput);
    expect(output.successRate).toBeLessThan(0.2);
  });

  it('ageAtDepletionDistribution entries have age and count', () => {
    const output = runMonteCarlo(BASE_INPUT);
    output.ageAtDepletionDistribution.forEach(entry => {
      expect(typeof entry.age).toBe('number');
      expect(typeof entry.count).toBe('number');
      expect(entry.count).toBeGreaterThan(0);
    });
  });
});
