// Joue un scénario d'étude de cas de bout en bout via l'API REST AIMA :
// organisation → comptes → auto-évaluation → soumission → audit → validation → export.
//
// Usage : node scripts/case-study/run.mjs [scenario.json]
// Variables : AIMA_URL (défaut http://localhost:8088), AIMA_ADMIN_EMAIL, AIMA_ADMIN_PASSWORD
import { existsSync, mkdirSync, readFileSync, writeFileSync } from 'node:fs'
import { dirname, join } from 'node:path'
import { fileURLToPath } from 'node:url'

const here = dirname(fileURLToPath(import.meta.url))
const root = join(here, '..', '..')
const scenario = JSON.parse(readFileSync(process.argv[2] ?? join(here, 'scenario-banque-atlas.json'), 'utf8'))
const BASE = (process.env.AIMA_URL ?? 'http://localhost:8088') + '/api/v1'
const OUT = join(root, 'docs', 'etude-de-cas')

async function call(token, method, path, body) {
  const res = await fetch(BASE + path, {
    method,
    headers: { 'Content-Type': 'application/json', ...(token && { Authorization: `Bearer ${token}` }) },
    body: body === undefined ? undefined : JSON.stringify(body),
  })
  if (!res.ok) throw new Error(`${method} ${path} → ${res.status} ${await res.text()}`)
  if (res.status === 204) return null
  return (res.headers.get('content-type') ?? '').includes('json') ? res.json() : Buffer.from(await res.arrayBuffer())
}
const login = async (email, password) => (await call(null, 'POST', '/auth/login', { email, password })).accessToken

// ---- 1. Administrateur : organisation et comptes (réutilisés s'ils existent déjà)
const admin = await login(process.env.AIMA_ADMIN_EMAIL ?? 'admin@aima.local', process.env.AIMA_ADMIN_PASSWORD ?? 'Admin@2026!')
const orgs = await call(admin, 'GET', '/organizations')
const org = orgs.find((o) => o.name === scenario.organization.name) ?? await call(admin, 'POST', '/organizations', scenario.organization)
const users = await call(admin, 'GET', '/users')
for (const [account, role, orgId] of [[scenario.respondent, 'ORG_USER', org.id], [scenario.auditor, 'AUDITOR', null]]) {
  if (!users.some((u) => u.email === account.email)) {
    await call(admin, 'POST', '/users', { ...account, role, organizationId: orgId })
  }
}
const respondent = await login(scenario.respondent.email, scenario.respondent.password)
const auditor = await login(scenario.auditor.email, scenario.auditor.password)

// ---- 2. Auto-évaluation par l'organisation (une évaluation existante du même titre est supprimée si brouillon)
// Une évaluation déjà validée est définitive : on se contente de réexporter ses résultats.
let assessment = null
for (const a of await call(respondent, 'GET', `/assessments?organizationId=${org.id}`)) {
  if (a.title !== scenario.assessment.title) continue
  if (a.status === 'VALIDATED') assessment = a
  else if (a.status === 'DRAFT') await call(respondent, 'DELETE', `/assessments/${a.id}`)
  else await call(respondent, 'POST', `/assessments/${a.id}/reopen`).then(() => call(respondent, 'DELETE', `/assessments/${a.id}`))
}
const rerun = assessment !== null
assessment ??= await call(respondent, 'POST', '/assessments', { organizationId: org.id, ...scenario.assessment })
const base = `/assessments/${assessment.id}`

// Scores « déclarés » (avant audit) : capturés avant la soumission, ou relus depuis l'export précédent
let declared
if (rerun) {
  console.log(`Évaluation déjà validée (score audité ${assessment.globalScore}) : réexport uniquement.`)
  const previous = join(OUT, 'resultats.json')
  if (!existsSync(previous)) throw new Error('Scores déclarés introuvables : supprimer l\'évaluation ou restaurer resultats.json')
  declared = { scoring: JSON.parse(readFileSync(previous, 'utf8')).declared }
} else {
  for (const [code, answer] of Object.entries(scenario.answers)) {
    await call(respondent, 'PUT', `${base}/answers/${code}`, answer)
  }
  declared = await call(respondent, 'GET', `${base}/results`)
  const submitted = await call(respondent, 'POST', `${base}/submit`)
  console.log(`Auto-évaluation soumise : score déclaré ${submitted.globalScore} (niveau ${submitted.globalLevel})`)

  // ---- 3. Audit : l'auditeur confirme ou corrige chaque réponse, puis valide
  for (const code of Object.keys(scenario.answers)) {
    const adjustment = scenario.audit[code]
    await call(auditor, 'PUT', `${base}/answers/${code}/review`,
      adjustment ? { decision: 'ADJUSTED', ...adjustment } : { decision: 'CONFIRMED' })
  }
  const validated = await call(auditor, 'POST', `${base}/validate`)
  console.log(`Audit validé : score audité ${validated.globalScore} (niveau ${validated.globalLevel})`)
}

// ---- 4. Export des résultats (données brutes + rapport PDF) pour le document d'analyse
const results = await call(respondent, 'GET', `${base}/results`)
const roadmap = await call(respondent, 'GET', `${base}/roadmap`)
const pdf = await call(respondent, 'GET', `${base}/report.pdf`)
mkdirSync(OUT, { recursive: true })
writeFileSync(join(OUT, 'resultats.json'), JSON.stringify({
  organization: org.name, assessmentId: assessment.id,
  declared: declared.scoring, audited: results.scoring, evidenceRatePercent: results.evidenceRatePercent, roadmap,
}, null, 2) + '\n')
writeFileSync(join(OUT, 'rapport-banque-atlas.pdf'), pdf)

const fmt = (s) => (s ? Number(s.value).toFixed(2) : 'N/A')
console.log('\nDomaine   déclaré  audité  brut   cible  écart  plafond')
for (const d of results.scoring.domains) {
  const dec = declared.scoring.domains.find((x) => x.code === d.code)
  console.log(`${d.code.padEnd(8)}  ${fmt(dec.score).padStart(6)}  ${fmt(d.score).padStart(6)}  ${Number(d.rawScore).toFixed(2).padStart(5)}  ${String(d.targetLevel).padStart(5)}  ${Number(d.gap).toFixed(2).padStart(5)}  ${d.cappedBy ?? ''}`)
}
console.log(`\nRecommandations : ${roadmap.totalActions} — ` + roadmap.phases.map((p) => `${p.label} ${p.actions.length}`).join(', '))
console.log(`Indice de confiance (preuves) : ${results.evidenceRatePercent} %`)
console.log(`Exports : ${OUT}`)
