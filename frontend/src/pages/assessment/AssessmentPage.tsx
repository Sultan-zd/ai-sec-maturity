import { Suspense, lazy, useState } from 'react'
import { Link, useParams, useSearchParams } from 'react-router'
import { ArrowLeft, Download, RotateCcw, Send, ShieldCheck } from 'lucide-react'
import { api } from '../../api/client'
import { useAnswers, useAssessment, useAssessmentAction, useReferential } from '../../api/hooks'
import { useAuth } from '../../auth/context'
import { Button, ErrorBox, LevelBadge, Pill, Progress, Spinner } from '../../components/ui'
import { fmtDate, statusLabels } from '../../lib/format'
import { QuestionnaireTab } from './QuestionnaireTab'
import { RoadmapTab } from './RoadmapTab'

// Les graphiques (Recharts) ne sont téléchargés qu'à l'ouverture de l'onglet Résultats.
const ResultsTab = lazy(() => import('./ResultsTab').then((m) => ({ default: m.ResultsTab })))

const tabs = [
  { key: 'questionnaire', label: 'Questionnaire' },
  { key: 'resultats', label: 'Résultats' },
  { key: 'feuille-de-route', label: 'Feuille de route' },
] as const

export function AssessmentPage() {
  const { id = '' } = useParams()
  const [params, setParams] = useSearchParams()
  const tab = params.get('onglet') ?? 'questionnaire'
  const { hasRole } = useAuth()

  const summary = useAssessment(id)
  const referential = useReferential(summary.data?.referentialId)
  const answers = useAnswers(id)
  const action = useAssessmentAction(id)
  const [downloadError, setDownloadError] = useState<unknown>(null)
  const [downloading, setDownloading] = useState(false)

  if (summary.isLoading || referential.isLoading || answers.isLoading) return <Spinner />
  if (summary.error || !summary.data) return <ErrorBox error={summary.error} />
  if (referential.error || !referential.data) return <ErrorBox error={referential.error} />

  const a = summary.data
  const isRespondent = hasRole('ADMIN', 'ORG_USER')
  const isAuditor = hasRole('ADMIN', 'AUDITOR')
  const canEdit = isRespondent && a.status === 'DRAFT'
  const canReview = isAuditor && a.status === 'SUBMITTED'
  const allReviewed = a.answeredCount > 0 && a.reviewedCount === a.answeredCount

  async function downloadPdf() {
    setDownloading(true)
    setDownloadError(null)
    try {
      await api.download(`/assessments/${id}/report.pdf`, 'rapport-aima.pdf')
    } catch (e) {
      setDownloadError(e)
    } finally {
      setDownloading(false)
    }
  }

  function run(kind: 'submit' | 'reopen' | 'validate', confirmation: string) {
    if (window.confirm(confirmation)) action.mutate(kind)
  }

  return (
    <div className="space-y-6">
      <Link to="/" className="inline-flex items-center gap-1 text-sm text-ink-2 hover:text-ink">
        <ArrowLeft className="size-4" aria-hidden /> Toutes les évaluations
      </Link>

      {/* En-tête */}
      <header className="space-y-4 rounded-lg border border-line bg-surface p-5">
        <div className="flex flex-wrap items-start justify-between gap-4">
          <div className="space-y-1">
            <div className="flex flex-wrap items-center gap-2">
              <h1 className="text-2xl font-semibold text-ink">{a.title}</h1>
              <Pill tone={a.status === 'DRAFT' ? 'neutral' : 'info'}>{statusLabels[a.status]}</Pill>
            </div>
            <p className="text-sm text-ink-2">
              {a.organizationName} · Référentiel AIMA v{a.referentialVersion}
              {a.scope ? ` · Périmètre : ${a.scope}` : ''}
            </p>
            <p className="text-xs text-ink-3">
              Créée le {fmtDate(a.createdAt)}
              {a.submittedAt && ` · soumise le ${fmtDate(a.submittedAt)}`}
              {a.validatedAt && ` · validée le ${fmtDate(a.validatedAt)}`}
            </p>
          </div>
          <div className="flex flex-wrap gap-2">
            <Button variant="secondary" onClick={downloadPdf} loading={downloading}>
              <Download className="size-4" aria-hidden /> Rapport PDF
            </Button>
            {isRespondent && a.status === 'DRAFT' && (
              <Button loading={action.isPending} disabled={a.answeredCount < a.totalQuestions}
                title={a.answeredCount < a.totalQuestions ? 'Répondez à toutes les questions avant de soumettre' : undefined}
                onClick={() => run('submit', 'Soumettre l’évaluation ? Les réponses seront verrouillées jusqu’à une éventuelle réouverture.')}>
                <Send className="size-4" aria-hidden /> Soumettre
              </Button>
            )}
            {isRespondent && a.status === 'SUBMITTED' && (
              <Button variant="secondary" loading={action.isPending}
                onClick={() => run('reopen', 'Rouvrir l’évaluation ? Les revues déjà effectuées par l’auditeur seront annulées.')}>
                <RotateCcw className="size-4" aria-hidden /> Rouvrir
              </Button>
            )}
            {isAuditor && a.status === 'SUBMITTED' && (
              <Button loading={action.isPending} disabled={!allReviewed}
                title={!allReviewed ? 'Revoyez toutes les réponses avant de valider' : undefined}
                onClick={() => run('validate', 'Valider l’audit ? Le score audité sera figé et l’évaluation ne sera plus modifiable.')}>
                <ShieldCheck className="size-4" aria-hidden /> Valider l’audit
              </Button>
            )}
          </div>
        </div>
        <div className="grid gap-4 md:grid-cols-3">
          <Progress value={a.completionPercent} label={`${a.answeredCount} / ${a.totalQuestions} réponses`} />
          {a.status !== 'DRAFT' && (
            <Progress value={a.answeredCount ? (100 * a.reviewedCount) / a.answeredCount : 0}
              label={`${a.reviewedCount} / ${a.answeredCount} revues par l’auditeur`} />
          )}
          <div className="text-sm"><span className="text-ink-3">Score figé : </span><LevelBadge level={a.globalLevel} score={a.globalScore} /></div>
        </div>
        <ErrorBox error={action.error ?? downloadError} />
      </header>

      {/* Onglets */}
      <div role="tablist" className="flex gap-1 border-b border-line">
        {tabs.map((t) => (
          <button key={t.key} role="tab" aria-selected={tab === t.key}
            onClick={() => setParams({ onglet: t.key }, { replace: true })}
            className={`-mb-px border-b-2 px-4 py-2 text-sm font-medium transition
              ${tab === t.key ? 'border-[var(--series-actual)] text-ink' : 'border-transparent text-ink-2 hover:text-ink'}`}>
            {t.label}
          </button>
        ))}
      </div>

      {tab === 'questionnaire' && (
        <QuestionnaireTab summary={a} referential={referential.data} answers={answers.data ?? []}
          canEdit={canEdit} canReview={canReview} />
      )}
      {tab === 'resultats' && (
        <Suspense fallback={<Spinner />}><ResultsTab summary={a} canEditTargets={canEdit} /></Suspense>
      )}
      {tab === 'feuille-de-route' && <RoadmapTab assessmentId={id} />}
    </div>
  )
}
