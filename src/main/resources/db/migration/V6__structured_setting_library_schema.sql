-- Structured setting library schema draft (safe additive migration)
-- Strategy:
-- 1. Keep existing columns for backward compatibility
-- 2. Add new fields/tables needed by the structured setting workspace
-- 3. Do not drop or rename old columns in this migration

ALTER TABLE `setting_libraries`
  ADD COLUMN `source_idea_id` BIGINT UNSIGNED NULL COMMENT '来源创意ID' AFTER `project_id`,
  ADD COLUMN `overview` LONGTEXT NULL COMMENT '设定总览，替代旧的长文本设定正文入口' AFTER `summary`,
  ADD COLUMN `genre_template` VARCHAR(32) NULL COMMENT '题材模板，例如 fantasy、romance、sci_fi' AFTER `overview`,
  ADD KEY `idx_setting_libraries_source_idea_id` (`source_idea_id`),
  ADD KEY `idx_setting_libraries_genre_template` (`genre_template`);

ALTER TABLE `setting_libraries`
  ADD CONSTRAINT `fk_setting_libraries_source_idea`
  FOREIGN KEY (`source_idea_id`) REFERENCES `ideas` (`id`) ON DELETE SET NULL;

ALTER TABLE `characters`
  ADD COLUMN `narrative_role` VARCHAR(32) NOT NULL DEFAULT 'supporting' COMMENT '叙事角色：protagonist、supporting、antagonist、ensemble' AFTER `alias`,
  ADD COLUMN `identity` VARCHAR(128) NULL COMMENT '核心身份，例如学生、官员、修士' AFTER `role_type`,
  ADD COLUMN `public_identity` VARCHAR(128) NULL COMMENT '对外公开身份' AFTER `identity`,
  ADD COLUMN `core_goal` LONGTEXT NULL COMMENT '角色外显目标' AFTER `background`,
  ADD COLUMN `inner_need` LONGTEXT NULL COMMENT '角色内在需求' AFTER `core_goal`,
  ADD COLUMN `core_flaw` LONGTEXT NULL COMMENT '角色核心缺陷' AFTER `inner_need`,
  ADD COLUMN `bottom_line` LONGTEXT NULL COMMENT '角色底线' AFTER `core_flaw`,
  ADD COLUMN `skills_summary` LONGTEXT NULL COMMENT '角色能力或技能概述' AFTER `bottom_line`,
  ADD COLUMN `secret_notes` LONGTEXT NULL COMMENT '角色秘密，仅作者可见' AFTER `skills_summary`,
  ADD COLUMN `importance` INT NOT NULL DEFAULT 0 COMMENT '角色重要度，数值越大越重要' AFTER `secret_notes`,
  ADD KEY `idx_characters_narrative_role` (`project_id`, `narrative_role`),
  ADD KEY `idx_characters_importance` (`project_id`, `importance`);

ALTER TABLE `locations`
  ADD COLUMN `key_features` LONGTEXT NULL COMMENT '地点关键特征' AFTER `description`,
  ADD COLUMN `entry_conditions` LONGTEXT NULL COMMENT '进入条件或门槛' AFTER `key_features`,
  ADD COLUMN `available_resources` LONGTEXT NULL COMMENT '地点可提供的资源' AFTER `entry_conditions`,
  ADD COLUMN `controlling_org_id` BIGINT UNSIGNED NULL COMMENT '当前控制该地点的组织ID' AFTER `available_resources`,
  ADD COLUMN `risk_level` VARCHAR(16) NULL COMMENT '风险等级：low、medium、high' AFTER `controlling_org_id`,
  ADD KEY `idx_locations_controlling_org_id` (`controlling_org_id`),
  ADD KEY `idx_locations_risk_level` (`project_id`, `risk_level`);

CREATE TABLE `organizations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '组织名称',
  `organization_type` VARCHAR(32) NOT NULL DEFAULT 'faction' COMMENT '组织类型，例如 faction、company、family、school',
  `public_mission` LONGTEXT NULL COMMENT '公开宗旨',
  `real_goal` LONGTEXT NULL COMMENT '真实目标',
  `controlled_resources` LONGTEXT NULL COMMENT '掌控资源概述',
  `power_scope` LONGTEXT NULL COMMENT '影响力范围',
  `base_location_id` BIGINT UNSIGNED NULL COMMENT '主要据点或基地地点ID',
  `entry_rules` LONGTEXT NULL COMMENT '加入条件或内部规则',
  `status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '状态：active、collapsed、hidden',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_organizations_project_name` (`project_id`, `name`),
  KEY `idx_organizations_project_type` (`project_id`, `organization_type`),
  KEY `idx_organizations_base_location_id` (`base_location_id`),
  CONSTRAINT `fk_organizations_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_organizations_base_location` FOREIGN KEY (`base_location_id`) REFERENCES `locations` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='组织与势力表';

ALTER TABLE `locations`
  ADD CONSTRAINT `fk_locations_controlling_org`
  FOREIGN KEY (`controlling_org_id`) REFERENCES `organizations` (`id`) ON DELETE SET NULL;

CREATE TABLE `items` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '物品名称',
  `item_type` VARCHAR(32) NOT NULL DEFAULT 'item' COMMENT '物品类型，例如 item、artifact、vehicle、document',
  `description` LONGTEXT NULL COMMENT '物品描述',
  `usage_rules` LONGTEXT NULL COMMENT '使用规则',
  `limitations` LONGTEXT NULL COMMENT '限制条件',
  `rarity` VARCHAR(16) NULL COMMENT '稀有度',
  `owner_character_id` BIGINT UNSIGNED NULL COMMENT '当前持有角色ID',
  `owner_org_id` BIGINT UNSIGNED NULL COMMENT '当前持有组织ID',
  `status` VARCHAR(16) NOT NULL DEFAULT 'available' COMMENT '状态：available、lost、destroyed、sealed',
  `notes` LONGTEXT NULL COMMENT '备注',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_items_project_name` (`project_id`, `name`),
  KEY `idx_items_project_type` (`project_id`, `item_type`),
  KEY `idx_items_owner_character` (`owner_character_id`),
  KEY `idx_items_owner_org` (`owner_org_id`),
  CONSTRAINT `fk_items_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_items_owner_character` FOREIGN KEY (`owner_character_id`) REFERENCES `characters` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_items_owner_org` FOREIGN KEY (`owner_org_id`) REFERENCES `organizations` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='重要物品与资产表';

ALTER TABLE `world_rules`
  ADD COLUMN `trigger_condition` LONGTEXT NULL COMMENT '触发条件' AFTER `description`,
  ADD COLUMN `effect_result` LONGTEXT NULL COMMENT '效果结果' AFTER `trigger_condition`,
  ADD COLUMN `cost` LONGTEXT NULL COMMENT '代价或副作用' AFTER `limitations`,
  ADD COLUMN `exceptions` LONGTEXT NULL COMMENT '例外情况' AFTER `cost`,
  ADD COLUMN `visibility_level` VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT '可见性：public、hidden、secret' AFTER `exceptions`,
  ADD COLUMN `importance` INT NOT NULL DEFAULT 0 COMMENT '重要度，数值越大越重要' AFTER `visibility_level`,
  ADD KEY `idx_world_rules_visibility` (`project_id`, `visibility_level`),
  ADD KEY `idx_world_rules_importance` (`project_id`, `importance`);

CREATE TABLE `story_events` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `name` VARCHAR(128) NOT NULL COMMENT '事件名称',
  `event_type` VARCHAR(32) NOT NULL DEFAULT 'story' COMMENT '事件类型，例如 reveal、alliance、betrayal、death、transfer',
  `description` LONGTEXT NULL COMMENT '事件描述',
  `event_time_text` VARCHAR(128) NULL COMMENT '事件时间描述，例如 第三卷中期、历法X年',
  `location_id` BIGINT UNSIGNED NULL COMMENT '事件发生地点ID',
  `chapter_id` BIGINT UNSIGNED NULL COMMENT '关联章节ID',
  `is_planned` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '是否为计划事件，0 表示已实际发生',
  `importance` INT NOT NULL DEFAULT 0 COMMENT '事件重要度',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_story_events_project_type` (`project_id`, `event_type`),
  KEY `idx_story_events_chapter_id` (`chapter_id`),
  KEY `idx_story_events_location_id` (`location_id`),
  CONSTRAINT `fk_story_events_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_story_events_location` FOREIGN KEY (`location_id`) REFERENCES `locations` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_story_events_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='关键剧情事件表';

CREATE TABLE `entity_relations` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `source_type` VARCHAR(32) NOT NULL COMMENT '源实体类型，例如 character、organization、location、item',
  `source_id` BIGINT UNSIGNED NOT NULL COMMENT '源实体ID',
  `target_type` VARCHAR(32) NOT NULL COMMENT '目标实体类型',
  `target_id` BIGINT UNSIGNED NOT NULL COMMENT '目标实体ID',
  `relation_type` VARCHAR(32) NOT NULL COMMENT '关系类型，例如 ally、enemy、member_of、located_in、owns、knows',
  `relation_status` VARCHAR(16) NOT NULL DEFAULT 'active' COMMENT '关系状态：active、ended、hidden',
  `strength_value` INT NULL COMMENT '关系强度，可用于好感度、信任度等',
  `visibility_level` VARCHAR(16) NOT NULL DEFAULT 'public' COMMENT '可见性：public、hidden、secret',
  `note` LONGTEXT NULL COMMENT '备注说明',
  `start_event_id` BIGINT UNSIGNED NULL COMMENT '关系开始事件ID',
  `end_event_id` BIGINT UNSIGNED NULL COMMENT '关系结束事件ID',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_entity_relations_source` (`project_id`, `source_type`, `source_id`),
  KEY `idx_entity_relations_target` (`project_id`, `target_type`, `target_id`),
  KEY `idx_entity_relations_type` (`project_id`, `relation_type`),
  KEY `idx_entity_relations_start_event` (`start_event_id`),
  KEY `idx_entity_relations_end_event` (`end_event_id`),
  CONSTRAINT `fk_entity_relations_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_entity_relations_start_event` FOREIGN KEY (`start_event_id`) REFERENCES `story_events` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_entity_relations_end_event` FOREIGN KEY (`end_event_id`) REFERENCES `story_events` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体关系表';

CREATE TABLE `entity_state_records` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `entity_type` VARCHAR(32) NOT NULL COMMENT '实体类型，例如 character、organization、location、item',
  `entity_id` BIGINT UNSIGNED NOT NULL COMMENT '实体ID',
  `state_type` VARCHAR(32) NOT NULL COMMENT '状态类型，例如 location、injury、ownership、identity、control',
  `old_value` JSON NULL COMMENT '旧状态值',
  `new_value` JSON NOT NULL COMMENT '新状态值',
  `event_id` BIGINT UNSIGNED NULL COMMENT '导致状态变化的事件ID',
  `chapter_id` BIGINT UNSIGNED NULL COMMENT '关联章节ID',
  `effective_at` DATETIME NULL COMMENT '生效时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_entity_state_records_entity` (`project_id`, `entity_type`, `entity_id`),
  KEY `idx_entity_state_records_state_type` (`project_id`, `state_type`),
  KEY `idx_entity_state_records_event_id` (`event_id`),
  KEY `idx_entity_state_records_chapter_id` (`chapter_id`),
  CONSTRAINT `fk_entity_state_records_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_entity_state_records_event` FOREIGN KEY (`event_id`) REFERENCES `story_events` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_entity_state_records_chapter` FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='实体状态变化记录表';
