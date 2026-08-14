import { del, patch, post, request, streamRequest } from './client';
import type {
  Chapter,
  ChapterGenerationBatch,
  ChapterGenerationBatchCreateRequest,
  ChapterGenerationBatchSummary,
  ChapterOutlineContinueRequest,
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
  SettingLibrarySnapshot,
  SettingWorkflow,
  StoryCharacter,
  StoryCharacterRequest,
  StoryDirtyMarkSnapshot,
  StoryEvent,
  StoryEventRequest,
  StoryItem,
  StoryItemRequest,
  StoryLocation,
  StoryLocationRequest,
  StoryMemory,
  StoryRebuildResult,
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
    platformTarget: data.platformTarget ?? 'general',
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
    sellingPoint: data.sellingPoint ?? sellingPoints.join(', ') ?? '',
    worldview: data.worldview ?? '',
    mainConflict: data.mainConflict ?? '',
    estimatedWords: data.estimatedWords ?? `${data.estimatedWordCount ?? 0} words`,
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
    scenePlan: Array.isArray(data.scenePlan) ? data.scenePlan : [],
    content: data.content ?? '',
    status,
  };
}

interface ChapterStreamEvent {
  type: 'queued' | 'started' | 'chunk' | 'post_processing' | 'done' | 'error';
  content?: string;
  chapter?: Chapter;
  message?: string;
}

function mapChapterGenerationBatch(data: any): ChapterGenerationBatch {
  return {
    batchId: data.batchId,
    projectId: data.projectId,
    batchType: data.batchType ?? 'chapter_content',
    modelConfigId: data.modelConfigId ?? null,
    status: data.status ?? 'queued',
    totalCount: data.totalCount ?? 0,
    pendingCount: data.pendingCount ?? 0,
    runningCount: data.runningCount ?? 0,
    succeededCount: data.succeededCount ?? 0,
    failedCount: data.failedCount ?? 0,
    skippedCount: data.skippedCount ?? 0,
    qualityCheckedCount: data.qualityCheckedCount ?? 0,
    qualityFailedCount: data.qualityFailedCount ?? 0,
    qualityIssueCount: data.qualityIssueCount ?? 0,
    errorMessage: data.errorMessage ?? null,
    createdAt: data.createdAt ?? null,
    startedAt: data.startedAt ?? null,
    finishedAt: data.finishedAt ?? null,
    items: (data.items ?? []).map((item: any) => ({
      id: item.id,
      chapterId: item.chapterId,
      chapterNo: item.chapterNo,
      status: item.status ?? 'pending',
      attemptCount: item.attemptCount ?? 0,
      generationJobId: item.generationJobId ?? null,
      qualityStatus: item.qualityStatus ?? 'not_run',
      qualityIssueCount: item.qualityIssueCount ?? 0,
      qualityReport: item.qualityReport ?? null,
      qualityErrorMessage: item.qualityErrorMessage ?? null,
      errorMessage: item.errorMessage ?? null,
      startedAt: item.startedAt ?? null,
      finishedAt: item.finishedAt ?? null,
    })),
  };
}

function mapChapterGenerationBatchSummary(data: any): ChapterGenerationBatchSummary {
  const { items: _items, ...summary } = mapChapterGenerationBatch(data);
  return summary;
}

function mapChapterStreamEvent(event: any, projectId?: number): ChapterStreamEvent {
  return {
    type: event.type,
    content: event.content ?? '',
    chapter: event.chapter ? mapChapter(event.chapter, projectId) : undefined,
    message: event.message ?? '',
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

function mapSettingLibrarySnapshot(data: any, projectId: number): SettingLibrarySnapshot {
  return {
    settingLibrary: mapSettingLibrary(data.settingLibrary),
    characters: (data.characters ?? []).map((item: any) => mapCharacter(item, projectId)),
    organizations: (data.organizations ?? []).map((item: any) => mapOrganization(item, projectId)),
    locations: (data.locations ?? []).map((item: any) => mapLocation(item, projectId)),
    items: (data.items ?? []).map((item: any) => mapItem(item, projectId)),
    worldRules: (data.worldRules ?? []).map((item: any) => mapWorldRule(item, projectId)),
    relations: (data.relations ?? []).map((item: any) => mapRelation(item, projectId)),
    events: (data.events ?? []).map((item: any) => mapEvent(item, projectId)),
    stateRecords: (data.stateRecords ?? []).map((item: any) => mapStateRecord(item, projectId)),
  };
}

export const novelApi = {
  listProjects: async () => (await request<any[]>('/projects')).map(mapProject),
  createProject: (payload: ProjectCreateRequest) => post<number>('/projects', payload),
  updateProject: (projectId: number, payload: ProjectUpdateRequest) =>
    patch<void>(`/projects/${projectId}`, payload),

  listModelConfigs: () => request<ModelConfig[]>('/model-configs'),
  createModelConfig: (payload: ModelConfigRequest) =>
    post<{ id: number }>('/model-configs', payload).then((data) => data.id),
  updateModelConfig: (id: number, payload: ModelConfigRequest) =>
    patch<void>(`/model-configs/${id}`, payload),
  setDefaultModel: (id: number) => post<void>(`/model-configs/${id}/default`),
  disableModelConfig: (id: number) => del<void>(`/model-configs/${id}`),

  generateIdeas: (projectId: number, suggestion?: string, ideaCount = 3) => {
    const payload: IdeaGenerateRequest = {
      projectId,
      modelType: '\u521b\u610f\u751f\u6210',
      briefDescription: suggestion || 'Generate long-form serialized novel ideas from the project brief.',
      ideaCount,
    };
    return post<any[]>(`/projects/${projectId}/ideas/generate`, payload)
      .then((items) => items.map((item) => mapIdea(item, projectId)));
  },
  listIdeas: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/ideas`).then((items) => items.map((item) => mapIdea(item, projectId))),
  updateIdea: (idea: Idea, changeNote = 'User edited idea content') =>
    patch<void>(`/ideas/${idea.id}`, {
      title: idea.title,
      sellingPoints: [idea.sellingPoint],
      worldview: idea.worldview,
      mainConflict: idea.mainConflict,
      estimatedWordCount: Number.parseInt(idea.estimatedWords, 10) || 0,
      summary: idea.content,
      changeNote,
    }),
  rewriteIdea: (ideaId: number, suggestion: string) =>
    post<any>(`/ideas/${ideaId}/rewrite`, { instruction: suggestion }).then((item) => mapIdea(item)),
  selectIdea: (ideaId: number) => post<void>(`/ideas/${ideaId}/select`),
  deleteIdea: (ideaId: number) => del<void>(`/ideas/${ideaId}`),

  generateSettingLibrary: (projectId: number) =>
    post<any>(`/projects/${projectId}/setting-library/generate`, { projectId }).then(mapSettingLibrary),
  getSettingLibrary: (projectId: number) =>
    request<any>(`/projects/${projectId}/setting-library`).then(mapSettingLibrary),
  getSettingLibrarySnapshot: (projectId: number) =>
    request<any>(`/projects/${projectId}/setting-library/snapshot`)
      .then((item) => mapSettingLibrarySnapshot(item, projectId)),
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
    patch<any>(`/global-outlines/${id}`, { title: 'Global outline', content }).then(mapGlobalOutline),
  confirmGlobalOutline: (id: number) =>
    post<void>(`/global-outlines/${id}/confirm`),
  saveGlobalOutline: (id: number, content: string) =>
    patch<void>(`/global-outlines/${id}`, { title: 'Global outline', content }),
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
  continueChapterOutlines: (projectId: number, payload: ChapterOutlineContinueRequest) =>
    post<any[]>(`/projects/${projectId}/chapters/continue-outline`, payload)
      .then((items) => items.map((item) => mapChapter(item, projectId))),
  createChapterGenerationBatch: (projectId: number, payload: ChapterGenerationBatchCreateRequest) =>
    post<any>(`/projects/${projectId}/chapter-generation-batches`, payload).then(mapChapterGenerationBatch),
  getChapterGenerationBatch: (batchId: number) =>
    request<any>(`/chapter-generation-batches/${batchId}`).then(mapChapterGenerationBatch),
  listChapterGenerationBatches: (projectId: number) =>
    request<any[]>(`/projects/${projectId}/chapter-generation-batches`)
      .then((items) => items.map(mapChapterGenerationBatchSummary)),
  getLatestChapterGenerationBatch: (projectId: number) =>
    request<any>(`/projects/${projectId}/chapter-generation-batches/latest`).then(mapChapterGenerationBatch),
  cancelChapterGenerationBatch: (batchId: number) =>
    post<any>(`/chapter-generation-batches/${batchId}/cancel`).then(mapChapterGenerationBatch),
  pauseChapterGenerationBatch: (batchId: number) =>
    post<any>(`/chapter-generation-batches/${batchId}/pause`).then(mapChapterGenerationBatch),
  resumeChapterGenerationBatch: (batchId: number) =>
    post<any>(`/chapter-generation-batches/${batchId}/resume`).then(mapChapterGenerationBatch),
  retryFailedChapterGenerationBatch: (batchId: number) =>
    post<any>(`/chapter-generation-batches/${batchId}/retry-failed`).then(mapChapterGenerationBatch),
  generateChapterContent: (chapter: Chapter, suggestion?: string) =>
    post<any>(`/chapters/${chapter.id}/generate-content`, {
      projectId: chapter.projectId,
      revisionAdvice: suggestion,
    }).then((item) => mapChapter(item, chapter.projectId)),
  streamGenerateChapterContent: (
    chapter: Chapter,
    suggestion: string | undefined,
    onEvent: (event: ChapterStreamEvent) => void,
  ) =>
    streamRequest<any>(
      `/chapters/${chapter.id}/generate-content/stream`,
      {
        projectId: chapter.projectId,
        revisionAdvice: suggestion,
      },
      (event) => onEvent(mapChapterStreamEvent(event, chapter.projectId)),
    ),
  updateChapter: (chapterId: number, content: string) =>
    patch<any>(`/chapters/${chapterId}`, { content }).then((item) => mapChapter(item)),
  rewriteChapterContent: (chapterId: number, suggestion: string) =>
    post<any>(`/chapters/${chapterId}/rewrite-content`, {
      instruction: suggestion || 'Rewrite the chapter with clearer action, conflict, and dialogue.',
    }).then((item) => mapChapter(item)),
  streamRewriteChapterContent: (
    chapterId: number,
    suggestion: string,
    onEvent: (event: ChapterStreamEvent) => void,
  ) =>
    streamRequest<any>(
      `/chapters/${chapterId}/rewrite-content/stream`,
      {
        instruction: suggestion || 'Rewrite the chapter with clearer action, conflict, and dialogue.',
      },
      (event) => onEvent(mapChapterStreamEvent(event)),
    ),
  getProjectMemory: (projectId: number) =>
    request<any>(`/projects/${projectId}/memories`).then(mapProjectMemory),
  getStoryDirtyMarks: (projectId: number, chapterNo?: number) =>
    request<StoryDirtyMarkSnapshot>(
      `/projects/${projectId}/story-dirty-marks${chapterNo == null ? '' : `?chapterNo=${chapterNo}`}`,
    ),
  rebuildStoryState: (projectId: number, startChapterNo?: number, modelConfigId?: number) =>
    post<StoryRebuildResult>(`/projects/${projectId}/story-rebuild`, {
      startChapterNo,
      modelConfigId,
    }),

  createCheck: (projectId: number) =>
    post<any>('/checks', { projectId, checkType: 'all' }).then((result) =>
      (result.issues ?? []).map((issue: any, index: number): CheckResult => ({
        id: index + 1,
        projectId,
        type: issue.type,
        severity: issue.severity === 'critical' ? '\u9ad8' : issue.severity === 'info' ? '\u4f4e' : '\u4e2d',
        summary: issue.description,
        suggestion: issue.suggestion,
      })),
    ),
  createExport: (projectId: number, format: ExportRecord['format'], scope: string) =>
    post<any>('/exports', { projectId, format, scope }).then((item) => ({
      projectId: item.projectId ?? projectId,
      format: item.format ?? format,
      scope: item.scope ?? scope,
      fileName: item.fileName,
      content: item.content ?? '',
    })),
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
