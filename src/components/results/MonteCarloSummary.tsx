'use client';

import { useState } from 'react';
import type { MonteCarloOutput } from '@/lib/simulation/types';
import { formatManYen, formatPercent } from '@/lib/utils/formatCurrency';
import Card from '@/components/ui/Card';

interface MonteCarloSummaryProps {
  mc: MonteCarloOutput;
}

const PERCENTILE_LABELS = [
  { key: 'p10' as const, label: 'P10',  note: 'かなり厳しめのケース', color: 'text-red-600' },
  { key: 'p25' as const, label: 'P25',  note: 'やや厳しめ',           color: 'text-orange-500' },
  { key: 'p50' as const, label: 'P50',  note: '中央値',               color: 'text-gray-700' },
  { key: 'p75' as const, label: 'P75',  note: 'やや良いケース',       color: 'text-blue-600' },
  { key: 'p90' as const, label: 'P90',  note: 'かなり良いケース',     color: 'text-green-600' },
];

export default function MonteCarloSummary({ mc }: MonteCarloSummaryProps) {
  const [isOpen, setIsOpen] = useState(false);
  const successPct = mc.successRate * 100;

  const barColor =
    successPct >= 70 ? 'bg-surplus' :
    successPct >= 40 ? 'bg-amber-400' :
                       'bg-shortfall';

  const successColor =
    successPct >= 70 ? 'text-surplus' :
    successPct >= 40 ? 'text-amber-500' :
                       'text-shortfall';

  return (
    <Card>
      <div className="space-y-3">
        {/* Header with β badge */}
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <p className="text-sm font-semibold text-gray-700">運用ブレを加味した参考試算</p>
            <span className="text-[10px] bg-gray-100 text-gray-500 px-1.5 py-0.5 rounded font-medium">β</span>
          </div>
          <span className="text-xs text-gray-400">{mc.trials.toLocaleString('ja-JP')}回</span>
        </div>

        {/* Success rate bar */}
        <div>
          <div className="flex items-end justify-between mb-1">
            <p className="text-xs text-gray-600 font-medium">想定寿命まで資産が持続する可能性</p>
            <p className={`text-2xl font-black ${successColor}`}>
              {formatPercent(mc.successRate)}
            </p>
          </div>
          <div className="w-full bg-gray-100 rounded-full h-3 mb-1.5">
            <div
              className={`h-3 rounded-full transition-all ${barColor}`}
              style={{ width: `${Math.min(successPct, 100)}%` }}
            />
          </div>
          <p className="text-xs text-gray-400 leading-relaxed">
            1,000回の試算のうち、約{formatPercent(mc.successRate)}が想定寿命まで資産を維持した結果です。
            将来の実際の結果を保証するものではありません。
          </p>
        </div>

        {/* Expandable details */}
        <button
          type="button"
          onClick={() => setIsOpen(v => !v)}
          className="flex items-center gap-1 text-xs text-brand-600 hover:text-brand-800"
        >
          <svg
            className={`w-4 h-4 transition-transform ${isOpen ? 'rotate-180' : ''}`}
            fill="none" stroke="currentColor" viewBox="0 0 24 24"
          >
            <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M19 9l-7 7-7-7" />
          </svg>
          {isOpen ? '詳細を閉じる' : '試算の詳細を見る（パーセンタイル）'}
        </button>

        {isOpen && (
          <div className="space-y-3 pt-1">
            {/* What is Monte Carlo */}
            <div className="bg-gray-50 rounded-xl p-3">
              <p className="text-xs font-semibold text-gray-600 mb-1">この試算について</p>
              <ul className="text-xs text-gray-500 space-y-1 leading-relaxed list-disc list-inside">
                <li>将来の運用リターンを毎年ランダムに変化させて1,000回試算</li>
                <li>支出・収入・年金・イベント費用は標準前提で固定</li>
                <li>変動するのは運用リターンのみ（対数正規分布）</li>
                <li>利回りの選択（保守的/標準/積極的）でブレ幅が変わります</li>
              </ul>
            </div>

            {/* Percentile table */}
            <div>
              <p className="text-xs text-gray-500 mb-2">想定寿命時点の資産残高の分布</p>
              <div className="space-y-1.5">
                {PERCENTILE_LABELS.map(col => (
                  <div key={col.key} className="flex items-center justify-between bg-gray-50 rounded-lg px-3 py-2">
                    <div className="flex items-center gap-2">
                      <span className="text-[10px] text-gray-400 font-mono w-7">{col.label}</span>
                      <span className="text-xs text-gray-600">{col.note}</span>
                    </div>
                    <span className={`text-xs font-bold ${mc.percentiles[col.key] < 0 ? 'text-red-500' : col.color}`}>
                      {mc.percentiles[col.key] < 0
                        ? `−${formatManYen(Math.abs(mc.percentiles[col.key]))}`
                        : formatManYen(mc.percentiles[col.key])}
                    </span>
                  </div>
                ))}
              </div>
              <p className="text-xs text-gray-400 mt-2">
                ※ P10とは「1,000回中900回はこれより良い結果だった」を意味します
              </p>
            </div>
          </div>
        )}
      </div>
    </Card>
  );
}
