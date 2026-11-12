import type { ReactNode } from 'react'
import { Navigate, Outlet } from 'react-router'
import type { Role } from '../api/types'
import { Spinner } from '../components/ui'
import { useAuth } from './context'

/**
 * Protège une page : connexion obligatoire, et éventuellement un rôle précis.
 * (Confort d'interface uniquement : la vraie protection est faite par l'API, qui renvoie 401/403.)
 */
export function RequireAuth({ roles, children }: { roles?: Role[]; children?: ReactNode }) {
  const { user, loading, hasRole } = useAuth()
  if (loading) return <Spinner label="Vérification de la session…" />
  if (!user) return <Navigate to="/connexion" replace />
  if (roles && !hasRole(...roles)) return <Navigate to="/" replace />
  return children ?? <Outlet />
}
