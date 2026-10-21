import type { ButtonHTMLAttributes, ReactNode } from 'react';
import { Link, type LinkProps } from 'react-router-dom';

export const cx = (...classes: (string | false | null | undefined)[]) => classes.filter(Boolean).join(' ');

type Variant = 'primary' | 'secondary' | 'ghost' | 'danger';
type Size = 'sm' | 'md' | 'lg';

const variants: Record<Variant, string> = {
  primary: 'bg-accent text-black hover:bg-accent-strong shadow-[0_0_24px_-6px] shadow-accent/60',
  secondary: 'border border-line bg-surface-2 text-fg hover:border-muted/60',
  ghost: 'text-muted hover:bg-surface-2 hover:text-fg',
  danger: 'bg-danger/15 text-danger hover:bg-danger/25',
};
const sizes: Record<Size, string> = {
  sm: 'h-9 px-3 text-sm gap-1.5',
  md: 'h-11 px-5 text-sm gap-2',
  lg: 'h-13 px-7 text-base gap-2',
};

export function buttonClass(variant: Variant = 'primary', size: Size = 'md', extra?: string) {
  return cx(
    'inline-flex items-center justify-center rounded-xl font-semibold transition-colors disabled:cursor-not-allowed disabled:opacity-50',
    variants[variant],
    sizes[size],
    extra,
  );
}

export function Button({
  variant,
  size,
  className,
  ...props
}: ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; size?: Size }) {
  return <button type="button" className={buttonClass(variant, size, className)} {...props} />;
}

export function ButtonLink({ variant, size, className, ...props }: LinkProps & { variant?: Variant; size?: Size }) {
  return <Link className={buttonClass(variant, size, className)} {...props} />;
}

/** Переключаемый чип: настроения, жанры, типы, подсказки. */
export function Chip({
  active = false,
  onClick,
  children,
  className,
}: {
  active?: boolean;
  onClick?: () => void;
  children: ReactNode;
  className?: string;
}) {
  return (
    <button
      type="button"
      aria-pressed={active}
      onClick={onClick}
      className={cx(
        'inline-flex h-9 items-center rounded-full border px-4 text-sm font-medium transition-colors',
        active
          ? 'border-accent bg-accent/15 text-accent'
          : 'border-line bg-surface-2 text-muted hover:border-muted/60 hover:text-fg',
        className,
      )}
    >
      {children}
    </button>
  );
}

export function Badge({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'accent' | 'success' }) {
  const tones = {
    neutral: 'bg-surface-2 text-muted border-line',
    accent: 'bg-accent/10 text-accent border-accent/30',
    success: 'bg-success/10 text-success border-success/30',
  };
  return (
    <span className={cx('inline-flex items-center rounded-md border px-2 py-0.5 text-xs font-medium', tones[tone])}>
      {children}
    </span>
  );
}

export function Rating({ value }: { value: number }) {
  return (
    <span className="inline-flex items-center gap-1 font-semibold text-accent" aria-label={`Рейтинг ${value.toFixed(1)}`}>
      ★ {value.toFixed(1)}
    </span>
  );
}

export function Skeleton({ className }: { className?: string }) {
  return <div className={cx('animate-pulse rounded-lg bg-surface-2', className)} />;
}

export function PageHeader({ title, subtitle, actions }: { title: string; subtitle?: ReactNode; actions?: ReactNode }) {
  return (
    <div className="mb-8 flex flex-col gap-4 sm:flex-row sm:items-end sm:justify-between">
      <div className="flex flex-col gap-2">
        <h1 className="text-3xl font-bold tracking-tight sm:text-4xl">{title}</h1>
        {subtitle && <p className="text-muted">{subtitle}</p>}
      </div>
      {actions && <div className="flex flex-wrap gap-3">{actions}</div>}
    </div>
  );
}

export function EmptyState({ icon, title, children }: { icon: string; title: string; children?: ReactNode }) {
  return (
    <div className="card flex flex-col items-center gap-3 px-6 py-14 text-center">
      <div className="text-4xl" aria-hidden>
        {icon}
      </div>
      <h3 className="text-lg font-semibold">{title}</h3>
      {children && <div className="max-w-md text-sm leading-relaxed text-muted">{children}</div>}
    </div>
  );
}

export function ErrorBox({ children }: { children: ReactNode }) {
  return (
    <div role="alert" className="rounded-xl border border-danger/40 bg-danger/10 px-4 py-3 text-sm text-danger">
      {children}
    </div>
  );
}
