-- 小说创作工具第一阶段基础表结构
-- 说明：本迁移只覆盖最小闭环所需的 MySQL 8 表结构，后续版本可在此基础上继续扩展。

CREATE TABLE `projects` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `title` VARCHAR(128) NOT NULL COMMENT '作品名称',
  `genres` JSON NOT NULL COMMENT '题材标签，支持多选',
  `target_word_count_min` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '目标字数下限',
  `target_word_count_max` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '目标字数上限',
  `platform_target` VARCHAR(32) NOT NULL DEFAULT 'general' COMMENT '目标平台，例如通用、番茄',
  `style_preference` LONGTEXT NULL COMMENT '风格偏好',
  `project_brief` LONGTEXT NOT NULL COMMENT '模糊描述或创作需求',
  `status` VARCHAR(32) NOT NULL DEFAULT 'drafting' COMMENT '作品状态',
  `selected_idea_id` BIGINT UNSIGNED NULL COMMENT '已选中的创意ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_projects_status` (`status`),
  KEY `idx_projects_selected_idea_id` (`selected_idea_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='作品主表';

CREATE TABLE `model_configs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `provider` VARCHAR(64) NOT NULL COMMENT '模型供应商',
  `display_name` VARCHAR(128) NOT NULL COMMENT '显示名称',
  `base_url` VARCHAR(255) NULL COMMENT '接口地址',
  `model_name` VARCHAR(128) NOT NULL COMMENT '模型名称',
  `api_key_ciphertext` VARCHAR(2048) NOT NULL COMMENT 'API密钥密文，建议使用 Base64 后存储',
  `usage_type` VARCHAR(32) NOT NULL DEFAULT 'default' COMMENT '用途类型，例如创意、大纲、正文、检查',
  `is_default` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否默认',
  `enabled` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否启用',
  `context_window` INT UNSIGNED NULL COMMENT '上下文窗口大小',
  `temperature` DECIMAL(4,2) NULL COMMENT '温度参数',
  `top_p` DECIMAL(4,2) NULL COMMENT 'TopP 参数',
  `supports_json` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否支持 JSON 输出',
  `supports_stream` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否支持流式输出',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_model_configs_provider_model` (`provider`, `model_name`),
  KEY `idx_model_configs_usage_type` (`usage_type`),
  KEY `idx_model_configs_enabled` (`enabled`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='模型配置表';

CREATE TABLE `ideas` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `title` VARCHAR(128) NOT NULL COMMENT '创意标题',
  `selling_points` JSON NOT NULL COMMENT '卖点列表',
  `worldview` LONGTEXT NOT NULL COMMENT '世界观说明',
  `main_conflict` LONGTEXT NOT NULL COMMENT '主线冲突',
  `estimated_word_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预估字数',
  `summary` LONGTEXT NOT NULL COMMENT '创意摘要',
  `status` VARCHAR(32) NOT NULL DEFAULT 'candidate' COMMENT '创意状态',
  `model_config_id` BIGINT UNSIGNED NULL COMMENT '生成该创意的模型配置ID',
  `selected_at` DATETIME NULL COMMENT '被选中时间',
  `rejected_at` DATETIME NULL COMMENT '被拒绝时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_ideas_project_status` (`project_id`, `status`),
  KEY `idx_ideas_model_config_id` (`model_config_id`),
  CONSTRAINT `fk_ideas_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_ideas_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创意表';

CREATE TABLE `idea_evaluations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `idea_id` BIGINT UNSIGNED NOT NULL COMMENT '创意ID',
  `round_no` INT UNSIGNED NOT NULL DEFAULT 1 COMMENT '评估轮次',
  `long_form_potential_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '长篇承载力评分',
  `conflict_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '冲突驱动力评分',
  `novelty_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '新颖度评分',
  `beginner_friendliness_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '新手可写性评分',
  `platform_fit_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '平台适配度评分',
  `risk_level` VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT '风险等级',
  `strengths` JSON NULL COMMENT '优势列表',
  `risks` JSON NULL COMMENT '风险列表',
  `suggestions` JSON NULL COMMENT '修改建议列表',
  `overall_comment` LONGTEXT NULL COMMENT '综合评价',
  `model_config_id` BIGINT UNSIGNED NULL COMMENT '评估所用模型配置ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_idea_evaluations_idea_round` (`idea_id`, `round_no`),
  KEY `idx_idea_evaluations_model_config_id` (`model_config_id`),
  CONSTRAINT `fk_idea_evaluations_idea` FOREIGN KEY (`idea_id`) REFERENCES `ideas` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_idea_evaluations_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='创意评估表';

CREATE TABLE `setting_libraries` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `summary` LONGTEXT NULL COMMENT '设定库总览',
  `confirmed_at` DATETIME NULL COMMENT '确认时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_setting_libraries_project_id` (`project_id`),
  CONSTRAINT `fk_setting_libraries_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='设定库入口表';

CREATE TABLE `characters` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '人物名称',
  `alias` JSON NULL COMMENT '别名列表',
  `role_type` VARCHAR(32) NOT NULL DEFAULT 'supporting' COMMENT '人物角色类型',
  `gender` VARCHAR(16) NULL COMMENT '性别',
  `age_text` VARCHAR(32) NULL COMMENT '年龄描述',
  `personality` LONGTEXT NULL COMMENT '性格',
  `motivation` LONGTEXT NULL COMMENT '人物动机',
  `background` LONGTEXT NULL COMMENT '人物背景',
  `relationship_summary` LONGTEXT NULL COMMENT '关系概述',
  `status` VARCHAR(32) NOT NULL DEFAULT 'active' COMMENT '人物状态',
  `first_appeared_chapter_id` BIGINT UNSIGNED NULL COMMENT '首次出场章节ID',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_characters_project_name` (`project_id`, `name`),
  KEY `idx_characters_project_role` (`project_id`, `role_type`),
  KEY `idx_characters_first_appeared_chapter_id` (`first_appeared_chapter_id`),
  CONSTRAINT `fk_characters_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='人物表';

CREATE TABLE `locations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '地点名称',
  `location_type` VARCHAR(32) NOT NULL DEFAULT 'place' COMMENT '地点类型',
  `description` LONGTEXT NULL COMMENT '地点描述',
  `rules` LONGTEXT NULL COMMENT '地点规则',
  `parent_location_id` BIGINT UNSIGNED NULL COMMENT '上级地点ID',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_locations_project_name` (`project_id`, `name`),
  KEY `idx_locations_parent_location_id` (`parent_location_id`),
  CONSTRAINT `fk_locations_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='地点表';

CREATE TABLE `world_rules` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '规则名称',
  `rule_type` VARCHAR(32) NOT NULL DEFAULT 'general' COMMENT '规则类型，例如力量体系、社会规则、科技规则',
  `description` LONGTEXT NOT NULL COMMENT '规则内容',
  `limitations` LONGTEXT NULL COMMENT '限制条件',
  `examples` LONGTEXT NULL COMMENT '示例说明',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_world_rules_project_type` (`project_id`, `rule_type`),
  KEY `idx_world_rules_project_name` (`project_id`, `name`),
  CONSTRAINT `fk_world_rules_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='世界规则表';

CREATE TABLE `global_outlines` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `title` VARCHAR(128) NOT NULL COMMENT '全局大纲标题',
  `content` LONGTEXT NOT NULL COMMENT '全局大纲内容',
  `confirmed_at` DATETIME NULL COMMENT '确认时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_global_outlines_project_id` (`project_id`),
  CONSTRAINT `fk_global_outlines_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='全局大纲表';

CREATE TABLE `volumes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `volume_no` INT UNSIGNED NOT NULL COMMENT '卷序号',
  `title` VARCHAR(128) NOT NULL COMMENT '卷标题',
  `summary` LONGTEXT NULL COMMENT '卷摘要',
  `goal` LONGTEXT NULL COMMENT '本卷目标',
  `estimated_word_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预估字数',
  `confirmed_at` DATETIME NULL COMMENT '确认时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_volumes_project_no` (`project_id`, `volume_no`),
  KEY `idx_volumes_project_id` (`project_id`),
  CONSTRAINT `fk_volumes_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='分卷表';

CREATE TABLE `story_arcs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `volume_id` BIGINT UNSIGNED NOT NULL COMMENT '分卷ID',
  `arc_no` INT UNSIGNED NOT NULL COMMENT '剧情单元序号',
  `title` VARCHAR(128) NOT NULL COMMENT '剧情单元标题',
  `summary` LONGTEXT NULL COMMENT '剧情单元摘要',
  `goal` LONGTEXT NULL COMMENT '剧情单元目标',
  `conflict` LONGTEXT NULL COMMENT '剧情单元冲突',
  `estimated_chapter_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '预估章节数',
  `confirmed_at` DATETIME NULL COMMENT '确认时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_story_arcs_volume_no` (`volume_id`, `arc_no`),
  KEY `idx_story_arcs_project_id` (`project_id`),
  KEY `idx_story_arcs_volume_id` (`volume_id`),
  CONSTRAINT `fk_story_arcs_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_story_arcs_volume` FOREIGN KEY (`volume_id`) REFERENCES `volumes` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='剧情单元表';

CREATE TABLE `chapters` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `volume_id` BIGINT UNSIGNED NOT NULL COMMENT '分卷ID',
  `story_arc_id` BIGINT UNSIGNED NULL COMMENT '剧情单元ID',
  `chapter_no` INT UNSIGNED NOT NULL COMMENT '章节序号',
  `title` VARCHAR(128) NOT NULL COMMENT '章节标题',
  `outline` LONGTEXT NOT NULL COMMENT '章节大纲',
  `scene_plan` JSON NULL COMMENT '场景拆分计划',
  `content` LONGTEXT NULL COMMENT '章节正文',
  `word_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '字数',
  `status` VARCHAR(32) NOT NULL DEFAULT 'outline_pending' COMMENT '章节状态',
  `confirmed_outline_at` DATETIME NULL COMMENT '章节大纲确认时间',
  `checked_at` DATETIME NULL COMMENT '检查完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_chapters_project_no` (`project_id`, `chapter_no`),
  KEY `idx_chapters_volume_id` (`volume_id`),
  KEY `idx_chapters_story_arc_id` (`story_arc_id`),
  KEY `idx_chapters_status` (`status`),
  CONSTRAINT `fk_chapters_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chapters_volume` FOREIGN KEY (`volume_id`) REFERENCES `volumes` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_chapters_story_arc` FOREIGN KEY (`story_arc_id`) REFERENCES `story_arcs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='章节表';

CREATE TABLE `generation_jobs` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `job_type` VARCHAR(64) NOT NULL COMMENT '任务类型',
  `related_entity_type` VARCHAR(32) NULL COMMENT '关联对象类型',
  `related_entity_id` BIGINT UNSIGNED NULL COMMENT '关联对象ID',
  `model_config_id` BIGINT UNSIGNED NULL COMMENT '使用的模型配置ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '任务状态',
  `priority` INT NOT NULL DEFAULT 0 COMMENT '优先级，数值越大优先级越高',
  `attempt_count` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '重试次数',
  `input_snapshot` JSON NOT NULL COMMENT '任务输入快照',
  `output_snapshot` JSON NULL COMMENT '任务输出快照',
  `error_message` LONGTEXT NULL COMMENT '错误信息',
  `locked_by` VARCHAR(128) NULL COMMENT '领取任务的工作节点标识',
  `locked_at` DATETIME NULL COMMENT '任务领取时间',
  `scheduled_at` DATETIME NULL COMMENT '计划执行时间',
  `started_at` DATETIME NULL COMMENT '开始执行时间',
  `finished_at` DATETIME NULL COMMENT '结束执行时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_generation_jobs_status_priority_created` (`status`, `priority`, `created_at`),
  KEY `idx_generation_jobs_project_status` (`project_id`, `status`),
  KEY `idx_generation_jobs_model_config_id` (`model_config_id`),
  KEY `idx_generation_jobs_related_entity` (`related_entity_type`, `related_entity_id`),
  CONSTRAINT `fk_generation_jobs_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_generation_jobs_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='生成任务表';

CREATE TABLE `check_results` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `chapter_id` BIGINT UNSIGNED NULL COMMENT '章节ID',
  `job_id` BIGINT UNSIGNED NULL COMMENT '生成任务ID',
  `check_type` VARCHAR(32) NOT NULL COMMENT '检查类型',
  `severity` VARCHAR(16) NOT NULL DEFAULT 'medium' COMMENT '严重程度',
  `target_type` VARCHAR(32) NULL COMMENT '关联对象类型',
  `target_id` BIGINT UNSIGNED NULL COMMENT '关联对象ID',
  `issue` LONGTEXT NOT NULL COMMENT '问题描述',
  `suggestion` LONGTEXT NULL COMMENT '修改建议',
  `resolved_at` DATETIME NULL COMMENT '处理完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_check_results_project_chapter` (`project_id`, `chapter_id`),
  KEY `idx_check_results_project_resolved` (`project_id`, `resolved_at`),
  KEY `idx_check_results_job_id` (`job_id`),
  CONSTRAINT `fk_check_results_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_check_results_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_check_results_job` FOREIGN KEY (`job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='检查结果表';

CREATE TABLE `content_versions` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `entity_type` VARCHAR(32) NOT NULL COMMENT '对象类型，例如创意、大纲、章节、人物',
  `entity_id` BIGINT UNSIGNED NOT NULL COMMENT '对象ID',
  `version_no` INT UNSIGNED NOT NULL COMMENT '版本号',
  `snapshot` JSON NOT NULL COMMENT '内容快照',
  `change_source` VARCHAR(32) NOT NULL COMMENT '变更来源，例如用户编辑、AI生成、AI重写',
  `change_note` LONGTEXT NULL COMMENT '变更说明',
  `model_config_id` BIGINT UNSIGNED NULL COMMENT '生成该版本的模型配置ID',
  `job_id` BIGINT UNSIGNED NULL COMMENT '关联的生成任务ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_content_versions_entity_version` (`entity_type`, `entity_id`, `version_no`),
  KEY `idx_content_versions_project_entity` (`project_id`, `entity_type`, `entity_id`),
  KEY `idx_content_versions_model_config_id` (`model_config_id`),
  KEY `idx_content_versions_job_id` (`job_id`),
  CONSTRAINT `fk_content_versions_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_content_versions_model_config` FOREIGN KEY (`model_config_id`) REFERENCES `model_configs` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_content_versions_job` FOREIGN KEY (`job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='版本快照表';

ALTER TABLE `projects`
  ADD CONSTRAINT `fk_projects_selected_idea`
  FOREIGN KEY (`selected_idea_id`) REFERENCES `ideas` (`id`) ON DELETE SET NULL;

ALTER TABLE `locations`
  ADD CONSTRAINT `fk_locations_parent_location`
  FOREIGN KEY (`parent_location_id`) REFERENCES `locations` (`id`) ON DELETE SET NULL;

ALTER TABLE `characters`
  ADD CONSTRAINT `fk_characters_first_appeared_chapter`
  FOREIGN KEY (`first_appeared_chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL;
