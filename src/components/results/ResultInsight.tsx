'use client';

import type { ScenarioResult, ScenarioInput, SpendableMargin } from '@/lib/simulation/types';
import type { SustainableMonthlyBudget } from '@/lib/simulation/reverseEngine';
import { formatManYen } from '@/lib/utils/formatCurrency';

interface ResultInsightProps {
  result: ScenarioResult;
  input: ScenarioInput;
  margins?: SpendableMargin[];
  sustainableBudget?: SustainableMonthlyBudget;
}

interface Insight {
  icon: string;
  text: string;
  type: 'warning' | 'info' | 'positive';
}

function buildInsights(
  result: ScenarioResult,
  input: ScenarioInput,
  margins?: SpendableMargin[],
  sustainableBudget?: SustainableMonthlyBudget,
): Insight[] {
  const insights: Insight[] = [];
  const totalExpenses = input.annualEssentialExpenses + input.annualComfortExpenses;
  const takeHomeIncome = input.annualIncome * 0.75;

  // Priority 1: sustainable monthly budget — "今いくら使えるか"
  if (sustainableBudget) {
    if (sustainableBudget.differenceMonthly > 0) {
      insights.push({
        icon: '💰',
        text: `このシナリオでは、${input.expectedLifespan}歳で使い切る前提で月${sustainableBudget.sustainableMonthly.toLocaleString('ja-JP')}万円まで使えます。現在の支出より月${sustainableBudget.differenceMonthly.toLocaleString('ja-JP')}万円の余裕があります。`,
        type: 'positive',
      });
    } else if (sustainableBudget.differenceMonthly < 0) {
      insights.push({
        icon: '⚠️',
        text: `このシナリオでは${input.expectedLifespan}歳まで資産を維持するには、現在の支出から月${Math.abs(sustainableBudget.differenceMonthly).toLocaleString('ja-JP')}万円程度の節約が必要な見込みです。`,
        type: 'warning',
      });
    }
  }

  // Over-saving warning
  if (result.gapValue > 5000 && result.ageAtDepletion === null &&
      sustainableBudget && sustainableBudget.differenceMonthly > 10) {
    insights.push({
      icon: '📦',
      text: '必要以上にため込んでいる可能性もあります。教育・介護・旅行・住居支援など、家族にとって価値ある使い道を話し合ってみてください。',
      type: 'info',
    });
  }

  // High expense ratio
  if (totalExpenses / takeHomeIncome > 0.85) {
    insights.push({
      icon: '💸',
      text: `手取り収入に対して支出割合が高め（${Math.round(totalExpenses / takeHomeIncome * 100)}%）。支出の見直しで余力が改善する可能性があります。`,
      type: 'warning',
    });
  }

  // Long lifespan
  if (input.expectedLifespan >= 95) {
    insights.push({
      icon: '📅',
      text: `想定寿命を${input.expectedLifespan}歳に設定しているため、長期の資産持続が必要です。慎重シナリオも確認することをお勧めします。`,
      type: 'info',
    });
  }

  // Early retirement gap
  if (input.retirementAge < 65) {
    const gapYears = 65 - input.retirementAge;
    insights.push({
      icon: '⏳',
      text: `退職（${input.retirementAge}歳）から年金開始（65歳）まで${gapYears}年間の収入空白期があります。この期間の支出は資産から賄う前提です。`,
      type: 'warning',
    });
  }

  // Children education costs
  if (input.children.length > 0) {
    const childCount = input.children.length;
    insights.push({
      icon: '🎓',
      text: `子ども${childCount}人分の教育費が計上されています。教育費イベントが重なる時期に資産が大きく減少する可能性があります。`,
      type: 'info',
    });
  }

  // Low assets
  if (input.financialAssets < 100) {
    insights.push({
      icon: '⚠️',
      text: `現在の金融資産が少なめです（${formatManYen(input.financialAssets)}）。積立の開始または増額で将来の見込みが改善する可能性があります。`,
      type: 'warning',
    });
  }

  // Positive: surplus with sustained assets (only when no sustainable budget insight was shown)
  if (result.gapValue > 1000 && result.ageAtDepletion === null && !sustainableBudget) {
    insights.push({
      icon: '✅',
      text: 'この前提では、退職後も資産が持続する見込みです。ただし将来の制度変更・物価変動によって結果は変わる可能性があります。',
      type: 'positive',
    });
  }

  return insights;
}

const insightStyles = {
  warning:  'bg-amber-50 border-amber-100 text-amber-700',
  info:     'bg-blue-50 border-blue-100 text-blue-700',
  positive: 'bg-green-50 border-green-100 text-green-700',
};

export default function ResultInsight({ result, input, margins, sustainableBudget }: ResultInsightProps) {
  const insights = buildInsights(result, input, margins, sustainableBudget);
  if (insights.length === 0) return null;

  return (
    <div className="space-y-2 mb-4">
      <p className="text-xs font-semibold text-gray-500 uppercase tracking-wide">この結果のポイント</p>
      {insights.map((ins, i) => (
        <div key={i} className={`flex items-start gap-2 rounded-xl border px-3 py-2 ${insightStyles[ins.type]}`}>
          <span className="text-base flex-shrink-0 mt-0.5">{ins.icon}</span>
          <p className="text-xs leading-relaxed">{ins.text}</p>
        </div>
      ))}
    </div>
  );
}
