'use client';

import type { ScenarioResult, SpendableMargin } from '@/lib/simulation/types';
import { SCENARIO_LABELS, SCENARIO_BG, SURPLUS_DISPLAY_FACTOR } from '@/lib/utils/uiConstants';
import { formatManYen, formatGap } from '@/lib/utils/formatCurrency';
import Badge from '@/components/ui/Badge';

interface ScenarioCardProps {
  result: ScenarioResult;
  margins?: SpendableMargin[];
  isActive?: boolean;
  onClick?: () => void;
  expectedLifespan?: number;
}

/** 総合判定ロジック:
 *  - 不足(gapValue < 0)                        → "不足傾向"
 *  - 余力あり、全マイルストーンOK               → "余力候補あり"
 *  - 余力あり、しかし途中で枯渇                 → "一定の余力・長寿リスク注意"
 *  - 大幅余力で全マイルストーンOK               → "ため込みすぎに注意"
 */
function getOverallJudgment(result: ScenarioResult, margins?: SpendableMargin[]): {
  label: string;
  color: string;
  bgColor: string;
  note: string;
} {
  const { gapValue, ageAtDepletion, milestoneBalances } = result;

  if (gapValue < 0) {
    return {
      label:   '不足傾向',
      color:   'text-red-700',
      bgColor: 'bg-red-50',
      note:    `退職時点で約${formatManYen(Math.abs(gapValue))}の不足が見込まれます。支出の見直しや退職時期の再検討で改善の可能性があります。`,
    };
  }

  // Surplus but post-retirement depletion occurs at some point
  if (ageAtDepletion !== null) {
    return {
      label:   '一定の余力あり・長寿リスク注意',
      color:   'text-amber-700',
      bgColor: 'bg-amber-50',
      note:    `退職時は余力がある見込みですが、${ageAtDepletion}歳前後で資産が不足し始める可能性があります。支出ペースには注意が必要です。`,
    };
  }

  // Check milestone depletion
  const hasLateDepletion =
    milestoneBalances.age90 === null || milestoneBalances.age95 === null;

  if (hasLateDepletion) {
    return {
      label:   '一定の余力あり・長期注意',
      color:   'text-amber-700',
      bgColor: 'bg-amber-50',
      note:    '退職時点では余力候補がありますが、長寿ケースでの資産持続に注意が必要です。',
    };
  }

  // Large surplus: check if user might be over-saving
  const heroMargin = margins?.find(m => m.age >= 80 && m.monthlyMargin > 0);
  if (gapValue > 3000 && heroMargin && heroMargin.monthlyMargin > 5) {
    return {
      label:   '余力あり・活用余地あり',
      color:   'text-green-700',
      bgColor: 'bg-green-50',
      note:    `この前提では一定の余力があり、教育・介護・旅行・住居支援などへの活用余地がある可能性があります。ただし必要以上にため込んでいる可能性もあります。`,
    };
  }

  return {
    label:   '概ね維持の見込み',
    color:   'text-green-700',
    bgColor: 'bg-green-50',
    note:    `この前提では、将来への備えを維持しつつ一定の支出余地がある可能性があります。`,
  };
}

export default function ScenarioCard({ result, margins, isActive = false, onClick, expectedLifespan = 100 }: ScenarioCardProps) {
  const {
    scenarioType, requiredAssets, projectedAssets, gapValue, milestoneBalances,
  } = result;

  const judgment       = getOverallJudgment(result, margins);
  const isSurplus      = gapValue >= 0;
  const surplusDisplay = isSurplus ? Math.floor(gapValue * SURPLUS_DISPLAY_FACTOR) : null;

  // Hero margin: pick the first milestone well after retirement with positive margin
  // Margins are already filtered to post-retirement ages only
  const heroMargin = margins?.find(m => m.monthlyMargin > 0);

  return (
    <button
      type="button"
      onClick={onClick}
      className={[
        'w-full text-left rounded-2xl border-2 p-4 transition-all',
        isActive ? SCENARIO_BG[scenarioType] : 'bg-white border-gray-100',
        onClick ? 'cursor-pointer' : 'cursor-default',
      ].join(' ')}
    >
      {/* Header row */}
      <div className="flex items-center justify-between mb-3">
        <Badge variant={scenarioType}>{SCENARIO_LABELS[scenarioType]}</Badge>
      </div>

      {/* ① 総合判定 — primary */}
      <div className={`rounded-xl px-3 py-2 mb-4 ${judgment.bgColor}`}>
        <p className={`text-sm font-bold ${judgment.color}`}>{judgment.label}</p>
        <p className="text-xs text-gray-600 mt-0.5 leading-relaxed">{judgment.note}</p>
      </div>

      {/* ② 追加情報 (heroMargin: 別カードで表示するため省略) */}

      {/* ③ 必要資産 / 退職時資産見込み */}
      <div className="grid grid-cols-2 gap-3 mb-3">
        <div className="bg-white bg-opacity-60 rounded-lg p-2">
          <p className="text-xs text-gray-500">必要資産見込み</p>
          <p className="text-base font-bold text-gray-800">{formatManYen(requiredAssets)}</p>
        </div>
        <div className="bg-white bg-opacity-60 rounded-lg p-2">
          <p className="text-xs text-gray-500">退職時資産見込み</p>
          <p className="text-base font-bold text-gray-800">{formatManYen(projectedAssets)}</p>
        </div>
      </div>

      {/* ④ 不足額または余力候補 */}
      <div className="mb-3">
        <p className="text-xs text-gray-500 mb-0.5">
          {isSurplus ? '退職時 余力候補' : '退職時 不足額'}
        </p>
        <p className={`text-2xl font-black ${isSurplus ? 'text-surplus' : 'text-shortfall'}`}>
          {formatGap(gapValue)}
        </p>
        {isSurplus && surplusDisplay !== null && (
          <p className="text-xs text-gray-400 mt-0.5">
            安全余裕を見た場合：約 {formatManYen(surplusDisplay)}
          </p>
        )}
      </div>

      {/* ⑤ 80/85/90/95歳時点残高 */}
      <div className="pt-3 border-t border-gray-100">
        <p className="text-xs text-gray-500 mb-2">長寿時点の残高見込み</p>
        <div className="grid grid-cols-4 gap-1.5">
          {[
            { age: 80, val: milestoneBalances.age80 },
            { age: 85, val: milestoneBalances.age85 },
            { age: 90, val: milestoneBalances.age90 },
            { age: 95, val: milestoneBalances.age95 },
          ].map(({ age, val }) => (
            <div key={age} className="text-center bg-white bg-opacity-60 rounded-lg py-1.5 px-1">
              <p className="text-[10px] text-gray-400">{age}歳</p>
              <p className={`text-xs font-bold mt-0.5 leading-tight ${
                val === null && age <= expectedLifespan ? 'text-red-500'
                : val === null ? 'text-gray-300'
                : 'text-gray-700'
              }`}>
                {val === null && age <= expectedLifespan ? '枯渇'
                  : val === null ? '—'
                  : formatManYen(val)}
              </p>
            </div>
          ))}
        </div>
        {(milestoneBalances.age90 === null || milestoneBalances.age95 === null) && (
          <p className="text-xs text-amber-600 mt-1.5 leading-relaxed">
            長寿ケースでは資産が不足する可能性があります。余力候補の使いすぎには注意してください。
          </p>
        )}
      </div>

      {/* Non-recommendation note */}
      <p className="mt-3 text-xs text-gray-400 leading-relaxed">
        ※ 特定の運用・投資・贈与を推奨するものではありません。
      </p>
    </button>
  );
}
