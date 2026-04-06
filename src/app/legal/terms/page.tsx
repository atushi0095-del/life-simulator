import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';

export default function TermsPage() {
  return (
    <AppShell>
      <PageHeader title="利用規約" backHref="/settings" />

      <div className="space-y-5 text-gray-700 pb-6">

        <p className="text-xs text-gray-500 leading-relaxed">
          最終更新：2025年　本規約は予告なく変更される場合があります。
        </p>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第1条 サービスの目的と対象</h2>
          <p className="text-sm leading-relaxed mb-2">
            本アプリ「人生資金シミュレーター」（以下「本サービス」）は、
            ユーザーが将来の資産計画について一般的な試算・可視化を行うための情報提供ツールです。
          </p>
          <p className="text-sm leading-relaxed mb-2">
            本サービスは、日本国内に居住する成年（18歳以上）の方を主な対象としています。
            未成年の方がご利用になる場合は、保護者の同意を得た上でご使用ください。
          </p>
          <div className="bg-amber-50 border border-amber-200 rounded-xl px-4 py-3">
            <p className="text-xs text-amber-800 font-semibold mb-1">本サービスが行わないこと</p>
            <ul className="text-xs text-amber-700 space-y-1 leading-relaxed list-disc list-inside">
              <li>特定の金融商品・投資戦略の推奨・勧誘</li>
              <li>税務・法務・相続計画に関する個別助言</li>
              <li>保険商品・不動産・暗号資産等の紹介・斡旋</li>
              <li>金融機関との仲介・送金・残高管理</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第2条 利用者の責任</h2>
          <p className="text-sm leading-relaxed mb-2">
            利用者は、本サービスの試算結果を参考情報として位置づけ、
            実際の資産運用・ライフプランに関する意思決定はご自身の責任において行うものとします。
          </p>
          <p className="text-sm leading-relaxed">
            本サービスへの入力情報の正確性は利用者自身が管理するものとし、
            入力誤りによる不適切な試算結果について提供者は責任を負いません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第3条 禁止事項</h2>
          <ul className="text-sm leading-relaxed list-disc list-inside space-y-1.5">
            <li>本サービスのソースコード・UI・計算ロジックの無断複製・転用・商業利用</li>
            <li>本サービスへの不正アクセス、サーバーへの過負荷をかける行為</li>
            <li>本サービスを通じた第三者への誹謗中傷・差別的表現の拡散</li>
            <li>法令・公序良俗に違反する目的での利用</li>
            <li>虚偽の情報を入力することで他者を誤解させる行為</li>
          </ul>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第4条 知的財産権</h2>
          <p className="text-sm leading-relaxed">
            本サービスに関する著作権・商標権その他の知的財産権は、
            提供者または正当な権利者に帰属します。
            利用者は、個人的・非商業的な目的に限り本サービスを利用できます。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第5条 サービスの中断・終了</h2>
          <p className="text-sm leading-relaxed">
            提供者は、システムメンテナンス・障害・その他の事由により、
            事前の通知なく本サービスの全部または一部を中断・変更・終了する場合があります。
            これにより利用者に生じた損害について、提供者は一切の責任を負いません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第6条 免責</h2>
          <p className="text-sm leading-relaxed">
            本サービスの提供者は、試算結果の正確性・完全性・適時性を保証しません。
            本サービスを利用した結果生じたいかなる直接的・間接的損害についても、
            提供者は責任を負いません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">第7条 準拠法・管轄</h2>
          <p className="text-sm leading-relaxed">
            本規約は日本法に準拠します。
            本サービスに関して紛争が生じた場合、提供者の所在地を管轄する裁判所を
            第一審の専属的合意管轄裁判所とします。
          </p>
        </section>

      </div>
    </AppShell>
  );
}
