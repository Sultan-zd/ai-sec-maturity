import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router'
import { ShieldCheck } from 'lucide-react'
import { useAuth } from '../auth/context'
import { Button, ErrorBox, Field, Input } from '../components/ui'

export function LoginPage() {
  const { user, login } = useAuth()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState<unknown>(null)
  const [pending, setPending] = useState(false)

  if (user) return <Navigate to="/" replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setPending(true)
    setError(null)
    try {
      await login(email.trim(), password)
    } catch (e) {
      setError(e)
    } finally {
      setPending(false)
    }
  }

  return (
    <div className="flex min-h-screen items-center justify-center px-4">
      <div className="w-full max-w-sm space-y-6">
        <div className="text-center">
          <ShieldCheck className="mx-auto size-10 text-brand" aria-hidden />
          <h1 className="mt-3 text-2xl font-semibold text-ink">AIMA</h1>
          <p className="mt-1 text-sm text-ink-2">
            Évaluation de la maturité en cybersécurité et gouvernance de l’intelligence artificielle
          </p>
        </div>
        <form onSubmit={submit} className="space-y-4 rounded-lg border border-line bg-surface p-6">
          <Field label="Adresse e-mail">
            <Input type="email" autoComplete="username" required value={email}
              onChange={(e) => setEmail(e.target.value)} autoFocus />
          </Field>
          <Field label="Mot de passe">
            <Input type="password" autoComplete="current-password" required value={password}
              onChange={(e) => setPassword(e.target.value)} />
          </Field>
          <ErrorBox error={error} />
          <Button type="submit" loading={pending} className="w-full">Se connecter</Button>
        </form>
        <p className="text-center text-xs text-ink-3">
          Référentiel fondé sur ISO/IEC 27001, 42001, 23894, 38507, NIST AI RMF, NIST AI 600-1 et OWASP Top 10 LLM.
        </p>
      </div>
    </div>
  )
}
