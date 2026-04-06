'use client';

import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ReferenceLine,
  ResponsiveContainer,
  Legend,
} from 'recharts';
import type { SimulationResults } from '@/lib/simulation/types';
import { SCENARIO_COLORS, SCENARIO_LABELS } from '@/lib/utils/uiConstants';
import { formatManYen } from '@/lib/utils/formatCurrency';

export interface EventMarker {
  age: number;
  label: string;
}

interface AssetProjectionChartProps {
  results: SimulationResults;
  retirementAge: number;
  eventMarkers?: EventMarker[];
}

interface ChartDataPoint {
  age: number;
  worst: number | null;
  main: number | null;
  upside: number | null;
}

function buildChartData(results: SimulationResults): ChartDataPoint[] {
  const worstTimeline  = results.worst.cashFlowTimeline;
  const mainTimeline   = results.main.cashFlowTimeline;
  const upsideTimeline = results.upside.cashFlowTimeline;

  const maxLen = Math.max(worstTimeline.length, mainTimeline.length, upsideTimeline.length);

  return Array.from({ length: maxLen }, (_, i) => ({
    age:    (worstTimeline[i] ?? mainTimeline[i] ?? upsideTimeline[i]).age,
    worst:  worstTimeline[i]  ? Math.round(worstTimeline[i].assets)  : null,
    main:   mainTimeline[i]   ? Math.round(mainTimeline[i].assets)   : null,
    upside: upsideTimeline[i] ? Math.round(upsideTimeline[i].assets) : null,
  }));
}

function formatYAxis(value: number): string {
  if (Math.abs(value) >= 10000) return `${(value / 10000).toFixed(0)}億`;
  return `${(value / 100).toFixed(0)}百万`;
}

export default function AssetProjectionChart({ results, retirementAge, eventMarkers = [] }: AssetProjectionChartProps) {
  const data = buildChartData(results);

  return (
    <div className="w-full">
      <p className="text-sm font-semibold text-gray-700 mb-3">資産推移グラフ（3シナリオ比較）</p>
      <ResponsiveContainer width="100%" height={260}>
        <AreaChart data={data} margin={{ top: 5, right: 5, left: 0, bottom: 0 }}>
          <defs>
            {(['worst', 'main', 'upside'] as const).map(s => (
              <linearGradient key={s} id={`color-${s}`} x1="0" y1="0" x2="0" y2="1">
                <stop offset="5%"  stopColor={SCENARIO_COLORS[s]} stopOpacity={0.15} />
                <stop offset="95%" stopColor={SCENARIO_COLORS[s]} stopOpacity={0.02} />
              </linearGradient>
            ))}
          </defs>

          <CartesianGrid strokeDasharray="3 3" stroke="#f0f0f0" />
          <XAxis
            dataKey="age"
            tick={{ fontSize: 11, fill: '#9ca3af' }}
            tickFormatter={v => `${v}歳`}
            interval={4}
          />
          <YAxis
            tick={{ fontSize: 10, fill: '#9ca3af' }}
            tickFormatter={formatYAxis}
            width={48}
          />
          <Tooltip
            formatter={(value: number, name: string) => [
              value !== null ? formatManYen(value) : '枯渇',
              SCENARIO_LABELS[name as keyof typeof SCENARIO_LABELS] ?? name,
            ]}
            labelFormatter={label => `${label}歳時点`}
            contentStyle={{ fontSize: 12, borderRadius: 8 }}
          />
          <Legend
            formatter={name => SCENARIO_LABELS[name as keyof typeof SCENARIO_LABELS] ?? name}
            wrapperStyle={{ fontSize: 12 }}
          />

          {/* Zero line */}
          <ReferenceLine y={0} stroke="#dc2626" strokeDasharray="4 4" strokeWidth={1.5} />

          {/* Retirement age reference */}
          <ReferenceLine
            x={retirementAge}
            stroke="#6b7280"
            strokeDasharray="4 4"
            label={{ value: '退職', position: 'insideTopRight', fontSize: 10, fill: '#6b7280' }}
          />

          {/* Additional event markers */}
          {eventMarkers.map((ev, i) => (
            <ReferenceLine
              key={`ev-${i}`}
              x={ev.age}
              stroke="#d97706"
              strokeDasharray="3 3"
              strokeWidth={1}
              label={{ value: ev.label, position: 'insideTopLeft', fontSize: 9, fill: '#d97706' }}
            />
          ))}

          {(['upside', 'main', 'worst'] as const).map(s => (
            <Area
              key={s}
              type="monotone"
              dataKey={s}
              stroke={SCENARIO_COLORS[s]}
              strokeWidth={2}
              fill={`url(#color-${s})`}
              dot={false}
              connectNulls={false}
            />
          ))}
        </AreaChart>
      </ResponsiveContainer>
    </div>
  );
}
