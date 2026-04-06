import BottomNav from './BottomNav';
import Footer from './Footer';

interface AppShellProps {
  children: React.ReactNode;
  hideNav?: boolean;
}

export default function AppShell({ children, hideNav = false }: AppShellProps) {
  return (
    <div className="min-h-screen bg-gray-50">
      <main className="max-w-lg mx-auto px-4 pt-6 pb-32">
        {children}
        <Footer />
      </main>
      {!hideNav && <BottomNav />}
    </div>
  );
}
