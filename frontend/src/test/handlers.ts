import { http, HttpResponse } from 'msw'
import { mockAuthUser, mockUser } from './mockUser'

export const handlers = [
  http.get('/api/v1/auth/me', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/login', () => HttpResponse.json({ data: mockAuthUser })),
  http.post('/api/v1/auth/logout', () => new HttpResponse(null, { status: 200 })),
  http.get('/api/v1/users', () =>
    HttpResponse.json({
      data: [mockUser],
      pagination: { page: 1, size: 20, totalElements: 1, totalPages: 1 },
    }),
  ),
  http.get('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.post('/api/v1/users', () => HttpResponse.json({ data: mockUser }, { status: 201 })),
  http.put('/api/v1/users/:id', () => HttpResponse.json({ data: mockUser })),
  http.delete('/api/v1/users/:id', () => new HttpResponse(null, { status: 204 })),
]
