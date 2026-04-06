import Link from 'next/link';

export default function Footer() {
  return (
    <footer className="mt-12 pb-24 px-4 text-center">
      <div className="flex items-center justify-center gap-4 text-xs text-gray-400">
        <Link href="/legal/terms"    className="hover:text-gray-600">利用規約</Link>
        <span>|</span>
        <Link href="/legal/privacy"  className="hover:text-gray-600">プライバシーポリシー</Link>
        <span>|</span>
        <Link href="/legal/disclaimer" className="hover:text-gray-600">免責事項</Link>
      </div>
      <p className="mt-2 text-xs text-gray-300">
        本アプリは一般的なシミュレーションツールです。金融商品の推奨・税務・法務上の助言は行いません。
      </p>
    </footer>
  );
}
