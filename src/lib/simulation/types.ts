// ============================================================
// Core domain types for the life simulation engine
// ============================================================

export type FamilyType = 'single' | 'couple' | 'family_with_children';
export type ReturnMode = 'conservative' | 'moderate' | 'growth';
export type ScenarioType = 'worst' | 'main' | 'upside';
export type LifePeriod = 'employment' | 'gap' | 'pension';
export type EducationPath = 'public' | 'private_all' | 'private_uni';

// ---- Education per-stage model ----

export type EducationStage = 'elementary' | 'middle' | 'high' | 'university';
export type StageType = 'public' | 'private';

export interface EducationConfig {
  elementary: StageType;
  middle:     StageType;
  high:       StageType;
  university: StageType;
}

// ---- Care type presets ----

export type CareType = 'home' | 'visiting' | 'facility' | 'custom';

// ---- Pluggable cost events ----

export interface ChildEvent {
  /** years from now until child is born (0 = already born this year) */
  birthYearOffset: number;
  educationPath: EducationPath;
  /** Optional per-stage config; if present, overrides educationPath */
  educationConfig?: EducationConfig;
}

export interface HousingEvent {
  type: 'rent' | 'purchase';
  yearsFromNow: number;
  /** 万円, one-time cost for purchase */
  cost: number;
  /** 万円/年, for rent type */
  annualRent: number;
}

export interface CareEvent {
  yearsFromNow: number;
  annualCost: number; // 万円/年
  durationYears: number;
  careType?: CareType;
}

export interface CarEvent {
  /** 何年後に購入するか (0 = 今年) */
  yearsFromNow: number;
  /** 購入費用（万円）— 諸費用込みの総額 */
  cost: number;
  /** 何年おきに買い替えるか。未設定なら一度きり */
  replacementEveryYears?: number;
}

export interface TemporaryIncome {
  label?: string;           // 例: "養育費受取"
  annualAmount: number;     // 万円/年（手取り額）
  yearsFromNow: number;     // 何年後から受取開始
  durationYears: number;    // 何年間受け取るか
}

// ---- Reverse simulation ----

export interface ReverseSimulationInput {
  targetAge: number;       // age at which user wants to maintain target balance
  targetBalance: number;   // desired balance at targetAge (万円)
}

export interface ReverseSimulationResult {
  scenarioType: ScenarioType;
  extraAnnualSpend: number;   // additional 万円/year the user could spend
  extraMonthlySpend: number;  // extraAnnualSpend / 12
  targetAge: number;
  targetBalance: number;
}

// ---- Sustainable monthly budget (total, not extra) ----
// Imported from reverseEngine; re-exported here for convenience
export type { SustainableMonthlyBudget } from './reverseEngine';

// ---- Spendable margin at milestone ages ----

export interface SpendableMargin {
  age: number;
  projectedBalance: number;  // 万円 — projected balance at this age
  annualMargin: number;       // 万円/year extra spendable while maintaining 0 balance at this age
  monthlyMargin: number;      // 万円/month
}

// ---- Income breakdown (detailed settings) ----

export interface IncomeBreakdown {
  primary: number;   // 万円/年 — 本人の収入
  spouse?: number;   // 万円/年 — 配偶者の収入
  other?: number;    // 万円/年 — その他の収入（副業・不動産等）
}

// ---- Career events (role demotion / re-employment) ----

export type CareerEventType = 'role_demotion' | 'reemployment';

export interface CareerEvent {
  type: CareerEventType;
  age: number;                 // 発生年齢
  incomeRate: number;          // 現在収入に対する割合 (例: 0.8 = 80%)
  label?: string;              // 表示ラベル (例: "役職定年")
}

// ---- Simulation input ----

export interface BasicInput {
  age: number;                        // 20–80
  familyType: FamilyType;
  financialAssets: number;            // 万円
  annualIncome: number;               // 万円 (gross; engine applies 0.75 take-home factor)
  annualEssentialExpenses: number;    // 万円 — minimum living costs
  annualComfortExpenses: number;      // 万円 — discretionary / leisure costs
  retirementAge: number;              // 50–75
  expectedLifespan: number;           // 70–100
  returnMode: ReturnMode;
  /** 退職金 (万円) — one-time income at retirement */
  retirementBonus?: number;
}

export interface ScenarioInput extends BasicInput {
  children: ChildEvent[];
  housing?: HousingEvent;
  care?: CareEvent;
  /** 車の購入（一度きり or 定期買い替え） */
  car?: CarEvent;
  /** 時限的収入（養育費・副業・賃貸収入など） */
  temporaryIncomes?: TemporaryIncome[];
  /** 万円/月, overrides default pension estimate if provided */
  pensionMonthly?: number;
  /** Optional reverse simulation target */
  reverseTarget?: ReverseSimulationInput;
  /** 退職後の支出 (指定時は退職後に切り替え) */
  postRetirementEssentialExpenses?: number;  // 万円/年
  postRetirementComfortExpenses?: number;    // 万円/年
  /** 収入内訳 (詳細設定) */
  incomeBreakdown?: IncomeBreakdown;
  /** キャリアイベント (役職定年・再雇用) */
  careerEvents?: CareerEvent[];
}

// ---- Year-by-year cash flow row ----

export interface AnnualCashFlow {
  year: number;
  age: number;
  period: LifePeriod;
  income: number;               // after take-home factor
  essentialExpenses: number;
  comfortExpenses: number;      // included in all scenarios
  eventCosts: number;
  netFlow: number;              // income - essentialExpenses - comfortExpenses - eventCosts
  assets: number;               // cumulative assets at end of year
}

// ---- Milestone snapshot ----

export interface MilestoneBalances {
  age80: number | null; // null = assets depleted before that age
  age85: number | null;
  age90: number | null;
  age95: number | null;
}

// ---- Per-scenario output ----

export interface ScenarioResult {
  scenarioType: ScenarioType;
  requiredAssets: number;    // 万円 — PV of post-retirement net deficits
  projectedAssets: number;   // 万円 — assets at retirementAge (overwritten by MC percentile)
  gapValue: number;          // projectedAssets - requiredAssets (raw, no discount)
  cashFlowTimeline: AnnualCashFlow[];
  ageAtDepletion: number | null;
  milestoneBalances: MilestoneBalances;
  /** Spendable margins at milestone ages (how much extra can be spent) */
  spendableMargins?: SpendableMargin[];
  /** Sustainable monthly budget: total spending that results in balance=0 at expectedLifespan */
  sustainableMonthlyBudget?: import('./reverseEngine').SustainableMonthlyBudget;
}

export type SimulationResults = Record<ScenarioType, ScenarioResult>;

// ---- Monte Carlo output ----

export interface AgeDepletionEntry {
  age: number;
  count: number;
}

export interface MonteCarloOutput {
  trials: number;
  successRate: number; // fraction of trials where assets last to expectedLifespan
  percentiles: {
    p10: number;
    p25: number;
    p50: number;
    p75: number;
    p90: number;
  };
  /** histogram: how many trials depleted assets at each age */
  ageAtDepletionDistribution: AgeDepletionEntry[];
}

// ---- Notes ----

export type NoteCategory = 'education' | 'care' | 'housing' | 'support' | 'dreams';

export interface FamilyNote {
  id: string;
  category: NoteCategory;
  content: string;
  createdAt: string;
  updatedAt: string;
  /** Whether to include this note in the family summary (default: true) */
  includeInSummary?: boolean;
}

// ---- Shared summary payload ----

export interface SharedSummary {
  shareId: string;
  input: ScenarioInput;
  results: SimulationResults;
  monteCarlo: MonteCarloOutput;
  notes: FamilyNote[];
  createdAt: string;
  expiresAt: string;
}
