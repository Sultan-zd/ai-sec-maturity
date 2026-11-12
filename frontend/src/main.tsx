import { StrictMode, Suspense, type ReactNode } from 'react'
import { createRoot } from 'react-dom/client'
import { Navigate, RouterProvider, createBrowserRouter } from 'react-router'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { ApiError } from './api/client'
import { AuthProvider } from './auth/AuthContext'
import { RequireAuth } from './auth/RequireAuth'
import { Layout } from './components/Layout'
import { Spinner } from './components/ui'
import { AssessmentsPage } from './pages/AssessmentsPage'
import { LoginPage } from './pages/LoginPage'
import { AssessmentPage, OrganizationsPage, UsersPage } from './pages/lazyPages'
import './index.css'

const queryClient = new QueryClient({
  defaultOptions: {
    queries: {
      staleTime: 30_000,
      // On ne réessaie pas une requête refusée (401/403/404) : le résultat ne changerait pas.
      retry: (count, error) => !(error instanceof ApiError && error.status < 500) && count < 2,
    },
  },
})

const page = (element: ReactNode) => <Suspense fallback={<Spinner />}>{element}</Suspense>

const router = createBrowserRouter([
  { path: '/connexion', element: <LoginPage /> },
  {
    element: <RequireAuth><Layout /></RequireAuth>,
    children: [
      { path: '/', element: <AssessmentsPage /> },
      { path: '/evaluations/:id', element: page(<AssessmentPage />) },
      { path: '/organisations', element: <RequireAuth roles={['ADMIN']}>{page(<OrganizationsPage />)}</RequireAuth> },
      { path: '/utilisateurs', element: <RequireAuth roles={['ADMIN']}>{page(<UsersPage />)}</RequireAuth> },
      { path: '*', element: <Navigate to="/" replace /> },
    ],
  },
])

createRoot(document.getElementById('root')!).render(
  <StrictMode>
    <QueryClientProvider client={queryClient}>
      <AuthProvider>
        <RouterProvider router={router} />
      </AuthProvider>
    </QueryClientProvider>
  </StrictMode>,
)
