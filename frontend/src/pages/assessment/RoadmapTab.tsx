import { useRoadmap } from '../../api/hooks'
import { Card, ErrorBox, Pill, Spinner } from '../../components/ui'
import { criticalityLabels, effortLabels, fmt2 } from '../../lib/format'

/** Feuille de route : une colonne par horizon, actions triées par priorité décroissante. */
export function RoadmapTab({ assessmentId }: { assessmentId: string }) {
  const { data, isLoading, error } = useRoadmap(assessmentId)
  if (isLoading) return <Spinner />
  if (error || !data) return <ErrorBox error={error} />
  if (data.totalActions === 0) {
    return <Card><p className="text-sm text-ink-2">Aucune action : tous les domaines évalués atteignent leur cible (ou aucune réponse n’est encore saisie).</p></Card>
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-ink-2">
        {data.totalActions} actions générées. Priorité = (cible − niveau) × criticité × poids du domaine × 100.
      </p>
      <div className="grid gap-4 lg:grid-cols-4">
        {data.phases.map((phase) => (
          <section key={phase.horizon} className="space-y-3">
            <header className="rounded-md bg-surface px-3 py-2">
              <h2 className="font-semibold text-ink">{phase.label}</h2>
              <p className="text-xs text-ink-3">{phase.period} · {phase.actions.length} action(s)</p>
            </header>
            {phase.actions.map((a) => (
              <article key={a.questionCode} className="space-y-2 rounded-lg border border-line bg-surface p-3 text-sm">
                <div className="flex items-center justify-between gap-2">
                  <span className="font-mono text-xs font-semibold text-ink-3">{a.questionCode}</span>
                  <span className="text-xs text-ink-2">Priorité <strong className="text-ink">{fmt2(a.priority)}</strong></span>
                </div>
                <p className="font-medium text-ink">{a.action}</p>
                {a.followUpActions.length > 0 && (
                  <ul className="list-disc space-y-0.5 pl-4 text-xs text-ink-2">
                    {a.followUpActions.map((f) => <li key={f}>Ensuite : {f}</li>)}
                  </ul>
                )}
                <div className="flex flex-wrap gap-1.5">
                  <Pill>Niveau {a.currentLevel} → {a.targetLevel}</Pill>
                  <Pill tone={a.criticality === 3 ? 'critical' : 'neutral'}>{criticalityLabels[a.criticality]}</Pill>
                  <Pill>Effort {effortLabels[a.effort].toLowerCase()}</Pill>
                </div>
              </article>
            ))}
          </section>
        ))}
      </div>
    </div>
  )
}
