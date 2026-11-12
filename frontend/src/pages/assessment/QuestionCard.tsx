import { useState } from 'react'
import { Check, CircleSlash, FileCheck2, Pencil } from 'lucide-react'
import { useReviewAnswer, useSaveAnswer } from '../../api/hooks'
import type { Answer, Question } from '../../api/types'
import { Button, ErrorBox, Input, Pill, Select, Textarea } from '../../components/ui'
import { criticalityLabels, effortLabels, levelColor, levelNames } from '../../lib/format'

interface Props {
  assessmentId: string
  question: Question
  answer: Answer | undefined
  canEdit: boolean
  canReview: boolean
}

/** Une question du questionnaire : choix du niveau, N/A, preuve, commentaire et revue de l'auditeur. */
export function QuestionCard({ assessmentId, question, answer, canEdit, canReview }: Props) {
  const save = useSaveAnswer(assessmentId)
  const [evidence, setEvidence] = useState(answer?.evidence ?? '')
  const [comment, setComment] = useState(answer?.comment ?? '')
  const [naOpen, setNaOpen] = useState(false)
  const [justification, setJustification] = useState(answer?.justification ?? '')
  // Les champs texte ne sont modifiés que depuis cette carte : l'état local fait foi après le
  // premier affichage (pas de resynchronisation, qui ferait perdre la saisie en cours ou le focus).

  const selected = answer && !answer.notApplicable ? answer.level : null

  function chooseLevel(level: number) {
    save.mutate({ code: question.code, body: { level, notApplicable: false, evidence, comment } })
  }

  /** Les champs texte sont enregistrés quand on quitte le champ, s'ils ont changé. */
  function saveTexts() {
    if (!answer) return
    if ((answer.evidence ?? '') === evidence && (answer.comment ?? '') === comment) return
    save.mutate({
      code: question.code,
      body: answer.notApplicable
        ? { notApplicable: true, justification: answer.justification, evidence, comment }
        : { level: answer.level, notApplicable: false, evidence, comment },
    })
  }

  function markNotApplicable() {
    save.mutate(
      { code: question.code, body: { notApplicable: true, justification, evidence, comment } },
      { onSuccess: () => setNaOpen(false) },
    )
  }

  return (
    <article id={question.code} className="scroll-mt-20 rounded-lg border border-line bg-surface p-5">
      <header className="flex flex-wrap items-start justify-between gap-2">
        <div className="space-y-1">
          <div className="flex flex-wrap items-center gap-2">
            <span className="font-mono text-xs font-semibold text-ink-3">{question.code}</span>
            <Pill tone={question.criticality === 3 ? 'critical' : 'neutral'}>
              Criticité : {criticalityLabels[question.criticality]}
            </Pill>
            <Pill>Effort : {effortLabels[question.effort]}</Pill>
          </div>
          <h3 className="font-medium text-ink">{question.text}</h3>
        </div>
        <SaveState answer={answer} pending={save.isPending} />
      </header>
      <p className="mt-2 flex items-start gap-1.5 text-xs text-ink-3">
        <FileCheck2 className="mt-0.5 size-3.5 shrink-0" aria-hidden /> Preuves attendues : {question.evidence}
      </p>

      {/* Les 5 niveaux de réponse */}
      <fieldset className="mt-4 space-y-1.5" disabled={!canEdit || save.isPending}>
        <legend className="sr-only">Niveau de maturité constaté</legend>
        {question.levels.map((l) => {
          const isSelected = selected === l.level
          return (
            <label key={l.level}
              className={`flex cursor-pointer items-start gap-3 rounded-md border px-3 py-2 text-sm transition
                ${isSelected ? 'border-[var(--series-actual)] bg-[color-mix(in_oklab,var(--series-actual)_8%,transparent)]' : 'border-line hover:bg-surface-2'}
                ${!canEdit ? 'cursor-default' : ''}`}>
              <input type="radio" name={`level-${question.code}`} className="mt-1 accent-[var(--series-actual)]"
                checked={isSelected} onChange={() => chooseLevel(l.level)} />
              <span className="w-36 shrink-0 whitespace-nowrap font-medium text-ink">
                <span className="mr-1.5 inline-block size-2 rounded-full align-middle" style={{ background: levelColor(l.level) }} aria-hidden />
                {l.level} · {levelNames[l.level - 1]}
              </span>
              <span className="text-ink-2">{l.description}</span>
            </label>
          )
        })}
      </fieldset>

      {/* Non applicable */}
      {answer?.notApplicable && (
        <div className="mt-3 rounded-md border border-line bg-surface-2 p-3 text-sm">
          <span className="font-medium text-ink">Non applicable</span>
          <span className="text-ink-2"> — {answer.justification}</span>
        </div>
      )}
      {canEdit && !answer?.notApplicable && (
        naOpen ? (
          <div className="mt-3 space-y-2 rounded-md border border-line p-3">
            <Textarea rows={2} placeholder="Justification obligatoire (ex. : aucune application LLM déployée)"
              value={justification} onChange={(e) => setJustification(e.target.value)} />
            <div className="flex gap-2">
              <Button variant="secondary" disabled={!justification.trim()} loading={save.isPending} onClick={markNotApplicable}>
                Confirmer « Non applicable »
              </Button>
              <Button variant="ghost" onClick={() => setNaOpen(false)}>Annuler</Button>
            </div>
          </div>
        ) : (
          <button className="mt-2 inline-flex items-center gap-1 text-xs text-ink-2 hover:text-ink" onClick={() => setNaOpen(true)}>
            <CircleSlash className="size-3.5" aria-hidden /> Marquer comme non applicable
          </button>
        )
      )}

      {/* Preuve et commentaire */}
      {answer && (
        <div className="mt-3 grid gap-2 md:grid-cols-2">
          <Input placeholder="Référence de la preuve (document, lien…)" value={evidence} disabled={!canEdit}
            onChange={(e) => setEvidence(e.target.value)} onBlur={saveTexts} maxLength={1000} />
          <Input placeholder="Commentaire" value={comment} disabled={!canEdit}
            onChange={(e) => setComment(e.target.value)} onBlur={saveTexts} maxLength={2000} />
        </div>
      )}

      <div className="mt-2"><ErrorBox error={save.error} /></div>

      {answer && answer.reviewStatus !== 'PENDING' && <ReviewResult answer={answer} />}
      {canReview && answer && <ReviewPanel assessmentId={assessmentId} answer={answer} />}
    </article>
  )
}

function SaveState({ answer, pending }: { answer: Answer | undefined; pending: boolean }) {
  if (pending) return <span className="text-xs text-ink-3">Enregistrement…</span>
  if (!answer) return <span className="text-xs text-ink-3">Sans réponse</span>
  return (
    <span className="inline-flex items-center gap-1 text-xs text-ink-2">
      <Check className="size-3.5 text-[var(--status-good)]" aria-hidden /> Enregistrée
    </span>
  )
}

function ReviewResult({ answer }: { answer: Answer }) {
  const adjusted = answer.reviewStatus === 'ADJUSTED'
  return (
    <div className={`mt-3 rounded-md border p-3 text-sm ${adjusted ? 'border-[var(--status-serious)]' : 'border-line'}`}>
      <span className="font-medium text-ink">
        {adjusted ? `Corrigé par l’auditeur : niveau ${answer.auditedLevel} (déclaré : ${answer.notApplicable ? 'N/A' : answer.level})`
          : 'Confirmé par l’auditeur'}
      </span>
      {answer.auditorComment && <p className="mt-1 text-ink-2">{answer.auditorComment}</p>}
    </div>
  )
}

/** Panneau de l'auditeur : confirmer la réponse ou retenir un autre niveau (commentaire obligatoire). */
function ReviewPanel({ assessmentId, answer }: { assessmentId: string; answer: Answer }) {
  const review = useReviewAnswer(assessmentId)
  const [adjusting, setAdjusting] = useState(false)
  const [level, setLevel] = useState(answer.auditedLevel ?? answer.level ?? 3)
  const [note, setNote] = useState(answer.auditorComment ?? '')

  return (
    <div className="mt-3 space-y-2 rounded-md border border-dashed border-line p-3">
      <div className="text-xs font-semibold uppercase tracking-wide text-ink-3">Revue de l’auditeur</div>
      {!adjusting ? (
        <div className="flex flex-wrap gap-2">
          <Button variant="secondary" loading={review.isPending}
            onClick={() => review.mutate({ code: answer.questionCode, body: { decision: 'CONFIRMED', auditorComment: note || undefined } })}>
            <Check className="size-4" aria-hidden /> Confirmer
          </Button>
          <Button variant="secondary" onClick={() => setAdjusting(true)}>
            <Pencil className="size-4" aria-hidden /> Corriger le niveau
          </Button>
        </div>
      ) : (
        <div className="grid gap-2 md:grid-cols-[10rem_1fr_auto]">
          <Select value={level} onChange={(e) => setLevel(Number(e.target.value))} aria-label="Niveau retenu">
            {levelNames.map((n, i) => <option key={n} value={i + 1}>{i + 1} · {n}</option>)}
          </Select>
          <Input placeholder="Justification de la correction (obligatoire)" value={note} onChange={(e) => setNote(e.target.value)} />
          <div className="flex gap-2">
            <Button disabled={!note.trim()} loading={review.isPending}
              onClick={() => review.mutate(
                { code: answer.questionCode, body: { decision: 'ADJUSTED', auditedLevel: level, auditorComment: note } },
                { onSuccess: () => setAdjusting(false) },
              )}>Valider</Button>
            <Button variant="ghost" onClick={() => setAdjusting(false)}>Annuler</Button>
          </div>
        </div>
      )}
      <ErrorBox error={review.error} />
    </div>
  )
}
