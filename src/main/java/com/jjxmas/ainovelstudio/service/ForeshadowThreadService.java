package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThread;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;

@Service
public class ForeshadowThreadService {

    private static final int THREAD_SCAN_LIMIT = 20;
    private static final int MAX_UNRESOLVED_THREADS = 6;
    private static final int MAX_FORESHADOW_THREADS = 6;

    private final ForeshadowThreadMapper foreshadowThreadMapper;

    public ForeshadowThreadService(ForeshadowThreadMapper foreshadowThreadMapper) {
        this.foreshadowThreadMapper = foreshadowThreadMapper;
    }

    public void applyFactExtraction(Chapter chapter, ChapterFactExtraction extraction) {
        if (chapter == null || extraction == null) {
            return;
        }
        int foreshadowIndex = 0;
        for (ChapterFactExtraction.ForeshadowChangeFact item : defaultList(extraction.getForeshadowChanges())) {
            upsertForeshadowChange(chapter, item, foreshadowIndex++);
        }
        int unresolvedIndex = 0;
        for (ChapterFactExtraction.UnresolvedThreadFact item : defaultList(extraction.getUnresolvedThreads())) {
            upsertUnresolvedThread(chapter, item, unresolvedIndex++);
        }
    }

    public ChapterContext.ActiveThreads buildActiveThreads(
            Chapter chapter,
            String title,
            String outline,
            List<String> scenePlan,
            String previousSummary) {
        String referenceText = normalizeReference(title, outline, scenePlan, previousSummary);
        List<ForeshadowThread> activeThreads = foreshadowThreadMapper.selectList(new LambdaQueryWrapper<ForeshadowThread>()
                .eq(ForeshadowThread::getProjectId, chapter.getProjectId())
                .eq(ForeshadowThread::getStatus, "active")
                .le(chapter.getChapterNo() != null, ForeshadowThread::getSourceChapterNo, chapter.getChapterNo())
                .orderByDesc(ForeshadowThread::getPriority)
                .orderByDesc(ForeshadowThread::getLastMentionedChapterNo)
                .orderByAsc(ForeshadowThread::getId)
                .last("LIMIT " + THREAD_SCAN_LIMIT));

        List<ForeshadowThread> matchedThreads = activeThreads.stream()
                .filter(item -> matches(item, referenceText))
                .toList();

        return ChapterContext.ActiveThreads.builder()
                .unresolvedThreads(selectThreadTexts(
                        matchedThreads,
                        activeThreads,
                        this::isUnresolvedThread,
                        this::unresolvedThreadText,
                        MAX_UNRESOLVED_THREADS))
                .activeForeshadowThreads(selectThreadTexts(
                        matchedThreads,
                        activeThreads,
                        this::isForeshadowThread,
                        this::foreshadowThreadText,
                        MAX_FORESHADOW_THREADS))
                .build();
    }

    private void upsertForeshadowChange(
            Chapter chapter,
            ChapterFactExtraction.ForeshadowChangeFact item,
            int index) {
        if (item == null) {
            return;
        }
        String threadKey = resolveThreadKey(item.getThreadKey(), item.getThreadTitle(), chapter.getChapterNo(), "foreshadow", index);
        ForeshadowThread thread = findThread(chapter.getProjectId(), threadKey);
        boolean isNew = thread == null;
        if (thread == null) {
            thread = baseThread(chapter, threadKey);
        }
        thread.setThreadTitle(blankToDefault(item.getThreadTitle(), thread.getThreadTitle()))
                .setThreadType(blankToDefault(item.getThreadType(), "foreshadow"))
                .setPriority(maxPriority(thread.getPriority(), 70))
                .setLastMentionedChapterId(chapter.getId())
                .setLastMentionedChapterNo(chapter.getChapterNo())
                .setSetupText(blankToDefault(thread.getSetupText(), item.getSetupText()))
                .setLatestProgress(blankToDefault(item.getProgressText(), thread.getLatestProgress()))
                .setPayoffHint(blankToDefault(item.getPayoffHint(), thread.getPayoffHint()));

        if (isResolvedChange(item.getChangeType())) {
            thread.setStatus("resolved")
                    .setResolutionChapterId(chapter.getId())
                    .setResolutionChapterNo(chapter.getChapterNo())
                    .setResolutionNote(blankToDefault(item.getProgressText(), item.getSetupText()));
        } else {
            thread.setStatus("active");
        }
        persistThread(thread, isNew);
    }

    private void upsertUnresolvedThread(
            Chapter chapter,
            ChapterFactExtraction.UnresolvedThreadFact item,
            int index) {
        if (item == null) {
            return;
        }
        String threadKey = resolveThreadKey(item.getThreadKey(), item.getThreadTitle(), chapter.getChapterNo(), "thread", index);
        ForeshadowThread thread = findThread(chapter.getProjectId(), threadKey);
        boolean isNew = thread == null;
        if (thread == null) {
            thread = baseThread(chapter, threadKey);
        }
        thread.setThreadTitle(blankToDefault(item.getThreadTitle(), thread.getThreadTitle()))
                .setThreadType(blankToDefault(thread.getThreadType(), blankToDefault(item.getThreadType(), "goal")))
                .setStatus("active")
                .setPriority(maxPriority(thread.getPriority(), priorityByUrgency(item.getUrgency())))
                .setLastMentionedChapterId(chapter.getId())
                .setLastMentionedChapterNo(chapter.getChapterNo())
                .setLatestProgress(blankToDefault(item.getDescription(), thread.getLatestProgress()))
                .setTargetPayoffChapterNo(item.getTargetChapterNo() == null || item.getTargetChapterNo() <= 0
                        ? thread.getTargetPayoffChapterNo()
                        : item.getTargetChapterNo());
        if (thread.getSetupText() == null || thread.getSetupText().isBlank()) {
            thread.setSetupText(blankToEmpty(item.getDescription()));
        }
        persistThread(thread, isNew);
    }

    private ForeshadowThread findThread(Long projectId, String threadKey) {
        return foreshadowThreadMapper.selectOne(new LambdaQueryWrapper<ForeshadowThread>()
                .eq(ForeshadowThread::getProjectId, projectId)
                .eq(ForeshadowThread::getThreadKey, threadKey)
                .last("LIMIT 1"));
    }

    private ForeshadowThread baseThread(Chapter chapter, String threadKey) {
        return new ForeshadowThread()
                .setProjectId(chapter.getProjectId())
                .setThreadKey(threadKey)
                .setThreadTitle(threadKey)
                .setThreadType("foreshadow")
                .setStatus("active")
                .setPriority(0)
                .setSourceChapterId(chapter.getId())
                .setSourceChapterNo(chapter.getChapterNo())
                .setLastMentionedChapterId(chapter.getId())
                .setLastMentionedChapterNo(chapter.getChapterNo());
    }

    private void persistThread(ForeshadowThread thread, boolean isNew) {
        if (isNew) {
            foreshadowThreadMapper.insert(thread);
            return;
        }
        foreshadowThreadMapper.updateById(thread);
    }

    private List<String> selectThreadTexts(
            List<ForeshadowThread> preferred,
            List<ForeshadowThread> fallback,
            Predicate<ForeshadowThread> filter,
            Function<ForeshadowThread, String> formatter,
            int limit) {
        List<String> matched = preferred.stream()
                .filter(filter)
                .limit(limit)
                .map(formatter)
                .toList();
        if (!matched.isEmpty()) {
            return matched;
        }
        return fallback.stream()
                .filter(filter)
                .limit(limit)
                .map(formatter)
                .toList();
    }

    private boolean isUnresolvedThread(ForeshadowThread thread) {
        return !"foreshadow".equalsIgnoreCase(blankToEmpty(thread.getThreadType()));
    }

    private boolean isForeshadowThread(ForeshadowThread thread) {
        return "foreshadow".equalsIgnoreCase(blankToEmpty(thread.getThreadType()));
    }

    private boolean matches(ForeshadowThread thread, String referenceText) {
        return contains(referenceText, thread.getThreadKey())
                || contains(referenceText, thread.getThreadTitle())
                || contains(referenceText, thread.getSetupText())
                || contains(referenceText, thread.getLatestProgress())
                || contains(referenceText, thread.getPayoffHint());
    }

    private boolean isResolvedChange(String changeType) {
        String normalized = blankToEmpty(changeType).toLowerCase(Locale.ROOT);
        return "payoff".equals(normalized) || "end".equals(normalized) || "resolved".equals(normalized);
    }

    private String unresolvedThreadText(ForeshadowThread thread) {
        return "key=%s | type=%s | title=%s | progress=%s | targetChapter=%s".formatted(
                blankToEmpty(thread.getThreadKey()),
                blankToEmpty(thread.getThreadType()),
                blankToEmpty(thread.getThreadTitle()),
                blankToDefault(thread.getLatestProgress(), blankToEmpty(thread.getSetupText())),
                thread.getTargetPayoffChapterNo() == null ? "?" : thread.getTargetPayoffChapterNo());
    }

    private String foreshadowThreadText(ForeshadowThread thread) {
        return "key=%s | title=%s | setup=%s | progress=%s | payoffHint=%s".formatted(
                blankToEmpty(thread.getThreadKey()),
                blankToEmpty(thread.getThreadTitle()),
                blankToEmpty(thread.getSetupText()),
                blankToEmpty(thread.getLatestProgress()),
                blankToEmpty(thread.getPayoffHint()));
    }

    private String resolveThreadKey(String preferredKey, String title, Integer chapterNo, String prefix, int index) {
        if (preferredKey != null && !preferredKey.isBlank()) {
            return preferredKey.trim();
        }
        String slug = slugify(title);
        if (!slug.isBlank()) {
            return slug;
        }
        return "%s_%s_%s".formatted(prefix, chapterNo == null ? 0 : chapterNo, index + 1);
    }

    private String slugify(String value) {
        String normalized = blankToEmpty(value).toLowerCase(Locale.ROOT)
                .replaceAll("[^a-z0-9]+", "_")
                .replaceAll("^_+|_+$", "");
        return normalized;
    }

    private int priorityByUrgency(String urgency) {
        return switch (blankToEmpty(urgency).toLowerCase(Locale.ROOT)) {
            case "critical" -> 100;
            case "high" -> 90;
            case "medium" -> 70;
            case "low" -> 50;
            default -> 60;
        };
    }

    private int maxPriority(Integer currentPriority, int newPriority) {
        return currentPriority == null ? newPriority : Math.max(currentPriority, newPriority);
    }

    private String normalizeReference(String title, String outline, List<String> scenePlan, String previousSummary) {
        StringJoiner joiner = new StringJoiner("\n");
        joiner.add(blankToEmpty(title));
        joiner.add(blankToEmpty(outline));
        joiner.add(blankToEmpty(previousSummary));
        if (scenePlan != null) {
            scenePlan.forEach(item -> joiner.add(blankToEmpty(item)));
        }
        return joiner.toString().toLowerCase(Locale.ROOT);
    }

    private boolean contains(String referenceText, String value) {
        String normalizedValue = blankToEmpty(value).toLowerCase(Locale.ROOT);
        return !normalizedValue.isBlank() && referenceText.contains(normalizedValue);
    }

    private <T> List<T> defaultList(List<T> value) {
        return value == null ? List.of() : value;
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
