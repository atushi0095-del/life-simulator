import type { FamilyType, ReturnMode, ScenarioType, EducationPath, EducationStage, StageType, CareType } from './types';

// ---- Annual investment return rates by ReturnMode × ScenarioType ----
export const RETURN_RATES: Record<ReturnMode, Record<ScenarioType, number>> = {
  conservative: { worst: -0.01, main: 0.02, upside: 0.04 },
  moderate:     { worst:  0.00, main: 0.04, upside: 0.07 },
  growth:       { worst:  0.01, main: 0.06, upside: 0.10 },
};

// ---- Standard deviation for Monte Carlo sampling ----
export const RETURN_STDDEV: Record<ReturnMode, number> = {
  conservative: 0.08,
  moderate:     0.15,
  growth:       0.20,
};

// ---- Annual expense inflation per scenario ----
export const EXPENSE_INFLATION: Record<ScenarioType, number> = {
  worst:  0.020,
  main:   0.015,
  upside: 0.015,
};

// ---- Annual income growth (applied until retirementAge) ----
export const INCOME_GROWTH: Record<ScenarioType, number> = {
  worst:  0.005,
  main:   0.015,
  upside: 0.020,
};

// ---- Take-home rate (gross income → disposable income) ----
// MVP: flat 75% to approximate social insurance + income tax
export const TAKE_HOME_RATE = 0.75;

// ---- Pension defaults ----
export const PENSION_START_AGE = 65;
export const DEFAULT_PENSION_MONTHLY: Record<FamilyType, number> = {
  single:               10, // 万円/月
  couple:               17,
  family_with_children: 17,
};

// ---- Education cost totals (万円, birth to age 22) ----
export const EDUCATION_TOTAL_COST: Record<EducationPath, number> = {
  public:      600,
  private_all: 1800,
  private_uni: 1100,
};
export const EDUCATION_DURATION_YEARS = 22;

// ---- Education cost per stage (万円, total for that stage) ----
// Based on MEXT surveys (simplified)
export const EDUCATION_STAGE_COST: Record<EducationStage, Record<StageType, number>> = {
  elementary:  { public: 193,  private: 959  }, // 6 years
  middle:      { public: 146,  private: 422  }, // 3 years
  high:        { public: 137,  private: 290  }, // 3 years
  university:  { public: 243,  private: 400  }, // 4 years (tuition + living)
};

/** Duration of each education stage in years */
export const EDUCATION_STAGE_YEARS: Record<EducationStage, number> = {
  elementary: 6,
  middle:     3,
  high:       3,
  university: 4,
};

/** Age at which each stage starts */
export const EDUCATION_STAGE_START_AGE: Record<EducationStage, number> = {
  elementary: 6,
  middle:     12,
  high:       15,
  university: 18,
};

// ---- Education presets (convenience shortcuts) ----
export const EDUCATION_PRESETS = {
  all_public:        { elementary: 'public',  middle: 'public',  high: 'public',  university: 'public'  },
  private_from_middle: { elementary: 'public',  middle: 'private', high: 'private', university: 'private' },
  private_from_high: { elementary: 'public',  middle: 'public',  high: 'private', university: 'private' },
  private_uni_only:  { elementary: 'public',  middle: 'public',  high: 'public',  university: 'private' },
  all_private:       { elementary: 'private', middle: 'private', high: 'private', university: 'private' },
} as const;

// ---- Care type presets ----
export const CARE_TYPE_PRESETS: Record<Exclude<CareType, 'custom'>, {
  annualCost: number;
  durationYears: number;
  label: string;
  description: string;
  monthlyEstimate: string;
}> = {
  home: {
    annualCost: 60,
    durationYears: 5,
    label: '在宅介護（家族中心）',
    description: '自己負担中心。訪問介護を最小限利用。',
    monthlyEstimate: '約5万円/月',
  },
  visiting: {
    annualCost: 120,
    durationYears: 5,
    label: '訪問介護サービス利用',
    description: '週数回のヘルパー利用。デイサービスも活用。',
    monthlyEstimate: '約10万円/月',
  },
  facility: {
    annualCost: 300,
    durationYears: 5,
    label: '施設入所',
    description: '特養・有料老人ホーム等。地域差が大きい。',
    monthlyEstimate: '約25万円/月',
  },
};

// ---- Default care costs ----
export const DEFAULT_CARE_ANNUAL_COST  = 120; // 万円/年
export const DEFAULT_CARE_DURATION     = 5;   // years

// ---- Monte Carlo ----
export const MONTE_CARLO_RUNS = 1000;

// ---- Milestone ages for balance snapshot ----
export const MILESTONE_AGES = [80, 85, 90, 95] as const;

// ---- UI display factor for surplus candidate (applied on the UI side only) ----
export const SURPLUS_DISPLAY_FACTOR = 0.8;
