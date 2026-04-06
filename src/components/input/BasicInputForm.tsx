'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import SliderInput from '@/components/ui/SliderInput';
import Button from '@/components/ui/Button';
import FamilyTypeSelector from './FamilyTypeSelector';
import ReturnModeSelector from './ReturnModeSelector';
import { useSimulation } from '@/context/SimulationContext';

type ExpenseMode = 'annual' | 'monthly';

interface BasicInputFormProps {
  /** When false, the action buttons at the bottom are hidden (e.g. when embedded in details page) */
  showActions?: boolean;
}

export default function BasicInputForm({ showActions = true }: BasicInputFormProps) {
  const router = useRouter();
  const { state, setInput, runSimulation } = useSimulation();
  const { input } = state;

  // Monthly/annual toggle for expenses
  const [expenseMode, setExpenseMode] = useState<ExpenseMode>('annual');

  const FACTOR = expenseMode === 'monthly' ? 12 : 1;
  const UNIT   = expenseMode === 'monthly' ? '万円/月' : '万円/年';

  // Displayed values (convert stored annual → display unit)
  const essentialDisplay = Math.round(input.annualEssentialExpenses / FACTOR);
  const comfortDisplay   = Math.round(input.annualComfortExpenses   / FACTOR);
  const totalDisplay     = essentialDisplay + comfortDisplay;

  function setEssential(v: number) { setInput({ annualEssentialExpenses: v * FACTOR }); }
  function setComfort(v: number)   { setInput({ annualComfortExpenses:   v * FACTOR }); }

  // Slider ranges in display unit
  const essMax = expenseMode === 'monthly' ? 42 : 500;
  const essMin = expenseMode === 'monthly' ? 5  : 60;
  const essStep = expenseMode === 'monthly' ? 1  : 12;
  const comMax  = expenseMode === 'monthly' ? 25 : 300;
  const comStep = expenseMode === 'monthly' ? 1  : 12;

  function handleRunNow() {
    runSimulation();
    router.push('/results');
  }

  return (
    <div className="space-y-6">
      <FamilyTypeSelector
        value={input.familyType}
        onChange={v => setInput({ familyType: v })}
      />

      <SliderInput
        label="現在の年齢"
        value={input.age}
        min={20} max={75} unit="歳"
        onChange={v => setInput({ age: v })}
      />

      <SliderInput
        label="現在の金融資産"
        value={input.financialAssets}
        min={0} max={10000} step={50} unit="万円"
        description="預貯金・投資信託・株式など合計"
        onChange={v => setInput({ financialAssets: v })}
      />

      <SliderInput
        label="年収（税込）"
        value={input.annualIncome}
        min={0} max={3000} step={50} unit="万円"
        description="給与・事業収入など。税引き後は約75%で計算します。"
        onChange={v => setInput({ annualIncome: v })}
      />

      {/* Expenses section */}
      <div className="space-y-4 bg-gray-50 rounded-2xl p-4">
        <div className="flex items-center justify-between">
          <p className="text-sm font-semibold text-gray-700">支出</p>
          {/* Monthly / Annual toggle */}
          <div className="flex bg-white border border-gray-200 rounded-lg overflow-hidden text-xs">
            <button
              type="button"
              onClick={() => setExpenseMode('monthly')}
              className={`px-3 py-1.5 font-medium transition-colors ${
                expenseMode === 'monthly' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'
              }`}
            >
              月額
            </button>
            <button
              type="button"
              onClick={() => setExpenseMode('annual')}
              className={`px-3 py-1.5 font-medium transition-colors ${
                expenseMode === 'annual' ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'
              }`}
            >
              年額
            </button>
          </div>
        </div>

        <p className="text-xs text-gray-500 -mt-1">
          合計 <span className="font-bold text-gray-700">{totalDisplay.toLocaleString('ja-JP')}{UNIT}</span>
          {expenseMode === 'monthly' && (
            <span className="text-gray-400">（年間：{(totalDisplay * 12).toLocaleString('ja-JP')}万円）</span>
          )}
        </p>

        <SliderInput
          label="最低生活費"
          value={essentialDisplay}
          min={essMin} max={essMax} step={essStep} unit={UNIT}
          description="家賃・食費・光熱費など必須支出"
          onChange={setEssential}
        />

        <SliderInput
          label="ゆとり費"
          value={comfortDisplay}
          min={0} max={comMax} step={comStep} unit={UNIT}
          description="旅行・趣味・外食など裁量支出"
          onChange={setComfort}
        />
      </div>

      <SliderInput
        label="退職予定年齢"
        value={input.retirementAge}
        min={50} max={75} unit="歳"
        onChange={v => setInput({ retirementAge: v })}
      />

      <SliderInput
        label="退職金（見込み）"
        value={input.retirementBonus ?? 0}
        min={0} max={5000} step={100} unit="万円"
        description="退職時に受け取る一時金。0なら「なし」として計算。"
        onChange={v => setInput({ retirementBonus: v })}
      />

      <SliderInput
        label="想定寿命"
        value={input.expectedLifespan}
        min={70} max={100} unit="歳"
        description="長めに設定するほど安全側の試算になります"
        onChange={v => setInput({ expectedLifespan: v })}
      />

      <ReturnModeSelector
        value={input.returnMode}
        onChange={v => setInput({ returnMode: v })}
      />

      {showActions && (
        <div className="space-y-3 pt-2">
          <Button fullWidth size="lg" onClick={handleRunNow}>
            今すぐシミュレーション
          </Button>
          <Button fullWidth variant="secondary" onClick={() => router.push('/input/details')}>
            詳細設定をしてから計算する
          </Button>
        </div>
      )}
    </div>
  );
}
