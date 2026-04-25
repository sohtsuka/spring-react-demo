CREATE TABLE online_batch_jobs (
    id                  BIGSERIAL    PRIMARY KEY,
    job_name            VARCHAR(40)  NOT NULL,
    status              VARCHAR(20)  NOT NULL,
    total_items         INT          NOT NULL,
    processed_items     INT          NOT NULL DEFAULT 0,
    success_count       INT          NOT NULL DEFAULT 0,
    failure_count       INT          NOT NULL DEFAULT 0,
    progress_percent    INT          NOT NULL DEFAULT 0,
    failure_at_item     INT,
    processing_delay_ms INT          NOT NULL,
    current_item        VARCHAR(100),
    recent_events       TEXT         NOT NULL DEFAULT '[]',
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    started_at          TIMESTAMP,
    completed_at        TIMESTAMP,
    updated_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_online_batch_jobs_created_at ON online_batch_jobs (created_at DESC);

COMMENT ON TABLE online_batch_jobs IS 'オンラインバッチジョブ';
COMMENT ON COLUMN online_batch_jobs.status IS 'ジョブ状態: ACCEPTED / RUNNING / COMPLETED / FAILED';
COMMENT ON COLUMN online_batch_jobs.failure_at_item IS 'この件番で失敗させる場合に指定';
COMMENT ON COLUMN online_batch_jobs.recent_events IS '直近イベントの JSON 配列';
