-- 第二版 MVP 闭环补充迁移
-- 说明：V1 已提供主要创作实体，本迁移只补充流程状态、版本操作语义和导出任务记录。

ALTER TABLE `projects`
  ADD COLUMN `workflow_stage` VARCHAR(32) NOT NULL DEFAULT 'idea' COMMENT '当前创作阶段：idea 创意、setting 设定库、outline 大纲、chapter 章节、check 检查、export 导出' AFTER `status`,
  ADD COLUMN `last_exported_at` DATETIME NULL COMMENT '最近导出时间' AFTER `selected_idea_id`,
  ADD KEY `idx_projects_workflow_stage` (`workflow_stage`);

ALTER TABLE `idea_evaluations`
  ADD COLUMN `overall_score` DECIMAL(5,2) NOT NULL DEFAULT 0.00 COMMENT '综合评分',
  ADD COLUMN `score_reason` LONGTEXT NULL COMMENT '评分理由',
  ADD KEY `idx_idea_evaluations_overall_score` (`overall_score`);

ALTER TABLE `setting_libraries`
  ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '设定库状态：draft 草稿、generated 已生成、edited 已编辑、confirmed 已确认' AFTER `summary`,
  ADD KEY `idx_setting_libraries_status` (`status`);

UPDATE `setting_libraries`
SET `status` = 'confirmed'
WHERE `confirmed_at` IS NOT NULL;

ALTER TABLE `global_outlines`
  ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '全局大纲状态：draft 草稿、generated 已生成、edited 已编辑、confirmed 已确认' AFTER `content`,
  ADD KEY `idx_global_outlines_status` (`status`);

UPDATE `global_outlines`
SET `status` = 'confirmed'
WHERE `confirmed_at` IS NOT NULL;

ALTER TABLE `volumes`
  ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '分卷状态：draft 草稿、generated 已生成、edited 已编辑、confirmed 已确认' AFTER `estimated_word_count`,
  ADD KEY `idx_volumes_status` (`status`);

UPDATE `volumes`
SET `status` = 'confirmed'
WHERE `confirmed_at` IS NOT NULL;

ALTER TABLE `story_arcs`
  ADD COLUMN `status` VARCHAR(32) NOT NULL DEFAULT 'draft' COMMENT '剧情单元状态：draft 草稿、generated 已生成、edited 已编辑、confirmed 已确认' AFTER `estimated_chapter_count`,
  ADD KEY `idx_story_arcs_status` (`status`);

UPDATE `story_arcs`
SET `status` = 'confirmed'
WHERE `confirmed_at` IS NOT NULL;

ALTER TABLE `chapters`
  ADD COLUMN `content_status` VARCHAR(32) NOT NULL DEFAULT 'not_generated' COMMENT '正文状态：not_generated 未生成、generating 生成中、generated 已生成、edited 已编辑、checked 已检查' AFTER `status`,
  ADD COLUMN `content_generated_at` DATETIME NULL COMMENT '正文最近生成时间' AFTER `confirmed_outline_at`,
  ADD COLUMN `content_updated_at` DATETIME NULL COMMENT '正文最近编辑或重生成时间' AFTER `content_generated_at`,
  ADD COLUMN `last_generation_job_id` BIGINT UNSIGNED NULL COMMENT '最近一次正文生成任务ID' AFTER `content_updated_at`,
  ADD COLUMN `last_content_version_no` INT UNSIGNED NOT NULL DEFAULT 0 COMMENT '当前正文最新版本号' AFTER `last_generation_job_id`,
  ADD KEY `idx_chapters_content_status` (`content_status`),
  ADD KEY `idx_chapters_last_generation_job_id` (`last_generation_job_id`),
  ADD CONSTRAINT `fk_chapters_last_generation_job`
    FOREIGN KEY (`last_generation_job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL;

ALTER TABLE `content_versions`
  ADD COLUMN `operation_type` VARCHAR(32) NOT NULL DEFAULT 'generate' COMMENT '操作类型：generate 生成、edit 用户编辑、rewrite 根据意见重生成、confirm 确认保存、export 导出快照' AFTER `change_source`,
  ADD COLUMN `revision_instruction` LONGTEXT NULL COMMENT '用户修改意见或生成指令' AFTER `change_note`,
  ADD KEY `idx_content_versions_operation_type` (`operation_type`);

CREATE TABLE `export_tasks` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT COMMENT '主键',
  `project_id` BIGINT UNSIGNED NOT NULL COMMENT '作品ID',
  `job_id` BIGINT UNSIGNED NULL COMMENT '关联生成任务ID',
  `format` VARCHAR(16) NOT NULL COMMENT '导出格式：markdown 或 txt',
  `scope` VARCHAR(32) NOT NULL DEFAULT 'full_project' COMMENT '导出范围：full_project 全书、volume 分卷、chapter 单章',
  `scope_entity_id` BIGINT UNSIGNED NULL COMMENT '导出范围对应对象ID',
  `status` VARCHAR(32) NOT NULL DEFAULT 'pending' COMMENT '导出状态：pending 待处理、running 处理中、succeeded 成功、failed 失败',
  `file_name` VARCHAR(255) NULL COMMENT '导出文件名',
  `file_path` VARCHAR(512) NULL COMMENT '导出文件存储路径',
  `file_size` BIGINT UNSIGNED NULL COMMENT '导出文件字节数',
  `request_snapshot` JSON NOT NULL COMMENT '导出请求快照',
  `error_message` LONGTEXT NULL COMMENT '导出失败原因',
  `exported_at` DATETIME NULL COMMENT '导出完成时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_export_tasks_project_status` (`project_id`, `status`),
  KEY `idx_export_tasks_project_created` (`project_id`, `created_at`),
  KEY `idx_export_tasks_job_id` (`job_id`),
  CONSTRAINT `fk_export_tasks_project` FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_export_tasks_job` FOREIGN KEY (`job_id`) REFERENCES `generation_jobs` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci COMMENT='导出任务记录表';
