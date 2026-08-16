CREATE TABLE `story_rebuild_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `generation_job_id` BIGINT UNSIGNED NULL,
  `model_config_id` BIGINT UNSIGNED NULL,
  `requested_start_chapter_no` INT NULL,
  `actual_start_chapter_no` INT NULL,
  `phase` VARCHAR(32) NOT NULL DEFAULT 'fact_projection',
  `next_fact_chapter_no` INT NULL,
  `next_memory_chapter_no` INT NULL,
  `memory_reset_done` TINYINT(1) NOT NULL DEFAULT 0,
  `processed_chapter_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `skipped_chapter_count` INT UNSIGNED NOT NULL DEFAULT 0,
  `processed_chapter_nos_json` LONGTEXT NULL,
  `skipped_chapter_nos_json` LONGTEXT NULL,
  `dirty_mark_ids_json` LONGTEXT NULL,
  `active_dirty_mark_count_before` INT UNSIGNED NOT NULL DEFAULT 0,
  `status` VARCHAR(16) NOT NULL DEFAULT 'pending',
  `result_json` LONGTEXT NULL,
  `error_message` LONGTEXT NULL,
  `started_at` DATETIME NULL,
  `finished_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_story_rebuild_generation_job` (`generation_job_id`),
  KEY `idx_story_rebuild_project_status` (`project_id`, `status`, `created_at`),
  CONSTRAINT `fk_story_rebuild_project`
    FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_story_rebuild_generation_job`
    FOREIGN KEY (`generation_job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_story_rebuild_model_config`
    FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Durable checkpoints for story projection and memory rebuild jobs';
