SET @entity_state_records_project_chapter_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'entity_state_records'
      AND index_name = 'idx_entity_state_records_project_chapter'
);

SET @entity_state_records_project_chapter_sql := IF(
    @entity_state_records_project_chapter_exists = 0,
    'ALTER TABLE `entity_state_records` ADD KEY `idx_entity_state_records_project_chapter` (`project_id`, `chapter_id`)',
    'SELECT 1'
);

PREPARE entity_state_records_project_chapter_stmt FROM @entity_state_records_project_chapter_sql;
EXECUTE entity_state_records_project_chapter_stmt;
DEALLOCATE PREPARE entity_state_records_project_chapter_stmt;

SET @story_events_project_chapter_planned_exists := (
    SELECT COUNT(*)
    FROM information_schema.statistics
    WHERE table_schema = DATABASE()
      AND table_name = 'story_events'
      AND index_name = 'idx_story_events_project_chapter_planned'
);

SET @story_events_project_chapter_planned_sql := IF(
    @story_events_project_chapter_planned_exists = 0,
    'ALTER TABLE `story_events` ADD KEY `idx_story_events_project_chapter_planned` (`project_id`, `chapter_id`, `is_planned`)',
    'SELECT 1'
);

PREPARE story_events_project_chapter_planned_stmt FROM @story_events_project_chapter_planned_sql;
EXECUTE story_events_project_chapter_planned_stmt;
DEALLOCATE PREPARE story_events_project_chapter_planned_stmt;
