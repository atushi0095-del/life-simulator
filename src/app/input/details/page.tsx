import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import BasicInputForm from '@/components/input/BasicInputForm';
import DetailsInputForm from '@/components/input/DetailsInputForm';

export default function DetailsPage() {
  return (
    <AppShell>
      <PageHeader
        title="詳細設定"
        backHref="/input"
        subtitle="基本設定と詳細設定をまとめて確認・変更できます"
      />

      {/* Basic settings — shown without action buttons so they don't duplicate */}
      <div className="mb-8">
        <div className="flex items-center gap-2 mb-4">
          <span className="w-6 h-6 rounded-full bg-brand-600 text-white text-xs font-bold flex items-center justify-center">1</span>
          <h2 className="text-base font-bold text-gray-800">基本設定</h2>
        </div>
        <BasicInputForm showActions={false} />
      </div>

      {/* Divider */}
      <div className="relative mb-8">
        <div className="absolute inset-0 flex items-center">
          <div className="w-full border-t border-gray-200" />
        </div>
        <div className="relative flex justify-center">
          <span className="px-4 bg-white text-xs text-gray-400 font-medium">以下は任意の詳細設定</span>
        </div>
      </div>

      {/* Details settings */}
      <div>
        <div className="flex items-center gap-2 mb-4">
          <span className="w-6 h-6 rounded-full bg-gray-400 text-white text-xs font-bold flex items-center justify-center">2</span>
          <h2 className="text-base font-bold text-gray-800">詳細設定（任意）</h2>
        </div>
        <DetailsInputForm />
      </div>
    </AppShell>
  );
}
