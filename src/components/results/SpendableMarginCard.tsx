'use client';

import type { SpendableMargin, ScenarioType } from '@/lib/simulation/types';
import type { SustainableMonthlyBudget } from '@/lib/simulation/reverseEngine';
import { formatManYen } from '@/lib/utils/formatCurrency';
import Card from '@/components/ui/Card';

interface SpendableMarginCardProps {
  margins: SpendableMargin[];
  scenarioType: ScenarioType;
  expectedLifespan?: number;
  sustainableBudget?: SustainableMonthlyBudget;
  retirementAge?: number;
}

export default function SpendableMarginCard({
  margins,
  scenarioType,
  expectedLifespan,
  sustainableBudget,
  retirementAge,
}: SpendableMarginCardProps) {
  if (!margins || margins.length === 0) return null;

  const targetAge = expectedLifespan ?? 90;

  return (
    <Card className="mb-4">
      {/* ① 今から毎月いくら使えるか（最重要 — 逆算と同じ数値） */}
      {sustainableBudget && (
        <div className={`rounded-xl p-4 mb-4 ${
          sustainableBudget.differenceMonthly >= 0
            ? 'bg-green-50 border border-green-200'
            : 'bg-red-50 border border-red-200'
        }`}>
          <p className="text-xs text-gray-500 mb-0.5">
            今から{targetAge}歳で使い切る前提での月間使用可能額
          </p>
          <p className={`text-2xl font-black mb-1 ${
            sustainableBudget.differenceMonthly >= 0 ? 'text-green-700' : 'text-red-700'
          }`}>
            月{sustainableBudget.sustainableMonthly.toLocaleString('ja-JP')}万円
          </p>
          {sustainableBudget.differenceMonthly >= 0 ? (
            <p className="text-xs text-green-700">
              現在の支出より月{sustainableBudget.differenceMonthly.toLocaleString('ja-JP')}万円
              <span className="font-bold">多く使える</span>余裕があります
            </p>
          ) : (
            <p className="text-xs text-red-700">
              月{Math.abs(sustainableBudget.differenceMonthly).toLocaleString('ja-JP')}万円
              <span className="font-bold">節約が必要</span>な見込みです
            </p>
          )}
        </div>
      )}

      {/* ② 退職後・年齢別の資産残高 ＋ 追加支出可能額 */}
      <p className="text-xs font-semibold text-gray-500 mb-2">
        退職後の年齢別 資産残高・追加支出可能額
      </p>
      <div className="overflow-x-auto">
        <table className="w-full text-xs">
          <thead>
            <tr className="border-b border-gray-100">
              <th className="py-1.5 text-left font-medium text-gray-400">年齢</th>
              <th className="py-1.5 text-right font-medium text-gray-400">資産残高</th>
              <th className="py-1.5 text-right font-medium text-gray-400">
                追加支出可能額
                <span className="block text-[9px] font-normal text-gray-300">（月額・目安）</span>
              </th>
            </tr>
          </thead>
          <tbody>
            {margins.map(m => (
              <tr
                key={m.age}
                className={`border-b border-gray-50 ${m.age === targetAge ? 'bg-brand-50' : ''}`}
              >
                <td className="py-2 text-gray-700 font-medium">
                  {m.age}歳
                  {m.age === targetAge && (
                    <span className="ml-1 text-[10px] text-brand-500 font-normal">想定寿命</span>
                  )}
                </td>
                <td className="py-2 text-right">
                  {m.projectedBalance >= 0 ? (
                    <span className="text-gray-700 font-medium">{formatManYen(m.projectedBalance)}</span>
                  ) : (
                    <span className="text-red-500 font-bold">枯渇</span>
                  )}
                </td>
                <td className="py-2 text-right">
                  {m.projectedBalance < 0 ? (
                    <span className="text-red-400 text-[10px]">—</span>
                  ) : m.monthlyMargin > 0 ? (
                    <span className="text-green-600 font-semibold">
                      +{m.monthlyMargin.toLocaleString('ja-JP')}万円
                    </span>
                  ) : m.monthlyMargin < 0 ? (
                    <span className="text-red-500 font-semibold">
                      {m.monthlyMargin.toLocaleString('ja-JP')}万円
                    </span>
                  ) : (
                    <span className="text-gray-400">0万円</span>
                  )}
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>

      <p className="text-xs text-gray-400 mt-3 leading-relaxed">
        ※ 「資産残高」は現在の支出設定のまま継続した場合の試算値です。
        「追加支出可能額」は安全余裕（80%）を加味した目安であり、退職後の追加支出を想定しています。
        実際の運用成績・物価変動・制度変更により変わります。
      </p>
    </Card>
  );
}
