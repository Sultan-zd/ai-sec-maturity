import type { ButtonHTMLAttributes, InputHTMLAttributes, ReactNode, SelectHTMLAttributes, TextareaHTMLAttributes } from 'react'
import { AlertTriangle, Loader2 } from 'lucide-react'
import { ApiError } from '../api/client'
import { levelColor, levelNames } from '../lib/format'

/** Petits composants d'interface partagés (Tailwind). */

type Variant = 'primary' | 'secondary' | 'danger' | 'ghost'

const variants: Record<Variant, string> = {
  primary: 'bg-brand text-white hover:opacity-90 dark:text-[#0b0b0b]',
  secondary: 'border border-line bg-surface text-ink hover:bg-surface-2',
  danger: 'bg-[var(--status-critical)] text-white hover:opacity-90',
  ghost: 'text-ink-2 hover:bg-surface-2',
}

export function Button({ variant = 'primary', loading, children, className = '', disabled, ...props }:
  ButtonHTMLAttributes<HTMLButtonElement> & { variant?: Variant; loading?: boolean }) {
  return (
    <button
      {...props}
      disabled={disabled || loading}
      className={`inline-flex items-center justify-center gap-2 rounded-md px-3.5 py-2 text-sm font-medium transition
        disabled:cursor-not-allowed disabled:opacity-50 ${variants[variant]} ${className}`}
    >
      {loading && <Loader2 className="size-4 animate-spin" aria-hidden />}
      {children}
    </button>
  )
}

export function Card({ title, actions, children, className = '' }:
  { title?: ReactNode; actions?: ReactNode; children: ReactNode; className?: string }) {
  return (
    <section className={`rounded-lg border border-line bg-surface p-5 ${className}`}>
      {(title || actions) && (
        <header className="mb-4 flex flex-wrap items-center justify-between gap-2">
          {title && <h2 className="text-base font-semibold text-ink">{title}</h2>}
          {actions}
        </header>
      )}
      {children}
    </section>
  )
}

/** Niveau de maturité : pastille de couleur + texte (la couleur n'est jamais seule porteuse de sens). */
export function LevelBadge({ level, score }: { level: number | null | undefined; score?: number | null }) {
  if (!level) return <span className="text-sm text-ink-3">Non évalué</span>
  return (
    <span className="inline-flex items-center gap-1.5 text-sm font-medium text-ink">
      <span className="size-2.5 rounded-full" style={{ background: levelColor(level) }} aria-hidden />
      Niveau {level} — {levelNames[level - 1]}
      {score != null && <span className="text-ink-3">({score.toFixed(2).replace('.', ',')})</span>}
    </span>
  )
}

export function Pill({ children, tone = 'neutral' }: { children: ReactNode; tone?: 'neutral' | 'info' | 'critical' }) {
  const tones = {
    neutral: 'bg-surface-2 text-ink-2 border-line',
    info: 'bg-[color-mix(in_oklab,var(--series-actual)_12%,transparent)] text-ink border-[color-mix(in_oklab,var(--series-actual)_35%,transparent)]',
    critical: 'bg-[color-mix(in_oklab,var(--status-critical)_12%,transparent)] text-ink border-[color-mix(in_oklab,var(--status-critical)_40%,transparent)]',
  }
  return <span className={`inline-flex items-center rounded-full border px-2 py-0.5 text-xs font-medium ${tones[tone]}`}>{children}</span>
}

export function Spinner({ label = 'Chargement…' }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 p-6 text-sm text-ink-2" role="status">
      <Loader2 className="size-4 animate-spin" aria-hidden /> {label}
    </div>
  )
}

export function ErrorBox({ error }: { error: unknown }) {
  if (!error) return null
  const message = error instanceof ApiError ? error.userMessage
    : error instanceof Error ? error.message : 'Erreur inattendue'
  return (
    <div role="alert" className="flex items-start gap-2 rounded-md border border-[var(--status-critical)] bg-surface p-3 text-sm text-ink">
      <AlertTriangle className="mt-0.5 size-4 shrink-0 text-[var(--status-critical)]" aria-hidden />
      <span>{message}</span>
    </div>
  )
}

const fieldClass = 'w-full rounded-md border border-line bg-surface px-3 py-2 text-sm text-ink placeholder:text-ink-3'

export function Field({ label, hint, children }: { label: string; hint?: string; children: ReactNode }) {
  return (
    <label className="block space-y-1">
      <span className="text-sm font-medium text-ink">{label}</span>
      {children}
      {hint && <span className="block text-xs text-ink-3">{hint}</span>}
    </label>
  )
}

export const Input = (props: InputHTMLAttributes<HTMLInputElement>) => <input {...props} className={`${fieldClass} ${props.className ?? ''}`} />
export const Textarea = (props: TextareaHTMLAttributes<HTMLTextAreaElement>) => <textarea {...props} className={`${fieldClass} ${props.className ?? ''}`} />
export const Select = (props: SelectHTMLAttributes<HTMLSelectElement>) => <select {...props} className={`${fieldClass} ${props.className ?? ''}`} />

/** Barre de progression avec valeur textuelle. */
export function Progress({ value, label }: { value: number; label?: string }) {
  return (
    <div className="space-y-1">
      <div className="flex justify-between text-xs text-ink-2">
        <span>{label}</span>
        <span>{Math.round(value)} %</span>
      </div>
      <div className="h-1.5 overflow-hidden rounded-full bg-surface-2" role="progressbar" aria-valuenow={Math.round(value)} aria-valuemin={0} aria-valuemax={100}>
        <div className="h-full rounded-full bg-actual transition-all" style={{ width: `${Math.min(100, value)}%` }} />
      </div>
    </div>
  )
}
