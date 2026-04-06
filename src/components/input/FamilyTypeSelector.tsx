'use client';

import type { FamilyType } from '@/lib/simulation/types';
import { FAMILY_TYPE_LABELS } from '@/lib/utils/uiConstants';

interface FamilyTypeSelectorProps {
  value: FamilyType;
  onChange: (value: FamilyType) => void;
}

const options: { value: FamilyType; icon: string; desc: string }[] = [
  { value: 'single',               icon: '👤', desc: '自分ひとり' },
  { value: 'couple',               icon: '👫', desc: '2人世帯' },
  { value: 'family_with_children', icon: '👨‍👩‍👧', desc: '子どもがいる' },
];

export default function FamilyTypeSelector({ value, onChange }: FamilyTypeSelectorProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-gray-700">家族タイプ</label>
      <div className="grid grid-cols-3 gap-2">
        {options.map(opt => (
          <button
            key={opt.value}
            type="button"
            onClick={() => onChange(opt.value)}
            className={[
              'flex flex-col items-center gap-1 p-3 rounded-xl border-2 transition-colors',
              value === opt.value
                ? 'border-brand-500 bg-brand-50 text-brand-700'
                : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300',
            ].join(' ')}
          >
            <span className="text-2xl">{opt.icon}</span>
            <span className="text-xs font-semibold">{FAMILY_TYPE_LABELS[opt.value]}</span>
            <span className="text-[10px] text-gray-400">{opt.desc}</span>
          </button>
        ))}
      </div>
    </div>
  );
}
