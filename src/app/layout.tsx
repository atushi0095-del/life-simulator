import type { Metadata, Viewport } from 'next';
import './globals.css';
import { SimulationProvider } from '@/context/SimulationContext';
import { Analytics } from '@vercel/analytics/next';

export const metadata: Metadata = {
  title: '人生資金シミュレーター',
  description: '将来必要な資産と見込み資産を3シナリオで比較し、家族と共有できるシミュレーターです。登録不要・データはこの端末のみ保存・約3分でシミュレーション完了。',
  manifest: '/manifest.json',
  keywords: ['資産シミュレーション', '老後資金', 'ライフプラン', '退職計画', '家族で共有', '無料'],
  openGraph: {
    type: 'website',
    locale: 'ja_JP',
    title: '人生資金シミュレーター — 今いくら使えるか、3シナリオで確認',
    description: '老後に必要な資産と見込み資産を比較。「今月いくら使えるか」を逆算。家族で共有できるサマリーも作成できます。登録不要・無料。',
    siteName: '人生資金シミュレーター',
  },
  twitter: {
    card: 'summary',
    title: '人生資金シミュレーター',
    description: '老後資金と今の使えるお金を3シナリオで試算。登録不要・無料。',
  },
};

export const viewport: Viewport = {
  width: 'device-width',
  initialScale: 1,
  themeColor: '#0284c7',
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="ja">
      <body>
        <SimulationProvider>
          {children}
        </SimulationProvider>
        <Analytics />
      </body>
    </html>
  );
}
