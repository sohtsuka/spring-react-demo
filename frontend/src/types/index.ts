// ユーザーロール
export type Role = 'ADMIN' | 'MANAGER' | 'USER'

// ユーザー
export interface User {
  id: number
  username: string
  email: string
  role: Role
  enabled: boolean
  createdAt: string
  updatedAt: string
}

// ログイン中ユーザー (認証コンテキスト用)
export interface AuthUser {
  id: number
  username: string
  email: string
  role: Role
}

// 共通 API レスポンス
export interface ApiResponse<T> {
  data: T
}

// ページネーション付きリスト
export interface PaginatedResponse<T> {
  data: T[]
  pagination: Pagination
}

export interface Pagination {
  page: number
  size: number
  totalElements: number
  totalPages: number
}

// エラーレスポンス
export interface ApiError {
  code: string
  message: string
  details?: FieldError[]
  timestamp: string
}

export interface FieldError {
  field: string
  message: string
}

// ユーザー作成・更新リクエスト
export interface CreateUserRequest {
  username: string
  email: string
  password: string
  role: Role
}

export interface UpdateUserRequest {
  username: string
  email: string
  role: Role
  enabled: boolean
}

// ログインリクエスト
export interface LoginRequest {
  username: string
  password: string
}

export type BatchJobStatus = 'ACCEPTED' | 'RUNNING' | 'COMPLETED' | 'FAILED'

export interface OnlineBatchJob {
  id: number
  jobName: string
  status: BatchJobStatus
  totalItems: number
  processedItems: number
  successCount: number
  failureCount: number
  progressPercent: number
  failureAtItem: number | null
  processingDelayMs: number
  currentItem: string | null
  createdAt: string
  startedAt: string | null
  completedAt: string | null
  recentEvents: string[]
}

export interface StartOnlineBatchRequest {
  jobName: string
  totalItems: number
  failureAtItem: number | null
  processingDelayMs: number
}
