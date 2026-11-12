import { useCallback, useEffect, useMemo, useState, type ReactNode } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { api, setUnauthorizedHandler, tokenStore } from '../api/client'
import type { LoginResponse, User } from '../api/types'
import { AuthContext, type AuthState } from './context'

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [user, setUser] = useState<User | null>(null)
  const [loading, setLoading] = useState(() => tokenStore.get() !== null)

  const logout = useCallback(() => {
    tokenStore.clear()
    queryClient.clear()          // aucune donnée de l'utilisateur précédent ne doit rester en cache
    setUser(null)
  }, [queryClient])

  // Jeton expiré ou invalide → retour à l'écran de connexion
  useEffect(() => setUnauthorizedHandler(logout), [logout])

  // Au rechargement de la page : on retrouve l'utilisateur à partir du jeton conservé
  useEffect(() => {
    if (!tokenStore.get()) return
    api.get<User>('/auth/me')
      .then(setUser)
      .catch(() => logout())
      .finally(() => setLoading(false))
  }, [logout])

  const login = useCallback(async (email: string, password: string) => {
    const response = await api.post<LoginResponse>('/auth/login', { email, password })
    tokenStore.set(response.accessToken)
    setUser(response.user)
  }, [])

  const value = useMemo<AuthState>(() => ({
    user,
    loading,
    login,
    logout,
    hasRole: (...roles) => user !== null && roles.includes(user.role),
  }), [user, loading, login, logout])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}
