import { lazy } from 'react'

// Pages chargées à la demande (code splitting) : le premier écran reste léger.
export const AssessmentPage = lazy(() =>
  import('./assessment/AssessmentPage').then((m) => ({ default: m.AssessmentPage })))
export const OrganizationsPage = lazy(() => import('./AdminPages').then((m) => ({ default: m.OrganizationsPage })))
export const UsersPage = lazy(() => import('./AdminPages').then((m) => ({ default: m.UsersPage })))
export const ProgressPage = lazy(() => import('./ProgressPage').then((m) => ({ default: m.ProgressPage })))
