import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';

export default function DisclaimerPage() {
  return (
    <AppShell>
      <PageHeader title="免責事項" backHref="/settings" />

      <div className="space-y-5 text-gray-700 pb-6">

        {/* Top warning */}
        <div className="bg-amber-50 border border-amber-200 rounded-xl p-4">
          <p className="text-sm font-semibold text-amber-800 mb-1">重要なお知らせ</p>
          <p className="text-xs text-amber-700 leading-relaxed">
            本アプリは金融アドバイスの提供を目的とするものではありません。
            以下の免責事項を必ずお読みください。
          </p>
        </div>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">1. 一般的な試算ツールであること</h2>
          <p className="text-sm leading-relaxed">
            本サービスが提供するシミュレーション結果は、ユーザーが入力した数値に基づく
            一般的な概算です。将来の実際の資産残高・収支を予測・保証するものではありません。
            試算はあくまで「話し合いのたたき台」としての参考情報です。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">2. モデルの前提・仮定について</h2>
          <p className="text-sm leading-relaxed mb-2">
            本サービスの計算モデルは以下の簡略化された前提を採用しています。
          </p>
          <ul className="text-sm leading-relaxed list-disc list-inside space-y-1.5 text-gray-600">
            <li>税・社会保険料の手取り推計は年収の約75%を一律適用（実際の税率・控除は個人差が大きい）</li>
            <li>年金収入は家族構成に基づく概算月額を65歳から適用（実際の年金額は個人の加入履歴による）</li>
            <li>インフレ率・運用リターン・収入成長率はシナリオ別の固定値または確率分布で近似</li>
            <li>モンテカルロ試算は運用リターンのみをランダム化（支出・収入変動は含まない）</li>
            <li>退職後の医療費・介護費の個別増加は標準的な概算値のみ反映</li>
          </ul>
          <p className="text-xs text-gray-500 mt-2 leading-relaxed">
            これらの仮定が実際の状況と異なる場合、試算結果は大きく変わる可能性があります。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">3. 制度変更・外部環境リスク</h2>
          <p className="text-sm leading-relaxed">
            将来の税制改正・年金制度の変更・社会保険料率の変動・物価の変動・金利の変化等により、
            実際の資産推移は試算と大きく乖離する可能性があります。
            本サービスはこれらの将来の変化を予測・反映することを保証しません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">4. 金融商品・投資の推奨をしないこと</h2>
          <p className="text-sm leading-relaxed">
            本サービスは特定の金融商品・銘柄・投資信託・保険商品・不動産・暗号資産等の
            購入・売却・解約を推奨するものではありません。
            「余力候補」の表示は試算上の参考値であり、特定の投資・消費・贈与行為を
            勧めるものではありません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">5. 税務・法務上の助言ではないこと</h2>
          <p className="text-sm leading-relaxed mb-2">
            試算には税金・社会保険料の概算が含まれますが、
            これらは簡便な近似であり、税務・法務上の正確な計算や個別助言ではありません。
          </p>
          <div className="bg-blue-50 border border-blue-200 rounded-xl px-4 py-3">
            <p className="text-xs text-blue-800 font-semibold mb-1">専門家へのご相談をお勧めする場面</p>
            <ul className="text-xs text-blue-700 space-y-1 leading-relaxed list-disc list-inside">
              <li>相続・贈与・遺言の計画</li>
              <li>税務最適化（ふるさと納税・NISA・iDeCo以外の複雑な税務）</li>
              <li>法人設立・事業承継</li>
              <li>離婚・成年後見等の法的手続き</li>
            </ul>
          </div>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">6. 家族間の意思決定について</h2>
          <p className="text-sm leading-relaxed">
            本サービスの家族共有サマリー機能は、家族内の話し合いを促進するための補助ツールです。
            サマリーの内容は一般的な試算に基づくものであり、家族間の金銭的な権利・義務関係を
            定めるものではありません。
            資産配分・扶養・相続に関する実際の合意は、必要に応じて専門家を交えてご判断ください。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">7. 「余力候補」「追加支出可能額」について</h2>
          <p className="text-sm leading-relaxed">
            本サービスで表示する「余力候補」「追加支出可能額」「逆算シミュレーション結果」は、
            試算上の参考値であり、実際に支出しても問題ない金額を保証するものではありません。
            安全余裕を控除した数値を表示していますが、将来の制度変更・運用成績・物価変動等により
            実際の支出可能額は大きく変わる可能性があります。
            これらの数値を根拠として過度な支出を行うことは推奨しません。
          </p>
        </section>

        <section>
          <h2 className="text-base font-bold text-gray-900 mb-2">8. 最終判断はご自身の責任であること</h2>
          <p className="text-sm leading-relaxed">
            本サービスの試算結果はあくまで参考情報です。
            資産運用・ライフプランに関する最終的な意思決定は、ご自身の責任において行ってください。
            本サービスの利用により生じた直接的・間接的損害等について、提供者は一切の責任を負いません。
          </p>
        </section>

      </div>
    </AppShell>
  );
}
