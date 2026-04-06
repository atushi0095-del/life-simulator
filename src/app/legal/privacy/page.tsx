import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';

export default function PrivacyPage() {
  return (
    <AppShell>
      <PageHeader title="プライバシーポリシー" backHref="/settings" />

      <div className="space-y-5 text-gray-700 pb-6">

        <p className="text-xs text-gray-500 leading-relaxed">
          最終更新：2025年　本ポリシーは予告なく変更される場合があります。
        </p>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">1. 運営者について</h2>
          <p className="text-sm leading-relaxed">
            本アプリ「人生資金シミュレーター」は個人が開発・運営するWebアプリケーションです。
            本ポリシーに関するお問い合わせは、アプリ内の設定ページよりご連絡ください。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">2. 取り扱うデータの範囲</h2>
          <p className="text-sm leading-relaxed mb-2">
            本サービスが取り扱うデータは以下のとおりです。
          </p>
          <div className="space-y-2">
            {[
              { cat: '入力データ', items: '年齢、家族構成、金融資産額、年収、支出額、退職予定年齢、想定寿命、運用スタイル' },
              { cat: '試算結果', items: 'シミュレーション計算結果（3シナリオ・モンテカルロ）' },
              { cat: 'メモ', items: 'ユーザーが入力した話し合いメモ（カテゴリ・本文・日時）' },
            ].map(row => (
              <div key={row.cat} className="bg-gray-50 rounded-xl px-4 py-3">
                <p className="text-xs font-semibold text-gray-700 mb-0.5">{row.cat}</p>
                <p className="text-xs text-gray-500 leading-relaxed">{row.items}</p>
              </div>
            ))}
          </div>
          <p className="text-xs text-gray-500 mt-2 leading-relaxed">
            氏名・住所・電話番号・メールアドレス等、個人を特定できる情報の入力は求めません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">3. 利用目的</h2>
          <ul className="text-sm leading-relaxed list-disc list-inside space-y-1">
            <li>シミュレーション計算の実行・結果表示</li>
            <li>入力内容のデバイス内保存（セッション継続のため）</li>
            <li>家族共有サマリーの生成（ユーザーが明示的に操作した場合のみ）</li>
          </ul>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">4. データの保存場所・外部送信</h2>
          <div className="bg-green-50 border border-green-200 rounded-xl px-4 py-3 mb-2">
            <p className="text-sm font-semibold text-green-800 mb-1">ローカルファースト設計</p>
            <p className="text-xs text-green-700 leading-relaxed">
              入力データ・試算結果・メモはすべてお使いのデバイスのブラウザストレージ（localStorage）に保存されます。
              外部サーバーへの自動送信は行いません。
            </p>
          </div>
          <p className="text-xs text-gray-500 leading-relaxed">
            将来的にアカウント連携機能（Supabase）を追加する場合、
            明示的なサインアップ・ログイン操作を経た上でのみデータを同期します。
            その際はポリシーを更新し、事前にご案内します。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">5. 第三者提供</h2>
          <p className="text-sm leading-relaxed">
            本サービスは、ユーザーデータを広告配信・マーケティング目的で第三者に提供することはありません。
            法令に基づく開示要求があった場合を除き、データを外部に提供しません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">6. 外部サービスの利用</h2>
          <p className="text-sm leading-relaxed">
            現時点ではアクセス解析ツール・広告配信ツールは使用していません。
            将来的に導入する場合は本ポリシーを更新します。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">7. データの保存期間・削除方法</h2>
          <p className="text-sm leading-relaxed mb-2">
            データはブラウザのストレージに保存されるため、保存期間はブラウザ設定に依存します。
            以下の方法でいつでも削除できます。
          </p>
          <ul className="text-sm leading-relaxed list-disc list-inside space-y-1">
            <li>設定ページの「データをすべてリセット」ボタン</li>
            <li>ブラウザの「サイトデータ・キャッシュの削除」機能</li>
          </ul>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">8. セキュリティ</h2>
          <p className="text-sm leading-relaxed">
            本サービスはHTTPS通信を前提として提供します。
            ただし、ブラウザのlocalStorageは同一デバイスを共有する他のユーザーから
            アクセスされる可能性があります。共有端末でのご利用はご注意ください。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">9. 未成年の利用</h2>
          <p className="text-sm leading-relaxed">
            本サービスは18歳未満の方の単独利用を想定していません。
            18歳未満の方がご利用になる場合は保護者の同意・監督のもとでお使いください。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">10. ポリシーの変更</h2>
          <p className="text-sm leading-relaxed">
            本ポリシーは、法令の改正やサービス内容の変更に伴い予告なく更新する場合があります。
            重要な変更がある場合はアプリ内でお知らせします。
          </p>
        </section>

      </div>
    </AppShell>
  );
}
