-- 同一任务类型和业务幂等键只允许创建一条任务；NULL 保持历史记录和非幂等任务可重复写入。
ALTER TABLE `generation_jobs`
  ADD COLUMN `dedupe_key` VARCHAR(191) NULL COMMENT '业务幂等键' AFTER `related_entity_id`,
  ADD UNIQUE KEY `uk_generation_jobs_type_dedupe` (`job_type`, `dedupe_key`);
