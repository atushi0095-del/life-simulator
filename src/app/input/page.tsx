import AppShell from '@/components/layout/AppShell';
import PageHeader from '@/components/ui/PageHeader';
import BasicInputForm from '@/components/input/BasicInputForm';

export default function InputPage() {
  return (
    <AppShell>
      <PageHeader
        title="基本情報を入力"
        backHref="/"
        subtitle="今の暮らしにどれくらい余裕があるか、約3分で確認できます"
      />
      <BasicInputForm />
    </AppShell>
  );
}
