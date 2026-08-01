ALTER TABLE `projects`
  ADD COLUMN `target_chapter_word_count` INT UNSIGNED NOT NULL DEFAULT 3000 COMMENT '单章目标字数' AFTER `target_word_count_max`;
