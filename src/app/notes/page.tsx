'use client';

import { useState, useEffect } from 'react';
import { useRouter } from 'next/navigation';
import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import Button from '@/components/ui/Button';
import Badge from '@/components/ui/Badge';
import Card from '@/components/ui/Card';
import { useSimulation } from '@/context/SimulationContext';
import { NOTE_CATEGORY_LABELS, NOTE_CATEGORY_EMOJI } from '@/lib/utils/uiConstants';
import type { NoteCategory } from '@/lib/simulation/types';

const ALL_CATEGORIES: NoteCategory[] = ['education', 'care', 'housing', 'support', 'dreams'];

/* ── Toast ─────────────────────────────────────────────────── */
function Toast({ message, onHide }: { message: string; onHide: () => void }) {
  useEffect(() => {
    const t = setTimeout(onHide, 2200);
    return () => clearTimeout(t);
  }, [onHide]);

  return (
    <div
      className="fixed bottom-24 left-1/2 -translate-x-1/2 z-50
        bg-gray-800 text-white text-sm font-medium
        px-5 py-2.5 rounded-full shadow-lg
        animate-fade-in-up pointer-events-none"
    >
      {message}
    </div>
  );
}

export default function NotesPage() {
  const router = useRouter();
  const { state, dispatch } = useSimulation();
  const { notes } = state;

  const [filterCategory, setFilterCategory] = useState<NoteCategory | 'all'>('all');
  const [newCategory, setNewCategory]        = useState<NoteCategory>('education');
  const [newContent, setNewContent]          = useState('');
  const [editingId, setEditingId]            = useState<string | null>(null);
  const [editContent, setEditContent]        = useState('');
  const [toast, setToast]                    = useState<string | null>(null);

  // Sort notes by updatedAt descending
  const sortedNotes = [...notes].sort(
    (a, b) => new Date(b.updatedAt).getTime() - new Date(a.updatedAt).getTime(),
  );

  const filteredNotes = filterCategory === 'all'
    ? sortedNotes
    : sortedNotes.filter(n => n.category === filterCategory);

  function showToast(msg: string) {
    setToast(msg);
  }

  function handleAddNote() {
    if (!newContent.trim()) return;
    dispatch({ type: 'ADD_NOTE', payload: { category: newCategory, content: newContent.trim() } });
    setNewContent('');
    showToast('メモを保存しました ✓');
  }

  function handleStartEdit(id: string, content: string) {
    setEditingId(id);
    setEditContent(content);
  }

  function handleSaveEdit(id: string) {
    if (editContent.trim()) {
      dispatch({ type: 'UPDATE_NOTE', payload: { id, patch: { content: editContent.trim() } } });
      showToast('メモを更新しました ✓');
    }
    setEditingId(null);
  }

  function handleDelete(id: string) {
    dispatch({ type: 'DELETE_NOTE', payload: { id } });
    showToast('メモを削除しました');
  }

  const canSave = newContent.trim().length > 0;

  return (
    <AppShell>
      <PageHeader
        title="家族のメモ"
        subtitle="話し合いたいことや決まったことを記録しましょう"
      />

      {/* 家族共有サマリーへのショートカット */}
      <button
        type="button"
        onClick={() => router.push('/summary')}
        className="w-full flex items-center justify-between bg-brand-50 border border-brand-200 rounded-2xl px-4 py-3 mb-4 hover:bg-brand-100 transition-colors"
      >
        <div className="flex items-center gap-2">
          <span className="text-xl">👨‍👩‍👧</span>
          <div className="text-left">
            <p className="text-sm font-semibold text-brand-700">家族共有サマリーを見る</p>
            <p className="text-xs text-brand-500">メモと試算結果をまとめて家族で共有</p>
          </div>
        </div>
        <svg className="w-4 h-4 text-brand-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
          <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
        </svg>
      </button>

      {/* New note form */}
      <Card className="mb-4">
        <div className="space-y-3">
          <p className="text-sm font-semibold text-gray-700">メモを追加</p>

          {/* Category picker */}
          <div className="flex flex-wrap gap-2">
            {ALL_CATEGORIES.map(cat => (
              <button
                key={cat}
                type="button"
                onClick={() => setNewCategory(cat)}
                className={[
                  'flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors',
                  newCategory === cat
                    ? 'border-brand-500 bg-brand-50 text-brand-700'
                    : 'border-gray-200 bg-white text-gray-600 hover:border-gray-300',
                ].join(' ')}
              >
                {NOTE_CATEGORY_EMOJI[cat]} {NOTE_CATEGORY_LABELS[cat]}
              </button>
            ))}
          </div>

          {/* Textarea */}
          <textarea
            value={newContent}
            onChange={e => setNewContent(e.target.value)}
            placeholder={`${NOTE_CATEGORY_LABELS[newCategory]}についてメモを入力...`}
            rows={3}
            className={[
              'w-full rounded-xl border p-3 text-sm resize-none transition-colors',
              'focus:outline-none focus:ring-2 focus:ring-brand-500',
              canSave ? 'border-brand-300 bg-white' : 'border-gray-200 bg-gray-50',
            ].join(' ')}
          />

          {/* Character hint */}
          {newContent.length > 0 && (
            <p className="text-xs text-gray-400 -mt-1 text-right">
              {newContent.trim().length}文字
            </p>
          )}

          <Button
            fullWidth
            onClick={handleAddNote}
            disabled={!canSave}
          >
            保存する
          </Button>
        </div>
      </Card>

      {/* Filter bar */}
      {notes.length > 0 && (
        <div className="flex gap-2 mb-4 overflow-x-auto pb-1">
          <button
            type="button"
            onClick={() => setFilterCategory('all')}
            className={[
              'flex-shrink-0 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors',
              filterCategory === 'all'
                ? 'border-gray-600 bg-gray-700 text-white'
                : 'border-gray-200 text-gray-600 hover:bg-gray-50',
            ].join(' ')}
          >
            すべて ({notes.length})
          </button>
          {ALL_CATEGORIES.map(cat => {
            const count = notes.filter(n => n.category === cat).length;
            if (count === 0) return null;
            return (
              <button
                key={cat}
                type="button"
                onClick={() => setFilterCategory(cat)}
                className={[
                  'flex-shrink-0 flex items-center gap-1 px-3 py-1.5 rounded-full text-xs font-medium border transition-colors',
                  filterCategory === cat
                    ? 'border-brand-500 bg-brand-50 text-brand-700'
                    : 'border-gray-200 text-gray-600 hover:bg-gray-50',
                ].join(' ')}
              >
                {NOTE_CATEGORY_EMOJI[cat]} {NOTE_CATEGORY_LABELS[cat]}
                <span className="ml-0.5 text-[10px] opacity-70">({count})</span>
              </button>
            );
          })}
        </div>
      )}

      {/* Note list */}
      {filteredNotes.length === 0 ? (
        <div className="text-center py-12">
          <div className="text-4xl mb-3">📝</div>
          <p className="text-gray-500 text-sm font-medium">
            {filterCategory === 'all' ? 'まだメモがありません' : 'このカテゴリのメモはありません'}
          </p>
          <p className="text-xs text-gray-400 mt-1">
            {filterCategory === 'all'
              ? '上のフォームからメモを追加してみましょう'
              : '別のカテゴリを選択するか、新しいメモを追加しましょう'}
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filteredNotes.map(note => (
            <Card key={note.id} className="relative">
              <div className="flex items-start justify-between gap-2 mb-2">
                <Badge variant="neutral">
                  {NOTE_CATEGORY_EMOJI[note.category as NoteCategory]}{' '}
                  {NOTE_CATEGORY_LABELS[note.category as NoteCategory]}
                </Badge>
                <div className="flex items-center gap-3">
                  <button
                    type="button"
                    onClick={() => handleStartEdit(note.id, note.content)}
                    className="text-xs text-gray-400 hover:text-brand-600 transition-colors"
                  >
                    編集
                  </button>
                  <button
                    type="button"
                    onClick={() => handleDelete(note.id)}
                    className="text-xs text-gray-400 hover:text-red-500 transition-colors"
                  >
                    削除
                  </button>
                </div>
              </div>

              {editingId === note.id ? (
                <div className="space-y-2">
                  <textarea
                    value={editContent}
                    onChange={e => setEditContent(e.target.value)}
                    rows={3}
                    autoFocus
                    className="w-full rounded-xl border border-brand-300 p-2 text-sm resize-none
                      focus:outline-none focus:ring-2 focus:ring-brand-500"
                  />
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      onClick={() => handleSaveEdit(note.id)}
                      disabled={!editContent.trim()}
                    >
                      保存
                    </Button>
                    <Button size="sm" variant="ghost" onClick={() => setEditingId(null)}>
                      キャンセル
                    </Button>
                  </div>
                </div>
              ) : (
                <p className="text-sm text-gray-700 whitespace-pre-wrap leading-relaxed">
                  {note.content}
                </p>
              )}

              <p className="mt-2 text-[10px] text-gray-300">
                更新: {new Date(note.updatedAt).toLocaleDateString('ja-JP', {
                  year: 'numeric', month: 'short', day: 'numeric',
                })}
              </p>
            </Card>
          ))}
        </div>
      )}

      {/* Toast */}
      {toast && <Toast message={toast} onHide={() => setToast(null)} />}
    </AppShell>
  );
}
