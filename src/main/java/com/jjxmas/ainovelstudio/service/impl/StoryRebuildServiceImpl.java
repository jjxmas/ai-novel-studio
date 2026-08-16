package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.mapper.StoryRebuildRunMapper;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildResult;
import com.jjxmas.ainovelstudio.pojo.dto.StoryRebuildRunResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.pojo.entity.StoryDirtyMark;
import com.jjxmas.ainovelstudio.pojo.entity.StoryRebuildRun;
import com.jjxmas.ainovelstudio.service.ChapterMemoryService;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.service.StoryDirtyMarkService;
import com.jjxmas.ainovelstudio.service.StoryRebuildService;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StoryRebuildServiceImpl implements StoryRebuildService {

    private static final String NOT_FOUND_MESSAGE = "\u4f5c\u54c1\u4e0d\u5b58\u5728";
    private static final String NO_ACTIVE_DIRTY_MARK_MESSAGE =
            "\u6ca1\u6709\u53ef\u56de\u7b97\u7684 active dirty mark";
    private static final String NO_REBUILDABLE_CHAPTER_MESSAGE =
            "\u6ca1\u6709\u53ef\u91cd\u8dd1\u540e\u5904\u7406\u7684\u7ae0\u8282\u6b63\u6587";
    private static final String REBUILD_COMPLETED_MESSAGE =
            "\u5df2\u6309\u7ae0\u8282\u987a\u5e8f\u91cd\u8dd1\u4e8b\u5b9e\u62bd\u53d6\u3001\u4e8b\u5b9e\u6295\u5f71\u3001\u7ebf\u7a0b\u6295\u5f71\u548c\u6458\u8981\u8bb0\u5fc6\u94fe";

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final ChapterMemoryService chapterMemoryService;
    private final StoryDirtyMarkService storyDirtyMarkService;
    private final StoryRebuildRunMapper storyRebuildRunMapper;
    private final GenerationJobService generationJobService;

    public StoryRebuildServiceImpl(
            ProjectMapper projectMapper,
            ChapterMapper chapterMapper,
            ChapterMemoryService chapterMemoryService,
            StoryDirtyMarkService storyDirtyMarkService,
            StoryRebuildRunMapper storyRebuildRunMapper,
            GenerationJobService generationJobService) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.chapterMemoryService = chapterMemoryService;
        this.storyDirtyMarkService = storyDirtyMarkService;
        this.storyRebuildRunMapper = storyRebuildRunMapper;
        this.generationJobService = generationJobService;
    }

    @Override
    public StoryRebuildResult rebuildFromChapter(Long projectId, Integer startChapterNo, Long modelConfigId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE);
        }

        Integer actualStartChapterNo = startChapterNo == null
                ? storyDirtyMarkService.earliestActiveDirtyChapterNo(projectId)
                : startChapterNo;
        if (actualStartChapterNo == null) {
            return StoryRebuildResult.builder()
                    .projectId(projectId)
                    .requestedStartChapterNo(startChapterNo)
                    .actualStartChapterNo(null)
                    .endChapterNo(null)
                    .processedChapterCount(0)
                    .skippedChapterCount(0)
                    .activeDirtyMarkCountBefore(0)
                    .resolvedDirtyMarkCount(0)
                    .activeDirtyMarkCountAfter(0)
                    .earliestDirtyChapterNoAfter(null)
                    .processedChapterNos(List.of())
                    .skippedChapterNos(List.of())
                    .status("noop")
                    .note(NO_ACTIVE_DIRTY_MARK_MESSAGE)
                    .build();
        }

        List<StoryDirtyMark> activeMarksBefore = storyDirtyMarkService.listActiveMarks(projectId);
        List<Long> capturedDirtyMarkIds = activeMarksBefore.stream()
                .filter(mark -> mark.getDirtyFromChapterNo() != null
                        && mark.getDirtyFromChapterNo() >= actualStartChapterNo)
                .map(StoryDirtyMark::getId)
                .filter(id -> id != null)
                .toList();
        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, projectId)
                .orderByAsc(Chapter::getChapterNo));

        List<Integer> processedChapterNos = new ArrayList<>();
        List<Integer> skippedChapterNos = new ArrayList<>();
        for (Chapter chapter : chapters) {
            if (chapter.getChapterNo() == null || chapter.getChapterNo() < actualStartChapterNo) {
                continue;
            }
            if (chapter.getContent() == null || chapter.getContent().isBlank()) {
                chapterMemoryService.clearFactProjection(chapter);
                if (chapter.getChapterNo() != null) {
                    skippedChapterNos.add(chapter.getChapterNo());
                }
                continue;
            }
            chapterMemoryService.refreshFactProjection(chapter, modelConfigId);
            if (chapter.getChapterNo() != null) {
                processedChapterNos.add(chapter.getChapterNo());
            }
        }

        if (!processedChapterNos.isEmpty() || !skippedChapterNos.isEmpty()) {
            chapterMemoryService.resetNarrativeMemory(projectId);
            for (Chapter chapter : chapters) {
                if (chapter.getContent() != null && !chapter.getContent().isBlank()) {
                    chapterMemoryService.refreshNarrativeMemory(chapter, modelConfigId);
                }
            }
        }

        boolean replayedAnyChapter = !processedChapterNos.isEmpty() || !skippedChapterNos.isEmpty();
        if (replayedAnyChapter) {
            storyDirtyMarkService.resolveActiveMarksByIds(capturedDirtyMarkIds);
        }
        int resolvedDirtyMarkCount = replayedAnyChapter
                ? capturedDirtyMarkIds.size() - storyDirtyMarkService.countActiveMarksByIds(capturedDirtyMarkIds)
                : 0;
        List<StoryDirtyMark> remainingActiveMarks = storyDirtyMarkService.listActiveMarks(projectId);

        Integer endChapterNo = chapters.stream()
                .map(Chapter::getChapterNo)
                .filter(chapterNo -> chapterNo != null && chapterNo >= actualStartChapterNo)
                .max(Integer::compareTo)
                .orElse(null);

        return StoryRebuildResult.builder()
                .projectId(projectId)
                .requestedStartChapterNo(startChapterNo)
                .actualStartChapterNo(actualStartChapterNo)
                .endChapterNo(endChapterNo)
                .processedChapterCount(processedChapterNos.size())
                .skippedChapterCount(skippedChapterNos.size())
                .activeDirtyMarkCountBefore(activeMarksBefore.size())
                .resolvedDirtyMarkCount(resolvedDirtyMarkCount)
                .activeDirtyMarkCountAfter(remainingActiveMarks.size())
                .earliestDirtyChapterNoAfter(storyDirtyMarkService.earliestActiveDirtyChapterNo(projectId))
                .processedChapterNos(processedChapterNos)
                .skippedChapterNos(skippedChapterNos)
                .status(replayedAnyChapter ? "completed" : "noop")
                .note(replayedAnyChapter ? REBUILD_COMPLETED_MESSAGE : NO_REBUILDABLE_CHAPTER_MESSAGE)
                .build();
    }

    @Override
    public StoryRebuildResult rebuildFromEarliestDirty(Long projectId, Long modelConfigId) {
        return rebuildFromChapter(projectId, null, modelConfigId);
    }

    @Override
    @Transactional
    public StoryRebuildRunResponse enqueueRebuild(Long projectId, Integer startChapterNo, Long modelConfigId) {
        requireProject(projectId);
        StoryRebuildRun activeRun = storyRebuildRunMapper.selectOne(new LambdaQueryWrapper<StoryRebuildRun>()
                .eq(StoryRebuildRun::getProjectId, projectId)
                .in(StoryRebuildRun::getStatus, List.of("pending", "running"))
                .orderByDesc(StoryRebuildRun::getId)
                .last("LIMIT 1"));
        if (activeRun != null) {
            return toRunResponse(activeRun);
        }

        Integer actualStartChapterNo = startChapterNo == null
                ? storyDirtyMarkService.earliestActiveDirtyChapterNo(projectId)
                : startChapterNo;
        List<StoryDirtyMark> activeMarks = storyDirtyMarkService.listActiveMarks(projectId);
        List<Long> capturedDirtyMarkIds = activeMarks.stream()
                .filter(mark -> actualStartChapterNo != null
                        && mark.getDirtyFromChapterNo() != null
                        && mark.getDirtyFromChapterNo() >= actualStartChapterNo)
                .map(StoryDirtyMark::getId)
                .filter(id -> id != null)
                .toList();
        StoryRebuildRun run = new StoryRebuildRun()
                .setProjectId(projectId)
                .setModelConfigId(modelConfigId)
                .setRequestedStartChapterNo(startChapterNo)
                .setActualStartChapterNo(actualStartChapterNo)
                .setPhase(actualStartChapterNo == null ? "completed" : "fact_projection")
                .setNextFactChapterNo(actualStartChapterNo)
                .setMemoryResetDone(false)
                .setProcessedChapterCount(0)
                .setSkippedChapterCount(0)
                .setProcessedChapterNosJson("[]")
                .setSkippedChapterNosJson("[]")
                .setDirtyMarkIdsJson(JsonUtils.toJson(capturedDirtyMarkIds))
                .setActiveDirtyMarkCountBefore(activeMarks.size())
                .setStatus(actualStartChapterNo == null ? "succeeded" : "pending");
        if (actualStartChapterNo == null) {
            StoryRebuildResult result = noOpResult(projectId, startChapterNo);
            run.setResultJson(JsonUtils.toJson(result)).setFinishedAt(LocalDateTime.now());
        }
        storyRebuildRunMapper.insert(run);
        if (actualStartChapterNo != null) {
            Long jobId = generationJobService.enqueueJob(
                    projectId,
                    "story_rebuild",
                    "story_rebuild_run",
                    run.getId(),
                    modelConfigId,
                    java.util.Map.of("runId", run.getId()),
                    "story-rebuild-run:" + run.getId(),
                    5,
                    LocalDateTime.now());
            run.setGenerationJobId(jobId);
            storyRebuildRunMapper.updateById(run);
        }
        return toRunResponse(run);
    }

    @Override
    public StoryRebuildRunResponse getRebuildRun(Long projectId, Long runId) {
        StoryRebuildRun run = requireRun(runId);
        if (!projectId.equals(run.getProjectId())) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "重建任务不存在");
        }
        return toRunResponse(run);
    }

    @Override
    public StoryRebuildRunResponse getLatestRebuildRun(Long projectId) {
        requireProject(projectId);
        StoryRebuildRun run = storyRebuildRunMapper.selectOne(new LambdaQueryWrapper<StoryRebuildRun>()
                .eq(StoryRebuildRun::getProjectId, projectId)
                .orderByDesc(StoryRebuildRun::getId)
                .last("LIMIT 1"));
        return run == null ? null : toRunResponse(run);
    }

    @Override
    public StoryRebuildResult processQueuedRebuild(Long runId, Runnable heartbeat) {
        StoryRebuildRun run = requireRun(runId);
        Runnable checkpointHeartbeat = heartbeat == null ? () -> { } : heartbeat;
        StoryRebuildResult completed = JsonUtils.toObject(run.getResultJson(), StoryRebuildResult.class);
        if ("succeeded".equals(run.getStatus()) && completed != null) {
            return completed;
        }
        run.setStatus("running")
                .setErrorMessage(null)
                .setStartedAt(run.getStartedAt() == null ? LocalDateTime.now() : run.getStartedAt());
        storyRebuildRunMapper.updateById(run);

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, run.getProjectId())
                .orderByAsc(Chapter::getChapterNo));
        List<Integer> processedChapterNos = new ArrayList<>(JsonUtils.toIntegerList(run.getProcessedChapterNosJson()));
        List<Integer> skippedChapterNos = new ArrayList<>(JsonUtils.toIntegerList(run.getSkippedChapterNosJson()));

        if ("fact_projection".equals(run.getPhase())) {
            replayFactProjection(run, chapters, processedChapterNos, skippedChapterNos, checkpointHeartbeat);
            if (processedChapterNos.isEmpty() && skippedChapterNos.isEmpty()) {
                run.setPhase("finalizing");
            } else {
                run.setPhase("narrative_memory")
                        .setNextMemoryChapterNo(firstChapterNo(chapters));
            }
            storyRebuildRunMapper.updateById(run);
        }

        if ("narrative_memory".equals(run.getPhase())) {
            if (!Boolean.TRUE.equals(run.getMemoryResetDone())) {
                chapterMemoryService.resetNarrativeMemory(run.getProjectId());
                run.setMemoryResetDone(true);
                storyRebuildRunMapper.updateById(run);
            }
            replayNarrativeMemory(run, chapters, checkpointHeartbeat);
            run.setPhase("finalizing");
            storyRebuildRunMapper.updateById(run);
        }

        List<Long> dirtyMarkIds = JsonUtils.toLongList(run.getDirtyMarkIdsJson());
        storyDirtyMarkService.resolveActiveMarksByIds(dirtyMarkIds);
        int resolvedDirtyMarkCount = dirtyMarkIds.size() - storyDirtyMarkService.countActiveMarksByIds(dirtyMarkIds);
        List<StoryDirtyMark> remainingMarks = storyDirtyMarkService.listActiveMarks(run.getProjectId());
        boolean replayedAnyChapter = !processedChapterNos.isEmpty() || !skippedChapterNos.isEmpty();
        StoryRebuildResult result = StoryRebuildResult.builder()
                .projectId(run.getProjectId())
                .requestedStartChapterNo(run.getRequestedStartChapterNo())
                .actualStartChapterNo(run.getActualStartChapterNo())
                .endChapterNo(lastReplayedChapterNo(processedChapterNos, skippedChapterNos))
                .processedChapterCount(processedChapterNos.size())
                .skippedChapterCount(skippedChapterNos.size())
                .activeDirtyMarkCountBefore(run.getActiveDirtyMarkCountBefore())
                .resolvedDirtyMarkCount(resolvedDirtyMarkCount)
                .activeDirtyMarkCountAfter(remainingMarks.size())
                .earliestDirtyChapterNoAfter(storyDirtyMarkService.earliestActiveDirtyChapterNo(run.getProjectId()))
                .processedChapterNos(processedChapterNos)
                .skippedChapterNos(skippedChapterNos)
                .status(replayedAnyChapter ? "completed" : "noop")
                .note(replayedAnyChapter ? REBUILD_COMPLETED_MESSAGE : NO_REBUILDABLE_CHAPTER_MESSAGE)
                .build();
        run.setPhase("completed")
                .setStatus("succeeded")
                .setResultJson(JsonUtils.toJson(result))
                .setFinishedAt(LocalDateTime.now());
        storyRebuildRunMapper.updateById(run);
        return result;
    }

    @Override
    public void markQueueFailure(Long runId, String errorMessage, boolean retry) {
        StoryRebuildRun run = requireRun(runId);
        run.setStatus(retry ? "pending" : "failed")
                .setErrorMessage(errorMessage == null ? "" : errorMessage)
                .setFinishedAt(retry ? null : LocalDateTime.now());
        storyRebuildRunMapper.updateById(run);
    }

    private void replayFactProjection(
            StoryRebuildRun run,
            List<Chapter> chapters,
            List<Integer> processedChapterNos,
            List<Integer> skippedChapterNos,
            Runnable heartbeat) {
        int nextChapterNo = run.getNextFactChapterNo() == null
                ? run.getActualStartChapterNo()
                : run.getNextFactChapterNo();
        for (Chapter chapter : chapters) {
            if (chapter.getChapterNo() == null || chapter.getChapterNo() < nextChapterNo) {
                continue;
            }
            if (chapter.getContent() == null || chapter.getContent().isBlank()) {
                chapterMemoryService.clearFactProjection(chapter);
                addOnce(skippedChapterNos, chapter.getChapterNo());
            } else {
                chapterMemoryService.refreshFactProjection(chapter, run.getModelConfigId());
                addOnce(processedChapterNos, chapter.getChapterNo());
            }
            run.setNextFactChapterNo(chapter.getChapterNo() + 1)
                    .setProcessedChapterCount(processedChapterNos.size())
                    .setSkippedChapterCount(skippedChapterNos.size())
                    .setProcessedChapterNosJson(JsonUtils.toJson(processedChapterNos))
                    .setSkippedChapterNosJson(JsonUtils.toJson(skippedChapterNos));
            storyRebuildRunMapper.updateById(run);
            heartbeat.run();
        }
    }

    private void replayNarrativeMemory(StoryRebuildRun run, List<Chapter> chapters, Runnable heartbeat) {
        int nextChapterNo = run.getNextMemoryChapterNo() == null
                ? Integer.MIN_VALUE
                : run.getNextMemoryChapterNo();
        for (Chapter chapter : chapters) {
            if (chapter.getChapterNo() == null
                    || chapter.getChapterNo() < nextChapterNo
                    || chapter.getContent() == null
                    || chapter.getContent().isBlank()) {
                continue;
            }
            chapterMemoryService.refreshNarrativeMemory(chapter, run.getModelConfigId());
            run.setNextMemoryChapterNo(chapter.getChapterNo() + 1);
            storyRebuildRunMapper.updateById(run);
            heartbeat.run();
        }
    }

    private StoryRebuildRun requireRun(Long runId) {
        StoryRebuildRun run = storyRebuildRunMapper.selectById(runId);
        if (run == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "重建任务不存在");
        }
        return run;
    }

    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, NOT_FOUND_MESSAGE);
        }
        return project;
    }

    private StoryRebuildRunResponse toRunResponse(StoryRebuildRun run) {
        return StoryRebuildRunResponse.builder()
                .runId(run.getId())
                .generationJobId(run.getGenerationJobId())
                .projectId(run.getProjectId())
                .status(run.getStatus())
                .phase(run.getPhase())
                .requestedStartChapterNo(run.getRequestedStartChapterNo())
                .actualStartChapterNo(run.getActualStartChapterNo())
                .nextFactChapterNo(run.getNextFactChapterNo())
                .nextMemoryChapterNo(run.getNextMemoryChapterNo())
                .processedChapterCount(run.getProcessedChapterCount())
                .skippedChapterCount(run.getSkippedChapterCount())
                .errorMessage(run.getErrorMessage())
                .result(JsonUtils.toObject(run.getResultJson(), StoryRebuildResult.class))
                .createdAt(run.getCreatedAt())
                .updatedAt(run.getUpdatedAt())
                .build();
    }

    private StoryRebuildResult noOpResult(Long projectId, Integer requestedStartChapterNo) {
        return StoryRebuildResult.builder()
                .projectId(projectId)
                .requestedStartChapterNo(requestedStartChapterNo)
                .processedChapterCount(0)
                .skippedChapterCount(0)
                .activeDirtyMarkCountBefore(0)
                .resolvedDirtyMarkCount(0)
                .activeDirtyMarkCountAfter(0)
                .processedChapterNos(List.of())
                .skippedChapterNos(List.of())
                .status("noop")
                .note(NO_ACTIVE_DIRTY_MARK_MESSAGE)
                .build();
    }

    private Integer firstChapterNo(List<Chapter> chapters) {
        return chapters.stream().map(Chapter::getChapterNo).filter(no -> no != null).min(Integer::compareTo).orElse(null);
    }

    private Integer lastReplayedChapterNo(List<Integer> processed, List<Integer> skipped) {
        return java.util.stream.Stream.concat(processed.stream(), skipped.stream()).max(Integer::compareTo).orElse(null);
    }

    private void addOnce(List<Integer> chapterNos, Integer chapterNo) {
        if (!chapterNos.contains(chapterNo)) {
            chapterNos.add(chapterNo);
        }
    }
}
