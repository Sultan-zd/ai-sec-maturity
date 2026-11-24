import { FlaskConical } from 'lucide-react'
import { useMeasurements } from '../../api/hooks'
import type { DomainResult, ScoreComparison } from '../../api/types'
import { Card, ErrorBox, Spinner } from '../../components/ui'
import { attackFamilyLabels, fmt2, fmtDate } from '../../lib/format'

/**
 * Fourchette de scores : ce que l'organisation déclare, le score officiel (mesuré > audité > déclaré)
 * et ce qui est prouvé (une réponse sans preuve, sans revue et sans mesure compte au niveau 1).
 */
export function ScoreRangeCard({ comparison, domains }: { comparison: ScoreComparison; domains: DomainResult[] }) {
  const { declared, official, proven, provenAnswers, evaluatedAnswers } = comparison
  const names = new Map(domains.map((d) => [d.code, d.name]))
  return (
    <Card title="Déclaré, officiel, prouvé">
      <p className="mb-4 text-sm text-ink-2">
        Le score <strong>prouvé</strong> ne retient que ce qui est démontré (preuve, revue de l'auditeur ou test du labo
        red-team) : {provenAnswers} réponse(s) sur {evaluatedAnswers}. Plus l'écart avec le <strong>déclaré</strong> est grand,
        plus le résultat repose sur de simples affirmations.
      </p>
      <div className="mb-4 grid grid-cols-3 gap-3 text-center">
        <RangeValue label="Déclaré" value={declared?.value} hint="réponses de l'organisation" />
        <RangeValue label="Officiel" value={official?.value} hint="mesuré > audité > déclaré" strong />
        <RangeValue label="Prouvé" value={proven?.value} hint="sans preuve = niveau 1" />
      </div>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-3">
              <th className="px-2 py-2">Domaine</th>
              <th className="px-2 py-2 text-right">Déclaré</th>
              <th className="px-2 py-2 text-right">Officiel</th>
              <th className="px-2 py-2 text-right">Prouvé</th>
            </tr>
          </thead>
          <tbody>
            {comparison.domains.map((d) => (
              <tr key={d.code} className="border-b border-line last:border-0">
                <td className="px-2 py-1.5 text-ink">{d.code} <span className="text-ink-3">{names.get(d.code)}</span></td>
                <td className="px-2 py-1.5 text-right tabular-nums text-ink-2">{fmt2(d.declared)}</td>
                <td className="px-2 py-1.5 text-right font-medium tabular-nums text-ink">{fmt2(d.official)}</td>
                <td className="px-2 py-1.5 text-right tabular-nums text-ink-2">{fmt2(d.proven)}</td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}

function RangeValue({ label, value, hint, strong }: { label: string; value?: number; hint: string; strong?: boolean }) {
  return (
    <div className={`rounded-md border p-3 ${strong ? 'border-[var(--series-actual)]' : 'border-line'}`}>
      <div className="text-xs font-medium uppercase tracking-wide text-ink-3">{label}</div>
      <div className="text-2xl font-semibold tabular-nums text-ink">{value == null ? '—' : fmt2(value)}</div>
      <div className="text-xs text-ink-3">{hint}</div>
    </div>
  )
}

/** Résultats du labo red-team : niveau déclaré vs niveau mesuré par une attaque réelle. */
export function MeasurementsCard({ assessmentId }: { assessmentId: string }) {
  const { data, isLoading, error } = useMeasurements(assessmentId)
  if (isLoading) return <Spinner />
  if (error) return <ErrorBox error={error} />
  if (!data || data.length === 0) return null

  return (
    <Card title="Tests techniques du labo red-team">
      <p className="mb-3 flex items-start gap-2 text-sm text-ink-2">
        <FlaskConical className="mt-0.5 size-4 shrink-0" aria-hidden />
        Ces niveaux ont été mesurés en attaquant le système. Ils remplacent le niveau déclaré dans le calcul.
        Un écart positif signifie que l'organisation s'était surévaluée.
      </p>
      <div className="overflow-x-auto">
        <table className="w-full text-sm">
          <thead>
            <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-3">
              <th className="px-2 py-2">Question</th><th className="px-2 py-2">Attaque</th>
              <th className="px-2 py-2 text-right">Déclaré</th><th className="px-2 py-2 text-right">Mesuré</th>
              <th className="px-2 py-2 text-right">Écart</th><th className="px-2 py-2">Preuve</th>
            </tr>
          </thead>
          <tbody>
            {data.map((m) => (
              <tr key={m.questionCode} className="border-b border-line align-top last:border-0">
                <td className="px-2 py-2 font-mono text-xs text-ink">{m.questionCode}</td>
                <td className="px-2 py-2 text-ink-2">{attackFamilyLabels[m.attackFamily]}</td>
                <td className="px-2 py-2 text-right tabular-nums text-ink-2">{m.declaredLevel ?? '—'}</td>
                <td className="px-2 py-2 text-right font-medium tabular-nums text-ink">{m.measuredLevel}</td>
                <td className={`px-2 py-2 text-right font-medium tabular-nums ${m.gap && m.gap > 0 ? 'text-[var(--status-critical)]' : 'text-ink-2'}`}>
                  {m.gap == null ? '—' : m.gap > 0 ? `+${m.gap}` : m.gap}
                </td>
                <td className="px-2 py-2 text-xs text-ink-2">
                  {m.evidence}
                  <div className="text-ink-3">
                    {m.tool} · {m.runId} · {fmtDate(m.measuredAt)}
                    {m.evidenceUrl && (
                      <> · <a className="underline" href={m.evidenceUrl} target="_blank" rel="noreferrer noopener">rapport</a></>
                    )}
                  </div>
                </td>
              </tr>
            ))}
          </tbody>
        </table>
      </div>
    </Card>
  )
}
