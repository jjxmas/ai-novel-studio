-- 为 generation_jobs 的领取和租约恢复补充索引；复用 V1 已有队列字段。
ALTER TABLE `generation_jobs`
  ADD KEY `idx_generation_jobs_claim` (`status`, `scheduled_at`, `priority`, `created_at`),
  ADD KEY `idx_generation_jobs_lease` (`status`, `locked_at`);
