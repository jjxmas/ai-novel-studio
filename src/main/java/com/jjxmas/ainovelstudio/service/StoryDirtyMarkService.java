package com.jjxmas.ainovelstudio.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.StoryDirtyMarkMapper;
import com.jjxmas.ainovelstudio.pojo.dto.StoryDirtyMarkResponse;
import com.jjxmas.ainovelstudio.pojo.dto.StoryDirtyMarkSnapshotResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.StoryDirtyMark;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class StoryDirtyMarkService {

    private static final String STATUS_ACTIVE = "active";
    private static final String STATUS_RESOLVED = "resolved";
    private static final String DIRTY_SCOPE_CHAPTER_POSTPROCESS = "chapter_postprocess";
    private static final String SOURCE_TYPE_CHAPTER = "chapter";
    private static final String DEFAULT_REASON_TYPE = "chapter_content_changed";
    private static final String DEFAULT_REASON_NOTE =
            "\u7ae0\u8282\u6b63\u6587\u53d8\u66f4\u540e\uff0c\u4e0b\u6e38\u7ae0\u8282\u4e8b\u5b9e\u5c42\u53ef\u80fd\u5931\u6548";
    private static final String DEFAULT_WARNING_REASON =
            "\u4e0a\u6e38\u7ae0\u8282\u53d8\u66f4\u540e\uff0c\u4e8b\u5b9e\u5c42\u5f85\u56de\u7b97";
    private static final String WARNING_PREFIX =
            "\u7b2c";
    private static final String WARNING_MIDDLE =
            "\u7ae0\u53d1\u751f\u53d8\u66f4\uff0c\u4e14\u81ea\u7b2c";
    private static final String WARNING_SUFFIX =
            "\u7ae0\u8d77\u7684\u4e8b\u5b9e\u94fe\u5c1a\u672a\u56de\u7b97\uff1a";
    private static final String WARNING_FALLBACK =
            "\u5f53\u524d\u7ae0\u8282\u547d\u4e2d\u4e86\u5f85\u56de\u7b97\u7684\u4e8b\u5b9e\u94fe\u810f\u533a\u95f4\uff1a";

    private final StoryDirtyMarkMapper storyDirtyMarkMapper;
    private final ChapterMapper chapterMapper;

    public StoryDirtyMarkService(StoryDirtyMarkMapper storyDirtyMarkMapper, ChapterMapper chapterMapper) {
        this.storyDirtyMarkMapper = storyDirtyMarkMapper;
        this.chapterMapper = chapterMapper;
    }

    public void markDownstreamDirty(Chapter chapter, String reasonType, String reasonNote) {
        if (chapter == null || chapter.getProjectId() == null || chapter.getId() == null || chapter.getChapterNo() == null) {
            return;
        }

        Chapter downstreamChapter = chapterMapper.selectOne(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, chapter.getProjectId())
                .gt(Chapter::getChapterNo, chapter.getChapterNo())
                .orderByAsc(Chapter::getChapterNo)
                .last("limit 1"));
        if (downstreamChapter == null || downstreamChapter.getChapterNo() == null) {
            return;
        }

        StoryDirtyMark existing = storyDirtyMarkMapper.selectOne(new LambdaQueryWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, chapter.getProjectId())
                .eq(StoryDirtyMark::getSourceType, SOURCE_TYPE_CHAPTER)
                .eq(StoryDirtyMark::getSourceId, chapter.getId())
                .eq(StoryDirtyMark::getDirtyScope, DIRTY_SCOPE_CHAPTER_POSTPROCESS)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .orderByDesc(StoryDirtyMark::getId)
                .last("limit 1"));

        StoryDirtyMark dirtyMark = existing == null ? new StoryDirtyMark() : existing;
        dirtyMark.setProjectId(chapter.getProjectId())
                .setSourceType(SOURCE_TYPE_CHAPTER)
                .setSourceId(chapter.getId())
                .setSourceChapterId(chapter.getId())
                .setSourceChapterNo(chapter.getChapterNo())
                .setDirtyFromChapterNo(downstreamChapter.getChapterNo())
                .setDirtyScope(DIRTY_SCOPE_CHAPTER_POSTPROCESS)
                .setReasonType(blankToDefault(reasonType, DEFAULT_REASON_TYPE))
                .setReasonNote(blankToDefault(reasonNote, DEFAULT_REASON_NOTE))
                .setStatus(STATUS_ACTIVE)
                .setResolvedAt(null);

        if (existing == null) {
            storyDirtyMarkMapper.insert(dirtyMark);
            return;
        }
        storyDirtyMarkMapper.updateById(dirtyMark);
    }

    public List<String> activeWarningsForChapter(Chapter chapter) {
        if (chapter == null || chapter.getProjectId() == null || chapter.getChapterNo() == null) {
            return List.of();
        }

        List<StoryDirtyMark> activeMarks = activeMarksFromChapter(chapter.getProjectId(), chapter.getChapterNo());
        if (activeMarks.isEmpty()) {
            return List.of();
        }

        List<String> warnings = new ArrayList<>(activeMarks.size());
        for (StoryDirtyMark activeMark : activeMarks) {
            Integer sourceChapterNo = activeMark.getSourceChapterNo();
            Integer dirtyFromChapterNo = activeMark.getDirtyFromChapterNo();
            String normalizedReason = blankToDefault(activeMark.getReasonNote(), DEFAULT_WARNING_REASON);
            if (sourceChapterNo != null && dirtyFromChapterNo != null) {
                warnings.add(WARNING_PREFIX + sourceChapterNo + WARNING_MIDDLE + dirtyFromChapterNo
                        + WARNING_SUFFIX + normalizedReason);
            } else {
                warnings.add(WARNING_FALLBACK + normalizedReason);
            }
        }
        return warnings;
    }

    public List<StoryDirtyMark> activeMarksFromChapter(Long projectId, Integer chapterNo) {
        if (projectId == null || chapterNo == null) {
            return List.of();
        }
        return storyDirtyMarkMapper.selectList(new LambdaQueryWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, projectId)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .le(StoryDirtyMark::getDirtyFromChapterNo, chapterNo)
                .orderByAsc(StoryDirtyMark::getDirtyFromChapterNo)
                .orderByAsc(StoryDirtyMark::getId));
    }

    public Integer earliestActiveDirtyChapterNo(Long projectId) {
        if (projectId == null) {
            return null;
        }

        StoryDirtyMark earliest = storyDirtyMarkMapper.selectOne(new LambdaQueryWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, projectId)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .orderByAsc(StoryDirtyMark::getDirtyFromChapterNo)
                .orderByAsc(StoryDirtyMark::getId)
                .last("limit 1"));
        return earliest == null ? null : earliest.getDirtyFromChapterNo();
    }

    public List<StoryDirtyMark> activeMarksAtOrAfter(Long projectId, Integer startChapterNo) {
        if (projectId == null || startChapterNo == null) {
            return List.of();
        }
        return storyDirtyMarkMapper.selectList(new LambdaQueryWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, projectId)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .ge(StoryDirtyMark::getDirtyFromChapterNo, startChapterNo)
                .orderByAsc(StoryDirtyMark::getDirtyFromChapterNo)
                .orderByAsc(StoryDirtyMark::getId));
    }

    public StoryDirtyMarkSnapshotResponse activeSnapshot(Long projectId, Integer chapterNo) {
        List<StoryDirtyMark> activeMarks = chapterNo == null
                ? listActiveMarks(projectId)
                : activeMarksFromChapter(projectId, chapterNo);
        Integer earliestDirtyChapterNo = activeMarks.stream()
                .map(StoryDirtyMark::getDirtyFromChapterNo)
                .filter(chapter -> chapter != null)
                .min(Integer::compareTo)
                .orElse(null);
        return StoryDirtyMarkSnapshotResponse.builder()
                .projectId(projectId)
                .queryChapterNo(chapterNo)
                .activeDirtyMarkCount(activeMarks.size())
                .earliestDirtyChapterNo(earliestDirtyChapterNo)
                .activeDirtyMarks(activeMarks.stream().map(this::toResponse).toList())
                .build();
    }

    public List<StoryDirtyMark> listActiveMarks(Long projectId) {
        if (projectId == null) {
            return List.of();
        }
        return storyDirtyMarkMapper.selectList(new LambdaQueryWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, projectId)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .orderByAsc(StoryDirtyMark::getDirtyFromChapterNo)
                .orderByAsc(StoryDirtyMark::getId));
    }

    public int resolveActiveMarksFromChapter(Long projectId, Integer startChapterNo) {
        if (projectId == null || startChapterNo == null) {
            return 0;
        }
        return storyDirtyMarkMapper.update(null, new LambdaUpdateWrapper<StoryDirtyMark>()
                .eq(StoryDirtyMark::getProjectId, projectId)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .ge(StoryDirtyMark::getDirtyFromChapterNo, startChapterNo)
                .set(StoryDirtyMark::getStatus, STATUS_RESOLVED)
                .set(StoryDirtyMark::getResolvedAt, LocalDateTime.now()));
    }

    public int resolveActiveMarksByIds(List<Long> dirtyMarkIds) {
        if (dirtyMarkIds == null || dirtyMarkIds.isEmpty()) {
            return 0;
        }
        return storyDirtyMarkMapper.update(null, new LambdaUpdateWrapper<StoryDirtyMark>()
                .in(StoryDirtyMark::getId, dirtyMarkIds)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE)
                .set(StoryDirtyMark::getStatus, STATUS_RESOLVED)
                .set(StoryDirtyMark::getResolvedAt, LocalDateTime.now()));
    }

    public int countActiveMarksByIds(List<Long> dirtyMarkIds) {
        if (dirtyMarkIds == null || dirtyMarkIds.isEmpty()) {
            return 0;
        }
        Long count = storyDirtyMarkMapper.selectCount(new LambdaQueryWrapper<StoryDirtyMark>()
                .in(StoryDirtyMark::getId, dirtyMarkIds)
                .eq(StoryDirtyMark::getStatus, STATUS_ACTIVE));
        return count == null ? 0 : count.intValue();
    }

    private String blankToDefault(String value, String defaultValue) {
        return value == null || value.isBlank() ? defaultValue : value;
    }

    private StoryDirtyMarkResponse toResponse(StoryDirtyMark dirtyMark) {
        return StoryDirtyMarkResponse.builder()
                .id(dirtyMark.getId())
                .projectId(dirtyMark.getProjectId())
                .sourceType(dirtyMark.getSourceType())
                .sourceId(dirtyMark.getSourceId())
                .sourceChapterId(dirtyMark.getSourceChapterId())
                .sourceChapterNo(dirtyMark.getSourceChapterNo())
                .dirtyFromChapterNo(dirtyMark.getDirtyFromChapterNo())
                .dirtyScope(dirtyMark.getDirtyScope())
                .reasonType(dirtyMark.getReasonType())
                .reasonNote(dirtyMark.getReasonNote())
                .status(dirtyMark.getStatus())
                .createdAt(dirtyMark.getCreatedAt())
                .resolvedAt(dirtyMark.getResolvedAt())
                .build();
    }
}
