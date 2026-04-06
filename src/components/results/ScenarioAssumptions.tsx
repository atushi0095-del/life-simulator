'use client';

import { useState } from 'react';
import type { ScenarioType } from '@/lib/simulation/types';
import { SCENARIO_LABELS, SCENARIO_DESCRIPTIONS, SCENARIO_ASSUMPTIONS } from '@/lib/utils/uiConstants';

interface ScenarioAssumptionsProps {
  activeScenario: ScenarioType;
}

export default function ScenarioAssumptions({ activeScenario }: ScenarioAssumptionsProps) {
  const [isOpen, setIsOpen] = useState(false);
  const assumptions = SCENARIO_ASSUMPTIONS[activeScenario];

  return (
    <div className="bg-white rounded-2xl border border-gray-100 shadow-sm overflow-hidden mb-4">
      <button
        type="button"
        onClick={() => setIsOpen(v => !v)}
        className="w-full flex items-center justify-between px-4 py-3 text-left"
      >
        <div>
          <p className="text-xs text-gray-500 mb-0.5">現在の前提</p>
          <p className="text-sm font-semibold text-gray-700">
            【{SCENARIO_LABELS[activeScenario]}】{SCENARIO_DESCRIPTIONS[activeScenario]}
          </p>
        </div>
        <svg
          className={`w-5 h-5 text-gray-400 flex-shrink-0 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="px-4 pb-4 border-t border-gray-100">
          <p className="text-xs text-gray-500 mt-3 mb-2">このシナリオの主な前提（モデル上の仮定）</p>
          <div className="space-y-2">
            {[
              { label: '想定利回り',     value: assumptions.returnRate },
              { label: '支出インフレ',   value: assumptions.expenseInflation },
              { label: '収入成長',       value: assumptions.incomeGrowth },
              { label: 'ゆとり費の扱い', value: assumptions.comfortExpenses },
              { label: '年金',           value: assumptions.pension },
            ].map(item => (
              <div key={item.label} className="flex items-start gap-3">
                <span className="text-xs text-gray-500 flex-shrink-0 w-24">{item.label}</span>
                <span className="text-xs text-gray-700 font-medium">{item.value}</span>
              </div>
            ))}
          </div>
          <div className="mt-3 bg-amber-50 rounded-lg p-3">
            <p className="text-xs text-amber-700 leading-relaxed">
              これらはモデル上の仮定であり、将来の経済環境・物価・税制・年金制度の変化を保証するものではありません。
              定期的な見直しを推奨します。
            </p>
          </div>
        </div>
      )}
    </div>
  );
}
