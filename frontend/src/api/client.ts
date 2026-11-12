import type { ProblemDetail } from './types'

/**
 * Client HTTP minimal autour de fetch :
 *  - ajoute le jeton JWT (en-tête Authorization) ;
 *  - transforme les erreurs « Problem Details » de l'API en ApiError lisibles ;
 *  - signale une session expirée (401) pour rediriger vers la connexion.
 */

const TOKEN_KEY = 'aima.token'

/**
 * Le jeton est gardé en sessionStorage : il disparaît à la fermeture de l'onglet,
 * ce qui limite son exposition (compromis courant pour une API à jetons).
 */
export const tokenStore = {
  get: (): string | null => {
    try {
      return sessionStorage.getItem(TOKEN_KEY)
    } catch {
      return null
    }
  },
  set: (token: string) => {
    try {
      sessionStorage.setItem(TOKEN_KEY, token)
    } catch {
      /* stockage indisponible : la session durera le temps de la page */
    }
  },
  clear: () => {
    try {
      sessionStorage.removeItem(TOKEN_KEY)
    } catch {
      /* rien à faire */
    }
  },
}

export class ApiError extends Error {
  readonly status: number
  readonly problem: ProblemDetail

  constructor(problem: ProblemDetail) {
    super(problem.detail ?? problem.title)
    this.status = problem.status
    this.problem = problem
  }

  /** Message à afficher, avec le détail champ par champ si présent. */
  get userMessage(): string {
    const fields = this.problem.errors
      ? Object.entries(this.problem.errors).map(([f, m]) => `${f} : ${m}`).join(' · ')
      : ''
    return fields ? `${this.message} (${fields})` : this.message
  }
}

let onUnauthorized: () => void = () => {}
export function setUnauthorizedHandler(handler: () => void) {
  onUnauthorized = handler
}

async function request(method: string, url: string, body?: unknown): Promise<Response> {
  const headers: Record<string, string> = { Accept: 'application/json' }
  const token = tokenStore.get()
  if (token) headers.Authorization = `Bearer ${token}`
  if (body !== undefined) headers['Content-Type'] = 'application/json'

  const response = await fetch(`/api/v1${url}`, {
    method,
    headers,
    body: body === undefined ? undefined : JSON.stringify(body),
  })

  if (!response.ok) {
    let problem: ProblemDetail = { status: response.status, title: response.statusText }
    try {
      problem = { ...problem, ...(await response.json()) }
    } catch {
      /* réponse sans corps JSON */
    }
    if (response.status === 401 && url !== '/auth/login') onUnauthorized()
    throw new ApiError(problem)
  }
  return response
}

export const api = {
  get: async <T>(url: string): Promise<T> => (await request('GET', url)).json(),
  post: async <T>(url: string, body?: unknown): Promise<T> => (await request('POST', url, body)).json(),
  put: async <T>(url: string, body?: unknown): Promise<T> => (await request('PUT', url, body)).json(),
  delete: async (url: string): Promise<void> => {
    await request('DELETE', url)
  },
  /** Télécharge un fichier (ex. rapport PDF) en conservant le nom proposé par le serveur. */
  download: async (url: string, fallbackName: string): Promise<void> => {
    const response = await request('GET', url)
    const disposition = response.headers.get('Content-Disposition') ?? ''
    const name = /filename="?([^";]+)"?/.exec(disposition)?.[1] ?? fallbackName
    const blob = await response.blob()
    const link = document.createElement('a')
    link.href = URL.createObjectURL(blob)
    link.download = name
    link.click()
    URL.revokeObjectURL(link.href)
  },
}
