'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import Button from '@/components/ui/Button';
import Card from '@/components/ui/Card';
import Disclaimer from '@/components/ui/Disclaimer';
import ScenarioCard from '@/components/results/ScenarioCard';
import SpendableMarginCard from '@/components/results/SpendableMarginCard';
import ReverseSimulationPanel from '@/components/results/ReverseSimulationPanel';
import AssetProjectionChart from '@/components/results/AssetProjectionChart';
import MonteCarloSummary from '@/components/results/MonteCarloSummary';
import ScenarioAssumptions from '@/components/results/ScenarioAssumptions';
import ResultInsight from '@/components/results/ResultInsight';
import { useSimulation } from '@/context/SimulationContext';
import type { ScenarioType } from '@/lib/simulation/types';
import type { EventMarker } from '@/components/results/AssetProjectionChart';
import { SCENARIO_LABELS } from '@/lib/utils/uiConstants';

const SCENARIO_ORDER: ScenarioType[] = ['worst', 'main', 'upside'];

export default function ResultsPage() {
  const router = useRouter();
  const { state, runSimulation } = useSimulation();
  const { results, monteCarlo, input, isDirty, spendableMargins } = state;

  const [activeScenario, setActiveScenario] = useState<ScenarioType>('main');

  useEffect(() => {
    if (!results || isDirty) runSimulation();
  }, []); // eslint-disable-line react-hooks/exhaustive-deps

  if (!results || !monteCarlo) {
    return (
      <AppShell>
        <div className="flex flex-col items-center justify-center min-h-64 gap-4">
          <div className="w-8 h-8 border-4 border-brand-600 border-t-transparent rounded-full animate-spin" />
          <p className="text-gray-500 text-sm">計算中...</p>
        </div>
      </AppShell>
    );
  }

  const activeResult            = results[activeScenario];
  const activeMargins           = spendableMargins?.[activeScenario] ?? [];
  const activeSustainableBudget = state.sustainableBudgets?.[activeScenario];
  const allBudgets              = state.sustainableBudgets;

  // Build event markers for the chart (excluding retirement which is already shown)
  const eventMarkers: EventMarker[] = [];
  if (input.housing?.type === 'purchase') {
    eventMarkers.push({ age: input.age + input.housing.yearsFromNow, label: '住宅' });
  }
  if (input.car) {
    eventMarkers.push({ age: input.age + input.car.yearsFromNow, label: '車' });
  }
  if (input.care) {
    eventMarkers.push({ age: input.age + input.care.yearsFromNow, label: '介護' });
  }
  if (input.retirementAge < 65) {
    eventMarkers.push({ age: 65, label: '年金' });
  }
  for (const child of input.children) {
    const uniAge = input.age + child.birthYearOffset + 18;
    if (uniAge > input.age && uniAge <= input.expectedLifespan) {
      eventMarkers.push({ age: uniAge, label: '大学' });
    }
  }

  return (
    <AppShell>
      <PageHeader
        title="シミュレーション結果"
        backHref="/input"
        subtitle={`${input.age}歳・退職${input.retirementAge}歳・寿命${input.expectedLifespan}歳の前提`}
      />

      {/* ① 【最重要】今いくら使えるか — 3シナリオ比較サマリー */}
      {allBudgets && (
        <Card className="mb-4">
          <p className="text-xs font-semibold text-gray-500 mb-1 uppercase tracking-wide">
            最重要：今いくら使えるか
          </p>
          <p className="text-xs text-gray-400 mb-3 leading-relaxed">
            {input.expectedLifespan}歳で資産をちょうど使い切る前提で、
            現在の支出からの追加使用可能額を試算しています
          </p>
          <div className="space-y-2">
            {SCENARIO_ORDER.map(s => {
              const b = allBudgets[s];
              const isActive = s === activeScenario;
              const isPlus   = b.differenceMonthly >= 0;
              return (
                <button
                  key={s}
                  type="button"
                  onClick={() => setActiveScenario(s)}
                  className={`w-full rounded-xl border px-3 py-3 text-left transition-all ${
                    isActive
                      ? s === 'worst'  ? 'bg-red-50 border-red-300'
                      : s === 'upside' ? 'bg-green-50 border-green-300'
                      :                  'bg-brand-50 border-brand-300'
                      : 'bg-white border-gray-100 hover:border-gray-200'
                  }`}
                >
                  <div className="flex items-center justify-between">
                    <div>
                      <p className={`text-xs font-bold ${
                        isActive
                          ? s === 'worst'  ? 'text-red-700'
                          : s === 'upside' ? 'text-green-700'
                          :                  'text-brand-700'
                          : 'text-gray-600'
                      }`}>
                        {SCENARIO_LABELS[s]}
                      </p>
                      <p className="text-[10px] text-gray-400 mt-0.5">
                        {isPlus
                          ? `現在より月${b.differenceMonthly.toLocaleString('ja-JP')}万円多く使えます`
                          : `月${Math.abs(b.differenceMonthly).toLocaleString('ja-JP')}万円の見直しが必要です`}
                      </p>
                    </div>
                    <div className="text-right">
                      <p className={`text-xl font-black ${isPlus ? 'text-green-700' : 'text-red-600'}`}>
                        {isPlus ? '+' : '-'}{Math.abs(b.differenceMonthly).toLocaleString('ja-JP')}
                        <span className="text-xs font-semibold ml-0.5">万円/月</span>
                      </p>
                      <p className="text-[10px] text-gray-400">
                        合計 月{b.sustainableMonthly.toLocaleString('ja-JP')}万円
                      </p>
                    </div>
                  </div>
                </button>
              );
            })}
          </div>
          <p className="text-[10px] text-gray-400 mt-2 leading-relaxed">
            ※ 現在の支出（月{Math.round((input.annualEssentialExpenses + input.annualComfortExpenses) / 12 * 10) / 10}万円）との差額。
            逆算シミュレーションの詳細は下段をご確認ください。
          </p>
        </Card>
      )}

      {/* ② 通常シミュレーション — ヘッダー */}
      <div className="flex items-center gap-3 mb-3">
        <div className="flex-1 border-t border-gray-200" />
        <p className="text-xs text-gray-400 font-medium whitespace-nowrap">通常シミュレーション</p>
        <div className="flex-1 border-t border-gray-200" />
      </div>
      <p className="text-xs text-gray-400 mb-3 text-center">
        現在の支出設定をそのまま継続した場合の将来資産見込み
      </p>

      {/* Scenario tabs */}
      <ScenarioAssumptions activeScenario={activeScenario} />
      <div className="flex gap-2 mb-4">
        {SCENARIO_ORDER.map(s => (
          <button
            key={s}
            type="button"
            onClick={() => setActiveScenario(s)}
            className={[
              'flex-1 py-2 rounded-xl text-sm font-semibold transition-colors border',
              activeScenario === s
                ? s === 'worst'  ? 'bg-red-50 border-red-300 text-red-700'
                : s === 'upside' ? 'bg-green-50 border-green-300 text-green-700'
                :                  'bg-brand-50 border-brand-300 text-brand-700'
                : 'bg-white border-gray-200 text-gray-500 hover:bg-gray-50',
            ].join(' ')}
          >
            {SCENARIO_LABELS[s]}
          </button>
        ))}
      </div>

      {/* Insights */}
      <ResultInsight
        result={activeResult}
        input={input}
        margins={activeMargins}
        sustainableBudget={activeSustainableBudget}
      />

      {/* Active scenario card */}
      <div className="mb-4">
        <ScenarioCard
          result={activeResult}
          margins={activeMargins}
          isActive
          expectedLifespan={input.expectedLifespan}
        />
      </div>

      {/* Spendable margin table */}
      {activeMargins.length > 0 && (
        <SpendableMarginCard
          margins={activeMargins}
          scenarioType={activeScenario}
          expectedLifespan={input.expectedLifespan}
          sustainableBudget={activeSustainableBudget}
          retirementAge={input.retirementAge}
        />
      )}

      {/* Asset projection chart */}
      <Card className="mb-4">
        <AssetProjectionChart results={results} retirementAge={input.retirementAge} eventMarkers={eventMarkers} />
      </Card>

      {/* ③ 逆算シミュレーション — セパレーター付き */}
      <div className="flex items-center gap-3 mb-3">
        <div className="flex-1 border-t border-gray-200" />
        <p className="text-xs text-gray-400 font-medium whitespace-nowrap">逆算シミュレーション</p>
        <div className="flex-1 border-t border-gray-200" />
      </div>
      <ReverseSimulationPanel activeScenario={activeScenario} />

      {/* Monte Carlo */}
      <div className="mb-4">
        <MonteCarloSummary mc={monteCarlo} />
      </div>

      {/* Recalculate prompt */}
      {isDirty && (
        <div className="mb-4">
          <Button fullWidth variant="secondary" onClick={runSimulation}>
            入力が変わりました — 再計算する
          </Button>
        </div>
      )}

      {/* Action buttons */}
      <div className="space-y-3 mb-6">
        <Button fullWidth onClick={() => router.push('/summary')}>
          家族共有サマリーを見る
        </Button>
        <Button fullWidth variant="secondary" onClick={() => router.push('/input/details')}>
          詳細設定（退職金・介護・教育など）を変更する
        </Button>
        <Button fullWidth variant="secondary" onClick={() => router.push('/notes')}>
          話し合いメモを追加する
        </Button>
        <Button fullWidth variant="ghost" onClick={() => router.push('/input')}>
          基本設定を変更する
        </Button>
      </div>

      <Disclaimer />
    </AppShell>
  );
}
