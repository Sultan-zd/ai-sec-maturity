import { NavLink, Outlet } from 'react-router'
import { ClipboardCheck, LogOut, ShieldCheck } from 'lucide-react'
import { useAuth } from '../auth/context'
import { roleLabels } from '../lib/format'

/** Gabarit commun : barre de navigation (liens selon le rôle) + contenu de la page. */
export function Layout() {
  const { user, logout, hasRole } = useAuth()
  const link = ({ isActive }: { isActive: boolean }) =>
    `rounded-md px-3 py-1.5 text-sm font-medium ${isActive ? 'bg-surface-2 text-ink' : 'text-ink-2 hover:text-ink'}`

  return (
    <div className="min-h-screen">
      <header className="border-b border-line bg-surface">
        <div className="mx-auto flex max-w-7xl flex-wrap items-center gap-x-6 gap-y-2 px-4 py-3">
          <NavLink to="/" className="flex items-center gap-2 font-semibold text-brand">
            <ShieldCheck className="size-5" aria-hidden />
            AIMA
            <span className="hidden text-xs font-normal text-ink-3 sm:inline">Maturité sécurité &amp; gouvernance IA</span>
          </NavLink>
          <nav className="flex flex-1 flex-wrap gap-1">
            <NavLink to="/" end className={link}>
              <span className="inline-flex items-center gap-1.5"><ClipboardCheck className="size-4" aria-hidden />Évaluations</span>
            </NavLink>
            {hasRole('ADMIN') && <NavLink to="/organisations" className={link}>Organisations</NavLink>}
            {hasRole('ADMIN') && <NavLink to="/utilisateurs" className={link}>Utilisateurs</NavLink>}
          </nav>
          {user && (
            <div className="flex items-center gap-3 text-sm">
              <div className="text-right leading-tight">
                <div className="font-medium text-ink">{user.fullName}</div>
                <div className="text-xs text-ink-3">
                  {roleLabels[user.role]}{user.organizationName ? ` · ${user.organizationName}` : ''}
                </div>
              </div>
              <button onClick={logout} className="rounded-md p-2 text-ink-2 hover:bg-surface-2" title="Se déconnecter" aria-label="Se déconnecter">
                <LogOut className="size-4" />
              </button>
            </div>
          )}
        </div>
      </header>
      <main className="mx-auto max-w-7xl px-4 py-6">
        <Outlet />
      </main>
    </div>
  )
}
