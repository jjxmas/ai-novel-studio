ALTER TABLE `story_events`
  ADD COLUMN `source_content_version_id` BIGINT UNSIGNED NULL AFTER `chapter_id`,
  ADD KEY `idx_story_events_source_version` (`source_content_version_id`),
  ADD CONSTRAINT `fk_story_events_source_version`
    FOREIGN KEY (`source_content_version_id`) REFERENCES `content_versions` (`id`) ON DELETE SET NULL;

ALTER TABLE `entity_state_records`
  ADD COLUMN `source_content_version_id` BIGINT UNSIGNED NULL AFTER `chapter_id`,
  ADD KEY `idx_entity_state_source_version` (`source_content_version_id`),
  ADD CONSTRAINT `fk_entity_state_source_version`
    FOREIGN KEY (`source_content_version_id`) REFERENCES `content_versions` (`id`) ON DELETE SET NULL;

ALTER TABLE `entity_relations`
  ADD COLUMN `start_chapter_id` BIGINT UNSIGNED NULL AFTER `start_event_id`,
  ADD COLUMN `start_chapter_no` INT NULL AFTER `start_chapter_id`,
  ADD COLUMN `start_content_version_id` BIGINT UNSIGNED NULL AFTER `start_chapter_no`,
  ADD COLUMN `end_chapter_id` BIGINT UNSIGNED NULL AFTER `end_event_id`,
  ADD COLUMN `end_chapter_no` INT NULL AFTER `end_chapter_id`,
  ADD COLUMN `end_content_version_id` BIGINT UNSIGNED NULL AFTER `end_chapter_no`,
  ADD KEY `idx_entity_relations_start_chapter` (`project_id`, `start_chapter_no`),
  ADD KEY `idx_entity_relations_end_chapter` (`project_id`, `end_chapter_no`),
  ADD KEY `idx_entity_relations_start_version` (`start_content_version_id`),
  ADD KEY `idx_entity_relations_end_version` (`end_content_version_id`),
  ADD CONSTRAINT `fk_entity_relations_start_chapter`
    FOREIGN KEY (`start_chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_entity_relations_end_chapter`
    FOREIGN KEY (`end_chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_entity_relations_start_version`
    FOREIGN KEY (`start_content_version_id`) REFERENCES `content_versions` (`id`) ON DELETE SET NULL,
  ADD CONSTRAINT `fk_entity_relations_end_version`
    FOREIGN KEY (`end_content_version_id`) REFERENCES `content_versions` (`id`) ON DELETE SET NULL;

UPDATE `story_events` se
JOIN `chapters` c ON c.`id` = se.`chapter_id`
LEFT JOIN `content_versions` cv
  ON cv.`entity_type` = 'chapter'
  AND cv.`entity_id` = c.`id`
  AND cv.`version_no` = c.`last_content_version_no`
SET se.`source_content_version_id` = cv.`id`
WHERE se.`is_planned` = 0;

UPDATE `entity_state_records` esr
JOIN `chapters` c ON c.`id` = esr.`chapter_id`
LEFT JOIN `content_versions` cv
  ON cv.`entity_type` = 'chapter'
  AND cv.`entity_id` = c.`id`
  AND cv.`version_no` = c.`last_content_version_no`
SET esr.`source_content_version_id` = cv.`id`;

UPDATE `entity_relations` er
LEFT JOIN `story_events` se_start ON se_start.`id` = er.`start_event_id`
LEFT JOIN `chapters` c_start ON c_start.`id` = se_start.`chapter_id`
LEFT JOIN `story_events` se_end ON se_end.`id` = er.`end_event_id`
LEFT JOIN `chapters` c_end ON c_end.`id` = se_end.`chapter_id`
SET er.`start_chapter_id` = c_start.`id`,
    er.`start_chapter_no` = c_start.`chapter_no`,
    er.`start_content_version_id` = se_start.`source_content_version_id`,
    er.`end_chapter_id` = c_end.`id`,
    er.`end_chapter_no` = c_end.`chapter_no`,
    er.`end_content_version_id` = se_end.`source_content_version_id`;

CREATE TABLE `foreshadow_thread_changes` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `project_id` BIGINT UNSIGNED NOT NULL,
  `thread_key` VARCHAR(128) NOT NULL,
  `thread_title` VARCHAR(255) NOT NULL,
  `thread_type` VARCHAR(32) NOT NULL,
  `change_kind` VARCHAR(16) NOT NULL,
  `change_type` VARCHAR(16) NOT NULL,
  `priority` INT NOT NULL DEFAULT 0,
  `chapter_id` BIGINT UNSIGNED NULL,
  `chapter_no` INT NULL,
  `origin_chapter_id` BIGINT UNSIGNED NULL,
  `origin_chapter_no` INT NULL,
  `source_content_version_id` BIGINT UNSIGNED NULL,
  `setup_text` LONGTEXT NULL,
  `progress_text` LONGTEXT NULL,
  `payoff_hint` LONGTEXT NULL,
  `target_payoff_chapter_no` INT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_foreshadow_changes_project_chapter` (`project_id`, `chapter_no`, `id`),
  KEY `idx_foreshadow_changes_thread` (`project_id`, `thread_key`, `chapter_no`),
  KEY `idx_foreshadow_changes_chapter_id` (`chapter_id`),
  KEY `idx_foreshadow_changes_source_version` (`source_content_version_id`),
  CONSTRAINT `fk_foreshadow_changes_project`
    FOREIGN KEY (`project_id`) REFERENCES `projects` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_foreshadow_changes_chapter`
    FOREIGN KEY (`chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_foreshadow_changes_origin_chapter`
    FOREIGN KEY (`origin_chapter_id`) REFERENCES `chapters` (`id`) ON DELETE SET NULL,
  CONSTRAINT `fk_foreshadow_changes_source_version`
    FOREIGN KEY (`source_content_version_id`) REFERENCES `content_versions` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci
  COMMENT='Chapter-derived changes used to rebuild foreshadow thread projections';

INSERT INTO `foreshadow_thread_changes` (
  `project_id`, `thread_key`, `thread_title`, `thread_type`, `change_kind`, `change_type`, `priority`,
  `chapter_id`, `chapter_no`, `origin_chapter_id`, `origin_chapter_no`, `source_content_version_id`,
  `setup_text`, `progress_text`, `payoff_hint`, `target_payoff_chapter_no`, `created_at`, `updated_at`
)
SELECT thread.`project_id`, thread.`thread_key`, thread.`thread_title`, thread.`thread_type`,
       IF(thread.`thread_type` = 'foreshadow', 'foreshadow', 'unresolved'),
       IF(thread.`status` = 'resolved', 'resolved', 'mention'), thread.`priority`,
       thread.`last_mentioned_chapter_id`, thread.`last_mentioned_chapter_no`,
       thread.`source_chapter_id`, thread.`source_chapter_no`, version.`id`,
       thread.`setup_text`, thread.`latest_progress`, thread.`payoff_hint`,
       thread.`target_payoff_chapter_no`, thread.`created_at`, thread.`updated_at`
FROM `foreshadow_threads` thread
LEFT JOIN `chapters` chapter ON chapter.`id` = thread.`last_mentioned_chapter_id`
LEFT JOIN `content_versions` version
  ON version.`entity_type` = 'chapter'
  AND version.`entity_id` = chapter.`id`
  AND version.`version_no` = chapter.`last_content_version_no`;
