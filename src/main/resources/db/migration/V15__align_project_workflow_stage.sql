UPDATE `projects`
SET `workflow_stage` = CASE `status`
        WHEN 'idea_selected' THEN 'setting'
        WHEN 'setting_confirmed' THEN 'outline'
        WHEN 'outline_pending' THEN 'outline'
        WHEN 'outline_confirmed' THEN 'chapter'
        WHEN 'writing' THEN 'chapter'
        WHEN 'exported' THEN 'export'
        ELSE `workflow_stage`
    END,
    `status` = 'drafting'
WHERE `status` IN (
    'idea_selected',
    'setting_confirmed',
    'outline_pending',
    'outline_confirmed',
    'writing',
    'exported'
);
