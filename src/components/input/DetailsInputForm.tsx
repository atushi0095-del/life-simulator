'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import Button from '@/components/ui/Button';
import SliderInput from '@/components/ui/SliderInput';
import Card from '@/components/ui/Card';
import { useSimulation } from '@/context/SimulationContext';
import { computeEducationTotalFromConfig } from '@/lib/simulation/events';
import { EDUCATION_STAGE_COST, EDUCATION_PRESETS, CARE_TYPE_PRESETS } from '@/lib/simulation/constants';
import { formatManYen } from '@/lib/utils/formatCurrency';
import type { ChildEvent, EducationConfig, EducationStage, StageType, CareType, CareerEvent, CareerEventType, TemporaryIncome } from '@/lib/simulation/types';

const STAGE_LABELS: Record<EducationStage, string> = {
  elementary: '小学校',
  middle:     '中学校',
  high:       '高校',
  university: '大学',
};

const STAGE_ORDER: EducationStage[] = ['elementary', 'middle', 'high', 'university'];

const PRESET_BUTTONS: { key: keyof typeof EDUCATION_PRESETS; label: string }[] = [
  { key: 'all_public',          label: 'すべて公立' },
  { key: 'private_from_high',   label: '高校から私立' },
  { key: 'private_uni_only',    label: '大学のみ私立' },
  { key: 'private_from_middle', label: '中学から私立' },
  { key: 'all_private',         label: 'すべて私立' },
];

const DEFAULT_EDUCATION_CONFIG: EducationConfig = {
  elementary: 'public', middle: 'public', high: 'public', university: 'public',
};

const CARE_TYPES: Exclude<CareType, 'custom'>[] = ['home', 'visiting', 'facility'];

/* ── 退職後支出コンポーネント（月額/年額トグル付き） ── */
function PostRetirementExpenses({
  essentialAnnual,
  comfortAnnual,
  onChangeEssential,
  onChangeComfort,
}: {
  essentialAnnual: number;
  comfortAnnual: number;
  onChangeEssential: (v: number) => void;
  onChangeComfort: (v: number) => void;
}) {
  const [mode, setMode] = useState<'annual' | 'monthly'>('monthly');
  const FACTOR = mode === 'monthly' ? 12 : 1;
  const UNIT   = mode === 'monthly' ? '万円/月' : '万円/年';

  const essDisplay = Math.round(essentialAnnual / FACTOR);
  const comDisplay = Math.round(comfortAnnual   / FACTOR);
  const totalDisplay = essDisplay + comDisplay;

  const essMax   = mode === 'monthly' ? 42 : 500;
  const essMin   = mode === 'monthly' ? 5  : 60;
  const essStep  = mode === 'monthly' ? 1  : 12;
  const comMax   = mode === 'monthly' ? 25 : 300;
  const comStep  = mode === 'monthly' ? 1  : 12;

  return (
    <div className="space-y-3 bg-gray-50 rounded-xl p-4">
      {/* Toggle */}
      <div className="flex items-center justify-between">
        <p className="text-xs text-gray-500">
          合計: <span className="font-bold text-gray-700">{totalDisplay}{UNIT}</span>
          {mode === 'monthly' && (
            <span className="text-gray-400 ml-1">（年{(totalDisplay * 12).toLocaleString('ja-JP')}万円）</span>
          )}
        </p>
        <div className="flex bg-white border border-gray-200 rounded-lg overflow-hidden text-xs">
          <button
            type="button"
            onClick={() => setMode('monthly')}
            className={`px-3 py-1.5 font-medium transition-colors ${mode === 'monthly' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}
          >月額</button>
          <button
            type="button"
            onClick={() => setMode('annual')}
            className={`px-3 py-1.5 font-medium transition-colors ${mode === 'annual' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'}`}
          >年額</button>
        </div>
      </div>
      <SliderInput
        label="退職後の最低生活費"
        value={essDisplay}
        min={essMin} max={essMax} step={essStep} unit={UNIT}
        description="住居費が減る場合など、現役時より下がることが多い"
        onChange={v => onChangeEssential(v * FACTOR)}
      />
      <SliderInput
        label="退職後のゆとり費"
        value={comDisplay}
        min={0} max={comMax} step={comStep} unit={UNIT}
        description="旅行・趣味など"
        onChange={v => onChangeComfort(v * FACTOR)}
      />
    </div>
  );
}

export default function DetailsInputForm() {
  const router = useRouter();
  const { state, setInput, runSimulation } = useSimulation();
  const { input } = state;

  function addChild() {
    const child: ChildEvent = {
      birthYearOffset: 0,
      educationPath: 'public',
      educationConfig: { ...DEFAULT_EDUCATION_CONFIG },
    };
    setInput({ children: [...input.children, child] });
  }

  function removeChild(index: number) {
    setInput({ children: input.children.filter((_, i) => i !== index) });
  }

  function updateChild(index: number, patch: Partial<ChildEvent>) {
    setInput({
      children: input.children.map((c, i) => (i === index ? { ...c, ...patch } : c)),
    });
  }

  function setChildStage(childIndex: number, stage: EducationStage, value: StageType) {
    const child = input.children[childIndex];
    const config: EducationConfig = child.educationConfig
      ? { ...child.educationConfig, [stage]: value }
      : { ...DEFAULT_EDUCATION_CONFIG, [stage]: value };
    updateChild(childIndex, { educationConfig: config });
  }

  function applyPreset(childIndex: number, presetKey: keyof typeof EDUCATION_PRESETS) {
    const config = { ...EDUCATION_PRESETS[presetKey] } as EducationConfig;
    updateChild(childIndex, { educationConfig: config });
  }

  function selectCarePreset(type: Exclude<CareType, 'custom'>) {
    const preset = CARE_TYPE_PRESETS[type];
    setInput({
      care: {
        yearsFromNow: input.care?.yearsFromNow ?? 20,
        annualCost: preset.annualCost,
        durationYears: preset.durationYears,
        careType: type,
      },
    });
  }

  function handleRun() {
    runSimulation();
    router.push('/results');
  }

  return (
    <div className="space-y-8">
      {/* Children */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-800">子どもの情報</h2>
          <Button variant="secondary" size="sm" onClick={addChild}>
            + 追加
          </Button>
        </div>

        {input.children.length === 0 && (
          <p className="text-sm text-gray-400 text-center py-4">
            子どもがいる場合は「追加」ボタンで設定できます
          </p>
        )}

        {input.children.map((child, i) => {
          const config = child.educationConfig ?? DEFAULT_EDUCATION_CONFIG;
          const totalCost = computeEducationTotalFromConfig(config);

          return (
            <div key={i} className="bg-gray-50 rounded-xl p-4 space-y-4">
              <div className="flex items-center justify-between">
                <p className="text-sm font-medium text-gray-700">子ども {i + 1}</p>
                <button
                  type="button"
                  onClick={() => removeChild(i)}
                  className="text-xs text-red-500 hover:text-red-700"
                >
                  削除
                </button>
              </div>

              <SliderInput
                label="誕生からの年数"
                value={child.birthYearOffset}
                min={0}
                max={10}
                unit="年後"
                description="すでに生まれている場合は0"
                onChange={v => updateChild(i, { birthYearOffset: v })}
              />

              {/* Education: per-stage selection */}
              <div className="space-y-3">
                <div className="flex items-center justify-between">
                  <label className="text-sm font-medium text-gray-700">進学パターン</label>
                  <span className="text-xs font-semibold text-brand-600">
                    合計: 約{formatManYen(totalCost)}
                  </span>
                </div>

                {/* Preset shortcuts */}
                <div className="flex flex-wrap gap-1.5">
                  {PRESET_BUTTONS.map(({ key, label }) => {
                    const presetConfig = EDUCATION_PRESETS[key];
                    const isActive = STAGE_ORDER.every(
                      s => config[s] === presetConfig[s],
                    );
                    return (
                      <button
                        key={key}
                        type="button"
                        onClick={() => applyPreset(i, key)}
                        className={[
                          'px-2.5 py-1 rounded-full text-xs font-medium border transition-colors',
                          isActive
                            ? 'bg-brand-50 border-brand-300 text-brand-700'
                            : 'bg-white border-gray-200 text-gray-500 hover:border-gray-300',
                        ].join(' ')}
                      >
                        {label}
                      </button>
                    );
                  })}
                </div>

                {/* Per-stage toggles */}
                <div className="space-y-1.5">
                  {STAGE_ORDER.map(stage => {
                    const stageType = config[stage];
                    const cost = EDUCATION_STAGE_COST[stage][stageType];
                    return (
                      <div
                        key={stage}
                        className="flex items-center justify-between bg-white rounded-lg px-3 py-2 border border-gray-100"
                      >
                        <div>
                          <span className="text-sm text-gray-700">{STAGE_LABELS[stage]}</span>
                          <span className="text-xs text-gray-400 ml-2">約{formatManYen(cost)}</span>
                        </div>
                        <div className="flex bg-gray-100 rounded-lg overflow-hidden text-xs">
                          <button
                            type="button"
                            onClick={() => setChildStage(i, stage, 'public')}
                            className={`px-3 py-1 font-medium transition-colors ${
                              stageType === 'public'
                                ? 'bg-brand-600 text-white'
                                : 'text-gray-500 hover:bg-gray-200'
                            }`}
                          >
                            公立
                          </button>
                          <button
                            type="button"
                            onClick={() => setChildStage(i, stage, 'private')}
                            className={`px-3 py-1 font-medium transition-colors ${
                              stageType === 'private'
                                ? 'bg-brand-600 text-white'
                                : 'text-gray-500 hover:bg-gray-200'
                            }`}
                          >
                            私立
                          </button>
                        </div>
                      </div>
                    );
                  })}
                </div>

                <p className="text-xs text-gray-400 leading-relaxed">
                  ※ 教育費は文科省の調査をもとにした概算です。高校無償化等の制度は未反映です。
                </p>
              </div>
            </div>
          );
        })}
      </section>

      {/* Housing */}
      <section className="space-y-4">
        <h2 className="text-base font-semibold text-gray-800">住宅</h2>
        <div className="space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="housing"
              checked={!input.housing}
              onChange={() => setInput({ housing: undefined })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">設定しない（すでに考慮済み）</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="housing"
              checked={input.housing?.type === 'purchase'}
              onChange={() => setInput({ housing: { type: 'purchase', yearsFromNow: 0, cost: 3000, annualRent: 0 } })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">住宅購入を予定している</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="housing"
              checked={input.housing?.type === 'rent'}
              onChange={() => setInput({ housing: { type: 'rent', yearsFromNow: 0, cost: 0, annualRent: 120 } })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">賃貸を継続する</span>
          </label>
        </div>

        {input.housing?.type === 'purchase' && (
          <div className="space-y-3 bg-gray-50 rounded-xl p-4">
            <SliderInput
              label="購入時期"
              value={input.housing.yearsFromNow}
              min={0}
              max={20}
              unit="年後"
              onChange={v => setInput({ housing: { ...input.housing!, yearsFromNow: v } })}
            />
            <SliderInput
              label="購入費用（頭金+諸費用）"
              value={input.housing.cost}
              min={500}
              max={10000}
              step={100}
              unit="万円"
              onChange={v => setInput({ housing: { ...input.housing!, cost: v } })}
            />
          </div>
        )}

        {input.housing?.type === 'rent' && (
          <div className="bg-gray-50 rounded-xl p-4">
            <SliderInput
              label="年間家賃"
              value={input.housing.annualRent}
              min={60}
              max={300}
              step={12}
              unit="万円"
              onChange={v => setInput({ housing: { ...input.housing!, annualRent: v } })}
            />
          </div>
        )}
      </section>

      {/* Car */}
      <section className="space-y-4">
        <h2 className="text-base font-semibold text-gray-800">車の購入</h2>
        <div className="space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="car"
              checked={!input.car}
              onChange={() => setInput({ car: undefined })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">設定しない</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="car"
              checked={!!input.car && !input.car.replacementEveryYears}
              onChange={() => setInput({ car: { yearsFromNow: 0, cost: 300 } })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">1回だけ購入する</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="car"
              checked={!!input.car?.replacementEveryYears}
              onChange={() => setInput({ car: { yearsFromNow: 0, cost: 300, replacementEveryYears: 10 } })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">定期的に買い替える</span>
          </label>
        </div>

        {input.car && (
          <div className="space-y-3 bg-gray-50 rounded-xl p-4">
            <SliderInput
              label="購入時期"
              value={input.car.yearsFromNow}
              min={0} max={20} unit="年後"
              onChange={v => setInput({ car: { ...input.car!, yearsFromNow: v } })}
            />
            <SliderInput
              label="購入費用（諸費用込み）"
              value={input.car.cost}
              min={50} max={1000} step={10} unit="万円"
              description="軽・コンパクト 100〜200万円 / 普通車 200〜400万円 / 高級車 500万円〜"
              onChange={v => setInput({ car: { ...input.car!, cost: v } })}
            />
            {input.car.replacementEveryYears !== undefined && (
              <SliderInput
                label="買い替え間隔"
                value={input.car.replacementEveryYears}
                min={3} max={20} unit="年おき"
                description="平均的な買い替え間隔は 7〜10 年"
                onChange={v => setInput({ car: { ...input.car!, replacementEveryYears: v } })}
              />
            )}
            <p className="text-xs text-gray-400 leading-relaxed">
              ※ 税金・保険・維持費は通常の生活費（必須支出）に含めてください。
              ここでは購入時の一時費用のみを計上します。
            </p>
          </div>
        )}
      </section>

      {/* Care */}
      <section className="space-y-4">
        <h2 className="text-base font-semibold text-gray-800">介護費用</h2>
        <div className="space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="care"
              checked={!input.care}
              onChange={() => setInput({ care: undefined })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">設定しない</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="care"
              checked={!!input.care}
              onChange={() => setInput({ care: { yearsFromNow: 20, annualCost: 120, durationYears: 5 } })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">将来的な介護費用を見込む</span>
          </label>
        </div>

        {input.care && (
          <div className="space-y-4 bg-gray-50 rounded-xl p-4">
            {/* Care type presets */}
            <div>
              <p className="text-xs font-semibold text-gray-600 mb-2">
                一般的なパターンから選ぶ（目安）
              </p>
              <div className="space-y-2">
                {CARE_TYPES.map(type => {
                  const preset = CARE_TYPE_PRESETS[type];
                  const isActive = input.care?.careType === type;
                  return (
                    <button
                      key={type}
                      type="button"
                      onClick={() => selectCarePreset(type)}
                      className={[
                        'w-full text-left rounded-xl border px-3 py-2.5 transition-colors',
                        isActive
                          ? 'bg-brand-50 border-brand-300'
                          : 'bg-white border-gray-200 hover:border-gray-300',
                      ].join(' ')}
                    >
                      <div className="flex items-center justify-between">
                        <span className={`text-sm font-medium ${isActive ? 'text-brand-700' : 'text-gray-700'}`}>
                          {preset.label}
                        </span>
                        <span className="text-xs text-gray-500">{preset.monthlyEstimate}</span>
                      </div>
                      <p className="text-xs text-gray-400 mt-0.5">{preset.description}</p>
                    </button>
                  );
                })}
              </div>
              <p className="text-xs text-gray-400 mt-2 leading-relaxed">
                ※ 介護費は地域・状態・サービス内容で大きく異なります。
                平均介護期間は約5年と言われますが個人差が非常に大きいです。
                よくわからない場合は「訪問介護」を目安にしてください。
              </p>
            </div>

            {/* Fine-tune sliders */}
            <div className="border-t border-gray-200 pt-3 space-y-3">
              <p className="text-xs text-gray-500 font-medium">詳細調整</p>
              <SliderInput
                label="介護開始時期"
                value={input.care.yearsFromNow}
                min={5}
                max={40}
                unit="年後"
                onChange={v => setInput({ care: { ...input.care!, yearsFromNow: v, careType: 'custom' } })}
              />
              <SliderInput
                label="年間介護費"
                value={input.care.annualCost}
                min={30}
                max={500}
                step={10}
                unit="万円"
                onChange={v => setInput({ care: { ...input.care!, annualCost: v, careType: 'custom' } })}
              />
              <SliderInput
                label="介護期間"
                value={input.care.durationYears}
                min={1}
                max={30}
                unit="年間"
                onChange={v => setInput({ care: { ...input.care!, durationYears: v, careType: 'custom' } })}
              />
            </div>
          </div>
        )}
      </section>

      {/* Post-retirement expenses */}
      <section className="space-y-4">
        <h2 className="text-base font-semibold text-gray-800">退職後の支出（任意）</h2>
        <p className="text-sm text-gray-500">
          退職後に支出が変わる場合は設定できます。未設定なら現在の支出がそのまま使われます。
        </p>
        <div className="space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="postRetExp"
              checked={input.postRetirementEssentialExpenses === undefined}
              onChange={() => setInput({ postRetirementEssentialExpenses: undefined, postRetirementComfortExpenses: undefined })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">現在の支出と同じ</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="postRetExp"
              checked={input.postRetirementEssentialExpenses !== undefined}
              onChange={() => setInput({
                postRetirementEssentialExpenses: Math.round(input.annualEssentialExpenses * 0.8),
                postRetirementComfortExpenses: input.annualComfortExpenses,
              })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">退職後は支出が変わる</span>
          </label>
        </div>
        {input.postRetirementEssentialExpenses !== undefined && (
          <PostRetirementExpenses
            essentialAnnual={input.postRetirementEssentialExpenses ?? 0}
            comfortAnnual={input.postRetirementComfortExpenses ?? 0}
            onChangeEssential={v => setInput({ postRetirementEssentialExpenses: v })}
            onChangeComfort={v => setInput({ postRetirementComfortExpenses: v })}
          />
        )}
      </section>

      {/* Income breakdown */}
      {(input.familyType === 'couple' || input.familyType === 'family_with_children') && (
        <section className="space-y-4">
          <h2 className="text-base font-semibold text-gray-800">収入内訳（任意）</h2>
          <p className="text-sm text-gray-500">
            夫婦の収入を分けて入力できます。基本設定の年収は合算値として使われます。
          </p>
          <div className="space-y-2">
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="radio"
                name="incomeBreakdown"
                checked={!input.incomeBreakdown}
                onChange={() => setInput({ incomeBreakdown: undefined })}
                className="text-brand-600"
              />
              <span className="text-sm text-gray-700">合算のまま（基本設定の年収を使用）</span>
            </label>
            <label className="flex items-center gap-3 cursor-pointer">
              <input
                type="radio"
                name="incomeBreakdown"
                checked={!!input.incomeBreakdown}
                onChange={() => setInput({
                  incomeBreakdown: {
                    primary: Math.round(input.annualIncome * 0.6),
                    spouse: Math.round(input.annualIncome * 0.4),
                  },
                })}
                className="text-brand-600"
              />
              <span className="text-sm text-gray-700">分けて入力する</span>
            </label>
          </div>
          {input.incomeBreakdown && (
            <div className="space-y-3 bg-gray-50 rounded-xl p-4">
              <p className="text-xs text-gray-500">
                合計: <span className="font-bold text-gray-700">
                  {((input.incomeBreakdown.primary ?? 0) + (input.incomeBreakdown.spouse ?? 0) + (input.incomeBreakdown.other ?? 0)).toLocaleString('ja-JP')}万円/年
                </span>
              </p>
              <SliderInput
                label="本人の年収"
                value={input.incomeBreakdown.primary}
                min={0} max={2000} step={50} unit="万円"
                onChange={v => {
                  const bd = { ...input.incomeBreakdown!, primary: v };
                  setInput({ incomeBreakdown: bd, annualIncome: bd.primary + (bd.spouse ?? 0) + (bd.other ?? 0) });
                }}
              />
              <SliderInput
                label="配偶者の年収"
                value={input.incomeBreakdown.spouse ?? 0}
                min={0} max={2000} step={50} unit="万円"
                onChange={v => {
                  const bd = { ...input.incomeBreakdown!, spouse: v };
                  setInput({ incomeBreakdown: bd, annualIncome: bd.primary + (bd.spouse ?? 0) + (bd.other ?? 0) });
                }}
              />
              <SliderInput
                label="その他の収入"
                value={input.incomeBreakdown.other ?? 0}
                min={0} max={500} step={10} unit="万円"
                description="副業・不動産収入など"
                onChange={v => {
                  const bd = { ...input.incomeBreakdown!, other: v };
                  setInput({ incomeBreakdown: bd, annualIncome: bd.primary + (bd.spouse ?? 0) + (bd.other ?? 0) });
                }}
              />
            </div>
          )}
        </section>
      )}

      {/* Temporary incomes */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-800">時限的収入（任意）</h2>
          <Button variant="secondary" size="sm" onClick={() => {
            const list: TemporaryIncome[] = [...(input.temporaryIncomes ?? [])];
            list.push({ label: '時限的収入', annualAmount: 60, yearsFromNow: 0, durationYears: 5 });
            setInput({ temporaryIncomes: list });
          }}>
            + 追加
          </Button>
        </div>
        <p className="text-sm text-gray-500">
          養育費受取・副業・賃貸収入など、一定期間だけ受け取る収入を設定できます（手取り額で入力）。
        </p>

        {(!input.temporaryIncomes || input.temporaryIncomes.length === 0) && (
          <p className="text-sm text-gray-400 text-center py-3">
            期限付きの収入がある場合は「追加」で設定
          </p>
        )}

        {input.temporaryIncomes?.map((ti, i) => (
          <div key={i} className="bg-gray-50 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-gray-700">{ti.label || '時限的収入'}</p>
              <button
                type="button"
                onClick={() => {
                  const list = input.temporaryIncomes!.filter((_, idx) => idx !== i);
                  setInput({ temporaryIncomes: list.length > 0 ? list : undefined });
                }}
                className="text-xs text-red-500 hover:text-red-700"
              >
                削除
              </button>
            </div>
            {/* Label input */}
            <div>
              <p className="text-xs text-gray-500 mb-1">ラベル（任意）</p>
              <input
                type="text"
                value={ti.label ?? ''}
                placeholder="例: 養育費受取、家賃収入 など"
                onChange={e => {
                  const list = [...input.temporaryIncomes!];
                  list[i] = { ...list[i], label: e.target.value };
                  setInput({ temporaryIncomes: list });
                }}
                className="w-full border border-gray-200 rounded-lg px-3 py-2 text-sm text-gray-700 bg-white focus:outline-none focus:border-brand-400"
              />
            </div>
            <SliderInput
              label="年間受取額（手取り）"
              value={ti.annualAmount}
              min={12} max={600} step={12} unit="万円/年"
              description={`月${Math.round(ti.annualAmount / 12)}万円 相当`}
              onChange={v => {
                const list = [...input.temporaryIncomes!];
                list[i] = { ...list[i], annualAmount: v };
                setInput({ temporaryIncomes: list });
              }}
            />
            <SliderInput
              label="受取開始時期"
              value={ti.yearsFromNow}
              min={0} max={30} unit="年後"
              onChange={v => {
                const list = [...input.temporaryIncomes!];
                list[i] = { ...list[i], yearsFromNow: v };
                setInput({ temporaryIncomes: list });
              }}
            />
            <SliderInput
              label="受取期間"
              value={ti.durationYears}
              min={1} max={30} unit="年間"
              onChange={v => {
                const list = [...input.temporaryIncomes!];
                list[i] = { ...list[i], durationYears: v };
                setInput({ temporaryIncomes: list });
              }}
            />
          </div>
        ))}
      </section>

      {/* Career events */}
      <section className="space-y-4">
        <div className="flex items-center justify-between">
          <h2 className="text-base font-semibold text-gray-800">キャリアイベント（任意）</h2>
          <Button variant="secondary" size="sm" onClick={() => {
            const events = [...(input.careerEvents ?? [])];
            events.push({ type: 'role_demotion', age: 55, incomeRate: 0.8, label: '役職定年' });
            setInput({ careerEvents: events });
          }}>
            + 追加
          </Button>
        </div>
        <p className="text-sm text-gray-500">
          役職定年・再雇用など、収入が変わるイベントを設定できます。
        </p>

        {(!input.careerEvents || input.careerEvents.length === 0) && (
          <p className="text-sm text-gray-400 text-center py-3">
            収入が変わるイベントがある場合は「追加」で設定
          </p>
        )}

        {input.careerEvents?.map((event, i) => (
          <div key={i} className="bg-gray-50 rounded-xl p-4 space-y-3">
            <div className="flex items-center justify-between">
              <p className="text-sm font-medium text-gray-700">{event.label || (event.type === 'role_demotion' ? '役職定年' : '再雇用')}</p>
              <button
                type="button"
                onClick={() => {
                  const events = input.careerEvents!.filter((_, idx) => idx !== i);
                  setInput({ careerEvents: events.length > 0 ? events : undefined });
                }}
                className="text-xs text-red-500 hover:text-red-700"
              >
                削除
              </button>
            </div>
            <div className="flex gap-2">
              <button
                type="button"
                onClick={() => {
                  const events = [...input.careerEvents!];
                  events[i] = { ...events[i], type: 'role_demotion', label: '役職定年' };
                  setInput({ careerEvents: events });
                }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                  event.type === 'role_demotion'
                    ? 'bg-brand-50 border-brand-300 text-brand-700'
                    : 'bg-white border-gray-200 text-gray-500'
                }`}
              >
                役職定年
              </button>
              <button
                type="button"
                onClick={() => {
                  const events = [...input.careerEvents!];
                  events[i] = { ...events[i], type: 'reemployment', label: '再雇用' };
                  setInput({ careerEvents: events });
                }}
                className={`px-3 py-1 rounded-full text-xs font-medium border transition-colors ${
                  event.type === 'reemployment'
                    ? 'bg-brand-50 border-brand-300 text-brand-700'
                    : 'bg-white border-gray-200 text-gray-500'
                }`}
              >
                再雇用
              </button>
            </div>
            <SliderInput
              label="発生年齢"
              value={event.age}
              min={45} max={70} unit="歳"
              onChange={v => {
                const events = [...input.careerEvents!];
                events[i] = { ...events[i], age: v };
                setInput({ careerEvents: events });
              }}
            />
            <SliderInput
              label="収入水準"
              value={Math.round(event.incomeRate * 100)}
              min={30} max={100} step={5} unit="%"
              description={`現在の年収の${Math.round(event.incomeRate * 100)}% = 約${Math.round(input.annualIncome * event.incomeRate).toLocaleString('ja-JP')}万円`}
              onChange={v => {
                const events = [...input.careerEvents!];
                events[i] = { ...events[i], incomeRate: v / 100 };
                setInput({ careerEvents: events });
              }}
            />
          </div>
        ))}
      </section>

      {/* Pension override */}
      <section className="space-y-4">
        <h2 className="text-base font-semibold text-gray-800">年金（任意）</h2>
        <p className="text-sm text-gray-500">
          デフォルトは家族タイプから概算します。
          ねんきん定期便等で確認できる場合は上書きできます。
        </p>
        <div className="space-y-2">
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="pension"
              checked={input.pensionMonthly === undefined}
              onChange={() => setInput({ pensionMonthly: undefined })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">デフォルト（家族タイプから推定）</span>
          </label>
          <label className="flex items-center gap-3 cursor-pointer">
            <input
              type="radio"
              name="pension"
              checked={input.pensionMonthly !== undefined}
              onChange={() => setInput({ pensionMonthly: 15 })}
              className="text-brand-600"
            />
            <span className="text-sm text-gray-700">手動で設定する</span>
          </label>
        </div>
        {input.pensionMonthly !== undefined && (
          <div className="bg-gray-50 rounded-xl p-4">
            <SliderInput
              label="月間年金受給額"
              value={input.pensionMonthly}
              min={5}
              max={40}
              unit="万円/月"
              onChange={v => setInput({ pensionMonthly: v })}
            />
          </div>
        )}
      </section>

      {/* Run button */}
      <div className="space-y-3 pt-2">
        <Button fullWidth size="lg" onClick={handleRun}>
          シミュレーションを見る
        </Button>
        <Button
          fullWidth
          variant="ghost"
          onClick={() => {
            runSimulation();
            router.push('/results');
          }}
        >
          詳細設定をスキップして計算
        </Button>
      </div>
    </div>
  );
}
