import Link from 'next/link';

interface DisclaimerProps {
  compact?: boolean;
}

export default function Disclaimer({ compact = false }: DisclaimerProps) {
  if (compact) {
    return (
      <p className="text-xs text-gray-400 leading-relaxed">
        本シミュレーションは一般的な試算であり、特定の金融商品・投資・贈与等を推奨するものではありません。
        最終的な判断はご自身の責任において行ってください。
      </p>
    );
  }

  return (
    <div className="bg-gray-50 border border-gray-200 rounded-xl p-4 space-y-2">
      <h3 className="text-xs font-semibold text-gray-500 uppercase tracking-wide">免責事項</h3>
      <ul className="text-xs text-gray-500 space-y-1 leading-relaxed list-disc list-inside">
        <li>本シミュレーションは一般的な試算です。実際の将来の結果を保証するものではありません。</li>
        <li>特定の金融商品・銘柄・投資戦略・贈与・相続スキーム等を推奨するものではありません。</li>
        <li>「余力候補」「追加支出可能額」は試算上の目安であり、支出を保証するものではありません。</li>
        <li>税務・法務上の最終判断については、専門家にご相談ください。</li>
        <li>最終的な意思決定はご自身の責任において行ってください。</li>
      </ul>
      <p className="text-xs text-gray-400">
        <Link href="/legal/disclaimer" className="underline hover:text-gray-600">免責ページ全文</Link>
        {' '}|{' '}
        <Link href="/legal/terms" className="underline hover:text-gray-600">利用規約</Link>
        {' '}|{' '}
        <Link href="/legal/privacy" className="underline hover:text-gray-600">プライバシーポリシー</Link>
      </p>
    </div>
  );
}
