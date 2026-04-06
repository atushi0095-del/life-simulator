'use client';

import { useState } from 'react';
import Button from '@/components/ui/Button';

export default function ShareButton() {
  const [copied, setCopied] = useState(false);

  function handlePrint() {
    window.print();
  }

  async function handleCopyLink() {
    try {
      await navigator.clipboard.writeText(window.location.href);
      setCopied(true);
      setTimeout(() => setCopied(false), 2000);
    } catch {
      // fallback
    }
  }

  return (
    <div className="flex gap-3">
      <Button variant="secondary" onClick={handleCopyLink} fullWidth>
        {copied ? 'コピーしました！' : 'リンクをコピー'}
      </Button>
      <Button variant="secondary" onClick={handlePrint} fullWidth>
        印刷 / PDF保存
      </Button>
    </div>
  );
}
