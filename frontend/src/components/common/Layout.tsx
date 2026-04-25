import { Link, Outlet, useLocation } from 'react-router'
import { LayoutDashboard, LogOut, Settings, TimerReset, User, Users } from 'lucide-react'
import { Avatar, AvatarFallback } from '@/components/ui/avatar'
import { Button } from '@/components/ui/button'
import { Separator } from '@/components/ui/separator'
import { useAuth } from '@/hooks/useAuth'
import { cn } from '@/lib/utils'

interface NavItem {
  href: string
  label: string
  icon: React.ComponentType<{ className?: string }>
  roles?: string[]
}

const navItems: NavItem[] = [
  { href: '/', label: 'ダッシュボード', icon: LayoutDashboard },
  { href: '/batch-demo', label: 'オンラインバッチ', icon: TimerReset },
  { href: '/profile', label: 'プロフィール', icon: User },
  {
    href: '/manager',
    label: 'マネージャー',
    icon: Settings,
    roles: ['ADMIN', 'MANAGER'],
  },
  { href: '/admin', label: '管理者', icon: Users, roles: ['ADMIN'] },
]

export function Layout() {
  const { user, logout, isLoggingOut } = useAuth()
  const location = useLocation()

  const visibleNavItems = navItems.filter((item) => {
    if (!item.roles) return true
    return user && item.roles.includes(user.role)
  })

  const initials = user?.username
    ? user.username.slice(0, 2).toUpperCase()
    : '??'

  return (
    <div className="flex min-h-screen bg-background">
      {/* サイドバー */}
      <aside className="hidden w-64 flex-col border-r bg-card md:flex">
        <div className="flex h-16 items-center px-6">
          <span className="text-lg font-semibold">App</span>
        </div>
        <Separator />
        <nav className="flex-1 space-y-1 p-4">
          {visibleNavItems.map((item) => {
            const isActive =
              item.href === '/'
                ? location.pathname === '/'
                : location.pathname.startsWith(item.href)
            return (
              <Link
                key={item.href}
                to={item.href}
                className={cn(
                  'flex items-center gap-3 rounded-md px-3 py-2 text-sm font-medium transition-colors',
                  isActive
                    ? 'bg-primary text-primary-foreground'
                    : 'text-muted-foreground hover:bg-accent hover:text-accent-foreground',
                )}
              >
                <item.icon className="h-4 w-4" />
                {item.label}
              </Link>
            )
          })}
        </nav>
        <Separator />
        <div className="p-4">
          <div className="flex items-center gap-3 rounded-md px-3 py-2">
            <Avatar className="h-8 w-8">
              <AvatarFallback className="text-xs">{initials}</AvatarFallback>
            </Avatar>
            <div className="flex-1 overflow-hidden">
              <p className="truncate text-sm font-medium">{user?.username}</p>
              <p className="truncate text-xs text-muted-foreground">
                {user?.role}
              </p>
            </div>
            <Button
              variant="ghost"
              size="icon"
              onClick={() => logout()}
              disabled={isLoggingOut}
              title="ログアウト"
            >
              <LogOut className="h-4 w-4" />
            </Button>
          </div>
        </div>
      </aside>

      {/* メインコンテンツ */}
      <main className="flex-1 overflow-auto">
        <Outlet />
      </main>
    </div>
  )
}
