import Link from 'next/link';
import AppShell from '@/components/layout/AppShell';

export default function TopPage() {
  return (
    <AppShell>
      {/* Hero */}
      <section className="text-center py-8">
        <div className="inline-flex items-center gap-2 bg-brand-50 border border-brand-200 rounded-full px-3 py-1 mb-4">
          <span className="text-xs text-brand-700 font-medium">一般的なシミュレーション・無料</span>
        </div>
        <h1 className="text-3xl font-bold text-gray-900 leading-tight mb-3">
          ライフプランシミュレーション<br />
          老後資金も<span className="text-brand-600">今も豊かに使える金額</span>が<br />
          わかる
        </h1>
        <p className="text-gray-500 text-base mb-6 leading-relaxed">
          将来必要なお金を貯めつつ、今使ってもよいお金の目安を
          <span className="font-semibold text-gray-700">慎重・標準・追い風</span>の
          3つの前提で試算。
          家族で話し合うたたき台に。
        </p>
        <Link
          href="/input"
          className="inline-block bg-brand-600 text-white font-bold text-lg px-8 py-4 rounded-2xl
            hover:bg-brand-700 active:bg-brand-800 transition-colors shadow-lg shadow-brand-200"
        >
          無料で試す
        </Link>
        <p className="mt-3 text-xs text-gray-400">
          登録不要・データはこの端末のみ保存・約3分
        </p>
      </section>

      {/* What you can know */}
      <section className="mt-4 bg-brand-50 rounded-2xl p-5">
        <h2 className="text-base font-bold text-brand-800 mb-3">このアプリでわかること</h2>
        <ul className="space-y-2">
          {[
            { icon: '💰', text: '将来必要な資産と、今の見込み資産の差' },
            { icon: '🛡️', text: '使いすぎ・ため込みすぎを避けられる余力の目安' },
            { icon: '📊', text: '「あと月○万円使える可能性」の逆算シミュレーション' },
            { icon: '👨‍👩‍👧', text: '家族で共有できるサマリーと話し合いメモ' },
          ].map(item => (
            <li key={item.text} className="flex items-start gap-3">
              <span className="text-lg flex-shrink-0 mt-0.5">{item.icon}</span>
              <span className="text-sm text-brand-900 leading-relaxed">{item.text}</span>
            </li>
          ))}
        </ul>
      </section>

      {/* How it works */}
      <section className="mt-8">
        <h2 className="text-lg font-bold text-gray-800 mb-4">3ステップで完結</h2>
        <div className="space-y-3">
          {[
            {
              step: '1',
              title: '基本情報を入力',
              desc: '年齢・収入・支出・退職年齢など約8項目。スライダーで直感的に入力。',
            },
            {
              step: '2',
              title: '余力と不足を確認',
              desc: '3シナリオで将来の資産推移を確認。「あといくら使えるか」の目安も表示。',
            },
            {
              step: '3',
              title: '家族と共有・メモを残す',
              desc: '結果をサマリーにまとめて家族と共有。話し合ったことはメモに記録。',
            },
          ].map(item => (
            <div key={item.step} className="flex items-start gap-4 bg-white rounded-2xl p-4 shadow-sm border border-gray-100">
              <span className="flex-shrink-0 w-8 h-8 bg-brand-600 text-white rounded-full flex items-center justify-center text-sm font-bold">
                {item.step}
              </span>
              <div>
                <p className="font-semibold text-gray-800">{item.title}</p>
                <p className="text-sm text-gray-500 mt-0.5">{item.desc}</p>
              </div>
            </div>
          ))}
        </div>
      </section>

      {/* 3 Scenarios */}
      <section className="mt-8">
        <h2 className="text-lg font-bold text-gray-800 mb-3">3つのシナリオとは？</h2>
        <div className="space-y-2">
          {[
            {
              label: '慎重',
              dot: 'bg-red-500',
              desc: '運用が伸びず、物価上昇が重めの前提。ゆとり費込みで計算。',
            },
            {
              label: '標準',
              dot: 'bg-brand-500',
              desc: '標準的な運用・インフレ・収入成長を想定した基本シナリオ。',
            },
            {
              label: '追い風',
              dot: 'bg-green-500',
              desc: '運用が比較的良好で、インフレ・支出も抑えられた楽観シナリオ。',
            },
          ].map(s => (
            <div key={s.label} className="flex items-start gap-3 bg-white rounded-xl px-4 py-3 border border-gray-100">
              <span className={`mt-1.5 flex-shrink-0 w-2.5 h-2.5 rounded-full ${s.dot}`} />
              <div>
                <span className="text-sm font-bold text-gray-800 mr-2">{s.label}</span>
                <span className="text-sm text-gray-500">{s.desc}</span>
              </div>
            </div>
          ))}
        </div>
        <p className="text-xs text-gray-400 mt-2 px-1">
          ※ シナリオは将来を予測・断定するものではありません。前提を変えた参考試算です。
        </p>
      </section>

      {/* Features grid */}
      <section className="mt-8">
        <h2 className="text-lg font-bold text-gray-800 mb-4">主な機能</h2>
        <div className="grid grid-cols-2 gap-3">
          {[
            { icon: '🔄', title: '逆算シミュレーション', desc: '「○歳で○万円残す前提で月いくら使える？」を逆算' },
            { icon: '📊', title: '3シナリオ比較', desc: '慎重・標準・追い風で不確実性を可視化' },
            { icon: '🎲', title: '確率シミュレーション', desc: '将来の運用ブレを加味した参考試算（1,000回）' },
            { icon: '📝', title: '家族メモ', desc: '教育・介護・住宅など話し合うべきことを記録' },
          ].map(item => (
            <div key={item.title} className="bg-white rounded-2xl p-4 shadow-sm border border-gray-100">
              <div className="text-2xl mb-2">{item.icon}</div>
              <p className="text-sm font-semibold text-gray-800">{item.title}</p>
              <p className="text-xs text-gray-500 mt-1">{item.desc}</p>
            </div>
          ))}
        </div>
      </section>

      {/* Disclaimer notice */}
      <section className="mt-8 bg-amber-50 border border-amber-200 rounded-2xl p-4">
        <p className="text-xs text-amber-800 leading-relaxed">
          <span className="font-semibold">ご注意：</span>
          本アプリは一般的な試算ツールであり、金融商品・投資・贈与等の推奨や、
          税務・法務上の助言は行いません。
          「余力候補」「追加支出可能額」は試算上の参考値であり、
          実際の支出可能額を保証するものではありません。
          話し合いのたたき台としてご活用ください。
        </p>
      </section>

      {/* CTA again */}
      <section className="mt-8 text-center pb-4">
        <Link
          href="/input"
          className="inline-block bg-brand-600 text-white font-bold text-base px-6 py-3 rounded-xl
            hover:bg-brand-700 transition-colors"
        >
          今すぐシミュレーション
        </Link>
        <p className="mt-2 text-xs text-gray-400">入力は約3分・登録不要・家族共有サマリーまで作成可能</p>
      </section>
    </AppShell>
  );
}
