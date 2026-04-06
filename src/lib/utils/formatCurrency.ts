/**
 * Format a number (万円) into a human-readable Japanese currency string.
 * Examples:
 *   120      → "120万円"
 *   10000    → "1億円"
 *   15500    → "1億5,500万円"
 *   -800     → "-800万円"
 */
export function formatManYen(value: number): string {
  const sign = value < 0 ? '-' : '';
  const abs  = Math.abs(value);

  if (abs >= 10000) {
    const oku  = Math.floor(abs / 10000);
    const man  = Math.round(abs % 10000);
    if (man === 0) return `${sign}${oku}億円`;
    return `${sign}${oku}億${man.toLocaleString('ja-JP')}万円`;
  }

  return `${sign}${Math.round(abs).toLocaleString('ja-JP')}万円`;
}

/**
 * Format with explicit ± prefix for gap/surplus display.
 * Positive → "+1,200万円", Negative → "-800万円"
 */
export function formatGap(value: number): string {
  const prefix = value >= 0 ? '+' : '';
  return prefix + formatManYen(value);
}

/**
 * Format a success rate (0–1) as percentage string.
 */
export function formatPercent(value: number, decimals = 0): string {
  return `${(value * 100).toFixed(decimals)}%`;
}
