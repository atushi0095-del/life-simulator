import { SURPLUS_DISPLAY_FACTOR } from '@/lib/simulation/constants';

/** Labels for scenario types (慎重/標準/追い風) */
export const SCENARIO_LABELS = {
  worst:  '慎重',
  main:   '標準',
  upside: '追い風',
} as const;

/** One-line description of each scenario */
export const SCENARIO_DESCRIPTIONS = {
  worst:  '運用が伸びず、物価上昇が重めの前提（ゆとり費込み）',
  main:   '標準的な前提',
  upside: '運用が比較的良好な前提',
} as const;

/** Detailed assumptions per scenario (for expandable section) */
export const SCENARIO_ASSUMPTIONS = {
  worst: {
    returnRate:       '−1〜+1%（運用スタイルによる）',
    expenseInflation: '2.0%/年',
    incomeGrowth:     '0.5%/年',
    comfortExpenses:  'ゆとり費込みで計算',
    pension:          '65歳から受給（家族タイプ別標準額）',
  },
  main: {
    returnRate:       '+2〜+6%（運用スタイルによる）',
    expenseInflation: '1.5%/年',
    incomeGrowth:     '1.5%/年',
    comfortExpenses:  'ゆとり費込みで計算',
    pension:          '65歳から受給（家族タイプ別標準額）',
  },
  upside: {
    returnRate:       '+4〜+10%（運用スタイルによる）',
    expenseInflation: '1.5%/年',
    incomeGrowth:     '2.0%/年',
    comfortExpenses:  'ゆとり費込みで計算',
    pension:          '65歳から受給（家族タイプ別標準額）',
  },
} as const;

export const SCENARIO_COLORS = {
  worst:  '#dc2626',
  main:   '#0284c7',
  upside: '#16a34a',
} as const;

export const SCENARIO_BG = {
  worst:  'bg-red-50 border-red-200',
  main:   'bg-blue-50 border-blue-200',
  upside: 'bg-green-50 border-green-200',
} as const;

/** Category labels for notes */
export const NOTE_CATEGORY_LABELS = {
  education: '教育',
  care:      '介護',
  housing:   '住宅',
  support:   '支援',
  dreams:    '夢・旅行',
} as const;

export const NOTE_CATEGORY_EMOJI = {
  education: '🎓',
  care:      '🏥',
  housing:   '🏠',
  support:   '🤝',
  dreams:    '✈️',
} as const;

/** Family type labels */
export const FAMILY_TYPE_LABELS = {
  single:               '独身',
  couple:               '夫婦',
  family_with_children: '子あり',
} as const;

/** Return mode labels */
export const RETURN_MODE_LABELS = {
  conservative: '保守的',
  moderate:     '標準',
  growth:       '積極的',
} as const;

export const RETURN_MODE_DESCRIPTIONS = {
  conservative: '株式は少なく安定重視。預貯金中心。',
  moderate:     '株式・債券をバランスよく運用。',
  growth:       '株式中心で積極的に運用。',
} as const;

/** Surplus display factor (applied on UI side only) */
export { SURPLUS_DISPLAY_FACTOR };
