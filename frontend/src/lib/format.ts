import type { AssessmentStatus, AttackFamily, CompanySize, Effort, Horizon, Role, Sector } from '../api/types'

const number2 = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
const number1 = new Intl.NumberFormat('fr-FR', { minimumFractionDigits: 1, maximumFractionDigits: 1 })
const dateTime = new Intl.DateTimeFormat('fr-FR', { dateStyle: 'medium', timeStyle: 'short' })

export const fmt2 = (v: number | null | undefined) => (v == null ? '—' : number2.format(v))
export const fmt1 = (v: number | null | undefined) => (v == null ? '—' : number1.format(v))
export const fmtDate = (v: string | null | undefined) => (v ? dateTime.format(new Date(v)) : '—')

export const levelNames = ['Initial', 'Reproductible', 'Défini', 'Maîtrisé', 'Optimisé']

/** Couleur de statut associée à un niveau (1 = critique … 4-5 = bon). Toujours affichée avec un libellé. */
export function levelColor(level: number | null | undefined): string {
  switch (level) {
    case 1: return 'var(--status-critical)'
    case 2: return 'var(--status-serious)'
    case 3: return 'var(--status-warning)'
    case 4:
    case 5: return 'var(--status-good)'
    default: return 'var(--ink-3)'
  }
}

export const statusLabels: Record<AssessmentStatus, string> = {
  DRAFT: 'Brouillon',
  SUBMITTED: 'Soumise — en attente d’audit',
  VALIDATED: 'Validée par l’auditeur',
}

export const roleLabels: Record<Role, string> = {
  ADMIN: 'Administrateur',
  AUDITOR: 'Auditeur',
  ORG_USER: 'Organisation',
}

export const effortLabels: Record<Effort, string> = { LOW: 'Faible', MEDIUM: 'Moyen', HIGH: 'Élevé' }

export const criticalityLabels: Record<number, string> = { 1: 'Normale', 2: 'Importante', 3: 'Critique' }

export const horizonLabels: Record<Horizon, string> = {
  QUICK_WIN: 'Quick wins',
  SHORT_TERM: 'Court terme',
  MEDIUM_TERM: 'Moyen terme',
  LONG_TERM: 'Long terme',
}

export const sectorLabels: Record<Sector, string> = {
  BANKING_FINANCE: 'Banque et finance',
  INSURANCE: 'Assurance',
  HEALTHCARE: 'Santé',
  PUBLIC_SECTOR: 'Secteur public',
  TELECOM: 'Télécommunications',
  INDUSTRY: 'Industrie',
  ENERGY: 'Énergie',
  RETAIL: 'Commerce',
  TECHNOLOGY: 'Technologie',
  EDUCATION: 'Éducation',
  OTHER: 'Autre',
}

export const sizeLabels: Record<CompanySize, string> = {
  TPE: 'TPE (< 10)',
  PME: 'PME (< 250)',
  ETI: 'ETI (< 5 000)',
  GE: 'Grande entreprise',
}

export const attackFamilyLabels: Record<AttackFamily, string> = {
  PROMPT_INJECTION: 'Injection de prompt',
  PROMPT_LEAKING: 'Fuite du prompt système',
  DATA_DISCLOSURE: 'Divulgation de données',
  UNSAFE_TOOL_USE: 'Utilisation dangereuse d\'outils',
  RAG_ACCESS_BREAK: 'Contournement des droits RAG',
  OTHER: 'Autre attaque',
}
