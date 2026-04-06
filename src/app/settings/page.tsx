'use client';

import { useState } from 'react';
import { useRouter } from 'next/navigation';
import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import Card from '@/components/ui/Card';
import Button from '@/components/ui/Button';
import { useSimulation, DEFAULT_INPUT } from '@/context/SimulationContext';
import { localStore } from '@/lib/storage/localStore';

/* ── Confirmation Modal ────────────────────────────────────── */
function ResetConfirmModal({
  onConfirm,
  onCancel,
}: {
  onConfirm: () => void;
  onCancel: () => void;
}) {
  return (
    /* Overlay */
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/50 px-6"
      onClick={onCancel}
    >
      {/* Dialog */}
      <div
        className="w-full max-w-sm bg-white rounded-2xl shadow-xl p-6 space-y-4"
        onClick={e => e.stopPropagation()}
      >
        <div className="text-center">
          <div className="text-3xl mb-2">⚠️</div>
          <h2 className="text-base font-bold text-gray-900">データをリセットしますか？</h2>
          <p className="text-sm text-gray-500 mt-2 leading-relaxed">
            入力内容・試算結果・すべてのメモが削除されます。
            この操作は取り消せません。
          </p>
        </div>
        <div className="space-y-2">
          <Button fullWidth variant="danger" onClick={onConfirm}>
            すべて削除する
          </Button>
          <Button fullWidth variant="ghost" onClick={onCancel}>
            キャンセル
          </Button>
        </div>
      </div>
    </div>
  );
}

export default function SettingsPage() {
  const router = useRouter();
  const { dispatch } = useSimulation();
  const [showModal, setShowModal] = useState(false);

  function handleConfirmReset() {
    localStore.clear();
    dispatch({
      type: 'LOAD_STORED',
      payload: { input: DEFAULT_INPUT, results: null, monteCarlo: null, notes: [] },
    });
    setShowModal(false);
    router.push('/');
  }

  return (
    <AppShell>
      <PageHeader title="設定" />

      {/* Data section */}
      <Card className="mb-4">
        <p className="text-sm font-semibold text-gray-700 mb-1">データ管理</p>
        <p className="text-xs text-gray-500 mb-4 leading-relaxed">
          入力データはこのデバイスのブラウザ（localStorage）にのみ保存されています。
          外部サーバーへは送信されません。ブラウザのキャッシュをクリアするとデータが削除されます。
        </p>

        <div className="bg-gray-50 rounded-xl px-4 py-3 mb-4 space-y-1.5">
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-500">保存場所</span>
            <span className="font-medium text-gray-700">ブラウザ（localStorage）</span>
          </div>
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-500">外部送信</span>
            <span className="font-medium text-green-700">なし</span>
          </div>
          <div className="flex items-center justify-between text-xs">
            <span className="text-gray-500">アカウント連携</span>
            <span className="font-medium text-gray-500">未実装（開発中）</span>
          </div>
        </div>

        <Button
          fullWidth
          variant="secondary"
          onClick={() => setShowModal(true)}
        >
          データをすべてリセット
        </Button>
        <p className="text-xs text-gray-400 text-center mt-2">
          入力・結果・メモがすべて削除されます
        </p>
      </Card>

      {/* Legal links */}
      <Card className="mb-4">
        <p className="text-sm font-semibold text-gray-700 mb-3">法的情報</p>
        <div className="divide-y divide-gray-100">
          {[
            { label: '利用規約',             href: '/legal/terms' },
            { label: 'プライバシーポリシー', href: '/legal/privacy' },
            { label: '免責事項',             href: '/legal/disclaimer' },
          ].map(item => (
            <button
              key={item.href}
              type="button"
              onClick={() => router.push(item.href)}
              className="w-full flex items-center justify-between py-3 text-sm text-gray-700 hover:text-brand-600 transition-colors"
            >
              {item.label}
              <svg className="w-4 h-4 text-gray-400" fill="none" stroke="currentColor" viewBox="0 0 24 24">
                <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M9 5l7 7-7 7" />
              </svg>
            </button>
          ))}
        </div>
      </Card>

      {/* App info */}
      <Card>
        <p className="text-sm font-semibold text-gray-700 mb-3">アプリについて</p>
        <div className="space-y-2 text-xs text-gray-500">
          <div className="flex items-center justify-between">
            <span>バージョン</span>
            <span className="font-medium text-gray-700">v0.1.0</span>
          </div>
          <div className="flex items-center justify-between">
            <span>最終更新</span>
            <span className="font-medium text-gray-700">2026年</span>
          </div>
          <div className="flex items-center justify-between">
            <span>シミュレーション試算回数</span>
            <span className="font-medium text-gray-700">1,000回</span>
          </div>
        </div>
        <div className="mt-4 pt-3 border-t border-gray-100">
          <p className="text-xs text-gray-400 leading-relaxed">
            本アプリは一般的なシミュレーションツールです。
            特定の金融商品・投資・税務・法務上の助言は行いません。
            試算結果は話し合いの参考情報としてのみご活用ください。
            最終的な意思決定はご自身の責任において行ってください。
          </p>
        </div>
      </Card>

      {/* Reset confirmation modal */}
      {showModal && (
        <ResetConfirmModal
          onConfirm={handleConfirmReset}
          onCancel={() => setShowModal(false)}
        />
      )}
    </AppShell>
  );
}
