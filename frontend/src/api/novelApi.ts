import { del, patch, post, request } from './client';
import type {
  Chapter,
  ChapterSummary,
  CheckResult,
  ContentVersion,
  ExportRecord,
  GlobalOutline,
  Idea,
  ModelConfig,
  ModelConfigRequest,
  Project,
  ProjectCreateRequest,
  ProjectMemory,
  SettingLibrary,
  StoryMemory,
  WorkflowStage,
} from './types';

const nowText = () => new Date().toLocaleString('zh-CN', { hour12: false });

function mapStage(status?: string): WorkflowStage {
  if (status === 'idea_selected') {
    return 'setting';
  }
  if (status === 'setting_confirmed') {
    return 'outline';
  }
  if (status === 'outline_confirmed' || status === 'writing') {
    return 'chapter';
  }
  if (status === 'exported') {
    return 'export';
  }
  return 'idea';
}

function mapProject(data: any): Project {
  return {
    id: data.id,
    title: data.title,
    genres: data.genres ?? [],
    projectBrief: data.projectBrief ?? '',
    targetWordCountMin: data.targetWordCountMin ?? 0,
    targetWordCountMax: data.targetWordCountMax ?? 0,
    targetChapterWordCount: data.targetChapterWordCount ?? 3000,
    platformTarget: data.platformTarget ?? '通用',
    stylePreference: data.stylePreference ?? '',
    stage: data.stage ?? mapStage(data.status),
    updatedAt: data.updatedAt ?? data.createdAt ?? nowText(),
  };
}

function mapIdea(data: any, projectId?: number): Idea {
  const sellingPoints = data.sellingPoints ?? [];
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    title: data.title,
    sellingPoint: data.sellingPoint ?? sellingPoints.join('；') ?? '',
    worldview: data.worldview ?? '',
    mainConflict: data.mainConflict ?? '',
    estimatedWords: data.estimatedWords ?? `${data.estimatedWordCount ?? 0} 字`,
    score: data.score ?? data.longFormPotentialScore ?? 0,
    selected: data.selected ?? data.status === 'selected',
    content: data.content ?? data.summary ?? '',
  };
}

function mapSettingLibrary(data: any): SettingLibrary {
  return {
    id: data.id,
    projectId: data.projectId,
    content: data.content ?? data.summary ?? '',
    confirmed: Boolean(data.confirmed),
  };
}

function mapGlobalOutline(data: any): GlobalOutline {
  return {
    id: data.id,
    projectId: data.projectId ?? 0,
    content: data.content ?? '',
    confirmed: Boolean(data.confirmed),
  };
}

function mapChapter(data: any, projectId?: number): Chapter {
  const status = data.status === 'edited'
    ? 'edited'
    : data.status === 'drafted' || data.status === 'content_ready'
      ? 'content_ready'
      : 'outline_ready';
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    chapterNo: data.chapterNo,
    title: data.title,
    outline: data.outline ?? '',
    content: data.content ?? '',
    status,
  };
}

function mapChapterSummary(data: any): ChapterSummary {
  return {
    id: data.id,
    chapterId: data.chapterId,
    chapterNo: data.chapterNo,
    summary: data.summary ?? '',
  };
}

function mapStoryMemory(data: any): StoryMemory {
  return {
    id: data.id,
    memoryType: data.memoryType,
    memoryKey: data.memoryKey,
    sequenceNo: data.sequenceNo ?? 0,
    startChapterNo: data.startChapterNo,
    endChapterNo: data.endChapterNo,
    content: data.content ?? '',
    status: data.status ?? 'active',
    current: Boolean(data.current ?? data.isCurrent ?? true),
  };
}

function mapProjectMemory(data: any): ProjectMemory {
  return {
    projectId: data.projectId,
    globalMemory: data.globalMemory ? mapStoryMemory(data.globalMemory) : null,
    highMemories: (data.highMemories ?? []).map(mapStoryMemory),
    middleMemories: (data.middleMemories ?? []).map(mapStoryMemory),
    recentWindows: (data.recentWindows ?? []).map(mapStoryMemory),
    recentChapterSummaries: (data.recentChapterSummaries ?? []).map(mapChapterSummary),
  };
}

function mapExport(data: any, projectId: number, format: ExportRecord['format'], scope: string): ExportRecord {
  return {
    id: data.id ?? Date.now(),
    projectId: data.projectId ?? projectId,
    format: data.format ?? format,
    scope: data.scope ?? scope,
    fileName: data.fileName,
    status: data.status ?? 'created',
  };
}

export const novelApi = {
  listProjects: async () => (await request<any[]>('/projects')).map(mapProject),
  createProject: async (payload: ProjectCreateRequest) => mapProject(await post('/projects', payload)),

  listModelConfigs: () => request<ModelConfig[]>('/model-configs'),
  createModelConfig: (payload: ModelConfigRequest) => post<ModelConfig>('/model-configs', payload),
  updateModelConfig: (id: number, payload: ModelConfigRequest) => patch<ModelConfig>(`/model-configs/${id}`, payload),
  setDefaultModel: (id: number) => post<ModelConfig>(`/model-configs/${id}/default`),
  disableModelConfig: (id: number) => del<ModelConfig>(`/model-configs/${id}`),

  generateIdeas: (projectId: number, suggestion?: string, ideaCount = 3) =>
    post<any[]>(`/projects/${projectId}/ideas/generate`, {
      projectId,
      briefDescription: suggestion || '根据作品简介生成适合长篇连载的创意方案',
      ideaCount,
    }).then((items) => items.map((item) => mapIdea(item, projectId))),
  listIdeas: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/ideas`).then((items) => items.map((item) => mapIdea(item, projectId))),
  updateIdea: (idea: Idea, changeNote = '用户直接修改创意内容') =>
    patch<any>(`/ideas/${idea.id}`, {
      title: idea.title,
      sellingPoints: [idea.sellingPoint],
      worldview: idea.worldview,
      mainConflict: idea.mainConflict,
      estimatedWordCount: Number.parseInt(idea.estimatedWords, 10) || 0,
      summary: idea.content,
      changeNote,
    }).then((item) => mapIdea(item, idea.projectId)),
  rewriteIdea: (ideaId: number, suggestion: string) =>
    post<any>(`/ideas/${ideaId}/rewrite`, { instruction: suggestion }).then((item) => mapIdea(item)),
  selectIdea: (ideaId: number) => post<any>(`/ideas/${ideaId}/select`).then((item) => mapIdea(item)),
  deleteIdea: (ideaId: number) => del<void>(`/ideas/${ideaId}`),

  generateSettingLibrary: (projectId: number) =>
    post<any>(`/projects/${projectId}/setting-library/generate`, { projectId }).then(mapSettingLibrary),
  getSettingLibrary: (projectId: number) =>
    request<any>(`/projects/${projectId}/setting-library`).then(mapSettingLibrary),
  updateSettingLibrary: (id: number, content: string) =>
    patch<any>(`/setting-library/${id}`, { summary: content }).then(mapSettingLibrary),
  confirmSettingLibrary: (id: number) => post<any>(`/setting-library/${id}/confirm`).then(mapSettingLibrary),

  generateGlobalOutline: (projectId: number) =>
    post<any>(`/projects/${projectId}/global-outline/generate`, {
      projectId,
      outlineLevel: 'global',
    }).then(mapGlobalOutline),
  getGlobalOutline: (projectId: number) => request<any>(`/projects/${projectId}/global-outline`).then(mapGlobalOutline),
  updateGlobalOutline: (id: number, content: string) =>
    patch<any>(`/global-outlines/${id}`, { title: '全局大纲', content }).then(mapGlobalOutline),
  confirmGlobalOutline: (id: number) => post<any>(`/global-outlines/${id}/confirm`).then(mapGlobalOutline),

  listChapters: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/chapters`).then((items) => items.map((item) => mapChapter(item, projectId))),
  generateChapterOutlines: (projectId: number) =>
    post<any[]>(`/projects/${projectId}/chapters/generate-outline`).then((items) => items.map((item) => mapChapter(item, projectId))),
  generateChapterContent: (chapter: Chapter, suggestion?: string) =>
    post<any>(`/chapters/${chapter.id}/generate-content`, {
      projectId: chapter.projectId,
      revisionAdvice: suggestion,
    }).then((item) => mapChapter(item, chapter.projectId)),
  updateChapter: (chapterId: number, content: string) =>
    patch<any>(`/chapters/${chapterId}`, { content }).then((item) => mapChapter(item)),
  rewriteChapterContent: (chapterId: number, suggestion: string) =>
    post<any>(`/chapters/${chapterId}/rewrite-content`, {
      instruction: suggestion,
      revisionAdvice: suggestion,
    }).then((item) => mapChapter(item)),
  getProjectMemory: (projectId: number) =>
    request<any>(`/projects/${projectId}/memories`).then(mapProjectMemory),

  createCheck: (projectId: number) =>
    post<any>('/checks', { projectId, checkType: 'all' }).then((result) =>
      (result.issues ?? []).map((issue: any, index: number) => ({
        id: index + 1,
        projectId,
        type: issue.type,
        severity: issue.severity === 'critical' ? '高' : issue.severity === 'info' ? '低' : '中',
        summary: issue.description,
        suggestion: issue.suggestion,
      })),
    ),
  createExport: (projectId: number, format: ExportRecord['format'], scope: string) =>
    post<any>('/exports', { projectId, format, scope }).then((item) => mapExport(item, projectId, format, scope)),
  listVersions: (projectId: number) =>
    request<any[]>(`/versions?projectId=${projectId}`).then((items) =>
      items.map((item) => ({
        id: item.id,
        projectId,
        targetType: item.entityType,
        targetId: item.entityId,
        actionType: item.changeSource,
        summary: item.changeNote,
        createdAt: item.createdAt ?? nowText(),
      })),
    ),
};
