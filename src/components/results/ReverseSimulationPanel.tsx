'use client';

import { useState } from 'react';
import Card from '@/components/ui/Card';
import { useSimulation } from '@/context/SimulationContext';
import { SCENARIO_LABELS } from '@/lib/utils/uiConstants';
import { DEFAULT_PENSION_MONTHLY } from '@/lib/simulation/constants';
import {
  computeTargetedSustainableBudget,
  type SustainableMonthlyBudget,
  type TargetedSustainableBudget,
} from '@/lib/simulation/reverseEngine';
import type { ScenarioType } from '@/lib/simulation/types';

interface CustomResultEntry {
  scenarioType: ScenarioType;
  budget: TargetedSustainableBudget;
}

interface ReverseSimulationPanelProps {
  activeScenario: ScenarioType;
}

export default function ReverseSimulationPanel({ activeScenario }: ReverseSimulationPanelProps) {
  const { state } = useSimulation();
  const { input, sustainableBudgets } = state;

  const [isOpen, setIsOpen] = useState(false);
  const [showCustom, setShowCustom] = useState(false);
  const [customTargetAge, setCustomTargetAge] = useState<number>(input?.expectedLifespan ?? 90);
  const [customTargetBalance, setCustomTargetBalance] = useState<number>(0);
  const [customResults, setCustomResults] = useState<CustomResultEntry[] | null>(null);
  const [isComputing, setIsComputing] = useState(false);

  if (!input || !sustainableBudgets) return null;

  const activeBudget = sustainableBudgets[activeScenario];
  const currentMonthly = Math.round((input.annualEssentialExpenses + input.annualComfortExpenses) / 12 * 10) / 10;
  const pensionMonthly = input.pensionMonthly ?? DEFAULT_PENSION_MONTHLY[input.familyType];

  // Effective post-retirement annual expenses (for display)
  const postRetAnnual = input.postRetirementEssentialExpenses != null
    ? (input.postRetirementEssentialExpenses + (input.postRetirementComfortExpenses ?? 0))
    : null;

  function runCustomSimulation() {
    if (!input) return;
    setIsComputing(true);
    const clampedAge = Math.min(customTargetAge, input.expectedLifespan);
    const entries: CustomResultEntry[] = (['worst', 'main', 'upside'] as const).map(s => ({
      scenarioType: s,
      budget: computeTargetedSustainableBudget(input, s, clampedAge, customTargetBalance),
    }));
    setCustomResults(entries);
    setIsComputing(false);
  }

  return (
    <Card className="mb-4">
      {/* Accordion header */}
      <button
        type="button"
        onClick={() => setIsOpen(!isOpen)}
        className="w-full flex items-center justify-between"
      >
        <div className="flex items-center gap-2">
          <span className="text-lg">🔄</span>
          <div className="text-left">
            <p className="text-sm font-semibold text-gray-700">逆算シミュレーション</p>
            <p className="text-xs text-gray-400">今いくら使えるか？ 想定寿命から逆算</p>
          </div>
        </div>
        <svg
          className={`w-4 h-4 text-gray-400 transition-transform ${isOpen ? 'rotate-180' : ''}`}
          fill="none" stroke="currentColor" viewBox="0 0 24 24"
        >
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
        </svg>
      </button>

      {isOpen && (
        <div className="mt-4 space-y-4">

          {/* ① 通常シミュレーションとの違いを明示 */}
          <div className="bg-blue-50 border border-blue-200 rounded-xl px-3 py-2.5">
            <p className="text-xs font-semibold text-blue-800 mb-1">
              📌 通常シミュレーションとは別の計算です
            </p>
            <div className="text-xs text-blue-700 leading-relaxed space-y-1">
              <p>
                <strong>通常シミュレーション</strong>：現在の支出設定のままだと将来の資産はどうなるか
              </p>
              <p>
                <strong>逆算シミュレーション</strong>：目標年齢・残高を達成するために今いくら使えるか
              </p>
              <p className="text-blue-500 mt-1">
                ※ 前提・結果は異なるため、両者を直接比較しないでください
              </p>
            </div>
          </div>

          {/* ② 試算の前提 */}
          <div className="bg-gray-50 rounded-xl px-3 py-2.5">
            <p className="text-xs text-gray-500 mb-2 font-medium">試算の前提</p>
            <div className="grid grid-cols-2 gap-x-4 gap-y-1 text-xs">
              <div className="flex justify-between">
                <span className="text-gray-400">現在の年齢</span>
                <span className="text-gray-700 font-medium">{input.age}歳</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">退職予定年齢</span>
                <span className="text-gray-700 font-medium">{input.retirementAge}歳</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">年金（月額）</span>
                <span className="text-gray-700 font-medium">{pensionMonthly}万円</span>
              </div>
              <div className="flex justify-between">
                <span className="text-gray-400">想定寿命</span>
                <span className="text-gray-700 font-medium">{input.expectedLifespan}歳</span>
              </div>
              {(input.retirementBonus ?? 0) > 0 && (
                <div className="flex justify-between">
                  <span className="text-gray-400">退職金</span>
                  <span className="text-gray-700 font-medium">{input.retirementBonus?.toLocaleString('ja-JP')}万円</span>
                </div>
              )}
              <div className="flex justify-between">
                <span className="text-gray-400">現在の支出</span>
                <span className="text-gray-700 font-medium">月{currentMonthly}万円</span>
              </div>
              {postRetAnnual != null && (
                <div className="flex justify-between col-span-2">
                  <span className="text-gray-400">退職後支出（設定値）</span>
                  <span className="text-gray-700 font-medium">
                    年{postRetAnnual.toLocaleString('ja-JP')}万円（月{Math.round(postRetAnnual / 12 * 10) / 10}万円）
                  </span>
                </div>
              )}
            </div>
          </div>

          {/* ③ 自動試算：想定寿命でゼロになる場合の追加使用可能額（ヒーロー） */}
          <div>
            <p className="text-xs font-semibold text-gray-500 mb-2">
              {input.expectedLifespan}歳でちょうど使い切る前提での追加使用可能額
            </p>
            <SustainableBudgetHero
              budget={activeBudget}
              scenarioType={activeScenario}
              currentMonthly={currentMonthly}
              expectedLifespan={input.expectedLifespan}
            />
          </div>

          {/* ④ 3シナリオ比較（自動試算） */}
          <div className="bg-gray-50 rounded-xl px-3 py-3">
            <p className="text-xs text-gray-500 mb-2.5 font-medium">
              3シナリオ比較（{input.expectedLifespan}歳でゼロ前提）
            </p>
            <div className="space-y-2">
              {(['worst', 'main', 'upside'] as const).map(s => {
                const b = sustainableBudgets[s];
                const isActive = s === activeScenario;
                const isPlus   = b.differenceMonthly >= 0;
                return (
                  <div
                    key={s}
                    className={`rounded-xl px-3 py-2.5 ${
                      isActive ? 'bg-white border border-brand-200' : 'bg-white border border-gray-100'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <span className={`text-xs font-bold ${isActive ? 'text-brand-700' : 'text-gray-600'}`}>
                        {SCENARIO_LABELS[s]}
                      </span>
                      <div className="text-right">
                        <p className={`text-sm font-black ${isPlus ? 'text-green-700' : 'text-red-600'}`}>
                          {isPlus ? '+' : '-'} 月{Math.abs(b.differenceMonthly).toLocaleString('ja-JP')}万円
                        </p>
                        <p className="text-[10px] text-gray-400">
                          合計 月{b.sustainableMonthly.toLocaleString('ja-JP')}万円
                        </p>
                      </div>
                    </div>
                    <p className={`text-[10px] mt-0.5 ${isPlus ? 'text-green-600' : 'text-red-500'}`}>
                      {isPlus
                        ? `現在の支出より月${b.differenceMonthly.toLocaleString('ja-JP')}万円多く使えます`
                        : `月${Math.abs(b.differenceMonthly).toLocaleString('ja-JP')}万円程度の見直しが必要です`}
                    </p>
                  </div>
                );
              })}
            </div>
            <p className="text-[10px] text-gray-400 mt-2">
              ※ 現在の支出（月{currentMonthly}万円）との差額。プラス＝追加利用可能、マイナス＝節約が必要。
            </p>
          </div>

          {/* ⑤ カスタム試算：目標年齢・残高を指定 */}
          <div className="border border-gray-200 rounded-xl overflow-hidden">
            <button
              type="button"
              onClick={() => setShowCustom(!showCustom)}
              className="w-full flex items-center justify-between px-3 py-2.5 bg-gray-50 hover:bg-gray-100 transition-colors"
            >
              <span className="text-xs font-semibold text-gray-600">
                🎯 カスタム試算（目標年齢・残したい資産を指定）
              </span>
              <svg
                className={`w-3.5 h-3.5 text-gray-400 transition-transform ${showCustom ? 'rotate-180' : ''}`}
                fill="none" stroke="currentColor" viewBox="0 0 24 24"
              >
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
              </svg>
            </button>

            {showCustom && (
              <div className="p-3 space-y-3">
                <p className="text-xs text-gray-500">
                  目標年齢と、その時点で残したい資産額を指定して試算します。
                  「ゼロ」にすると使い切る前提になります。
                </p>
                <div className="grid grid-cols-2 gap-3">
                  <div>
                    <label className="text-xs text-gray-500 block mb-1">目標年齢</label>
                    <div className="flex items-center gap-1">
                      <input
                        type="number"
                        value={customTargetAge}
                        min={input.retirementAge + 1}
                        max={input.expectedLifespan}
                        onChange={e => setCustomTargetAge(Number(e.target.value))}
                        className="w-full border border-gray-200 rounded-lg px-2 py-1.5 text-sm text-center focus:outline-none focus:border-brand-400"
                      />
                      <span className="text-xs text-gray-500 flex-shrink-0">歳</span>
                    </div>
                  </div>
                  <div>
                    <label className="text-xs text-gray-500 block mb-1">残したい資産</label>
                    <div className="flex items-center gap-1">
                      <input
                        type="number"
                        value={customTargetBalance}
                        min={0}
                        step={100}
                        onChange={e => setCustomTargetBalance(Number(e.target.value))}
                        className="w-full border border-gray-200 rounded-lg px-2 py-1.5 text-sm text-center focus:outline-none focus:border-brand-400"
                      />
                      <span className="text-xs text-gray-500 flex-shrink-0">万円</span>
                    </div>
                  </div>
                </div>

                <button
                  type="button"
                  onClick={runCustomSimulation}
                  disabled={isComputing}
                  className="w-full bg-brand-600 text-white text-sm font-semibold py-2.5 rounded-xl disabled:opacity-50"
                >
                  {isComputing ? '計算中...' : '試算する'}
                </button>

                {customResults && (
                  <div className="space-y-2 pt-1">
                    <p className="text-xs font-semibold text-gray-600">
                      {customResults[0].budget.targetAge}歳時点で
                      {customTargetBalance > 0
                        ? `${customTargetBalance.toLocaleString('ja-JP')}万円を残す`
                        : '資産をちょうど使い切る'}
                      前提
                    </p>
                    {customResults.map(({ scenarioType, budget: r }) => {
                      const isPlus = r.differenceMonthly >= 0;
                      return (
                        <div
                          key={scenarioType}
                          className={`rounded-xl border px-3 py-2.5 ${
                            isPlus ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
                          }`}
                        >
                          <div className="flex items-center justify-between mb-1">
                            <span className="text-xs font-bold text-gray-700">
                              {SCENARIO_LABELS[scenarioType]}
                            </span>
                            <div className="text-right">
                              <p className={`text-sm font-black ${isPlus ? 'text-green-700' : 'text-red-600'}`}>
                                {isPlus ? '+' : '-'} 月{Math.abs(r.differenceMonthly).toLocaleString('ja-JP')}万円
                              </p>
                              <p className="text-[10px] text-gray-400">
                                合計 月{r.sustainableMonthly.toLocaleString('ja-JP')}万円
                              </p>
                            </div>
                          </div>
                          <p className={`text-[10px] ${isPlus ? 'text-green-600' : 'text-red-500'}`}>
                            {isPlus
                              ? `現在の支出より月${r.differenceMonthly.toLocaleString('ja-JP')}万円多く使えます`
                              : `月${Math.abs(r.differenceMonthly).toLocaleString('ja-JP')}万円程度の見直しが必要です`}
                          </p>
                          {/* Forward verification */}
                          <p className="text-[10px] text-gray-400 mt-1 border-t border-gray-200 pt-1">
                            検証：{r.targetAge}歳時点の残高 ≈{' '}
                            {r.verifiedBalance >= 0
                              ? `${r.verifiedBalance.toLocaleString('ja-JP')}万円`
                              : '枯渇'}
                            {customTargetBalance > 0 && r.verifiedBalance >= 0 && (
                              <span>（目標: {customTargetBalance.toLocaleString('ja-JP')}万円）</span>
                            )}
                          </p>
                        </div>
                      );
                    })}
                    <p className="text-[10px] text-gray-400 leading-relaxed">
                      ※「検証」は逆算した支出額で再計算した場合の残高です。目標値に近ければ計算は正確です。
                    </p>
                  </div>
                )}
              </div>
            )}
          </div>

          <p className="text-xs text-gray-400 leading-relaxed">
            ※ この試算は一般的な前提に基づく参考値です。将来の実際の結果を保証するものではありません。
            収入増減・制度変更・物価変動により変わる可能性があります。
          </p>
        </div>
      )}
    </Card>
  );
}

// ── ヒーロー表示：差額を主、合計を副 ──
function SustainableBudgetHero({
  budget,
  scenarioType,
  currentMonthly,
  expectedLifespan,
}: {
  budget: SustainableMonthlyBudget;
  scenarioType: ScenarioType;
  currentMonthly: number;
  expectedLifespan: number;
}) {
  const isPlus = budget.differenceMonthly >= 0;

  return (
    <div className={`rounded-2xl p-4 border-2 ${
      isPlus ? 'bg-green-50 border-green-200' : 'bg-red-50 border-red-200'
    }`}>
      <p className="text-xs text-gray-500 mb-1">
        {SCENARIO_LABELS[scenarioType]}シナリオ
        ／ {expectedLifespan}歳でちょうど使い切る前提
      </p>

      {/* 主表示：差額 */}
      <div className={`rounded-xl px-3 py-3 mb-3 ${isPlus ? 'bg-green-100' : 'bg-red-100'}`}>
        <p className={`text-xs font-semibold mb-0.5 ${isPlus ? 'text-green-700' : 'text-red-700'}`}>
          {isPlus ? '現在の支出より毎月あと使える額' : '現在の支出より毎月節約が必要な額'}
        </p>
        <p className={`text-3xl font-black ${isPlus ? 'text-green-700' : 'text-red-700'}`}>
          {isPlus ? '+' : '-'} 月{Math.abs(budget.differenceMonthly).toLocaleString('ja-JP')}万円
        </p>
        {isPlus ? (
          <p className="text-xs text-green-600 mt-1.5 leading-relaxed">
            月{currentMonthly}万円の支出を続けながら、さらに月{budget.differenceMonthly.toLocaleString('ja-JP')}万円使っても
            {expectedLifespan}歳まで資産が持続する見込みです
          </p>
        ) : (
          <p className="text-xs text-red-600 mt-1.5 leading-relaxed">
            {expectedLifespan}歳まで資産を持続させるには、
            現在の支出を月{Math.abs(budget.differenceMonthly).toLocaleString('ja-JP')}万円程度削減することが必要な見込みです
          </p>
        )}
      </div>

      {/* 副表示：合計 */}
      <div className="flex items-center justify-between text-xs text-gray-500">
        <span>使用可能な月間支出（合計）</span>
        <span className={`font-bold text-sm ${isPlus ? 'text-green-700' : 'text-red-700'}`}>
          月{budget.sustainableMonthly.toLocaleString('ja-JP')}万円
        </span>
      </div>
      <div className="flex items-center justify-between text-xs text-gray-400 mt-0.5">
        <span>現在の支出設定</span>
        <span>月{currentMonthly}万円</span>
      </div>
    </div>
  );
}
