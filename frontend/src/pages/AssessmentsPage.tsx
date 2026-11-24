import { useState, type FormEvent } from 'react'
import { Link, useNavigate } from 'react-router'
import { Plus, TrendingUp } from 'lucide-react'
import { useAssessments, useCreateAssessment, useOrganizations } from '../api/hooks'
import type { AssessmentStatus } from '../api/types'
import { useAuth } from '../auth/context'
import { Button, Card, ErrorBox, Field, Input, LevelBadge, Pill, Progress, Select, Spinner, Textarea } from '../components/ui'
import { fmtDate, statusLabels } from '../lib/format'

const statusTone: Record<AssessmentStatus, 'neutral' | 'info' | 'critical'> = {
  DRAFT: 'neutral',
  SUBMITTED: 'info',
  VALIDATED: 'info',
}

export function AssessmentsPage() {
  const { hasRole } = useAuth()
  const { data, isLoading, error } = useAssessments()
  const [creating, setCreating] = useState(false)

  return (
    <div className="space-y-6">
      <div className="flex flex-wrap items-end justify-between gap-3">
        <div>
          <h1 className="text-2xl font-semibold text-ink">Évaluations</h1>
          <p className="text-sm text-ink-2">
            {hasRole('AUDITOR') ? 'Évaluations soumises à revoir et historique des audits.'
              : 'Auto-évaluations de maturité de votre organisation.'}
          </p>
        </div>
        <div className="flex flex-wrap gap-2">
          {data && data.length >= 2 && (
            <Link to="/progression" className="inline-flex items-center gap-2 rounded-md border border-line px-3 py-2 text-sm font-medium text-ink hover:bg-surface-2">
              <TrendingUp className="size-4" aria-hidden />Comparer deux évaluations
            </Link>
          )}
          {hasRole('ADMIN', 'ORG_USER') && !creating && (
            <Button onClick={() => setCreating(true)}><Plus className="size-4" aria-hidden />Nouvelle évaluation</Button>
          )}
        </div>
      </div>

      {creating && <CreateAssessmentForm onClose={() => setCreating(false)} />}
      <ErrorBox error={error} />
      {isLoading && <Spinner />}

      {data && data.length === 0 && (
        <Card><p className="text-sm text-ink-2">Aucune évaluation pour le moment.</p></Card>
      )}

      <div className="grid gap-4 md:grid-cols-2 xl:grid-cols-3">
        {data?.map((a) => (
          <Link key={a.id} to={`/evaluations/${a.id}`}
            className="group rounded-lg border border-line bg-surface p-5 transition hover:border-[var(--series-actual)]">
            <div className="flex items-start justify-between gap-2">
              <div>
                <h2 className="font-semibold text-ink group-hover:underline">{a.title}</h2>
                <p className="text-sm text-ink-2">{a.organizationName}</p>
              </div>
              <Pill tone={statusTone[a.status]}>{statusLabels[a.status]}</Pill>
            </div>
            <div className="mt-4 space-y-3">
              <Progress value={a.completionPercent} label={`${a.answeredCount} / ${a.totalQuestions} réponses`} />
              {a.status !== 'DRAFT' && (
                <Progress value={a.answeredCount ? (100 * a.reviewedCount) / a.answeredCount : 0}
                  label={`${a.reviewedCount} réponses revues par l’auditeur`} />
              )}
              <div className="flex items-center justify-between text-sm">
                <LevelBadge level={a.globalLevel} score={a.globalScore} />
                <span className="text-xs text-ink-3">Modifiée le {fmtDate(a.updatedAt)}</span>
              </div>
            </div>
          </Link>
        ))}
      </div>
    </div>
  )
}

function CreateAssessmentForm({ onClose }: { onClose: () => void }) {
  const { user } = useAuth()
  const navigate = useNavigate()
  const organizations = useOrganizations()
  const create = useCreateAssessment()
  const [organizationId, setOrganizationId] = useState(user?.organizationId ?? '')
  const [title, setTitle] = useState(() => `Évaluation ${new Date().getFullYear()}`)
  const [scope, setScope] = useState('')

  async function submit(event: FormEvent) {
    event.preventDefault()
    const created = await create.mutateAsync({ organizationId, title, scope: scope || undefined })
    navigate(`/evaluations/${created.id}`)
  }

  return (
    <Card title="Nouvelle évaluation">
      <form onSubmit={submit} className="grid gap-4 md:grid-cols-2">
        {user?.role === 'ADMIN' && (
          <Field label="Organisation">
            <Select required value={organizationId} onChange={(e) => setOrganizationId(e.target.value)}>
              <option value="">— Choisir —</option>
              {organizations.data?.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
            </Select>
          </Field>
        )}
        <Field label="Titre">
          <Input required maxLength={255} value={title} onChange={(e) => setTitle(e.target.value)} />
        </Field>
        <div className="md:col-span-2">
          <Field label="Périmètre (optionnel)" hint="Ex. : chatbot client, scoring crédit, copilote interne…">
            <Textarea rows={2} maxLength={2000} value={scope} onChange={(e) => setScope(e.target.value)} />
          </Field>
        </div>
        <div className="md:col-span-2"><ErrorBox error={create.error} /></div>
        <div className="flex gap-2 md:col-span-2">
          <Button type="submit" loading={create.isPending}>Créer et commencer</Button>
          <Button type="button" variant="ghost" onClick={onClose}>Annuler</Button>
        </div>
      </form>
    </Card>
  )
}
