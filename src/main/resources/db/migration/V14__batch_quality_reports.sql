ALTER TABLE `generation_batches`
  ADD COLUMN `quality_checked_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '完成质量检查数' AFTER `skipped_count`,
  ADD COLUMN `quality_failed_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '质量检查失败数' AFTER `quality_checked_count`,
  ADD COLUMN `quality_issue_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '质量问题总数' AFTER `quality_failed_count`;

ALTER TABLE `generation_batch_items`
  ADD COLUMN `quality_status` VARCHAR(32) NOT NULL DEFAULT 'not_run' COMMENT '质量检查状态' AFTER `generation_job_id`,
  ADD COLUMN `quality_issue_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '质量问题数' AFTER `quality_status`,
  ADD COLUMN `quality_report` JSON NULL COMMENT '质量检查报告' AFTER `quality_issue_count`,
  ADD COLUMN `quality_error_message` LONGTEXT NULL COMMENT '质量检查错误' AFTER `quality_report`;
