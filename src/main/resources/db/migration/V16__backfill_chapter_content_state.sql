-- 对齐 V2 已有章节正文状态列；旧迁移保持不可变。
UPDATE `chapters`
SET `content_status` = 'generated',
    `content_generated_at` = COALESCE(`content_generated_at`, `updated_at`, `created_at`),
    `content_updated_at` = COALESCE(`content_updated_at`, `updated_at`, `created_at`)
WHERE `content` IS NOT NULL
  AND TRIM(`content`) <> ''
  AND `content_status` = 'not_generated';
