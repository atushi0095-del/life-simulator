type BadgeVariant = 'default' | 'worst' | 'main' | 'upside' | 'neutral';

interface BadgeProps {
  children: React.ReactNode;
  variant?: BadgeVariant;
  size?: 'sm' | 'md';
}

const variantClasses: Record<BadgeVariant, string> = {
  default: 'bg-gray-100 text-gray-700',
  worst:   'bg-red-100 text-red-700',
  main:    'bg-blue-100 text-blue-700',
  upside:  'bg-green-100 text-green-700',
  neutral: 'bg-gray-50 text-gray-600 border border-gray-200',
};

export default function Badge({ children, variant = 'default', size = 'sm' }: BadgeProps) {
  return (
    <span
      className={[
        'inline-flex items-center rounded-full font-medium',
        size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-3 py-1 text-sm',
        variantClasses[variant],
      ].join(' ')}
    >
      {children}
    </span>
  );
}
