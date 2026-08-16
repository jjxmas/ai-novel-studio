-- 每次首次执行、继续或重试使用独立的持久化队列幂等键。
ALTER TABLE `generation_batches`
  ADD COLUMN `run_no` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '队列执行轮次' AFTER `status`;
