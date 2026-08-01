CREATE TABLE `outline_workflow_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `setting_library_id` BIGINT UNSIGNED NOT NULL,
  `model_config_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'draft_ready',
  `draft_json` JSON NOT NULL,
  `check_json` JSON NULL,
  `committed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_outline_workflow_project_status` (`project_id`, `status`),
  CONSTRAINT `fk_outline_workflow_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_outline_workflow_setting` FOREIGN KEY (`setting_library_id`) REFERENCES `setting_libraries` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_outline_workflow_model` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='大纲生成工作流草案';
