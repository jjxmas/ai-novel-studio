import { del, patch, post, request } from './client';
import type {
  Chapter,
  ChapterSummary,
  CheckResult,
  ContentVersion,
  EntityRelation,
  EntityRelationRequest,
  EntityStateRecord,
  EntityStateRecordRequest,
  ExportRecord,
  GlobalOutline,
  Idea,
  IdeaGenerateRequest,
  ModelConfig,
  ModelConfigRequest,
  OutlineWorkflow,
  Organization,
  OrganizationRequest,
  Project,
  ProjectCreateRequest,
  ProjectMemory,
  ProjectUpdateRequest,
  SettingLibrary,
  SettingWorkflow,
  StoryCharacter,
  StoryCharacterRequest,
  StoryEvent,
  StoryEventRequest,
  StoryItem,
  StoryItemRequest,
  StoryLocation,
  StoryLocationRequest,
  StoryMemory,
  WorkflowStage,
  WorldRule,
  WorldRuleRequest,
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
  const longFormPotentialScore = data.longFormPotentialScore ?? data.score ?? null;
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    title: data.title,
    sellingPoint: data.sellingPoint ?? sellingPoints.join('、') ?? '',
    worldview: data.worldview ?? '',
    mainConflict: data.mainConflict ?? '',
    estimatedWords: data.estimatedWords ?? `${data.estimatedWordCount ?? 0}字`,
    score: longFormPotentialScore ?? 0,
    selected: data.selected ?? data.status === 'selected',
    content: data.content ?? data.summary ?? '',
    longFormPotentialScore,
    conflictScore: data.conflictScore ?? null,
    noveltyScore: data.noveltyScore ?? null,
    beginnerFriendlinessScore: data.beginnerFriendlinessScore ?? null,
    platformFitScore: data.platformFitScore ?? null,
    riskLevel: data.riskLevel ?? null,
    strengths: data.strengths ?? [],
    risks: data.risks ?? [],
    suggestions: data.suggestions ?? [],
    overallComment: data.overallComment ?? '',
  };
}

function mapSettingLibrary(data: any): SettingLibrary {
  return {
    id: data.id,
    projectId: data.projectId,
    sourceIdeaId: data.sourceIdeaId ?? null,
    summary: data.summary ?? data.overview ?? '',
    overview: data.overview ?? data.summary ?? '',
    genreTemplate: data.genreTemplate ?? null,
    status: data.status ?? 'draft',
    confirmed: Boolean(data.confirmed),
    confirmedAt: data.confirmedAt ?? null,
    characterCount: data.characterCount ?? 0,
    organizationCount: data.organizationCount ?? 0,
    locationCount: data.locationCount ?? 0,
    itemCount: data.itemCount ?? 0,
    ruleCount: data.ruleCount ?? 0,
    relationCount: data.relationCount ?? 0,
    eventCount: data.eventCount ?? 0,
    stateRecordCount: data.stateRecordCount ?? 0,
    completenessScore: data.completenessScore ?? 0,
  };
}

function mapSettingWorkflow(data: any): SettingWorkflow {
  return {
    id: data.id,
    projectId: data.projectId,
    sourceIdeaId: data.sourceIdeaId,
    status: data.status ?? 'blueprint_ready',
    blueprint: data.blueprint ?? {},
    draft: data.draft ?? {},
    checks: data.checks ?? {},
    blueprintConfirmedAt: data.blueprintConfirmedAt ?? null,
    committedAt: data.committedAt ?? null,
  };
}

function mapGlobalOutline(data: any): GlobalOutline {
  return {
    id: data.id,
    projectId: data.projectId ?? 0,
    content: data.content ?? '',
    confirmed: Boolean(data.confirmed),
    volumes: (data.volumes ?? []).map((volume: any) => ({
      id: volume.id,
      volumeNo: volume.volumeNo ?? 0,
      title: volume.title ?? '',
      summary: volume.summary ?? '',
      goal: volume.goal ?? '',
      estimatedWordCount: volume.estimatedWordCount ?? 0,
    })),
  };
}

function mapOutlineWorkflow(data: any): OutlineWorkflow {
  return {
    id: data.id,
    projectId: data.projectId,
    settingLibraryId: data.settingLibraryId,
    status: data.status ?? 'draft_ready',
    draft: data.draft ?? {},
    checks: data.checks ?? {},
    committedAt: data.committedAt ?? null,
  };
}

function mapCharacter(data: any, projectId?: number): StoryCharacter {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    aliases: data.aliases ?? data.alias ?? [],
    roleType: data.roleType ?? 'supporting',
    narrativeRole: data.narrativeRole ?? 'supporting',
    identity: data.identity ?? '',
    publicIdentity: data.publicIdentity ?? '',
    gender: data.gender ?? '',
    ageText: data.ageText ?? '',
    personality: data.personality ?? '',
    motivation: data.motivation ?? '',
    background: data.background ?? '',
    coreGoal: data.coreGoal ?? '',
    innerNeed: data.innerNeed ?? '',
    coreFlaw: data.coreFlaw ?? '',
    bottomLine: data.bottomLine ?? '',
    skillsSummary: data.skillsSummary ?? '',
    secretNotes: data.secretNotes ?? '',
    relationshipSummary: data.relationshipSummary ?? '',
    importance: data.importance ?? 0,
    status: data.status ?? 'active',
    firstAppearedChapterId: data.firstAppearedChapterId ?? null,
    notes: data.notes ?? '',
  };
}

function mapOrganization(data: any, projectId?: number): Organization {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    organizationType: data.organizationType ?? 'faction',
    publicMission: data.publicMission ?? '',
    realGoal: data.realGoal ?? '',
    controlledResources: data.controlledResources ?? '',
    powerScope: data.powerScope ?? '',
    baseLocationId: data.baseLocationId ?? null,
    entryRules: data.entryRules ?? '',
    status: data.status ?? 'active',
    notes: data.notes ?? '',
  };
}

function mapLocation(data: any, projectId?: number): StoryLocation {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    locationType: data.locationType ?? 'place',
    parentLocationId: data.parentLocationId ?? null,
    description: data.description ?? '',
    keyFeatures: data.keyFeatures ?? '',
    entryConditions: data.entryConditions ?? '',
    availableResources: data.availableResources ?? '',
    controllingOrgId: data.controllingOrgId ?? null,
    riskLevel: data.riskLevel ?? 'medium',
    rules: data.rules ?? '',
    notes: data.notes ?? '',
  };
}

function mapItem(data: any, projectId?: number): StoryItem {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    itemType: data.itemType ?? 'item',
    description: data.description ?? '',
    usageRules: data.usageRules ?? '',
    limitations: data.limitations ?? '',
    rarity: data.rarity ?? '',
    ownerCharacterId: data.ownerCharacterId ?? null,
    ownerOrgId: data.ownerOrgId ?? null,
    status: data.status ?? 'available',
    notes: data.notes ?? '',
  };
}

function mapWorldRule(data: any, projectId?: number): WorldRule {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    ruleType: data.ruleType ?? 'general',
    description: data.description ?? '',
    triggerCondition: data.triggerCondition ?? '',
    effectResult: data.effectResult ?? '',
    limitations: data.limitations ?? '',
    cost: data.cost ?? '',
    exceptions: data.exceptions ?? '',
    visibilityLevel: data.visibilityLevel ?? 'public',
    importance: data.importance ?? 0,
    examples: data.examples ?? '',
    notes: data.notes ?? '',
  };
}

function mapRelation(data: any, projectId?: number): EntityRelation {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    sourceType: data.sourceType ?? 'character',
    sourceId: data.sourceId ?? 0,
    targetType: data.targetType ?? 'character',
    targetId: data.targetId ?? 0,
    relationType: data.relationType ?? 'knows',
    relationStatus: data.relationStatus ?? 'active',
    strengthValue: data.strengthValue ?? null,
    visibilityLevel: data.visibilityLevel ?? 'public',
    note: data.note ?? '',
    startEventId: data.startEventId ?? null,
    endEventId: data.endEventId ?? null,
  };
}

function mapEvent(data: any, projectId?: number): StoryEvent {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    name: data.name ?? '',
    eventType: data.eventType ?? 'story',
    description: data.description ?? '',
    eventTimeText: data.eventTimeText ?? '',
    locationId: data.locationId ?? null,
    chapterId: data.chapterId ?? null,
    planned: Boolean(data.planned),
    importance: data.importance ?? 0,
  };
}

function mapStateRecord(data: any, projectId?: number): EntityStateRecord {
  return {
    id: data.id,
    projectId: data.projectId ?? projectId ?? 0,
    entityType: data.entityType ?? 'character',
    entityId: data.entityId ?? 0,
    stateType: data.stateType ?? '',
    oldValue: data.oldValue ?? null,
    newValue: data.newValue ?? {},
    eventId: data.eventId ?? null,
    chapterId: data.chapterId ?? null,
    effectiveAt: data.effectiveAt ?? null,
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
  updateProject: async (projectId: number, payload: ProjectUpdateRequest) =>
    mapProject(await patch(`/projects/${projectId}`, payload)),

  listModelConfigs: () => request<ModelConfig[]>('/model-configs'),
  createModelConfig: (payload: ModelConfigRequest) => post<ModelConfig>('/model-configs', payload),
  updateModelConfig: (id: number, payload: ModelConfigRequest) => patch<ModelConfig>(`/model-configs/${id}`, payload),
  setDefaultModel: (id: number) => post<ModelConfig>(`/model-configs/${id}/default`),
  disableModelConfig: (id: number) => del<ModelConfig>(`/model-configs/${id}`),

  generateIdeas: (projectId: number, suggestion?: string, ideaCount = 3) => {
    const payload: IdeaGenerateRequest = {
      projectId,
      modelType: '创意生成',
      briefDescription: suggestion || '根据作品简介生成适合长篇连载的创意方案',
      ideaCount,
    };
    return post<any[]>(`/projects/${projectId}/ideas/generate`, payload)
      .then((items) => items.map((item) => mapIdea(item, projectId)));
  },
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
  updateSettingLibrary: (id: number, overview: string) =>
    patch<any>(`/setting-library/${id}`, { summary: overview, overview }).then(mapSettingLibrary),
  confirmSettingLibrary: (id: number) =>
    post<any>(`/setting-library/${id}/confirm`).then(mapSettingLibrary),

  startSettingWorkflow: (projectId: number) =>
    post<any>(`/projects/${projectId}/setting-workflows`, { projectId }).then(mapSettingWorkflow),
  getLatestSettingWorkflow: (projectId: number) =>
    request<any>(`/projects/${projectId}/setting-workflows/latest`).then(mapSettingWorkflow),
  approveSettingWorkflowBlueprint: (workflowId: number) =>
    post<any>(`/setting-workflows/${workflowId}/approve-blueprint`).then(mapSettingWorkflow),
  regenerateSettingWorkflowModule: (workflowId: number, moduleKey: string) =>
    post<any>(`/setting-workflows/${workflowId}/regenerate-module`, { moduleKey }).then(mapSettingWorkflow),
  commitSettingWorkflow: (workflowId: number) =>
    post<any>(`/setting-workflows/${workflowId}/commit`).then(mapSettingLibrary),

  listCharacters: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/characters`).then((items) => items.map((item) => mapCharacter(item, projectId))),
  createCharacter: (projectId: number, payload: StoryCharacterRequest) =>
    post<any>(`/projects/${projectId}/characters`, payload).then((item) => mapCharacter(item, projectId)),
  updateCharacter: (projectId: number, characterId: number, payload: StoryCharacterRequest) =>
    patch<any>(`/projects/${projectId}/characters/${characterId}`, payload).then((item) => mapCharacter(item, projectId)),
  deleteCharacter: (projectId: number, characterId: number) =>
    del<void>(`/projects/${projectId}/characters/${characterId}`),

  listOrganizations: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/organizations`).then((items) => items.map((item) => mapOrganization(item, projectId))),
  createOrganization: (projectId: number, payload: OrganizationRequest) =>
    post<any>(`/projects/${projectId}/organizations`, payload).then((item) => mapOrganization(item, projectId)),
  updateOrganization: (projectId: number, organizationId: number, payload: OrganizationRequest) =>
    patch<any>(`/projects/${projectId}/organizations/${organizationId}`, payload).then((item) => mapOrganization(item, projectId)),
  deleteOrganization: (projectId: number, organizationId: number) =>
    del<void>(`/projects/${projectId}/organizations/${organizationId}`),

  listLocations: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/locations`).then((items) => items.map((item) => mapLocation(item, projectId))),
  createLocation: (projectId: number, payload: StoryLocationRequest) =>
    post<any>(`/projects/${projectId}/locations`, payload).then((item) => mapLocation(item, projectId)),
  updateLocation: (projectId: number, locationId: number, payload: StoryLocationRequest) =>
    patch<any>(`/projects/${projectId}/locations/${locationId}`, payload).then((item) => mapLocation(item, projectId)),
  deleteLocation: (projectId: number, locationId: number) =>
    del<void>(`/projects/${projectId}/locations/${locationId}`),

  listItems: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/items`).then((items) => items.map((item) => mapItem(item, projectId))),
  createItem: (projectId: number, payload: StoryItemRequest) =>
    post<any>(`/projects/${projectId}/items`, payload).then((item) => mapItem(item, projectId)),
  updateItem: (projectId: number, itemId: number, payload: StoryItemRequest) =>
    patch<any>(`/projects/${projectId}/items/${itemId}`, payload).then((item) => mapItem(item, projectId)),
  deleteItem: (projectId: number, itemId: number) =>
    del<void>(`/projects/${projectId}/items/${itemId}`),

  listWorldRules: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/world-rules`).then((items) => items.map((item) => mapWorldRule(item, projectId))),
  createWorldRule: (projectId: number, payload: WorldRuleRequest) =>
    post<any>(`/projects/${projectId}/world-rules`, payload).then((item) => mapWorldRule(item, projectId)),
  updateWorldRule: (projectId: number, ruleId: number, payload: WorldRuleRequest) =>
    patch<any>(`/projects/${projectId}/world-rules/${ruleId}`, payload).then((item) => mapWorldRule(item, projectId)),
  deleteWorldRule: (projectId: number, ruleId: number) =>
    del<void>(`/projects/${projectId}/world-rules/${ruleId}`),

  listRelations: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/relations`).then((items) => items.map((item) => mapRelation(item, projectId))),
  createRelation: (projectId: number, payload: EntityRelationRequest) =>
    post<any>(`/projects/${projectId}/relations`, payload).then((item) => mapRelation(item, projectId)),
  updateRelation: (projectId: number, relationId: number, payload: EntityRelationRequest) =>
    patch<any>(`/projects/${projectId}/relations/${relationId}`, payload).then((item) => mapRelation(item, projectId)),
  deleteRelation: (projectId: number, relationId: number) =>
    del<void>(`/projects/${projectId}/relations/${relationId}`),

  listEvents: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/events`).then((items) => items.map((item) => mapEvent(item, projectId))),
  createEvent: (projectId: number, payload: StoryEventRequest) =>
    post<any>(`/projects/${projectId}/events`, payload).then((item) => mapEvent(item, projectId)),
  updateEvent: (projectId: number, eventId: number, payload: StoryEventRequest) =>
    patch<any>(`/projects/${projectId}/events/${eventId}`, payload).then((item) => mapEvent(item, projectId)),
  deleteEvent: (projectId: number, eventId: number) =>
    del<void>(`/projects/${projectId}/events/${eventId}`),

  listStateRecords: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/state-records`).then((items) => items.map((item) => mapStateRecord(item, projectId))),
  createStateRecord: (projectId: number, payload: EntityStateRecordRequest) =>
    post<any>(`/projects/${projectId}/state-records`, payload).then((item) => mapStateRecord(item, projectId)),
  updateStateRecord: (projectId: number, recordId: number, payload: EntityStateRecordRequest) =>
    patch<any>(`/projects/${projectId}/state-records/${recordId}`, payload).then((item) => mapStateRecord(item, projectId)),
  deleteStateRecord: (projectId: number, recordId: number) =>
    del<void>(`/projects/${projectId}/state-records/${recordId}`),

  generateGlobalOutline: (projectId: number) =>
    post<any>(`/projects/${projectId}/global-outline/generate`, {
      projectId,
      outlineLevel: 'global',
    }).then(mapGlobalOutline),
  getGlobalOutline: (projectId: number) =>
    request<any>(`/projects/${projectId}/global-outline`).then(mapGlobalOutline),
  updateGlobalOutline: (id: number, content: string) =>
    patch<any>(`/global-outlines/${id}`, { title: '全局大纲', content }).then(mapGlobalOutline),
  confirmGlobalOutline: (id: number) =>
    post<any>(`/global-outlines/${id}/confirm`).then(mapGlobalOutline),
  startOutlineWorkflow: (projectId: number) =>
    post<any>(`/projects/${projectId}/outline-workflows`, { projectId }).then(mapOutlineWorkflow),
  getLatestOutlineWorkflow: (projectId: number) =>
    request<any>(`/projects/${projectId}/outline-workflows/latest`).then(mapOutlineWorkflow),
  commitOutlineWorkflow: (workflowId: number) =>
    post<any>(`/outline-workflows/${workflowId}/commit`).then(mapGlobalOutline),

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
      instruction: suggestion || '保持章节目标和关键设定一致，重写得更具体、更有动作和对白。',
    }).then((item) => mapChapter(item)),
  getProjectMemory: (projectId: number) =>
    request<any>(`/projects/${projectId}/memories`).then(mapProjectMemory),

  createCheck: (projectId: number) =>
    post<any>('/checks', { projectId, checkType: 'all' }).then((result) =>
      (result.issues ?? []).map((issue: any, index: number): CheckResult => ({
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
      items.map((item): ContentVersion => ({
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
