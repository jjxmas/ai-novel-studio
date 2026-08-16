package com.jjxmas.ainovelstudio.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.jjxmas.ainovelstudio.common.exception.BusinessException;
import com.jjxmas.ainovelstudio.common.exception.ErrorCode;
import com.jjxmas.ainovelstudio.mapper.ChapterMapper;
import com.jjxmas.ainovelstudio.mapper.ProjectMapper;
import com.jjxmas.ainovelstudio.pojo.dto.ExportRequest;
import com.jjxmas.ainovelstudio.pojo.dto.ExportResponse;
import com.jjxmas.ainovelstudio.pojo.entity.Chapter;
import com.jjxmas.ainovelstudio.pojo.entity.Project;
import com.jjxmas.ainovelstudio.service.ExportService;
import com.jjxmas.ainovelstudio.service.VersionService;
import java.time.LocalDateTime;
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
            throw new BusinessException(ErrorCode.NOT_FOUND, "PROJECT_NOT_FOUND");
        }
        String extension = normalizeFormat(request.getFormat());
        String scope = normalizeScope(request.getScope());
        List<Chapter> chapters = exportableChapters(request, scope);
        if (chapters.isEmpty()) {
            throw new BusinessException(ErrorCode.WORKFLOW_GATE_NOT_MET, "NO_EXPORTABLE_CHAPTERS");
        }

        String content = "md".equals(extension) ? buildMarkdown(project, chapters) : buildTxt(project, chapters);
        String fileName = sanitizeFileName(project.getTitle()) + "." + extension;

        project.setWorkflowStage("export").setLastExportedAt(LocalDateTime.now());
        projectMapper.updateById(project);
        versionService.recordVersion(
                project.getId(),
                "export",
                project.getId(),
                Map.of("fileName", fileName, "format", extension, "scope", scope, "content", content),
                "export",
                "Export " + extension.toUpperCase() + " content snapshot",
                null,
                null);

        return ExportResponse.builder()
                .fileName(fileName)
                .filePath("/exports/download")
                .format(extension)
                .scope(scope)
                .content(content)
                .build();
    }

    private List<Chapter> exportableChapters(ExportRequest request, String scope) {
        LambdaQueryWrapper<Chapter> query = new LambdaQueryWrapper<Chapter>()
                .eq(Chapter::getProjectId, request.getProjectId())
                .isNotNull(Chapter::getContent)
                .ne(Chapter::getContent, "")
                .orderByAsc(Chapter::getChapterNo);
        if ("chapter".equals(scope)) {
            query.eq(Chapter::getId, requireScopeEntityId(request));
        } else if ("volume".equals(scope)) {
            query.eq(Chapter::getVolumeId, requireScopeEntityId(request));
        }
        return chapterMapper.selectList(query);
    }

    private String normalizeFormat(String format) {
        String value = format == null ? "" : format.trim().toLowerCase();
        if ("markdown".equals(value) || "md".equals(value)) {
            return "md";
        }
        if ("txt".equals(value)) {
            return "txt";
        }
        throw new BusinessException(ErrorCode.EXPORT_FAILED, "UNSUPPORTED_EXPORT_FORMAT");
    }

    private String normalizeScope(String scope) {
        String value = scope == null ? "" : scope.trim().toLowerCase();
        return switch (value) {
            case "chapter" -> "chapter";
            case "volume" -> "volume";
            case "full_project", "full-project", "full" -> "full_project";
            default -> throw new BusinessException(ErrorCode.PARAMETER_ERROR, "UNSUPPORTED_EXPORT_SCOPE");
        };
    }

    private Long requireScopeEntityId(ExportRequest request) {
        if (request.getScopeEntityId() == null) {
            throw new BusinessException(ErrorCode.PARAMETER_ERROR, "SCOPE_ENTITY_ID_REQUIRED");
        }
        return request.getScopeEntityId();
    }

    private String buildMarkdown(Project project, List<Chapter> chapters) {
        StringBuilder builder = new StringBuilder("# ").append(blankToTitle(project.getTitle())).append("\n\n");
        for (Chapter chapter : chapters) {
            builder.append("## Chapter ")
                    .append(chapter.getChapterNo() == null ? "" : chapter.getChapterNo())
                    .append(" ")
                    .append(blankToEmpty(chapter.getTitle()))
                    .append("\n\n")
                    .append(blankToEmpty(chapter.getContent()))
                    .append("\n\n");
        }
        return builder.toString();
    }

    private String buildTxt(Project project, List<Chapter> chapters) {
        StringBuilder builder = new StringBuilder(blankToTitle(project.getTitle())).append("\n\n");
        for (Chapter chapter : chapters) {
            builder.append("Chapter ")
                    .append(chapter.getChapterNo() == null ? "" : chapter.getChapterNo())
                    .append(" ")
                    .append(blankToEmpty(chapter.getTitle()))
                    .append("\n\n")
                    .append(blankToEmpty(chapter.getContent()))
                    .append("\n\n");
        }
        return builder.toString();
    }

    private String sanitizeFileName(String value) {
        String title = blankToTitle(value).replaceAll("[\\\\/:*?\"<>|]", "_").trim();
        return title.isBlank() ? "novel" : title;
    }

    private String blankToTitle(String value) {
        return value == null || value.isBlank() ? "novel" : value;
    }

    private String blankToEmpty(String value) {
        return value == null ? "" : value;
    }
}
