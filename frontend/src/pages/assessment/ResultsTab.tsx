import type { ReactNode } from 'react'
import { AlertTriangle } from 'lucide-react'
import { useResults, useUpdateTargets } from '../../api/hooks'
import type { AssessmentSummary } from '../../api/types'
import { GapBars } from '../../components/charts/GapBars'
import { MaturityRadar } from '../../components/charts/MaturityRadar'
import { MeasurementsCard, ScoreRangeCard } from './EvidencePanels'
import { Card, ErrorBox, LevelBadge, Select, Spinner } from '../../components/ui'
import { fmt2, levelColor, levelNames } from '../../lib/format'

/** Résultats : chiffres clés, radar actuel/cible, écarts, tableau détaillé (avec réglage des cibles). */
export function ResultsTab({ summary, canEditTargets }: { summary: AssessmentSummary; canEditTargets: boolean }) {
  const { data, isLoading, error } = useResults(summary.id)
  const updateTargets = useUpdateTargets(summary.id)

  if (isLoading) return <Spinner />
  if (error || !data) return <ErrorBox error={error} />
  const s = data.scoring
  const capped = s.domains.filter((d) => d.cappedBy)

  return (
    <div className="space-y-6">
      {/* Chiffres clés */}
      <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-4">
        {/* Sous le seuil de complétion, le score n'est qu'une estimation : on ne l'affiche pas comme un résultat */}
        {s.global && s.globalReliable ? (
          <>
            <Stat label="Score global" value={`${fmt2(s.global.value)} / 5`} detail={<LevelBadge level={s.global.level} />} />
            <Stat label="Maturité" value={`${Math.round(s.global.percent)} %`} detail="0 % = niveau 1, 100 % = niveau 5" />
          </>
        ) : (
          <>
            <Stat label="Score global" value="—"
              detail={s.global
                ? `Provisoire : ${fmt2(s.global.value)} / 5 (estimation sur ${Math.round(s.completionPercent)} % des questions)`
                : 'Répondez aux questions pour obtenir un score'} />
            <Stat label="Maturité" value="—"
              detail={`Disponible à partir de ${Math.round(s.minCompletionPercent)} % de complétion`} />
          </>
        )}
        <Stat label="Complétion" value={`${Math.round(s.completionPercent)} %`}
          detail={`${s.answered} évaluées · ${s.notApplicable} non applicables · ${s.total} au total`} />
        <Stat label="Indice de confiance" value={`${Math.round(data.evidenceRatePercent)} %`} detail="Réponses appuyées par une preuve" />
      </div>

      {capped.length > 0 && (
        <div role="alert" className="flex items-start gap-2 rounded-md border border-[var(--status-critical)] bg-surface p-3 text-sm text-ink">
          <AlertTriangle className="mt-0.5 size-4 shrink-0 text-[var(--status-critical)]" aria-hidden />
          <span>
            <strong>Maillon faible :</strong>{' '}
            {capped.map((d) => `${d.code} est plafonné à ${fmt2(d.cap)} par la question critique ${d.cappedBy} (score brut ${fmt2(d.rawScore)})`).join(' ; ')}.
          </span>
        </div>
      )}

      <div className="grid gap-6 lg:grid-cols-2">
        <Card title="Profil de maturité : actuel / cible"><MaturityRadar domains={s.domains} /></Card>
        <Card title="Écart à la cible (en niveaux)"><GapBars domains={s.domains} /></Card>
      </div>

      <ScoreRangeCard comparison={data.comparison} domains={s.domains} />
      <MeasurementsCard assessmentId={summary.id} />

      <Card title="Détail par domaine">
        <ErrorBox error={updateTargets.error} />
        <div className="overflow-x-auto">
          <table className="w-full text-sm">
            <thead>
              <tr className="border-b border-line text-left text-xs uppercase tracking-wide text-ink-3">
                <th className="px-2 py-2">Domaine</th><th className="px-2 py-2">Poids</th><th className="px-2 py-2">Score</th>
                <th className="px-2 py-2">Niveau</th><th className="px-2 py-2">Cible</th><th className="px-2 py-2">Écart</th>
              </tr>
            </thead>
            <tbody>
              {s.domains.map((d) => (
                <tr key={d.code} className="border-b border-line align-top last:border-0">
                  <td className="px-2 py-2">
                    <div className="font-medium text-ink">{d.code} — {d.name}</div>
                    {d.subdomains.map((sd) => (
                      <div key={sd.code} className="text-xs text-ink-3">
                        {sd.code} {sd.name} : {sd.score ? fmt2(sd.score.value) : 'non évalué'} ({sd.answered}/{sd.total})
                      </div>
                    ))}
                  </td>
                  <td className="px-2 py-2 text-ink-2">{Math.round(d.weight * 100)} %</td>
                  <td className="px-2 py-2 text-ink">
                    {fmt2(d.score?.value)}
                    {d.cappedBy && <div className="text-xs text-[var(--status-critical)]">plafonné (brut {fmt2(d.rawScore)})</div>}
                  </td>
                  <td className="px-2 py-2">
                    {d.score ? (
                      <span className="inline-flex items-center gap-1.5 text-ink">
                        <span className="size-2.5 rounded-full" style={{ background: levelColor(d.score.level) }} aria-hidden />
                        {d.score.level} · {levelNames[d.score.level - 1]}
                      </span>
                    ) : <span className="text-ink-3">N/A</span>}
                  </td>
                  <td className="px-2 py-2">
                    {canEditTargets ? (
                      <Select aria-label={`Cible ${d.code}`} className="w-20 py-1" value={d.targetLevel}
                        disabled={updateTargets.isPending}
                        onChange={(e) => updateTargets.mutate({ [d.code]: Number(e.target.value) })}>
                        {[1, 2, 3, 4, 5].map((l) => <option key={l} value={l}>{l}</option>)}
                      </Select>
                    ) : <span className="text-ink">{d.targetLevel}</span>}
                  </td>
                  <td className={`px-2 py-2 font-medium ${d.gap ? 'text-[var(--status-critical)]' : 'text-ink-2'}`}>{fmt2(d.gap)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

function Stat({ label, value, detail }: { label: string; value: string; detail: ReactNode }) {
  return (
    <div className="rounded-lg border border-line bg-surface p-4">
      <div className="text-xs font-medium uppercase tracking-wide text-ink-3">{label}</div>
      <div className="mt-1 text-3xl font-semibold tabular-nums text-ink">{value}</div>
      <div className="mt-1 text-xs text-ink-2">{detail}</div>
    </div>
  )
}
