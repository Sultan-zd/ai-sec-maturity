import { Link, useSearchParams } from 'react-router'
import { ArrowLeft, ArrowRight } from 'lucide-react'
import { useAssessments, useProgress } from '../api/hooks'
import type { AssessmentSummary } from '../api/types'
import { Card, ErrorBox, Field, Select, Spinner } from '../components/ui'
import { fmt2, fmtDate } from '../lib/format'

/** Progression entre deux évaluations d'une même organisation : « avant → après ». */
export function ProgressPage() {
  const [params, setParams] = useSearchParams()
  const from = params.get('de') ?? undefined
  const to = params.get('a') ?? undefined
  const assessments = useAssessments()
  const progress = useProgress(from, to)

  const list = [...(assessments.data ?? [])].sort((x, y) => x.createdAt.localeCompare(y.createdAt))
  const fromAssessment = list.find((a) => a.id === from)
  // « Après » : uniquement les évaluations de la même organisation que « Avant »
  const candidates = fromAssessment ? list.filter((a) => a.organizationId === fromAssessment.organizationId && a.id !== from) : []

  const choose = (key: 'de' | 'a', value: string) => {
    const next = new URLSearchParams(params)
    if (value) next.set(key, value)
    else next.delete(key)
    if (key === 'de') next.delete('a')
    setParams(next, { replace: true })
  }

  return (
    <div className="space-y-6">
      <Link to="/" className="inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink">
        <ArrowLeft className="size-4" aria-hidden />Toutes les évaluations
      </Link>
      <div>
        <h1 className="text-2xl font-semibold text-ink">Progression dans le temps</h1>
        <p className="text-sm text-ink-2">Comparez deux évaluations de la même organisation (scores officiels).</p>
      </div>

      <Card>
        {assessments.isLoading && <Spinner />}
        <ErrorBox error={assessments.error} />
        <div className="grid gap-4 sm:grid-cols-2">
          <Field label="Avant">
            <Select value={from ?? ''} onChange={(e) => choose('de', e.target.value)}>
              <option value="">— Choisir —</option>
              {list.map((a) => <option key={a.id} value={a.id}>{label(a)}</option>)}
            </Select>
          </Field>
          <Field label="Après">
            <Select value={to ?? ''} disabled={!fromAssessment} onChange={(e) => choose('a', e.target.value)}>
              <option value="">{fromAssessment && candidates.length === 0 ? 'Aucune autre évaluation de cette organisation' : '— Choisir —'}</option>
              {candidates.map((a) => <option key={a.id} value={a.id}>{label(a)}</option>)}
            </Select>
          </Field>
        </div>
      </Card>

      {progress.isLoading && <Spinner />}
      <ErrorBox error={progress.error} />
      {progress.data && <ProgressView p={progress.data} />}
    </div>
  )
}

function ProgressView({ p }: { p: NonNullable<ReturnType<typeof useProgress>['data']> }) {
  return (
    <>
      {!p.sameReferential && (
        <p role="alert" className="rounded-md border border-[var(--status-warning)] p-3 text-sm text-ink">
          Les deux évaluations n'utilisent pas la même version du référentiel : seuls les domaines et questions communs sont comparés.
        </p>
      )}
      {(!p.fromReliable || !p.toReliable) && (
        <p role="alert" className="rounded-md border border-[var(--status-warning)] p-3 text-sm text-ink">
          Au moins une évaluation est complétée à moins de 80 % : la variation du score global n'est qu'indicative.
        </p>
      )}

      <div className="grid gap-4 sm:grid-cols-3">
        <Big label={p.from.title} value={fmt2(p.fromGlobal?.value)} />
        <div className="flex flex-col items-center justify-center rounded-lg border border-line bg-surface p-4">
          <ArrowRight className="size-5 text-ink-3" aria-hidden />
          <Delta value={p.globalDelta} large />
        </div>
        <Big label={p.to.title} value={fmt2(p.toGlobal?.value)} />
      </div>

      <Card title="Par domaine">
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-3">
                <th className="px-2 py-2">Domaine</th><th className="px-2 py-2 text-right">Avant</th>
                <th className="px-2 py-2 text-right">Après</th><th className="px-2 py-2 text-right">Variation</th>
              </tr>
            </thead>
            <tbody>
              {p.domains.map((d) => (
                <tr key={d.code} className="border-b border-line last:border-0">
                  <td className="px-2 py-1.5 text-ink">{d.code} <span className="text-ink-3">{d.name}</span></td>
                  <td className="px-2 py-1.5 text-right tabular-nums text-ink-2">{fmt2(d.from)}</td>
                  <td className="px-2 py-1.5 text-right tabular-nums text-ink">{fmt2(d.to)}</td>
                  <td className="px-2 py-1.5 text-right"><Delta value={d.delta} /></td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>

      <div className="grid gap-4 lg:grid-cols-3">
        <Card title={`Questions en progrès (${p.improved.length})`}>
          <Changes items={p.improved} />
        </Card>
        <Card title={`Questions en recul (${p.regressed.length})`}>
          <Changes items={p.regressed} />
        </Card>
        <Card title="Feuille de route">
          <p className="text-sm text-ink-2">
            {p.fromRecommendations} action(s) avant → {p.toRecommendations} après.
          </p>
          <p className="mt-1 text-sm text-ink">
            <strong>{p.closedRecommendations.length}</strong> recommandation(s) clôturée(s)
            {p.closedRecommendations.length > 0 && <> : <span className="font-mono text-xs">{p.closedRecommendations.slice(0, 12).join(', ')}{p.closedRecommendations.length > 12 ? '…' : ''}</span></>}
          </p>
        </Card>
      </div>
    </>
  )
}

const label = (a: AssessmentSummary) => `${a.title} — ${a.organizationName} (${fmtDate(a.createdAt)})`

function Big({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border border-line bg-surface p-4 text-center">
      <div className="truncate text-xs font-medium uppercase tracking-wide text-ink-3" title={label}>{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-ink">{value} <span className="text-base text-ink-3">/ 5</span></div>
    </div>
  )
}

/** Variation : le signe et le libellé portent le sens (pas uniquement la couleur). */
function Delta({ value, large }: { value: number | null; large?: boolean }) {
  if (value == null) return <span className="text-ink-3">—</span>
  const tone = value > 0 ? 'text-[var(--status-good)]' : value < 0 ? 'text-[var(--status-critical)]' : 'text-ink-2'
  return (
    <span className={`font-semibold tabular-nums ${tone} ${large ? 'text-2xl' : ''}`}>
      {value > 0 ? '+' : value < 0 ? '−' : ''}{fmt2(Math.abs(value))}
    </span>
  )
}

function Changes({ items }: { items: { questionCode: string; fromLevel: number; toLevel: number }[] }) {
  if (items.length === 0) return <p className="text-sm text-ink-3">Aucune.</p>
  return (
    <ul className="max-h-64 space-y-1 overflow-y-auto text-sm">
      {items.map((c) => (
        <li key={c.questionCode} className="flex justify-between gap-2">
          <span className="font-mono text-xs text-ink">{c.questionCode}</span>
          <span className="tabular-nums text-ink-2">niveau {c.fromLevel} → {c.toLevel}</span>
        </li>
      ))}
    </ul>
  )
}
