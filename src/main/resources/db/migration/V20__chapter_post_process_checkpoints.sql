-- 保留每个正文版本最新的事实抽取来源标识，旧的重复审计记录仍然保留。
UPDATE `chapter_fact_extraction_runs` AS duplicate_run
JOIN (
  SELECT `chapter_id`, `source_content_version_id`, MAX(`id`) AS `keep_id`
  FROM `chapter_fact_extraction_runs`
  WHERE `source_content_version_id` IS NOT NULL
  GROUP BY `chapter_id`, `source_content_version_id`
  HAVING COUNT(*) > 1
) AS duplicated
  ON duplicated.`chapter_id` = duplicate_run.`chapter_id`
 AND duplicated.`source_content_version_id` = duplicate_run.`source_content_version_id`
SET duplicate_run.`source_content_version_id` = NULL
WHERE duplicate_run.`id` <> duplicated.`keep_id`;

ALTER TABLE `chapter_fact_extraction_runs`
  ADD UNIQUE KEY `uk_fact_extraction_chapter_source_version` (`chapter_id`, `source_content_version_id`);

CREATE TABLE `chapter_post_process_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `chapter_id` BIGINT UNSIGNED NOT NULL,
  `content_version_no` INT UNSIGNED NOT NULL,
  `generation_job_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(16) NOT NULL DEFAULT 'running',
  `completed_step` INT UNSIGNED NOT NULL DEFAULT 0,
  `quality_status` VARCHAR(16) NULL,
  `quality_issue_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `quality_error_message` LONGTEXT NULL,
  `error_message` LONGTEXT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_post_process_chapter_version` (`chapter_id`, `content_version_no`),
  KEY `idx_post_process_project_status` (`project_id`, `status`, `updated_at`),
  KEY `idx_post_process_generation_job` (`generation_job_id`),
  CONSTRAINT `fk_post_process_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_process_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_post_process_generation_job` FOREIGN KEY (`generation_job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节后处理步骤检查点';
