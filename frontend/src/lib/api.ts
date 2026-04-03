const BASE_URL = '/api/v1'
const SAFE_METHODS = new Set(['GET', 'HEAD', 'OPTIONS', 'TRACE'])

/**
 * Cookie から指定名のクッキー値を取得する
 */
function getCookieValue(name: string): string | null {
  const match = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.substring(name.length + 1)) : null
}

export class HttpError extends Error {
  constructor(
    public readonly status: number,
    message: string,
  ) {
    super(message)
    this.name = 'HttpError'
  }
}

async function request<T>(
  method: string,
  path: string,
  options?: {
    body?: unknown
    params?: object
  },
): Promise<T> {
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  }

  if (!SAFE_METHODS.has(method.toUpperCase())) {
    const csrfToken = getCookieValue('XSRF-TOKEN')
    if (csrfToken) {
      headers['X-XSRF-TOKEN'] = csrfToken
    }
  }

  let url = `${BASE_URL}${path}`
  if (options?.params) {
    const sp = new URLSearchParams()
    for (const [k, v] of Object.entries(options.params)) {
      if (v !== undefined) sp.set(k, String(v))
    }
    const qs = sp.toString()
    if (qs) url += `?${qs}`
  }

  const response = await fetch(url, {
    method,
    headers,
    credentials: 'include',
    body: options?.body !== undefined ? JSON.stringify(options.body) : undefined,
  })

  if (!response.ok) {
    throw new HttpError(response.status, response.statusText)
  }

  const contentType = response.headers.get('content-type')
  if (!contentType || !contentType.includes('application/json')) {
    return undefined as T
  }

  return response.json() as Promise<T>
}

const api = {
  get: <T>(
    path: string,
    options?: { params?: object },
  ) => request<T>('GET', path, options),
  post:   <T>(path: string, body?: unknown) => request<T>('POST', path, { body }),
  put:    <T>(path: string, body?: unknown) => request<T>('PUT', path, { body }),
  patch:  <T>(path: string, body?: unknown) => request<T>('PATCH', path, { body }),
  delete: (path: string) => request<void>('DELETE', path),
}

export default api
