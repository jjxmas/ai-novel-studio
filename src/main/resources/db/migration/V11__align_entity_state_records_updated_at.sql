SET @entity_state_records_updated_at_exists := (
    SELECT COUNT(*)
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'entity_state_records'
      AND column_name = 'updated_at'
);

SET @entity_state_records_updated_at_sql := IF(
    @entity_state_records_updated_at_exists = 0,
    'ALTER TABLE `entity_state_records` ADD COLUMN `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP AFTER `created_at`',
    'SELECT 1'
);

PREPARE entity_state_records_updated_at_stmt FROM @entity_state_records_updated_at_sql;
EXECUTE entity_state_records_updated_at_stmt;
DEALLOCATE PREPARE entity_state_records_updated_at_stmt;
