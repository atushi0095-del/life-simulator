'use client';

import type {
  SimulationResults, ScenarioInput, MonteCarloOutput, FamilyNote,
  NoteCategory, SpendableMargin, ScenarioType,
} from '@/lib/simulation/types';
import type { SustainableMonthlyBudget } from '@/lib/simulation/reverseEngine';
import {
  SCENARIO_LABELS, FAMILY_TYPE_LABELS, RETURN_MODE_LABELS,
  NOTE_CATEGORY_LABELS, NOTE_CATEGORY_EMOJI,
} from '@/lib/utils/uiConstants';
import { formatManYen, formatPercent } from '@/lib/utils/formatCurrency';

interface FamilySummaryCardProps {
  input: ScenarioInput;
  results: SimulationResults;
  mc: MonteCarloOutput;
  notes?: FamilyNote[];
  spendableMargins?: Record<ScenarioType, SpendableMargin[]> | null;
  sustainableBudgets?: Record<ScenarioType, SustainableMonthlyBudget> | null;
}

const NOTE_CATEGORIES: NoteCategory[] = ['education', 'care', 'housing', 'support', 'dreams'];

// ── ライフイベント年齢マップ ──────────────────────────────
function buildEventMap(input: ScenarioInput): Map<number, string[]> {
  const map = new Map<number, string[]>();
  const add = (age: number, label: string) => {
    if (age < input.age || age > input.expectedLifespan) return;
    const arr = map.get(age) ?? [];
    arr.push(label);
    map.set(age, arr);
  };

  add(input.retirementAge, '退職');
  if (input.retirementAge < 65) add(65, '年金開始');

  if (input.housing?.type === 'purchase') {
    add(input.age + input.housing.yearsFromNow, '住宅購入');
  } else if (input.housing?.type === 'rent') {
    add(input.age + input.housing.yearsFromNow, '賃貸開始');
  }

  if (input.care) {
    add(input.age + input.care.yearsFromNow, '介護費開始');
  }

  if (input.car) {
    add(input.age + input.car.yearsFromNow, '車購入');
  }

  for (const child of input.children) {
    const birthAge = input.age + child.birthYearOffset;
    add(birthAge + 6,  '子:小学校');
    add(birthAge + 12, '子:中学校');
    add(birthAge + 15, '子:高校');
    add(birthAge + 18, '子:大学');
  }

  return map;
}

/**
 * FP スタイルキャッシュフロー計画表 CSV を生成してダウンロード。
 * 列 = 年（経過年数 / 年齢）、行 = 収入・支出・収支・残高の各項目。
 * 金額単位: 収入・支出・収支は月額（年額 ÷ 12）、残高は年末万円。
 */
function downloadCsv(
  input: ScenarioInput,
  results: SimulationResults,
  mc: MonteCarloOutput,
  sustainableBudgets?: Record<ScenarioType, SustainableMonthlyBudget> | null,
  spendableMargins?: Record<ScenarioType, SpendableMargin[]> | null,
) {
  const timeline = results.main.cashFlowTimeline;
  const lines: string[] = [];

  // ── ① 前提情報 ─────────────────────────────────────────
  lines.push('【試算の前提】');
  lines.push(`作成日,${new Date().toLocaleDateString('ja-JP')}`);
  lines.push(`現在の年齢,${input.age}歳`);
  lines.push(`家族タイプ,${FAMILY_TYPE_LABELS[input.familyType]}`);
  lines.push(`現在の金融資産,${input.financialAssets}万円`);
  lines.push(`年収（税込）,${input.annualIncome}万円`);
  lines.push(`必須支出,${input.annualEssentialExpenses}万円/年`);
  lines.push(`ゆとり支出,${input.annualComfortExpenses}万円/年`);
  lines.push(`退職予定年齢,${input.retirementAge}歳`);
  lines.push(`想定寿命,${input.expectedLifespan}歳`);
  lines.push(`運用スタイル,${RETURN_MODE_LABELS[input.returnMode]}`);
  if (input.retirementBonus) lines.push(`退職金,${input.retirementBonus}万円`);
  if (input.postRetirementEssentialExpenses != null) {
    lines.push(`退職後必須支出,${input.postRetirementEssentialExpenses}万円/年`);
  }
  if (input.postRetirementComfortExpenses != null) {
    lines.push(`退職後ゆとり支出,${input.postRetirementComfortExpenses}万円/年`);
  }
  if (input.pensionMonthly != null) lines.push(`年金（月額）,${input.pensionMonthly}万円/月`);
  if (input.care) {
    lines.push(`介護設定,年${input.care.annualCost}万円 / ${input.care.durationYears}年間 / ${input.care.yearsFromNow}年後開始`);
  }
  if (input.children.length > 0) lines.push(`子ども人数,${input.children.length}人`);
  lines.push('');

  // ── ② FP スタイル キャッシュフロー計画表 ───────────────
  lines.push('【キャッシュフロー計画表（標準シナリオ）】');
  lines.push('※収入・支出・収支は月額（万円）、残高は年末（万円）');
  lines.push('');

  const eventMap = buildEventMap(input);

  // 各セルをカンマ対応でエスケープ
  const esc = (cells: (string | number)[]) =>
    cells.map(c => {
      const s = String(c);
      return s.includes(',') ? `"${s}"` : s;
    }).join(',');

  const r1 = (v: number) => Math.round(v * 10) / 10;

  // 行を組み立て（先頭セル = 行ラベル、以降は年ごとのデータ）
  const rowElapsed:  (string | number)[] = ['経過年数',           ...timeline.map(row => row.year)];
  const rowAge:      (string | number)[] = ['年齢（歳）',         ...timeline.map(row => row.age)];
  const rowEvent:    (string | number)[] = ['ライフイベント',      ...timeline.map(row => {
    const labels = [...(eventMap.get(row.age) ?? [])];
    if (labels.length === 0 && row.eventCosts > 0) labels.push('各種支出');
    return labels.join('・');
  })];
  const rowPeriod:   (string | number)[] = ['期間区分',           ...timeline.map(row =>
    row.period === 'employment' ? '就業期' : row.period === 'gap' ? '空白期' : '年金期'
  )];
  const rowIncome:   (string | number)[] = ['収入合計（月額）',   ...timeline.map(row => r1(row.income / 12))];
  const rowExpEss:   (string | number)[] = ['必須支出（月額）',   ...timeline.map(row => r1(row.essentialExpenses / 12))];
  const rowExpCom:   (string | number)[] = ['ゆとり支出（月額）', ...timeline.map(row => r1(row.comfortExpenses / 12))];
  const rowExpEvt:   (string | number)[] = ['イベント支出（月額）',...timeline.map(row => r1(row.eventCosts / 12))];
  const rowExpTotal: (string | number)[] = ['支出合計（月額）',   ...timeline.map(row =>
    r1((row.essentialExpenses + row.comfortExpenses + row.eventCosts) / 12)
  )];
  const rowNet:      (string | number)[] = ['月次収支',           ...timeline.map(row => r1(row.netFlow / 12))];
  const rowBalance:  (string | number)[] = ['貯蓄残高（年末）',   ...timeline.map(row => Math.round(row.assets))];

  lines.push(esc(rowElapsed));
  lines.push(esc(rowAge));
  lines.push(esc(rowEvent));
  lines.push(esc(rowPeriod));
  lines.push(esc(rowIncome));
  lines.push(esc(rowExpEss));
  lines.push(esc(rowExpCom));
  lines.push(esc(rowExpEvt));
  lines.push(esc(rowExpTotal));
  lines.push(esc(rowNet));
  lines.push(esc(rowBalance));
  lines.push('');

  // ── ③ 3シナリオ比較 ────────────────────────────────────
  lines.push('【3シナリオ比較】');
  lines.push('シナリオ,必要資産,退職時資産見込み,余力/不足,90歳残高,95歳残高');
  (['worst', 'main', 'upside'] as const).forEach(s => {
    const res = results[s];
    const mb  = res.milestoneBalances;
    lines.push([
      SCENARIO_LABELS[s],
      `${res.requiredAssets}万円`,
      `${res.projectedAssets}万円`,
      `${res.gapValue >= 0 ? '+' : ''}${res.gapValue}万円`,
      mb.age90 != null ? `${mb.age90}万円` : '枯渇',
      mb.age95 != null ? `${mb.age95}万円` : '枯渇',
    ].join(','));
  });
  lines.push('');

  // ── ④ 今いくら使えるか ─────────────────────────────────
  if (sustainableBudgets) {
    lines.push('【今いくら使えるか（逆算試算）】');
    lines.push('シナリオ,月間使用可能額合計,現在との差（月）');
    (['worst', 'main', 'upside'] as const).forEach(s => {
      const b = sustainableBudgets[s];
      lines.push(`${SCENARIO_LABELS[s]},${b.sustainableMonthly}万円,${b.differenceMonthly >= 0 ? '+' : ''}${b.differenceMonthly}万円`);
    });
    lines.push('');
  }

  // ── ⑤ モンテカルロ ────────────────────────────────────
  lines.push('【リスク試算（モンテカルロ法）】');
  lines.push(`資産持続可能性,${formatPercent(mc.successRate)}`);
  lines.push(`P50（中央値・最終資産）,${mc.percentiles.p50}万円`);
  lines.push('');

  lines.push('【免責】');
  lines.push('本試算は一般的な前提に基づく参考値です。特定の金融商品・投資を推奨するものではありません。');
  lines.push('将来の結果を保証するものではありません。');

  const csv  = lines.join('\n');
  const blob = new Blob(['\uFEFF' + csv], { type: 'text/csv;charset=utf-8;' });
  const url  = URL.createObjectURL(blob);
  const link = document.createElement('a');
  link.href     = url;
  link.download = `生涯資金シミュレーション_${new Date().toISOString().slice(0, 10)}.csv`;
  document.body.appendChild(link);
  link.click();
  document.body.removeChild(link);
  URL.revokeObjectURL(url);
}

export default function FamilySummaryCard({
  input, results, mc, notes = [], spendableMargins, sustainableBudgets,
}: FamilySummaryCardProps) {
  const main      = results.main;
  const isSurplus = main.gapValue >= 0;
  const createdAt = new Date().toLocaleDateString('ja-JP', { year: 'numeric', month: 'long', day: 'numeric' });

  const mainBudget = sustainableBudgets?.main;
  const currentMonthly = Math.round((input.annualEssentialExpenses + input.annualComfortExpenses) / 12 * 10) / 10;

  // Fallback: old hero margin logic
  const mainMargins = spendableMargins?.main ?? [];
  const heroMargin  = mainMargins.find(m => m.age >= 80 && m.monthlyMargin > 0)
    ?? mainMargins.find(m => m.monthlyMargin > 0);

  return (
    <div id="family-summary-print" className="bg-white rounded-2xl border border-gray-200 p-6 space-y-6 print:shadow-none">
      {/* Header */}
      <div className="border-b border-gray-100 pb-4">
        <h2 className="text-xl font-bold text-gray-900">家族共有サマリー</h2>
        <p className="text-sm text-gray-500 mt-1">作成日: {createdAt}</p>
        <p className="text-xs text-amber-700 bg-amber-50 border border-amber-200 rounded-lg px-3 py-2 mt-3 leading-relaxed">
          これは一般的な試算に基づく話し合いのたたき台です。
          特定の投資・保険・税務戦略を推奨するものではありません。
        </p>
        {/* CSV download button */}
        <button
          type="button"
          onClick={() => downloadCsv(input, results, mc, sustainableBudgets, spendableMargins)}
          className="mt-3 w-full flex items-center justify-center gap-2 border border-gray-200 rounded-xl py-2.5 text-sm font-medium text-gray-600 hover:bg-gray-50 transition-colors"
        >
          <span>📥</span>
          <span>CSVでダウンロード</span>
        </button>
      </div>

      {/* Key finding — 今いくら使えるか（最重要） */}
      <section>
        <h3 className="text-sm font-semibold text-brand-700 mb-2">📌 ポイント：今いくら使えるか</h3>

        {mainBudget ? (
          <div className={`rounded-xl p-4 ${
            mainBudget.differenceMonthly >= 0 ? 'bg-green-50 border border-green-200' : 'bg-red-50 border border-red-200'
          }`}>
            <p className="text-sm text-gray-700 leading-relaxed">
              標準シナリオの前提では、{input.expectedLifespan}歳で使い切る前提で
              <strong className={`ml-1 ${mainBudget.differenceMonthly >= 0 ? 'text-green-700' : 'text-red-700'}`}>
                月{mainBudget.sustainableMonthly.toLocaleString('ja-JP')}万円
              </strong>
              まで使えます。
            </p>
            {mainBudget.differenceMonthly >= 0 ? (
              <p className="text-xs text-green-700 mt-1.5">
                現在の支出（月{currentMonthly}万円）より
                月<strong>{mainBudget.differenceMonthly.toLocaleString('ja-JP')}万円</strong>多く使える余裕があります
              </p>
            ) : (
              <p className="text-xs text-red-700 mt-1.5">
                現在の支出（月{currentMonthly}万円）より
                月<strong>{Math.abs(mainBudget.differenceMonthly).toLocaleString('ja-JP')}万円</strong>の見直しが必要な見込みです
              </p>
            )}
            <p className="text-xs text-gray-500 mt-2">
              ただし慎重シナリオでは余力が大きく縮小する可能性があります。複数シナリオで確認してください。
            </p>
          </div>
        ) : heroMargin && heroMargin.monthlyMargin > 0 ? (
          <div className="bg-brand-50 rounded-xl p-4">
            <p className="text-sm text-gray-700 leading-relaxed">
              標準シナリオの前提では、{heroMargin.age}歳まで資産を維持しつつ、
              退職後に<strong>月{heroMargin.monthlyMargin.toLocaleString('ja-JP')}万円</strong>程度の
              追加支出ができる可能性があります（安全余裕込み）。
            </p>
          </div>
        ) : (
          <div className="bg-brand-50 rounded-xl p-4">
            <p className="text-sm text-gray-700 leading-relaxed">
              {input.retirementAge}歳で退職する前提で、{input.expectedLifespan}歳までに必要な資産は約
              <strong>{formatManYen(main.requiredAssets)}</strong>。
              退職時の資産見込みは約<strong>{formatManYen(main.projectedAssets)}</strong>で、
              {isSurplus
                ? '一定の余力がある可能性がありますが、長寿リスクに注意が必要です。'
                : `約${formatManYen(Math.abs(main.gapValue))}の不足が見込まれます。`}
            </p>
          </div>
        )}
      </section>

      {/* Premise — basic */}
      <section>
        <h3 className="text-sm font-semibold text-gray-600 mb-2">試算の前提（基本設定）</h3>
        <div className="grid grid-cols-2 gap-2 text-sm">
          {[
            { label: '現在の年齢',       value: `${input.age}歳` },
            { label: '家族タイプ',       value: FAMILY_TYPE_LABELS[input.familyType] },
            { label: '現在の金融資産',   value: formatManYen(input.financialAssets) },
            { label: '年収（税込）',     value: formatManYen(input.annualIncome) },
            { label: '退職予定年齢',     value: `${input.retirementAge}歳` },
            { label: '想定寿命',         value: `${input.expectedLifespan}歳` },
            { label: '運用スタイル',     value: RETURN_MODE_LABELS[input.returnMode] },
            { label: '年間必須支出',     value: formatManYen(input.annualEssentialExpenses) },
            { label: '年間ゆとり支出',   value: formatManYen(input.annualComfortExpenses) },
          ].map(item => (
            <div key={item.label} className="bg-gray-50 rounded-lg px-3 py-2">
              <p className="text-xs text-gray-500">{item.label}</p>
              <p className="font-semibold text-gray-800">{item.value}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Premise — details (only when set) */}
      {(input.retirementBonus || input.postRetirementEssentialExpenses != null ||
        input.care || input.children.length > 0 || input.pensionMonthly != null ||
        input.careerEvents?.length) && (
        <section>
          <h3 className="text-sm font-semibold text-gray-600 mb-2">試算の前提（詳細設定）</h3>
          <div className="grid grid-cols-2 gap-2 text-sm">
            {(input.retirementBonus ?? 0) > 0 && (
              <div className="bg-gray-50 rounded-lg px-3 py-2">
                <p className="text-xs text-gray-500">退職金</p>
                <p className="font-semibold text-gray-800">{formatManYen(input.retirementBonus ?? 0)}</p>
              </div>
            )}
            {input.pensionMonthly != null && (
              <div className="bg-gray-50 rounded-lg px-3 py-2">
                <p className="text-xs text-gray-500">年金（月額設定）</p>
                <p className="font-semibold text-gray-800">{input.pensionMonthly}万円/月</p>
              </div>
            )}
            {input.postRetirementEssentialExpenses != null && (
              <div className="bg-gray-50 rounded-lg px-3 py-2">
                <p className="text-xs text-gray-500">退職後必須支出</p>
                <p className="font-semibold text-gray-800">
                  年{formatManYen(input.postRetirementEssentialExpenses)}
                  （月{Math.round(input.postRetirementEssentialExpenses / 12 * 10) / 10}万円）
                </p>
              </div>
            )}
            {input.postRetirementComfortExpenses != null && (
              <div className="bg-gray-50 rounded-lg px-3 py-2">
                <p className="text-xs text-gray-500">退職後ゆとり支出</p>
                <p className="font-semibold text-gray-800">
                  年{formatManYen(input.postRetirementComfortExpenses)}
                  （月{Math.round(input.postRetirementComfortExpenses / 12 * 10) / 10}万円）
                </p>
              </div>
            )}
            {input.care && (
              <div className="bg-gray-50 rounded-lg px-3 py-2 col-span-2">
                <p className="text-xs text-gray-500">介護費用</p>
                <p className="font-semibold text-gray-800">
                  年{input.care.annualCost}万円 × {input.care.durationYears}年間、
                  {input.care.yearsFromNow}年後開始
                </p>
              </div>
            )}
            {input.children.length > 0 && (
              <div className="bg-gray-50 rounded-lg px-3 py-2">
                <p className="text-xs text-gray-500">教育費（子ども）</p>
                <p className="font-semibold text-gray-800">{input.children.length}人分</p>
              </div>
            )}
            {input.careerEvents && input.careerEvents.length > 0 && (
              <div className="bg-gray-50 rounded-lg px-3 py-2 col-span-2">
                <p className="text-xs text-gray-500">キャリアイベント</p>
                {input.careerEvents.map((ev, i) => (
                  <p key={i} className="font-semibold text-gray-800 text-xs">
                    {ev.age}歳：{ev.label}（収入{Math.round(ev.incomeRate * 100)}%）
                  </p>
                ))}
              </div>
            )}
          </div>
        </section>
      )}

      {/* 今いくら使えるか 3シナリオ比較 */}
      {sustainableBudgets && (
        <section>
          <h3 className="text-sm font-semibold text-gray-600 mb-2">
            今いくら使えるか — 3シナリオ比較
            <span className="text-xs font-normal text-gray-400 ml-1">
              （{input.expectedLifespan}歳でゼロ前提の逆算試算）
            </span>
          </h3>
          <div className="space-y-2">
            {(['worst', 'main', 'upside'] as const).map(s => {
              const b = sustainableBudgets[s];
              const isPlus = b.differenceMonthly >= 0;
              return (
                <div key={s} className="bg-gray-50 rounded-xl px-3 py-2.5">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-gray-700">{SCENARIO_LABELS[s]}</span>
                    <span className={`text-sm font-bold ${isPlus ? 'text-green-700' : 'text-red-600'}`}>
                      {isPlus ? '+' : '-'} 月{Math.abs(b.differenceMonthly).toLocaleString('ja-JP')}万円
                    </span>
                  </div>
                  <p className="text-xs text-gray-400 mt-0.5">
                    合計 月{b.sustainableMonthly.toLocaleString('ja-JP')}万円
                    {isPlus
                      ? ` / 現在より+${b.differenceMonthly}万円/月`
                      : ` / 月${Math.abs(b.differenceMonthly)}万円節約が必要`}
                  </p>
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* 3-scenario normal simulation comparison */}
      <section>
        <h3 className="text-sm font-semibold text-gray-600 mb-2">
          3シナリオ比較（通常シミュレーション）
          <span className="text-xs font-normal text-gray-400 ml-1">— 現在の支出継続の場合</span>
        </h3>
        <div className="space-y-2">
          {(['worst', 'main', 'upside'] as const).map(s => {
            const r   = results[s];
            const gap = r.gapValue;
            const sMargins = spendableMargins?.[s] ?? [];
            const sHero = sMargins.find(m => m.age >= 80 && m.monthlyMargin > 0);
            return (
              <div key={s} className="bg-gray-50 rounded-xl px-3 py-3 space-y-1">
                <div className="flex items-center justify-between">
                  <div className="flex items-center gap-2">
                    <span className={`w-2 h-2 rounded-full ${s === 'worst' ? 'bg-red-500' : s === 'main' ? 'bg-brand-500' : 'bg-green-500'}`} />
                    <span className="text-sm font-medium text-gray-700">{SCENARIO_LABELS[s]}</span>
                  </div>
                  <span className={`text-sm font-bold ${gap >= 0 ? 'text-surplus' : 'text-shortfall'}`}>
                    {gap >= 0 ? '+' : ''}{formatManYen(gap)}
                  </span>
                </div>
                <div className="flex gap-4 pl-4">
                  <span className="text-xs text-gray-400">
                    90歳: {r.milestoneBalances.age90 !== null
                      ? formatManYen(r.milestoneBalances.age90)
                      : <span className="text-red-500">枯渇</span>}
                  </span>
                  <span className="text-xs text-gray-400">
                    95歳: {r.milestoneBalances.age95 !== null
                      ? formatManYen(r.milestoneBalances.age95)
                      : <span className="text-red-500">枯渇</span>}
                  </span>
                </div>
                {sHero && sHero.monthlyMargin > 0 && (
                  <p className="text-xs text-green-600 pl-4">
                    退職後追加支出可能：月{sHero.monthlyMargin.toLocaleString('ja-JP')}万円（{sHero.age}歳まで目安）
                  </p>
                )}
              </div>
            );
          })}
        </div>
      </section>

      {/* Monte Carlo */}
      <section>
        <h3 className="text-sm font-semibold text-gray-600 mb-2">運用ブレを加味した参考試算</h3>
        <div className="bg-gray-50 rounded-xl p-4">
          <p className="text-xs text-gray-500 mb-1">想定寿命まで資産が持続する可能性（1,000回試算）</p>
          <p className={`text-2xl font-black ${mc.successRate >= 0.7 ? 'text-surplus' : mc.successRate >= 0.4 ? 'text-amber-500' : 'text-shortfall'}`}>
            {formatPercent(mc.successRate)}
          </p>
          <p className="text-xs text-gray-400 mt-1">
            ※ 運用リターンのみをランダム化した参考試算です
          </p>
        </div>
      </section>

      {/* Discussion points */}
      <section>
        <h3 className="text-sm font-semibold text-gray-600 mb-2">話し合うべき論点</h3>
        <ul className="space-y-1 text-sm text-gray-600">
          {[
            '今の生活で使いすぎ・ため込みすぎはないか',
            '余力候補がある場合、どう活用するか（教育・介護・旅行・住居支援など）',
            '退職年齢・セカンドキャリアについて',
            '老後の生活費（最低限とゆとり）の認識合わせ',
            '介護への備えと役割分担',
            '試算の前提が変わった場合の見直し時期',
          ].map(point => (
            <li key={point} className="flex items-start gap-2">
              <span className="text-brand-500 mt-0.5">•</span>
              {point}
            </li>
          ))}
        </ul>
      </section>

      {/* Family notes */}
      {notes.length > 0 && (
        <section>
          <h3 className="text-sm font-semibold text-gray-600 mb-3">話し合いメモ</h3>
          <div className="space-y-3">
            {NOTE_CATEGORIES.map(cat => {
              const catNotes = notes.filter(n => n.category === cat);
              if (catNotes.length === 0) return null;
              return (
                <div key={cat}>
                  <p className="text-xs font-semibold text-gray-500 mb-1.5">
                    {NOTE_CATEGORY_EMOJI[cat]} {NOTE_CATEGORY_LABELS[cat]}
                  </p>
                  {catNotes.map(note => (
                    <div key={note.id} className="bg-gray-50 rounded-lg px-3 py-2 mb-1.5">
                      <p className="text-xs text-gray-700 whitespace-pre-wrap leading-relaxed">{note.content}</p>
                      <p className="text-[10px] text-gray-300 mt-1">
                        {new Date(note.updatedAt).toLocaleDateString('ja-JP')}
                      </p>
                    </div>
                  ))}
                </div>
              );
            })}
          </div>
        </section>
      )}

      {/* Disclaimer */}
      <section className="bg-gray-50 rounded-xl p-4">
        <p className="text-xs text-gray-400 leading-relaxed">
          本サマリーは一般的な試算であり、特定の金融商品・投資・贈与等を推奨するものではありません。
          「余力候補」「追加支出可能額」は試算上の参考値であり、実際の支出可能額を保証するものではありません。
          税務・法務上の最終判断は専門家にご相談ください。最終的な意思決定はご自身の責任で行ってください。
          将来の制度変更・物価変動・税制改正等によって結果は大きく変わる可能性があります。
        </p>
      </section>
    </div>
  );
}
