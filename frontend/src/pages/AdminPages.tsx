import { useState, type FormEvent } from 'react'
import { useCreateOrganization, useCreateUser, useOrganizations, useUsers } from '../api/hooks'
import type { CompanySize, Role, Sector } from '../api/types'
import { Button, Card, ErrorBox, Field, Input, Select, Spinner } from '../components/ui'
import { fmtDate, roleLabels, sectorLabels, sizeLabels } from '../lib/format'

const th = 'px-3 py-2 text-left text-xs font-semibold uppercase tracking-wide text-ink-3'
const td = 'px-3 py-2 text-sm text-ink'

// ---------------------------------------------------------------- organisations

export function OrganizationsPage() {
  const { data, isLoading, error } = useOrganizations()
  const create = useCreateOrganization()
  const [form, setForm] = useState({ name: '', sector: 'BANKING_FINANCE' as Sector, size: 'PME' as CompanySize, country: 'Maroc' })

  async function submit(event: FormEvent) {
    event.preventDefault()
    await create.mutateAsync(form)
    setForm({ ...form, name: '' })
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-ink">Organisations</h1>
      <Card title="Ajouter une organisation">
        <form onSubmit={submit} className="grid gap-4 md:grid-cols-4">
          <Field label="Nom"><Input required value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} /></Field>
          <Field label="Secteur">
            <Select value={form.sector} onChange={(e) => setForm({ ...form, sector: e.target.value as Sector })}>
              {Object.entries(sectorLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </Select>
          </Field>
          <Field label="Taille">
            <Select value={form.size} onChange={(e) => setForm({ ...form, size: e.target.value as CompanySize })}>
              {Object.entries(sizeLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </Select>
          </Field>
          <Field label="Pays"><Input value={form.country} onChange={(e) => setForm({ ...form, country: e.target.value })} /></Field>
          <div className="md:col-span-4 space-y-3">
            <ErrorBox error={create.error} />
            <Button type="submit" loading={create.isPending}>Ajouter</Button>
          </div>
        </form>
      </Card>
      <Card title="Liste">
        {isLoading && <Spinner />}
        <ErrorBox error={error} />
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead><tr className="border-b border-line"><th className={th}>Nom</th><th className={th}>Secteur</th><th className={th}>Taille</th><th className={th}>Pays</th><th className={th}>Créée le</th></tr></thead>
            <tbody>
              {data?.map((o) => (
                <tr key={o.id} className="border-b border-line last:border-0">
                  <td className={`${td} font-medium`}>{o.name}</td>
                  <td className={td}>{sectorLabels[o.sector]}</td>
                  <td className={td}>{o.size}</td>
                  <td className={td}>{o.country ?? '—'}</td>
                  <td className={td}>{fmtDate(o.createdAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}

// ---------------------------------------------------------------- utilisateurs

export function UsersPage() {
  const { data, isLoading, error } = useUsers()
  const organizations = useOrganizations()
  const create = useCreateUser()
  const empty = { email: '', fullName: '', password: '', role: 'ORG_USER' as Role, organizationId: '' }
  const [form, setForm] = useState(empty)

  async function submit(event: FormEvent) {
    event.preventDefault()
    await create.mutateAsync({
      ...form,
      organizationId: form.role === 'ORG_USER' ? form.organizationId : undefined,
    })
    setForm(empty)
  }

  return (
    <div className="space-y-6">
      <h1 className="text-2xl font-semibold text-ink">Utilisateurs</h1>
      <Card title="Créer un compte">
        <form onSubmit={submit} className="grid gap-4 md:grid-cols-3">
          <Field label="Nom complet"><Input required value={form.fullName} onChange={(e) => setForm({ ...form, fullName: e.target.value })} /></Field>
          <Field label="E-mail"><Input type="email" required value={form.email} onChange={(e) => setForm({ ...form, email: e.target.value })} /></Field>
          <Field label="Mot de passe initial" hint="10 caractères minimum, avec au moins une lettre et un chiffre">
            <Input type="password" required minLength={10} autoComplete="new-password" value={form.password}
              onChange={(e) => setForm({ ...form, password: e.target.value })} />
          </Field>
          <Field label="Rôle">
            <Select value={form.role} onChange={(e) => setForm({ ...form, role: e.target.value as Role })}>
              {Object.entries(roleLabels).map(([k, v]) => <option key={k} value={k}>{v}</option>)}
            </Select>
          </Field>
          {form.role === 'ORG_USER' && (
            <Field label="Organisation">
              <Select required value={form.organizationId} onChange={(e) => setForm({ ...form, organizationId: e.target.value })}>
                <option value="">— Choisir —</option>
                {organizations.data?.map((o) => <option key={o.id} value={o.id}>{o.name}</option>)}
              </Select>
            </Field>
          )}
          <div className="md:col-span-3 space-y-3">
            <ErrorBox error={create.error} />
            <Button type="submit" loading={create.isPending}>Créer le compte</Button>
          </div>
        </form>
      </Card>
      <Card title="Comptes">
        {isLoading && <Spinner />}
        <ErrorBox error={error} />
        <div className="overflow-x-auto">
          <table className="w-full">
            <thead><tr className="border-b border-line"><th className={th}>Nom</th><th className={th}>E-mail</th><th className={th}>Rôle</th><th className={th}>Organisation</th><th className={th}>Dernière connexion</th></tr></thead>
            <tbody>
              {data?.map((u) => (
                <tr key={u.id} className="border-b border-line last:border-0">
                  <td className={`${td} font-medium`}>{u.fullName}</td>
                  <td className={td}>{u.email}</td>
                  <td className={td}>{roleLabels[u.role]}</td>
                  <td className={td}>{u.organizationName ?? '—'}</td>
                  <td className={td}>{fmtDate(u.lastLoginAt)}</td>
                </tr>
              ))}
            </tbody>
          </table>
        </div>
      </Card>
    </div>
  )
}
