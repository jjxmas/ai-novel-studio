package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.common.util.JsonUtils;
import com.jjxmas.ainovelstudio.converter.ChapterConverter;
import com.jjxmas.ainovelstudio.converter.OutlineConverter;
import com.jjxmas.ainovelstudio.pojo.dto.ChapterResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.service.GenerationJobService;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineGenerateRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineResponse;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineRewriteRequest;
import com.jjxmas.ainovelstudio.pojo.dto.OutlineUpdateRequest;
import com.jjxmas.ainovelstudio.pojo.entity.Outline;
import com.jjxmas.ainovelstudio.pojo.entity.StoryArc;
import com.jjxmas.ainovelstudio.pojo.entity.Volume;
import com.jjxmas.ainovelstudio.mapper.OutlineMapper;
import com.jjxmas.ainovelstudio.mapper.StoryArcMapper;
import com.jjxmas.ainovelstudio.mapper.VolumeMapper;
import com.jjxmas.ainovelstudio.service.OutlineService;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.entity.SettingLibrary;
import com.jjxmas.ainovelstudio.mapper.SettingLibraryMapper;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
/**
 * 大纲服务实现，负责全局大纲、卷、剧情弧和章节大纲的生成与维护。
 */
public class OutlineServiceImpl extends ServiceImpl<OutlineMapper, Outline> implements OutlineService {

    private final ProjectMapper projectMapper;
    private final SettingLibraryMapper settingLibraryMapper;
    private final VolumeMapper volumeMapper;
    private final StoryArcMapper storyArcMapper;
    private final ChapterMapper chapterMapper;
    private final GenerationJobService generationJobService;
    private final VersionService versionService;
    private final OutlineConverter outlineConverter;
    private final ChapterConverter chapterConverter;
    private final CacheManager cacheManager;

    /**
     * 注入大纲流程所需的 Mapper、任务服务和版本服务。
     */
    public OutlineServiceImpl(
            ProjectMapper projectMapper,
            SettingLibraryMapper settingLibraryMapper,
            VolumeMapper volumeMapper,
            StoryArcMapper storyArcMapper,
            ChapterMapper chapterMapper,
            GenerationJobService generationJobService,
            VersionService versionService,
            OutlineConverter outlineConverter,
            ChapterConverter chapterConverter,
            CacheManager cacheManager) {
        this.projectMapper = projectMapper;
        this.settingLibraryMapper = settingLibraryMapper;
        this.volumeMapper = volumeMapper;
        this.storyArcMapper = storyArcMapper;
        this.chapterMapper = chapterMapper;
        this.generationJobService = generationJobService;
        this.versionService = versionService;
        this.outlineConverter = outlineConverter;
        this.chapterConverter = chapterConverter;
        this.cacheManager = cacheManager;
    }

    /**
     * 生成或覆盖项目全局大纲，并记录任务和版本。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#request.projectId")
    public OutlineResponse generateOutline(OutlineGenerateRequest request) {
        if (!"global".equals(request.getOutlineLevel())) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "第二版只开放全局大纲生成入口");
        }
        requireConfirmedSetting(request.getProjectId());
        String sourceContent = request.getSourceContent() == null || request.getSourceContent().isBlank()
                ? "已确认设定库"
                : request.getSourceContent();
        Outline outline = findByProjectId(request.getProjectId());
        if (outline == null) {
            outline = new Outline().setProjectId(request.getProjectId());
        }
        outline.setTitle("全局大纲")
                .setContent("""
                        【主线目标】
                        主角围绕《%s》展开长期成长，先解决生存问题，再进入更大的势力冲突。

                        【长篇结构】
                        前期：确立目标、规则和主要关系。
                        中期：扩大地图，引入更强对手和长期伏笔。
                        后期：回收关键伏笔，完成主线冲突。

                        【章节节奏】
                        每章保留明确目标、阻碍和推进结果，避免只有设定说明。
                        """.formatted(sourceContent))
                .setConfirmedAt(null);
        saveOrUpdate(outline);

        Map<String, Object> snapshot = outlineSnapshot(outline);
        Long jobId = generationJobService.recordFinishedJob(
                request.getProjectId(),
                "global_outline_generation",
                "global_outline",
                outline.getId(),
                request.getModelConfigId(),
                Map.of("sourceContent", sourceContent),
                snapshot);
        versionService.recordVersion(request.getProjectId(), "global_outline", outline.getId(), snapshot, "ai_generate", "mock 生成全局大纲", request.getModelConfigId(), jobId);
        return toResponse(outline);
    }

    /**
     * 查询指定项目的全局大纲。
     */
    @Override
    @Cacheable(value = "globalOutlines", key = "#projectId")
    public OutlineResponse getGlobalOutline(Long projectId) {
        requireProject(projectId);
        Outline outline = findByProjectId(projectId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return toResponse(outline);
    }

    /**
     * 按项目 ID 更新全局大纲并清除确认状态。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public void updateGlobalOutline(Long projectId, OutlineUpdateRequest request) {
        Outline outline = requireOutline(projectId);
        outline.setTitle(request.getTitle()).setContent(request.getContent()).setConfirmedAt(null);
        updateById(outline);
        evictOutlineCaches(projectId);
        versionService.recordVersion(
                projectId,
                "global_outline",
                outline.getId(),
                outlineSnapshot(outline),
                "user_edit",
                request.getChangeNote() == null ? "用户直接修改全局大纲" : request.getChangeNote(),
                null,
                null);
    }

    /**
     * 按大纲 ID 更新全局大纲。
     */
    @Override
    @Transactional
    public void updateGlobalOutlineById(Long outlineId, OutlineUpdateRequest request) {
        Outline outline = requireOutlineById(outlineId);
        updateGlobalOutline(outline.getProjectId(), request);
        evictOutlineCaches(outline.getProjectId());
    }

    /**
     * 根据用户指令重写全局大纲并记录版本。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public OutlineResponse rewriteGlobalOutline(Long projectId, OutlineRewriteRequest request) {
        Outline outline = requireOutline(projectId);
        outline.setContent(outline.getContent() + "\n\n【根据修改意见调整】\n" + request.getInstruction())
                .setConfirmedAt(null);
        updateById(outline);
        Map<String, Object> snapshot = outlineSnapshot(outline);
        Long jobId = generationJobService.recordFinishedJob(
                projectId,
                "global_outline_rewrite",
                "global_outline",
                outline.getId(),
                request.getModelConfigId(),
                Map.of("instruction", request.getInstruction()),
                snapshot);
        versionService.recordVersion(projectId, "global_outline", outline.getId(), snapshot, "ai_rewrite", "根据用户修改意见重生成全局大纲", request.getModelConfigId(), jobId);
        return toResponse(outline);
    }

    /**
     * 确认全局大纲并推进项目状态。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public void confirmGlobalOutline(Long projectId) {
        Outline outline = requireOutline(projectId);
        outline.setConfirmedAt(LocalDateTime.now());
        updateById(outline);
        evictOutlineCaches(projectId);
        Project project = requireProject(projectId);
        project.setStatus("outline_confirmed");
        projectMapper.updateById(project);
        versionService.recordVersion(
                projectId,
                "global_outline",
                outline.getId(),
                outlineSnapshot(outline),
                "confirm",
                "确认全局大纲",
                null,
                null);
    }

    /**
     * 按大纲 ID 确认全局大纲。
     */
    @Override
    @Transactional
    public void confirmGlobalOutlineById(Long outlineId) {
        Outline outline = requireOutlineById(outlineId);
        confirmGlobalOutline(outline.getProjectId());
        evictOutlineCaches(outline.getProjectId());
    }

    /**
     * 基于已确认全局大纲生成卷、剧情弧和章节大纲。
     */
    @Override
    @Transactional
    @CacheEvict(value = {"globalOutlines", "chapterContextOutlines"}, key = "#projectId")
    public List<ChapterResponse> generateChapterOutlines(Long projectId) {
        Outline outline = requireConfirmedOutline(projectId);
        chapterMapper.delete(new LambdaQueryWrapper<Chapter>().eq(Chapter::getProjectId, projectId));
        storyArcMapper.delete(new LambdaQueryWrapper<StoryArc>().eq(StoryArc::getProjectId, projectId));
        volumeMapper.delete(new LambdaQueryWrapper<Volume>().eq(Volume::getProjectId, projectId));

        return java.util.stream.IntStream.rangeClosed(1, 3)
                .boxed()
                .flatMap(volumeNo -> createMockVolumeChapters(projectId, outline, volumeNo).stream())
                .toList();
    }

    /**
     * 为指定卷生成模拟卷、剧情弧和章节大纲。
     */
    private List<ChapterResponse> createMockVolumeChapters(Long projectId, Outline outline, int volumeNo) {
        Volume volume = new Volume()
                .setProjectId(projectId)
                .setVolumeNo(volumeNo)
                .setTitle("第" + volumeNo + "卷")
                .setSummary("围绕全局大纲推进第" + volumeNo + " 个阶段。")
                .setGoal("完成阶段目标并留下下一卷牵引。")
                .setEstimatedWordCount(120_000);
        volumeMapper.insert(volume);

        StoryArc arc = new StoryArc()
                .setProjectId(projectId)
                .setVolumeId(volume.getId())
                .setArcNo(1)
                .setTitle("第" + volumeNo + "卷核心剧情单元")
                .setSummary("建立冲突、升级阻碍、完成阶段性转折。")
                .setGoal("让主角获得新的认知或资源。")
                .setConflict("主角当前能力与外部压力不匹配。")
                .setEstimatedChapterCount(2);
        storyArcMapper.insert(arc);

        return java.util.stream.IntStream.rangeClosed(1, 2)
                .mapToObj(index -> createMockChapter(projectId, volume, arc, (volumeNo - 1) * 2 + index, outline))
                .toList();
    }

    /**
     * 创建单个模拟章节大纲并记录版本。
     */
    private ChapterResponse createMockChapter(Long projectId, Volume volume, StoryArc arc, int chapterNo, Outline outline) {
        Chapter chapter = new Chapter()
                .setProjectId(projectId)
                .setVolumeId(volume.getId())
                .setStoryArcId(arc.getId())
                .setChapterNo(chapterNo)
                .setTitle("第" + chapterNo + "章 阶段推进")
                .setOutline("本章承接《" + outline.getTitle() + "》，围绕一个明确目标、一个阻碍和一个推进结果展开。")
                .setScenePlan(JsonUtils.toJson(List.of("开场目标", "冲突升级", "结尾钩子")))
                .setStatus("outline_pending");
        chapterMapper.insert(chapter);
        versionService.recordVersion(
                projectId,
                "chapter_outline",
                chapter.getId(),
                Map.of("title", chapter.getTitle(), "outline", chapter.getOutline(), "chapterNo", chapter.getChapterNo()),
                "ai_generate",
                "mock 生成章节大纲",
                null,
                null);
        return chapterConverter.toResponse(chapter);
    }

    /**
     * 获取项目实体，不存在时抛出业务异常。
     */
    private Project requireProject(Long projectId) {
        Project project = projectMapper.selectById(projectId);
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        return project;
    }

    /**
     * 校验项目是否已有确认后的设定库。
     */
    private void requireConfirmedSetting(Long projectId) {
        requireProject(projectId);
        SettingLibrary setting = settingLibraryMapper.selectOne(new LambdaQueryWrapper<SettingLibrary>()
                .eq(SettingLibrary::getProjectId, projectId)
                .isNotNull(SettingLibrary::getConfirmedAt)
                .last("LIMIT 1"));
        if (setting == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认设定库，再生成全局大纲");
        }
    }

    /**
     * 获取并校验项目已确认的全局大纲。
     */
    private Outline requireConfirmedOutline(Long projectId) {
        Outline outline = requireOutline(projectId);
        if (outline.getConfirmedAt() == null) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先确认全局大纲，再生成章节大纲");
        }
        return outline;
    }

    /**
     * 获取项目全局大纲，不存在时抛出业务异常。
     */
    private Outline requireOutline(Long projectId) {
        requireProject(projectId);
        Outline outline = findByProjectId(projectId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return outline;
    }

    /**
     * 按大纲 ID 获取全局大纲，不存在时抛出业务异常。
     */
    private Outline requireOutlineById(Long outlineId) {
        Outline outline = getById(outlineId);
        if (outline == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "全局大纲不存在");
        }
        return outline;
    }

    /**
     * 查询指定项目的第一条全局大纲。
     */
    private Outline findByProjectId(Long projectId) {
        return getOne(new LambdaQueryWrapper<Outline>().eq(Outline::getProjectId, projectId).last("LIMIT 1"));
    }

    private void evictOutlineCaches(Long projectId) {
        for (String cacheName : List.of("globalOutlines", "chapterContextOutlines")) {
            Cache cache = cacheManager.getCache(cacheName);
            if (cache != null) {
                cache.evict(projectId);
            }
        }
    }

    /**
     * 将大纲实体和卷信息转换为响应对象。
     */
    private OutlineResponse toResponse(Outline outline) {
        OutlineResponse response = outlineConverter.toResponse(outline);
        response.setVolumes(outlineConverter.toVolumeResponseList(volumeMapper.selectList(new LambdaQueryWrapper<Volume>()
                .eq(Volume::getProjectId, outline.getProjectId())
                .orderByAsc(Volume::getVolumeNo))));
        return response;
    }

    /**
     * 构造全局大纲版本快照内容。
     */
    private Map<String, Object> outlineSnapshot(Outline outline) {
        return Map.of(
                "title", outline.getTitle(),
                "content", outline.getContent(),
                "confirmed", outline.getConfirmedAt() != null);
    }
}
