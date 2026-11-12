import { useMemo, useState } from 'react'
import type { Answer, AssessmentSummary, Referential } from '../../api/types'
import { QuestionCard } from './QuestionCard'

interface Props {
  summary: AssessmentSummary
  referential: Referential
  answers: Answer[]
  canEdit: boolean
  canReview: boolean
}

/** Questionnaire : navigation par domaine à gauche, questions du domaine choisi à droite. */
export function QuestionnaireTab({ summary, referential, answers, canEdit, canReview }: Props) {
  const [domainCode, setDomainCode] = useState(referential.domains[0]?.code)
  const byCode = useMemo(() => new Map(answers.map((a) => [a.questionCode, a])), [answers])
  const domain = referential.domains.find((d) => d.code === domainCode) ?? referential.domains[0]

  const progress = (codes: string[]) => ({
    answered: codes.filter((c) => byCode.has(c)).length,
    reviewed: codes.filter((c) => byCode.get(c) && byCode.get(c)!.reviewStatus !== 'PENDING').length,
    total: codes.length,
  })

  return (
    <div className="grid gap-6 lg:grid-cols-[16rem_1fr]">
      <nav aria-label="Domaines" className="space-y-1 lg:sticky lg:top-4 lg:self-start">
        {referential.domains.map((d) => {
          const codes = d.subdomains.flatMap((sd) => sd.questions.map((q) => q.code))
          const p = progress(codes)
          const active = d.code === domain.code
          return (
            <button key={d.code} onClick={() => setDomainCode(d.code)}
              aria-current={active ? 'true' : undefined}
              className={`w-full rounded-md border px-3 py-2 text-left text-sm transition
                ${active ? 'border-[var(--series-actual)] bg-surface' : 'border-transparent hover:bg-surface'}`}>
              <div className="flex items-center justify-between gap-2">
                <span className="font-semibold text-ink">{d.code}</span>
                <span className={`text-xs ${p.answered === p.total ? 'text-[var(--status-good)]' : 'text-ink-3'}`}>
                  {p.answered}/{p.total}
                </span>
              </div>
              <div className="text-xs text-ink-2">{d.name}</div>
              {canReview && <div className="text-xs text-ink-3">{p.reviewed} revue(s)</div>}
            </button>
          )
        })}
      </nav>

      <div className="space-y-6">
        <div>
          <h2 className="text-lg font-semibold text-ink">{domain.code} — {domain.name}</h2>
          <p className="text-sm text-ink-2">
            {domain.description} · Poids {Math.round(domain.weight * 100)} % · Cible : niveau {summary.targets[domain.code]}
          </p>
        </div>
        {domain.subdomains.map((sd) => (
          <section key={sd.code} className="space-y-3">
            <div>
              <h3 className="font-semibold text-ink">{sd.code} — {sd.name}</h3>
              <p className="text-xs text-ink-3">
                Références : {Object.entries(sd.references).map(([src, clauses]) => `${src} ${clauses.join(', ')}`).join(' · ')}
              </p>
            </div>
            {sd.questions.map((q) => (
              <QuestionCard key={q.code} assessmentId={summary.id} question={q} answer={byCode.get(q.code)}
                canEdit={canEdit} canReview={canReview} />
            ))}
          </section>
        ))}
      </div>
    </div>
  )
}
