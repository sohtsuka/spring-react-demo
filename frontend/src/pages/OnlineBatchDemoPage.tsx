import { useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Play, RefreshCw } from 'lucide-react'
import { onlineBatchApi } from '@/api/onlineBatch'
import { Badge } from '@/components/ui/badge'
import { Button } from '@/components/ui/button'
import {
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
} from '@/components/ui/card'
import { Input } from '@/components/ui/input'
import { Label } from '@/components/ui/label'
import { useToast } from '@/hooks/useToast'
import type { BatchJobStatus, StartOnlineBatchRequest } from '@/types'

const defaultForm: StartOnlineBatchRequest = {
  jobName: '売上CSV取込デモ',
  totalItems: 8,
  failureAtItem: null,
  processingDelayMs: 400,
}

const statusLabels: Record<BatchJobStatus, string> = {
  ACCEPTED: '受付済み',
  RUNNING: '実行中',
  COMPLETED: '完了',
  FAILED: '失敗',
}

export function OnlineBatchDemoPage() {
  const queryClient = useQueryClient()
  const { toast } = useToast()
  const [form, setForm] = useState(defaultForm)
  const [selectedJobId, setSelectedJobId] = useState<number | null>(null)

  const jobsQuery = useQuery({
    queryKey: ['online-batch-jobs'],
    queryFn: onlineBatchApi.getJobs,
    refetchInterval: (query) => {
      const jobs = query.state.data
      return jobs?.some((job) => isActiveStatus(job.status)) ? 1000 : false
    },
  })

  const resolvedSelectedJobId = selectedJobId ?? jobsQuery.data?.[0]?.id ?? null

  const selectedJobQuery = useQuery({
    queryKey: ['online-batch-job', resolvedSelectedJobId],
    queryFn: () => onlineBatchApi.getJob(resolvedSelectedJobId!),
    enabled: resolvedSelectedJobId !== null,
    refetchInterval: (query) => {
      const job = query.state.data
      return job && isActiveStatus(job.status) ? 1000 : false
    },
  })

  const startMutation = useMutation({
    mutationFn: onlineBatchApi.startJob,
    onSuccess: (job) => {
      setSelectedJobId(job.id)
      void queryClient.invalidateQueries({ queryKey: ['online-batch-jobs'] })
      void queryClient.invalidateQueries({ queryKey: ['online-batch-job', job.id] })
      toast({
        title: 'オンラインバッチを受け付けました',
        description: `${job.jobName} をバックグラウンドで実行しています`,
      })
    },
    onError: () => {
      toast({
        title: 'エラー',
        description: 'オンラインバッチの起動に失敗しました',
        variant: 'destructive',
      })
    },
  })

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    startMutation.mutate(form)
  }

  const selectedJob = selectedJobQuery.data

  return (
    <div className="space-y-6 p-6">
      <div>
        <h1 className="text-3xl font-bold tracking-tight">オンラインバッチデモ</h1>
        <p className="text-muted-foreground">
          リクエストで受け付けたバッチを非同期実行し、進捗をポーリングで確認できます
        </p>
      </div>

      <div className="grid gap-6 xl:grid-cols-[420px_minmax(0,1fr)]">
        <Card>
          <CardHeader>
            <CardTitle>ジョブ起動</CardTitle>
            <CardDescription>
              デモ用のジョブを登録すると、Spring 側で逐次処理が始まります
            </CardDescription>
          </CardHeader>
          <CardContent>
            <form className="space-y-4" onSubmit={handleSubmit}>
              <div className="space-y-2">
                <Label htmlFor="jobName">ジョブ名</Label>
                <Input
                  id="jobName"
                  value={form.jobName}
                  onChange={(event) =>
                    setForm((current) => ({ ...current, jobName: event.target.value }))
                  }
                />
              </div>

              <div className="grid gap-4 md:grid-cols-2">
                <div className="space-y-2">
                  <Label htmlFor="totalItems">処理件数</Label>
                  <Input
                    id="totalItems"
                    type="number"
                    min={1}
                    max={50}
                    value={form.totalItems}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        totalItems: Number(event.target.value),
                      }))
                    }
                  />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="processingDelayMs">処理間隔(ms)</Label>
                  <Input
                    id="processingDelayMs"
                    type="number"
                    min={0}
                    max={2000}
                    step={100}
                    value={form.processingDelayMs}
                    onChange={(event) =>
                      setForm((current) => ({
                        ...current,
                        processingDelayMs: Number(event.target.value),
                      }))
                    }
                  />
                </div>
              </div>

              <div className="space-y-2">
                <Label htmlFor="failureAtItem">失敗させる件番</Label>
                <Input
                  id="failureAtItem"
                  type="number"
                  min={1}
                  max={50}
                  placeholder="未入力なら成功"
                  value={form.failureAtItem ?? ''}
                  onChange={(event) =>
                    setForm((current) => ({
                      ...current,
                      failureAtItem: event.target.value === '' ? null : Number(event.target.value),
                    }))
                  }
                />
              </div>

              <Button type="submit" disabled={startMutation.isPending} className="w-full">
                <Play className="h-4 w-4" />
                バッチを起動
              </Button>
            </form>
          </CardContent>
        </Card>

        <div className="space-y-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0">
              <div>
                <CardTitle>ジョブ一覧</CardTitle>
                <CardDescription>
                  新しい順に表示しています。進行中のジョブがある間は自動更新します
                </CardDescription>
              </div>
              <Button
                variant="outline"
                size="sm"
                onClick={() => void queryClient.invalidateQueries({ queryKey: ['online-batch-jobs'] })}
              >
                <RefreshCw className="h-4 w-4" />
                更新
              </Button>
            </CardHeader>
            <CardContent>
              {jobsQuery.isLoading && <p className="text-sm text-muted-foreground">読み込み中です</p>}
              {jobsQuery.isError && (
                <p className="text-sm text-destructive">ジョブ一覧の取得に失敗しました</p>
              )}
              {jobsQuery.data && jobsQuery.data.length === 0 && (
                <p className="text-sm text-muted-foreground">まだジョブはありません</p>
              )}
              <div className="space-y-3">
                {jobsQuery.data?.map((job) => (
                  <button
                    key={job.id}
                    type="button"
                    className={`w-full rounded-lg border p-4 text-left transition-colors ${
                      resolvedSelectedJobId === job.id
                        ? 'border-primary bg-accent/30'
                        : 'hover:bg-accent/30'
                    }`}
                    onClick={() => setSelectedJobId(job.id)}
                  >
                    <div className="flex items-start justify-between gap-4">
                      <div className="space-y-1">
                        <p className="font-medium">{job.jobName}</p>
                        <p className="text-sm text-muted-foreground">
                          {job.processedItems}/{job.totalItems} 件処理
                        </p>
                      </div>
                      <BatchStatusBadge status={job.status} />
                    </div>
                  </button>
                ))}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>ジョブ詳細</CardTitle>
              <CardDescription>選択中のジョブの進捗と処理イベントです</CardDescription>
            </CardHeader>
            <CardContent>
              {!selectedJob && <p className="text-sm text-muted-foreground">ジョブを選択してください</p>}
              {selectedJobQuery.isError && (
                <p className="text-sm text-destructive">ジョブ詳細の取得に失敗しました</p>
              )}
              {selectedJob && (
                <div className="space-y-5">
                  <div className="flex items-center justify-between gap-4">
                    <div>
                      <p className="text-lg font-semibold">{selectedJob.jobName}</p>
                      <p className="text-sm text-muted-foreground">
                        現在処理中: {selectedJob.currentItem ?? '待機または完了'}
                      </p>
                    </div>
                    <BatchStatusBadge status={selectedJob.status} />
                  </div>

                  <div className="space-y-2">
                    <div className="flex items-center justify-between text-sm">
                      <span>進捗</span>
                      <span>{selectedJob.progressPercent}%</span>
                    </div>
                    <div className="h-3 overflow-hidden rounded-full bg-muted">
                      <div
                        className={`h-full rounded-full transition-all ${
                          selectedJob.status === 'FAILED' ? 'bg-destructive' : 'bg-primary'
                        }`}
                        style={{ width: `${selectedJob.progressPercent}%` }}
                      />
                    </div>
                  </div>

                  <div className="grid gap-3 md:grid-cols-3">
                    <StatCard label="成功" value={`${selectedJob.successCount} 件`} />
                    <StatCard label="失敗" value={`${selectedJob.failureCount} 件`} />
                    <StatCard label="設定" value={`${selectedJob.processingDelayMs}ms / 件`} />
                  </div>

                  <div className="space-y-2">
                    <p className="text-sm font-medium">処理イベント</p>
                    <div className="rounded-lg border bg-muted/20 p-4">
                      <ul className="space-y-2 text-sm">
                        {selectedJob.recentEvents.map((event) => (
                          <li key={event} className="font-mono text-xs text-muted-foreground">
                            {event}
                          </li>
                        ))}
                      </ul>
                    </div>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>
      </div>
    </div>
  )
}

function BatchStatusBadge({ status }: { status: BatchJobStatus }) {
  const variant = status === 'FAILED' ? 'destructive' : status === 'COMPLETED' ? 'default' : 'secondary'
  return <Badge variant={variant}>{statusLabels[status]}</Badge>
}

function StatCard({ label, value }: { label: string; value: string }) {
  return (
    <div className="rounded-lg border bg-muted/20 p-4">
      <p className="text-sm text-muted-foreground">{label}</p>
      <p className="mt-1 text-lg font-semibold">{value}</p>
    </div>
  )
}

function isActiveStatus(status: BatchJobStatus) {
  return status === 'ACCEPTED' || status === 'RUNNING'
}
