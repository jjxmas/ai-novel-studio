package com.jjxmas.ainovelstudio.module.export.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.module.chapter.entity.Chapter;
import com.jjxmas.ainovelstudio.module.chapter.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.module.export.dto.ExportRequest;
import com.jjxmas.ainovelstudio.module.export.dto.ExportResponse;
import com.jjxmas.ainovelstudio.module.export.service.ExportService;
import com.jjxmas.ainovelstudio.module.project.entity.Project;
import com.jjxmas.ainovelstudio.module.project.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.module.version.service.VersionService;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ExportServiceImpl implements ExportService {

    private final ProjectMapper projectMapper;
    private final ChapterMapper chapterMapper;
    private final VersionService versionService;

    public ExportServiceImpl(ProjectMapper projectMapper, ChapterMapper chapterMapper, VersionService versionService) {
        this.projectMapper = projectMapper;
        this.chapterMapper = chapterMapper;
        this.versionService = versionService;
    }

    @Override
    @Transactional
    public ExportResponse exportProject(ExportRequest request) {
        Project project = projectMapper.selectById(request.getProjectId());
        if (project == null) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "作品不存在");
        }
        String format = request.getFormat().toLowerCase();
        if (!"markdown".equals(format) && !"md".equals(format) && !"txt".equals(format)) {
            throw new BusinessException(ErrorCode.EXPORT_FAILED, "第二版只支持 Markdown/TXT 导出");
        }

        List<Chapter> chapters = chapterMapper.selectList(new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, request.getProjectId())
                .orderByAsc(Chapter::getChapterNo));
        List<Chapter> exportableChapters = chapters.stream()
                .filter(chapter -> chapter.getContent() != null && !chapter.getContent().isBlank())
                .toList();
        if (exportableChapters.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先生成章节正文，再导出作品");
        }
        if (exportableChapters.stream().noneMatch(chapter -> chapter.getCheckedAt() != null)) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "请先完成至少一次章节检查，再导出作品");
        }

        boolean markdown = "markdown".equals(format) || "md".equals(format);
        String content = markdown ? buildMarkdown(project, exportableChapters) : buildTxt(project, exportableChapters);
        String extension = markdown ? "md" : "txt";
        String fileName = project.getTitle() + "." + extension;
        project.setStatus("exported");
        projectMapper.updateById(project);
        versionService.recordVersion(
                project.getId(),
                "export",
                project.getId(),
                Map.of("fileName", fileName, "format", extension, "scope", request.getScope(), "content", content),
                "export",
                "导出 " + extension.toUpperCase() + " 内容快照",
                null,
                null);
        return ExportResponse.builder()
                .fileName(fileName)
                .filePath("/exports/" + project.getId() + "/" + fileName)
                .format(extension)
                .scope(request.getScope())
                .content(content)
                .build();
    }

    private String buildMarkdown(Project project, List<Chapter> chapters) {
        StringBuilder builder = new StringBuilder("# ").append(project.getTitle()).append("\n\n");
        for (Chapter chapter : chapters) {
            builder.append("## 第").append(chapter.getChapterNo()).append("章 ")
                    .append(chapter.getTitle()).append("\n\n")
                    .append(chapter.getContent() == null ? chapter.getOutline() : chapter.getContent())
                    .append("\n\n");
        }
        return builder.toString();
    }

    private String buildTxt(Project project, List<Chapter> chapters) {
        StringBuilder builder = new StringBuilder(project.getTitle()).append("\n\n");
        for (Chapter chapter : chapters) {
            builder.append("第").append(chapter.getChapterNo()).append("章 ")
                    .append(chapter.getTitle()).append("\n\n")
                    .append(chapter.getContent() == null ? chapter.getOutline() : chapter.getContent())
                    .append("\n\n");
        }
        return builder.toString();
    }
}
