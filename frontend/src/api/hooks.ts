import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { api } from './client'
import type {
  Answer, AnswerRequest, AssessmentResults, AssessmentSummary, CompanySize, Organization, Referential,
  ReviewRequest, Roadmap, Role, Sector, User,
} from './types'

/**
 * Accès aux données avec TanStack Query : cache, rechargement automatique et invalidation
 * ciblée après chaque modification (une réponse enregistrée rafraîchit les résultats, etc.).
 */

export const keys = {
  me: ['me'] as const,
  organizations: ['organizations'] as const,
  users: ['users'] as const,
  assessments: ['assessments'] as const,
  assessment: (id: string) => ['assessment', id] as const,
  answers: (id: string) => ['assessment', id, 'answers'] as const,
  results: (id: string) => ['assessment', id, 'results'] as const,
  roadmap: (id: string) => ['assessment', id, 'roadmap'] as const,
  referential: (id: number) => ['referential', id] as const,
}

// ---------------------------------------------------------------- lecture

export const useOrganizations = () =>
  useQuery({ queryKey: keys.organizations, queryFn: () => api.get<Organization[]>('/organizations') })

export const useUsers = () => useQuery({ queryKey: keys.users, queryFn: () => api.get<User[]>('/users') })

export const useAssessments = () =>
  useQuery({ queryKey: keys.assessments, queryFn: () => api.get<AssessmentSummary[]>('/assessments') })

export const useAssessment = (id: string) =>
  useQuery({ queryKey: keys.assessment(id), queryFn: () => api.get<AssessmentSummary>(`/assessments/${id}`) })

export const useAnswers = (id: string) =>
  useQuery({ queryKey: keys.answers(id), queryFn: () => api.get<Answer[]>(`/assessments/${id}/answers`) })

export const useResults = (id: string) =>
  useQuery({
    queryKey: keys.results(id),
    queryFn: () => api.get<AssessmentResults>(`/assessments/${id}/results`),
  })

export const useRoadmap = (id: string) =>
  useQuery({ queryKey: keys.roadmap(id), queryFn: () => api.get<Roadmap>(`/assessments/${id}/roadmap`) })

/** Une version du référentiel ne change jamais : elle est gardée en cache indéfiniment. */
export const useReferential = (id: number | undefined) =>
  useQuery({
    queryKey: keys.referential(id ?? 0),
    queryFn: () => api.get<Referential>(`/referentials/${id}`),
    enabled: id !== undefined,
    staleTime: Infinity,
  })

// ---------------------------------------------------------------- écriture

/** Après une action sur une évaluation, on rafraîchit tout ce qui en dépend. */
function useInvalidateAssessment(id: string) {
  const client = useQueryClient()
  return () => {
    client.invalidateQueries({ queryKey: ['assessment', id] })
    client.invalidateQueries({ queryKey: keys.assessments })
  }
}

export function useSaveAnswer(assessmentId: string) {
  const invalidate = useInvalidateAssessment(assessmentId)
  const client = useQueryClient()
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: AnswerRequest }) =>
      api.put<Answer>(`/assessments/${assessmentId}/answers/${code}`, body),
    onSuccess: (saved) => {
      // Mise à jour immédiate de la liste en cache, sans attendre le rechargement
      client.setQueryData<Answer[]>(keys.answers(assessmentId), (old) =>
        old ? [...old.filter((a) => a.questionCode !== saved.questionCode), saved] : [saved])
      invalidate()
    },
  })
}

export function useReviewAnswer(assessmentId: string) {
  const invalidate = useInvalidateAssessment(assessmentId)
  return useMutation({
    mutationFn: ({ code, body }: { code: string; body: ReviewRequest }) =>
      api.put<Answer>(`/assessments/${assessmentId}/answers/${code}/review`, body),
    onSuccess: invalidate,
  })
}

export function useAssessmentAction(assessmentId: string) {
  const invalidate = useInvalidateAssessment(assessmentId)
  return useMutation({
    mutationFn: (action: 'submit' | 'reopen' | 'validate') =>
      api.post<AssessmentSummary>(`/assessments/${assessmentId}/${action}`),
    onSuccess: invalidate,
  })
}

export function useUpdateTargets(assessmentId: string) {
  const invalidate = useInvalidateAssessment(assessmentId)
  return useMutation({
    mutationFn: (targets: Record<string, number>) =>
      api.put<AssessmentSummary>(`/assessments/${assessmentId}/targets`, { targets }),
    onSuccess: invalidate,
  })
}

export function useCreateAssessment() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: { organizationId: string; title: string; scope?: string }) =>
      api.post<AssessmentSummary>('/assessments', body),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.assessments }),
  })
}

export function useCreateOrganization() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: { name: string; sector: Sector; size: CompanySize; country?: string; description?: string }) =>
      api.post<Organization>('/organizations', body),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.organizations }),
  })
}

export function useCreateUser() {
  const client = useQueryClient()
  return useMutation({
    mutationFn: (body: { email: string; fullName: string; password: string; role: Role; organizationId?: string }) =>
      api.post<User>('/users', body),
    onSuccess: () => client.invalidateQueries({ queryKey: keys.users }),
  })
}
