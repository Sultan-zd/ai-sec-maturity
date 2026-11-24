// Types TypeScript calqués sur les DTO Java du backend (package ma.uca.aima.*.api).

export type Role = 'ADMIN' | 'AUDITOR' | 'ORG_USER'
export type Effort = 'LOW' | 'MEDIUM' | 'HIGH'
export type AssessmentStatus = 'DRAFT' | 'SUBMITTED' | 'VALIDATED'
export type ReviewStatus = 'PENDING' | 'CONFIRMED' | 'ADJUSTED'
export type Horizon = 'QUICK_WIN' | 'SHORT_TERM' | 'MEDIUM_TERM' | 'LONG_TERM'
export type Sector =
  | 'BANKING_FINANCE' | 'INSURANCE' | 'HEALTHCARE' | 'PUBLIC_SECTOR' | 'TELECOM' | 'INDUSTRY'
  | 'ENERGY' | 'RETAIL' | 'TECHNOLOGY' | 'EDUCATION' | 'OTHER'
export type CompanySize = 'TPE' | 'PME' | 'ETI' | 'GE'

// ---------------------------------------------------------------- utilisateurs

export interface User {
  id: string
  email: string
  fullName: string
  role: Role
  organizationId: string | null
  organizationName: string | null
  enabled: boolean
  lastLoginAt: string | null
}

export interface LoginResponse {
  accessToken: string
  tokenType: string
  expiresAt: string
  user: User
}

export interface Organization {
  id: string
  name: string
  sector: Sector
  size: CompanySize
  country: string | null
  description: string | null
  createdAt: string
}

// ---------------------------------------------------------------- référentiel

export interface QuestionLevel {
  level: number
  description: string
  recommendation: string | null
}

export interface Question {
  code: string
  subdomainCode: string
  text: string
  evidence: string
  criticality: number
  effort: Effort
  levels: QuestionLevel[]
}

export interface Subdomain {
  code: string
  name: string
  description: string
  references: Record<string, string[]>
  questions: Question[]
}

export interface Domain {
  code: string
  name: string
  description: string
  weight: number
  order: number
  subdomains: Subdomain[]
}

export interface MaturityLevel {
  level: number
  name: string
  minScore: number
  description: string
}

export interface Referential {
  id: number
  code: string
  version: string
  name: string
  description: string
  defaultTargetLevel: number
  maturityLevels: MaturityLevel[]
  domains: Domain[]
}

// ---------------------------------------------------------------- évaluations

export interface AssessmentSummary {
  id: string
  title: string
  scope: string | null
  status: AssessmentStatus
  organizationId: string
  organizationName: string
  referentialId: number
  referentialVersion: string
  targets: Record<string, number>
  answeredCount: number
  totalQuestions: number
  completionPercent: number
  reviewedCount: number
  globalScore: number | null
  globalLevel: number | null
  createdAt: string
  updatedAt: string
  submittedAt: string | null
  validatedAt: string | null
}

export interface Answer {
  questionCode: string
  level: number | null
  notApplicable: boolean
  justification: string | null
  comment: string | null
  evidence: string | null
  updatedAt: string
  reviewStatus: ReviewStatus
  auditedLevel: number | null
  auditorComment: string | null
  reviewedAt: string | null
  effectiveLevel: number | null
}

export interface AnswerRequest {
  level?: number | null
  notApplicable?: boolean
  justification?: string | null
  comment?: string | null
  evidence?: string | null
}

export interface ReviewRequest {
  decision: 'CONFIRMED' | 'ADJUSTED'
  auditedLevel?: number
  auditorComment?: string
}

// ---------------------------------------------------------------- résultats

export interface Score {
  value: number
  level: number
  levelName: string
  percent: number
}

export interface SubdomainResult {
  code: string
  name: string
  score: Score | null
  answered: number
  notApplicable: number
  total: number
}

export interface DomainResult {
  code: string
  name: string
  weight: number
  score: Score | null
  rawScore: number | null
  cappedBy: string | null
  cap: number | null
  targetLevel: number
  gap: number | null
  subdomains: SubdomainResult[]
}

export interface Recommendation {
  questionCode: string
  questionText: string
  domainCode: string
  subdomainCode: string
  currentLevel: number
  targetLevel: number
  criticality: number
  effort: Effort
  priority: number
  horizon: Horizon
  action: string
  followUpActions: string[]
}

export interface ScoringResult {
  global: Score | null
  answered: number
  notApplicable: number
  total: number
  completionPercent: number
  domains: DomainResult[]
  recommendations: Recommendation[]
  /** false tant que la complétion est sous minCompletionPercent : le score global n'est qu'une estimation */
  globalReliable: boolean
  minCompletionPercent: number
}

export interface DomainComparison {
  code: string
  declared: number | null
  official: number | null
  proven: number | null
}

/** Fourchette : déclaré (organisation) / officiel (mesuré > audité > déclaré) / prouvé (sans preuve = niveau 1) */
export interface ScoreComparison {
  declared: Score | null
  official: Score | null
  proven: Score | null
  provenAnswers: number
  evaluatedAnswers: number
  domains: DomainComparison[]
}

export interface AssessmentResults {
  assessmentId: string
  status: AssessmentStatus
  evidenceRatePercent: number
  scoring: ScoringResult
  comparison: ScoreComparison
}

export interface DomainDelta {
  code: string
  name: string
  from: number | null
  to: number | null
  delta: number | null
}

export interface QuestionChange {
  questionCode: string
  fromLevel: number
  toLevel: number
}

/** Progression entre deux évaluations d'une même organisation (scores officiels). */
export interface AssessmentProgress {
  from: AssessmentSummary
  to: AssessmentSummary
  sameReferential: boolean
  fromGlobal: Score | null
  toGlobal: Score | null
  fromReliable: boolean
  toReliable: boolean
  globalDelta: number | null
  domains: DomainDelta[]
  improved: QuestionChange[]
  regressed: QuestionChange[]
  fromRecommendations: number
  toRecommendations: number
  closedRecommendations: string[]
}

export type AttackFamily = 'PROMPT_INJECTION' | 'PROMPT_LEAKING' | 'DATA_DISCLOSURE' | 'UNSAFE_TOOL_USE' | 'RAG_ACCESS_BREAK' | 'OTHER'

/** Mesure du labo red-team, comparée aux niveaux déclaré et audité (gap = déclaré − mesuré). */
export interface Measurement {
  questionCode: string
  attackFamily: AttackFamily
  measuredLevel: number
  declaredLevel: number | null
  auditedLevel: number | null
  gap: number | null
  tool: string
  runId: string
  evidence: string
  evidenceUrl: string | null
  measuredAt: string
  recordedAt: string
}

export interface RoadmapPhase {
  horizon: Horizon
  label: string
  period: string
  actions: Recommendation[]
}

export interface Roadmap {
  assessmentId: string
  totalActions: number
  phases: RoadmapPhase[]
}

/** Erreur au format RFC 9457 renvoyée par l'API. */
export interface ProblemDetail {
  status: number
  title: string
  detail?: string
  errors?: Record<string, string>
}
