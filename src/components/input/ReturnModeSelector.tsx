'use client';

import type { ReturnMode } from '@/lib/simulation/types';
import { RETURN_MODE_LABELS, RETURN_MODE_DESCRIPTIONS } from '@/lib/utils/uiConstants';

interface ReturnModeSelectorProps {
  value: ReturnMode;
  onChange: (value: ReturnMode) => void;
}

const modes: ReturnMode[] = ['conservative', 'moderate', 'growth'];

const modeColors: Record<ReturnMode, { selected: string; dot: string }> = {
  conservative: { selected: 'border-blue-400 bg-blue-50 text-blue-700',   dot: 'bg-blue-400' },
  moderate:     { selected: 'border-brand-500 bg-brand-50 text-brand-700', dot: 'bg-brand-500' },
  growth:       { selected: 'border-green-500 bg-green-50 text-green-700', dot: 'bg-green-500' },
};

export default function ReturnModeSelector({ value, onChange }: ReturnModeSelectorProps) {
  return (
    <div className="space-y-2">
      <label className="text-sm font-medium text-gray-700">想定運用スタイル</label>
      <div className="space-y-2">
        {modes.map(mode => (
          <button
            key={mode}
            type="button"
            onClick={() => onChange(mode)}
            className={[
              'w-full flex items-start gap-3 p-3 rounded-xl border-2 text-left transition-colors',
              value === mode
                ? modeColors[mode].selected
                : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300',
            ].join(' ')}
          >
            <span className={`mt-1 flex-shrink-0 w-3 h-3 rounded-full ${modeColors[mode].dot}`} />
            <div>
              <p className="text-sm font-semibold">{RETURN_MODE_LABELS[mode]}</p>
              <p className="text-xs text-gray-500 mt-0.5">{RETURN_MODE_DESCRIPTIONS[mode]}</p>
            </div>
          </button>
        ))}
      </div>
    </div>
  );
}
