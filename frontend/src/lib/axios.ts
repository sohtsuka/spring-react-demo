import axios from 'axios'

/**
 * Cookie から指定名のクッキー値を取得する
 */
function getCookieValue(name: string): string | null {
  const match = document.cookie
    .split('; ')
    .find((row) => row.startsWith(`${name}=`))
  return match ? decodeURIComponent(match.split('=')[1] ?? '') : null
}

const api = axios.create({
  baseURL: '/api/v1',
  headers: {
    'Content-Type': 'application/json',
  },
  withCredentials: true, // Cookie (JSESSIONID) を自動送信
})

// リクエストインターセプター: CSRF トークンを付与
api.interceptors.request.use((config) => {
  if (!['get', 'head', 'options', 'trace'].includes(config.method ?? '')) {
    const csrfToken = getCookieValue('XSRF-TOKEN')
    if (csrfToken) {
      config.headers['X-CSRF-TOKEN'] = csrfToken
    }
  }
  return config
})

// レスポンスインターセプター: 401 は呼び出し元 (QueryClient) に委譲
api.interceptors.response.use(
  (response) => response,
  (error: unknown) => {
    return Promise.reject(error)
  },
)

export default api
