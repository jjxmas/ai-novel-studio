package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.mapper.ContentVersionMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadChangeMapper;
import com.jjxmas.ainovelstudio.mapper.ForeshadowThreadMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterContext;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterFactExtraction;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.ContentVersion;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThread;
import com.jjxmas.ainovelstudio.pojo.entity.ForeshadowThreadChange;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.StringJoiner;
import java.util.function.Function;
import java.util.function.Predicate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ForeshadowThreadService {

    private static final int THREAD_SCAN_LIMIT = 20;
    private static final int MAX_UNRESOLVED_THREADS = 6;
    private static final int MAX_FORESHADOW_THREADS = 6;

    private final ForeshadowThreadMapper foreshadowThreadMapper;
    private final ForeshadowThreadChangeMapper foreshadowThreadChangeMapper;
    private final ContentVersionMapper contentVersionMapper;

    public ForeshadowThreadService(
            ForeshadowThreadMapper foreshadowThreadMapper,
            ForeshadowThreadChangeMapper foreshadowThreadChangeMapper,
            ContentVersionMapper contentVersionMapper) {
        this.foreshadowThreadMapper = foreshadowThreadMapper;
        this.foreshadowThreadChangeMapper = foreshadowThreadChangeMapper;
        this.contentVersionMapper = contentVersionMapper;
    }

    @Transactional
    public void applyFactExtraction(Chapter chapter, ChapterFactExtraction extraction) {
        if (chapter == null || extraction == null) {
            return;
        }
        foreshadowThreadChangeMapper.delete(new LambdaQueryWrapper<ForeshadowThreadChange>()
                .eq(ForeshadowThreadChange::getProjectId, chapter.getProjectId())
                .eq(ForeshadowThreadChange::getChapterId, chapter.getId()));

        Long sourceContentVersionId = resolveSourceContentVersionId(chapter);
        int foreshadowIndex = 0;
        for (ChapterFactExtraction.ForeshadowChangeFact item : defaultList(extraction.getForeshadowChanges())) {
            if (item != null) {
                String threadKey = resolveThreadKey(
                        item.getThreadKey(), item.getThreadTitle(), chapter.getChapterNo(), "foreshadow", foreshadowIndex);
                foreshadowThreadChangeMapper.insert(baseChange(chapter, sourceContentVersionId, threadKey)
                        .setThreadTitle(blankToDefault(item.getThreadTitle(), threadKey))
                        .setThreadType(blankToDefault(item.getThreadType(), "foreshadow"))
                        .setChangeKind("foreshadow")
                        .setChangeType(blankToDefault(item.getChangeType(), "mention"))
                        .setPriority(70)
                        .setSetupText(item.getSetupText())
                        .setProgressText(item.getProgressText())
                        .setPayoffHint(item.getPayoffHint()));
            }
            foreshadowIndex++;
        }
        int unresolvedIndex = 0;
        for (ChapterFactExtraction.UnresolvedThreadFact item : defaultList(extraction.getUnresolvedThreads())) {
            if (item != null) {
                String threadKey = resolveThreadKey(
                        item.getThreadKey(), item.getThreadTitle(), chapter.getChapterNo(), "thread", unresolvedIndex);
                foreshadowThreadChangeMapper.insert(baseChange(chapter, sourceContentVersionId, threadKey)
                        .setThreadTitle(blankToDefault(item.getThreadTitle(), threadKey))
                        .setThreadType(blankToDefault(item.getThreadType(), "goal"))
                        .setChangeKind("unresolved")
                        .setChangeType("mention")
                        .setPriority(priorityByUrgency(item.getUrgency()))
                        .setSetupText(item.getDescription())
                        .setProgressText(item.getDescription())
                        .setTargetPayoffChapterNo(item.getTargetChapterNo()));
            }
            unresolvedIndex++;
        }
        rebuildProjection(chapter.getProjectId());
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
                .and(wrapper -> {
                    wrapper.eq(ForeshadowThread::getStatus, "active");
                    if (chapter.getChapterNo() != null) {
                        wrapper.or(resolved -> resolved
                                .eq(ForeshadowThread::getStatus, "resolved")
                                .gt(ForeshadowThread::getResolutionChapterNo, chapter.getChapterNo()));
                    }
                })
                .le(chapter.getChapterNo() != null, ForeshadowThread::getSourceChapterNo, chapter.getChapterNo())
                .orderByDesc(ForeshadowThread::getPriority)
                .orderByDesc(ForeshadowThread::getLastMentionedChapterNo)
                .orderByAsc(ForeshadowThread::getId)
                .last("LIMIT " + THREAD_SCAN_LIMIT))
                .stream()
                .filter(thread -> threadVisibleAt(thread, chapter.getChapterNo()))
                .toList();

        List<ForeshadowThread> matchedThreads = activeThreads.stream()
                .filter(item -> matches(item, referenceText))
                .toList();

        return ChapterContext.ActiveThreads.builder()
                .unresolvedThreads(selectThreadTexts(
                        matchedThreads,
                        activeThreads,
                        this::isUnresolvedThread,
                        item -> unresolvedThreadText(item, chapter.getChapterNo()),
                        MAX_UNRESOLVED_THREADS))
                .activeForeshadowThreads(selectThreadTexts(
                        matchedThreads,
                        activeThreads,
                        this::isForeshadowThread,
                        item -> foreshadowThreadText(item, chapter.getChapterNo()),
                        MAX_FORESHADOW_THREADS))
                .build();
    }

    void rebuildProjection(Long projectId) {
        List<ForeshadowThreadChange> changes = foreshadowThreadChangeMapper.selectList(
                new LambdaQueryWrapper<ForeshadowThreadChange>()
                        .eq(ForeshadowThreadChange::getProjectId, projectId)
                        .orderByAsc(ForeshadowThreadChange::getChapterNo)
                        .orderByAsc(ForeshadowThreadChange::getId));
        Map<String, ForeshadowThread> threadsByKey = new LinkedHashMap<>();
        for (ForeshadowThreadChange change : changes) {
            ForeshadowThread thread = threadsByKey.computeIfAbsent(
                    change.getThreadKey(), ignored -> baseThread(change));
            applyChange(thread, change);
        }

        foreshadowThreadMapper.delete(new LambdaQueryWrapper<ForeshadowThread>()
                .eq(ForeshadowThread::getProjectId, projectId));
        threadsByKey.values().forEach(foreshadowThreadMapper::insert);
    }

    private ForeshadowThreadChange baseChange(
            Chapter chapter,
            Long sourceContentVersionId,
            String threadKey) {
        return new ForeshadowThreadChange()
                .setProjectId(chapter.getProjectId())
                .setThreadKey(threadKey)
                .setChapterId(chapter.getId())
                .setChapterNo(chapter.getChapterNo())
                .setOriginChapterId(chapter.getId())
                .setOriginChapterNo(chapter.getChapterNo())
                .setSourceContentVersionId(sourceContentVersionId);
    }

    private ForeshadowThread baseThread(ForeshadowThreadChange change) {
        return new ForeshadowThread()
                .setProjectId(change.getProjectId())
                .setThreadKey(change.getThreadKey())
                .setThreadTitle(change.getThreadKey())
                .setThreadType("foreshadow")
                .setStatus("active")
                .setPriority(0)
                .setSourceChapterId(change.getOriginChapterId())
                .setSourceChapterNo(change.getOriginChapterNo());
    }

    private void applyChange(ForeshadowThread thread, ForeshadowThreadChange change) {
        thread.setThreadTitle(blankToDefault(change.getThreadTitle(), thread.getThreadTitle()))
                .setPriority(maxPriority(thread.getPriority(), change.getPriority() == null ? 0 : change.getPriority()))
                .setLastMentionedChapterId(change.getChapterId())
                .setLastMentionedChapterNo(change.getChapterNo());

        if ("unresolved".equalsIgnoreCase(change.getChangeKind())) {
            thread.setThreadType(blankToDefault(change.getThreadType(), "goal"))
                    .setStatus("active")
                    .setResolutionChapterId(null)
                    .setResolutionChapterNo(null)
                    .setResolutionNote(null)
                    .setLatestProgress(blankToDefault(change.getProgressText(), thread.getLatestProgress()));
            if (thread.getSetupText() == null || thread.getSetupText().isBlank()) {
                thread.setSetupText(blankToEmpty(change.getSetupText()));
            }
            if (change.getTargetPayoffChapterNo() != null && change.getTargetPayoffChapterNo() > 0) {
                thread.setTargetPayoffChapterNo(change.getTargetPayoffChapterNo());
            }
            return;
        }

        thread.setThreadType(blankToDefault(change.getThreadType(), "foreshadow"))
                .setSetupText(blankToDefault(thread.getSetupText(), change.getSetupText()))
                .setLatestProgress(blankToDefault(change.getProgressText(), thread.getLatestProgress()))
                .setPayoffHint(blankToDefault(change.getPayoffHint(), thread.getPayoffHint()));
        if (isResolvedChange(change.getChangeType())) {
            thread.setStatus("resolved")
                    .setResolutionChapterId(change.getChapterId())
                    .setResolutionChapterNo(change.getChapterNo())
                    .setResolutionNote(blankToDefault(change.getProgressText(), change.getSetupText()));
        } else {
            thread.setStatus("active")
                    .setResolutionChapterId(null)
                    .setResolutionChapterNo(null)
                    .setResolutionNote(null);
        }
    }

    private Long resolveSourceContentVersionId(Chapter chapter) {
        LambdaQueryWrapper<ContentVersion> query = new LambdaQueryWrapper<ContentVersion>()
                .eq(ContentVersion::getEntityType, "chapter")
                .eq(ContentVersion::getEntityId, chapter.getId());
        if (chapter.getLastContentVersionNo() != null) {
            query.eq(ContentVersion::getVersionNo, chapter.getLastContentVersionNo());
        }
        ContentVersion version = contentVersionMapper.selectOne(query
                .orderByDesc(ContentVersion::getVersionNo)
                .last("LIMIT 1"));
        return version == null ? null : version.getId();
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

    private String unresolvedThreadText(ForeshadowThread thread, Integer chapterNo) {
        boolean hasFutureProgress = hasFutureProgress(thread, chapterNo);
        return "key=%s | type=%s | title=%s | progress=%s | targetChapter=%s".formatted(
                blankToEmpty(thread.getThreadKey()),
                blankToEmpty(thread.getThreadType()),
                blankToEmpty(thread.getThreadTitle()),
                hasFutureProgress
                        ? blankToEmpty(thread.getSetupText())
                        : blankToDefault(thread.getLatestProgress(), blankToEmpty(thread.getSetupText())),
                hasFutureProgress || thread.getTargetPayoffChapterNo() == null
                        ? "?"
                        : thread.getTargetPayoffChapterNo());
    }

    private String foreshadowThreadText(ForeshadowThread thread, Integer chapterNo) {
        boolean hasFutureProgress = hasFutureProgress(thread, chapterNo);
        return "key=%s | title=%s | setup=%s | progress=%s | payoffHint=%s".formatted(
                blankToEmpty(thread.getThreadKey()),
                blankToEmpty(thread.getThreadTitle()),
                blankToEmpty(thread.getSetupText()),
                hasFutureProgress ? "" : blankToEmpty(thread.getLatestProgress()),
                hasFutureProgress ? "" : blankToEmpty(thread.getPayoffHint()));
    }

    private boolean hasFutureProgress(ForeshadowThread thread, Integer chapterNo) {
        return chapterNo != null
                && thread.getLastMentionedChapterNo() != null
                && thread.getLastMentionedChapterNo() > chapterNo;
    }

    private boolean threadVisibleAt(ForeshadowThread thread, Integer chapterNo) {
        if (chapterNo == null) {
            return "active".equalsIgnoreCase(blankToEmpty(thread.getStatus()));
        }
        if (thread.getSourceChapterNo() != null && thread.getSourceChapterNo() > chapterNo) {
            return false;
        }
        if ("active".equalsIgnoreCase(blankToEmpty(thread.getStatus()))) {
            return true;
        }
        return "resolved".equalsIgnoreCase(blankToEmpty(thread.getStatus()))
                && thread.getResolutionChapterNo() != null
                && thread.getResolutionChapterNo() > chapterNo;
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
