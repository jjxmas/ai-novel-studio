CREATE TABLE `setting_workflow_runs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `source_idea_id` BIGINT UNSIGNED NOT NULL,
  `model_config_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(32) NOT NULL DEFAULT 'blueprint_ready',
  `blueprint_json` JSON NOT NULL,
  `draft_json` JSON NULL,
  `check_json` JSON NULL,
  `blueprint_confirmed_at` DATETIME NULL,
  `committed_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_setting_workflow_project_status` (`project_id`, `status`),
  CONSTRAINT `fk_setting_workflow_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_setting_workflow_idea` FOREIGN KEY (`source_idea_id`) REFERENCES `ideas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_setting_workflow_model` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设定库生成工作流草案';
