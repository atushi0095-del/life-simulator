'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import FamilySummaryCard from '@/components/summary/FamilySummaryCard';
import ShareButton from '@/components/summary/ShareButton';
import { useSimulation } from '@/context/SimulationContext';
import { NOTE_CATEGORY_LABELS, NOTE_CATEGORY_EMOJI } from '@/lib/utils/uiConstants';
import type { FamilyNote, NoteCategory } from '@/lib/simulation/types';

type NoteFilter = 'all' | 'recent3' | 'selected';

export default function SummaryPage() {
  const router = useRouter();
  const { state, dispatch } = useSimulation();
  const { results, monteCarlo, input, notes, spendableMargins, sustainableBudgets } = state;

  const [noteFilter, setNoteFilter] = useState<NoteFilter>('selected');

  useEffect(() => {
    if (!results || !monteCarlo) router.replace('/input');
  }, [results, monteCarlo, router]);

  if (!results || !monteCarlo) return null;

  // Filter notes for inclusion in summary
  const sortedNotes = [...notes].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  );

  let includedNotes: FamilyNote[];
  if (noteFilter === 'selected') {
    includedNotes = sortedNotes.filter(n => n.includeInSummary !== false); // default true
  } else if (noteFilter === 'recent3') {
    includedNotes = sortedNotes.slice(0, 3);
  } else {
    includedNotes = sortedNotes;
  }

  function toggleNoteInclusion(id: string) {
    const note = notes.find(n => n.id === id);
    if (!note) return;
    dispatch({
      type: 'UPDATE_NOTE',
      payload: { id, patch: { includeInSummary: note.includeInSummary === false ? true : false } },
    });
  }

  return (
    <AppShell>
      <PageHeader
        title="家族共有サマリー"
        backHref="/results"
        subtitle="家族との話し合いにご活用ください"
      />

      {/* Pre-share disclaimer */}
      <div className="bg-amber-50 border border-amber-200 rounded-2xl p-4 mb-4">
        <p className="text-xs text-amber-800 leading-relaxed">
          <span className="font-semibold">共有前にご確認ください：</span>
          このサマリーは一般的な試算に基づく話し合いのたたき台です。
          「余力候補」「追加支出可能額」は試算上の参考値であり、実際の支出を保証するものではありません。
        </p>
      </div>

      {/* Note filter */}
      {notes.length > 0 && (
        <div className="mb-4 space-y-3">
          <div className="flex items-center gap-2">
            <p className="text-xs text-gray-500 flex-shrink-0">メモの表示：</p>
            <div className="flex bg-white border border-gray-200 rounded-lg overflow-hidden text-xs">
              {[
                { key: 'selected' as const, label: '選択メモ' },
                { key: 'recent3' as const,  label: '直近3件' },
                { key: 'all' as const,      label: `すべて(${notes.length})` },
              ].map(opt => (
                <button
                  key={opt.key}
                  type="button"
                  onClick={() => setNoteFilter(opt.key)}
                  className={`px-3 py-1.5 font-medium transition-colors ${
                    noteFilter === opt.key ? 'bg-brand-600 text-white' : 'text-gray-500 hover:bg-gray-50'
                  }`}
                >
                  {opt.label}
                </button>
              ))}
            </div>
          </div>

          {/* Per-note selection toggles (only when filter is "selected") */}
          {noteFilter === 'selected' && (
            <div className="space-y-1.5 bg-gray-50 rounded-xl p-3">
              <p className="text-xs text-gray-500 mb-1">サマリーに含めるメモを選択：</p>
              {sortedNotes.map(note => {
                const included = note.includeInSummary !== false;
                return (
                  <button
                    key={note.id}
                    type="button"
                    onClick={() => toggleNoteInclusion(note.id)}
                    className={`w-full text-left flex items-start gap-2 rounded-lg px-3 py-2 text-xs transition-colors ${
                      included ? 'bg-white border border-brand-200' : 'bg-white border border-gray-100 opacity-60'
                    }`}
                  >
                    <span className={`mt-0.5 flex-shrink-0 w-4 h-4 rounded border flex items-center justify-center text-[10px] ${
                      included ? 'bg-brand-600 border-brand-600 text-white' : 'border-gray-300'
                    }`}>
                      {included ? '✓' : ''}
                    </span>
                    <div className="flex-1 min-w-0">
                      <span className="text-gray-400">
                        {NOTE_CATEGORY_EMOJI[note.category as NoteCategory]}{' '}
                        {NOTE_CATEGORY_LABELS[note.category as NoteCategory]}:
                      </span>{' '}
                      <span className="text-gray-600 line-clamp-1">{note.content}</span>
                    </div>
                  </button>
                );
              })}
            </div>
          )}
        </div>
      )}

      <div className="mb-4">
        <ShareButton />
      </div>

      <FamilySummaryCard
        input={input}
        results={results}
        mc={monteCarlo}
        notes={includedNotes}
        spendableMargins={spendableMargins}
        sustainableBudgets={sustainableBudgets}
      />

      <div className="mt-6 space-y-3">
        <button
          type="button"
          onClick={() => window.print()}
          className="w-full py-3 bg-gray-100 hover:bg-gray-200 rounded-xl text-sm text-gray-700 font-medium transition-colors print:hidden"
        >
          🖨️ 印刷する（PDF として保存も可能）
        </button>
        <button
          type="button"
          onClick={() => router.push('/notes')}
          className="w-full text-center py-3 text-sm text-brand-600 font-medium"
        >
          話し合いメモを記録・編集する →
        </button>
      </div>
    </AppShell>
  );
}
