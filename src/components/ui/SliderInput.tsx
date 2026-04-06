'use client';

import { useState, useEffect, useRef, useCallback } from 'react';

interface SliderInputProps {
  label: string;
  value: number;
  min: number;
  max: number;
  step?: number;
  unit?: string;
  description?: string;
  onChange: (value: number) => void;
}

export default function SliderInput({
  label,
  value,
  min,
  max,
  step = 1,
  unit = '',
  description,
  onChange,
}: SliderInputProps) {
  const percent = ((value - min) / (max - min)) * 100;

  // ── Local string state for the number input ──
  // This allows typing "5", "50", "500" without intermediate clamping.
  const [localText, setLocalText] = useState<string>(String(value));
  const composingRef = useRef(false);   // true during IME composition
  const committingRef = useRef(false);  // true right after we commit → suppress sync

  // Sync local text from external value changes (e.g. slider drag)
  useEffect(() => {
    if (committingRef.current) {
      committingRef.current = false;
      return;
    }
    setLocalText(String(value));
  }, [value]);

  /** Commit: parse, clamp, snap, call onChange */
  const commit = useCallback((raw: string) => {
    const stripped = raw.replace(/,/g, '').replace(/\s/g, '');
    const parsed = Number(stripped);

    if (stripped === '' || isNaN(parsed)) {
      // Revert to current value
      setLocalText(String(value));
      return;
    }

    const clamped = Math.min(max, Math.max(min, parsed));
    const snapped = Math.round(clamped / step) * step;
    // Avoid floating-point artefacts
    const rounded = Math.round(snapped * 1000) / 1000;

    committingRef.current = true;
    setLocalText(String(rounded));
    onChange(rounded);
  }, [value, min, max, step, onChange]);

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between gap-2">
        <label className="text-sm font-medium text-gray-700 flex-1">{label}</label>
        {/* Editable number input — commit on blur / Enter */}
        <div className="flex items-center gap-1">
          <input
            type="text"
            inputMode="decimal"
            value={localText}
            onChange={e => setLocalText(e.target.value)}
            onCompositionStart={() => { composingRef.current = true; }}
            onCompositionEnd={e => {
              composingRef.current = false;
              setLocalText(e.currentTarget.value);
            }}
            onBlur={() => commit(localText)}
            onKeyDown={e => {
              if (e.key === 'Enter' && !composingRef.current) {
                e.preventDefault();
                commit(localText);
                (e.target as HTMLInputElement).blur();
              }
            }}
            onFocus={e => e.target.select()}
            className="w-24 text-right text-base font-bold text-brand-700 border border-gray-200
              rounded-lg px-2 py-1.5 focus:outline-none focus:ring-2 focus:ring-brand-400
              bg-white"
          />
          {unit && <span className="text-sm text-gray-500 whitespace-nowrap">{unit}</span>}
        </div>
      </div>

      {description && (
        <p className="text-xs text-gray-500">{description}</p>
      )}

      <input
        type="range"
        min={min}
        max={max}
        step={step}
        value={value}
        onChange={e => onChange(Number(e.target.value))}
        className="w-full h-2 bg-gray-200 rounded-full appearance-none cursor-pointer
          [&::-webkit-slider-thumb]:appearance-none
          [&::-webkit-slider-thumb]:w-5
          [&::-webkit-slider-thumb]:h-5
          [&::-webkit-slider-thumb]:rounded-full
          [&::-webkit-slider-thumb]:bg-brand-600
          [&::-webkit-slider-thumb]:shadow-md
          [&::-webkit-slider-thumb]:cursor-pointer"
        style={{
          background: `linear-gradient(to right, #0284c7 ${percent}%, #e5e7eb ${percent}%)`,
        }}
      />

      <div className="flex justify-between text-xs text-gray-400">
        <span>{min.toLocaleString('ja-JP')}{unit}</span>
        <span>{max.toLocaleString('ja-JP')}{unit}</span>
      </div>
    </div>
  );
}
