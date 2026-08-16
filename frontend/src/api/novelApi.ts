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
  ExportResult,
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
  StoryRebuildRun,
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
    stage: data.workflowStage ?? data.stage ?? mapStage(data.status),
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

function mapChapter(data: ChapterWire, projectId?: number): Chapter {
  const contentStatus = data.contentStatus ?? (data.content ? 'generated' : 'not_generated');
  const status = contentStatus === 'edited'
    ? 'edited'
    : ['generated', 'checked'].includes(contentStatus) || data.status === 'drafted' || data.status === 'content_ready'
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
    wordCount: data.wordCount ?? 0,
    hasContent: Boolean(data.hasContent ?? data.content),
    contentStatus,
    contentGeneratedAt: data.contentGeneratedAt ?? null,
    contentUpdatedAt: data.contentUpdatedAt ?? null,
    lastGenerationJobId: data.lastGenerationJobId ?? null,
    lastContentVersionNo: data.lastContentVersionNo ?? 0,
    status,
  };
}

interface ChapterStreamEvent {
  type: 'queued' | 'started' | 'chunk' | 'post_processing' | 'done' | 'error';
  content?: string;
  chapter?: Chapter;
  message?: string;
}

interface ChapterWire {
  id: number;
  projectId?: number;
  chapterNo?: number;
  title: string;
  outline?: string | null;
  scenePlan?: string[] | null;
  content?: string | null;
  wordCount?: number | null;
  hasContent?: boolean | null;
  status?: string | null;
  contentStatus?: string | null;
  contentGeneratedAt?: string | null;
  contentUpdatedAt?: string | null;
  lastGenerationJobId?: number | null;
  lastContentVersionNo?: number | null;
}

interface ChapterPageWire {
  items: ChapterWire[];
  total: number;
  page: number;
  size: number;
}

interface ChapterStreamEventWire {
  type: ChapterStreamEvent['type'];
  content?: string | null;
  chapter?: ChapterWire | null;
  message?: string | null;
}

interface VersionWire {
  id: number;
  projectId: number;
  entityType: string;
  entityId: number;
  versionNo: number;
  changeSource: string;
  operationType: string;
  changeNote: string;
  revisionInstruction?: string | null;
  createdAt: string;
}

interface CheckIssueWire {
  type: string;
  severity: string;
  description: string;
  suggestion: string;
  reference?: string | null;
}

interface CheckWire {
  issueCount: number;
  issues: CheckIssueWire[];
  summary: string;
}

interface ChapterGenerationBatchSummaryWire {
  batchId: number;
  projectId: number;
  batchType: string;
  modelConfigId?: number | null;
  status: string;
  totalCount: number;
  pendingCount: number;
  runningCount: number;
  succeededCount: number;
  failedCount: number;
  skippedCount: number;
  qualityCheckedCount: number;
  qualityFailedCount: number;
  qualityIssueCount: number;
  errorMessage?: string | null;
  createdAt?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
}

interface ChapterGenerationBatchItemWire {
  id: number;
  chapterId: number;
  chapterNo: number;
  status: string;
  attemptCount: number;
  generationJobId?: number | null;
  qualityStatus: string;
  qualityIssueCount: number;
  qualityReport?: CheckWire | null;
  qualityErrorMessage?: string | null;
  errorMessage?: string | null;
  startedAt?: string | null;
  finishedAt?: string | null;
}

interface ChapterGenerationBatchWire extends ChapterGenerationBatchSummaryWire {
  items: ChapterGenerationBatchItemWire[];
}

interface ExportWire {
  fileName: string;
  filePath: string;
  format: 'md' | 'txt';
  scope: string;
  content: string;
}

function mapChapterGenerationBatchSummary(data: ChapterGenerationBatchSummaryWire): ChapterGenerationBatchSummary {
  return {
    batchId: data.batchId,
    projectId: data.projectId,
    batchType: data.batchType,
    modelConfigId: data.modelConfigId ?? null,
    status: data.status,
    totalCount: data.totalCount,
    pendingCount: data.pendingCount,
    runningCount: data.runningCount,
    succeededCount: data.succeededCount,
    failedCount: data.failedCount,
    skippedCount: data.skippedCount,
    qualityCheckedCount: data.qualityCheckedCount,
    qualityFailedCount: data.qualityFailedCount,
    qualityIssueCount: data.qualityIssueCount,
    errorMessage: data.errorMessage ?? null,
    createdAt: data.createdAt ?? null,
    startedAt: data.startedAt ?? null,
    finishedAt: data.finishedAt ?? null,
  };
}

function mapChapterGenerationBatch(data: ChapterGenerationBatchWire): ChapterGenerationBatch {
  return {
    ...mapChapterGenerationBatchSummary(data),
    items: data.items.map((item) => ({
      id: item.id,
      chapterId: item.chapterId,
      chapterNo: item.chapterNo,
      status: item.status,
      attemptCount: item.attemptCount,
      generationJobId: item.generationJobId ?? null,
      qualityStatus: item.qualityStatus,
      qualityIssueCount: item.qualityIssueCount,
      qualityReport: item.qualityReport ?? null,
      qualityErrorMessage: item.qualityErrorMessage ?? null,
      errorMessage: item.errorMessage ?? null,
      startedAt: item.startedAt ?? null,
      finishedAt: item.finishedAt ?? null,
    })),
  };
}

function mapChapterStreamEvent(event: ChapterStreamEventWire, projectId?: number): ChapterStreamEvent {
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

function mapExport(data: ExportWire): ExportResult {
  return {
    fileName: data.fileName,
    filePath: data.filePath,
    format: data.format === 'md' ? 'markdown' : 'txt',
    scope: data.scope,
    content: data.content,
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
  deleteProject: (projectId: number) => del<void>(`/projects/${projectId}`),

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
    request<ChapterWire[]>(`/projects/${projectId}/chapters/catalog`)
      .then((items) => items.map((item) => mapChapter(item, projectId))),
  listChapterPage: (projectId: number, page: number, size: number, keyword = '') => {
    const params = new URLSearchParams({ page: String(page), size: String(size) });
    if (keyword.trim()) params.set('keyword', keyword.trim());
    return request<ChapterPageWire>(`/projects/${projectId}/chapters?${params}`).then((result) => ({
      items: result.items.map((item) => mapChapter(item, projectId)),
      total: result.total,
      page: result.page,
      size: result.size,
    }));
  },
  getChapter: (chapterId: number) =>
    request<ChapterWire>(`/chapters/${chapterId}`).then((item) => mapChapter(item)),
  continueChapterOutlines: (projectId: number, payload: ChapterOutlineContinueRequest) =>
    post<ChapterWire[]>(`/projects/${projectId}/chapters/continue-outline`, payload)
      .then((items) => items.map((item) => mapChapter(item, projectId))),
  createChapterGenerationBatch: (projectId: number, payload: ChapterGenerationBatchCreateRequest) =>
    post<ChapterGenerationBatchWire>(`/projects/${projectId}/chapter-generation-batches`, payload)
      .then(mapChapterGenerationBatch),
  getChapterGenerationBatch: (batchId: number) =>
    request<ChapterGenerationBatchWire>(`/chapter-generation-batches/${batchId}`)
      .then(mapChapterGenerationBatch),
  listChapterGenerationBatches: (projectId: number) =>
    request<ChapterGenerationBatchSummaryWire[]>(`/projects/${projectId}/chapter-generation-batches`)
      .then((items) => items.map(mapChapterGenerationBatchSummary)),
  getLatestChapterGenerationBatch: (projectId: number) =>
    request<ChapterGenerationBatchWire>(`/projects/${projectId}/chapter-generation-batches/latest`)
      .then(mapChapterGenerationBatch),
  cancelChapterGenerationBatch: (batchId: number) =>
    post<ChapterGenerationBatchWire>(`/chapter-generation-batches/${batchId}/cancel`)
      .then(mapChapterGenerationBatch),
  pauseChapterGenerationBatch: (batchId: number) =>
    post<ChapterGenerationBatchWire>(`/chapter-generation-batches/${batchId}/pause`)
      .then(mapChapterGenerationBatch),
  resumeChapterGenerationBatch: (batchId: number) =>
    post<ChapterGenerationBatchWire>(`/chapter-generation-batches/${batchId}/resume`)
      .then(mapChapterGenerationBatch),
  retryFailedChapterGenerationBatch: (batchId: number) =>
    post<ChapterGenerationBatchWire>(`/chapter-generation-batches/${batchId}/retry-failed`)
      .then(mapChapterGenerationBatch),
  generateChapterContent: (chapter: Chapter, suggestion?: string) =>
    post<ChapterWire>(`/chapters/${chapter.id}/generate-content`, {
      projectId: chapter.projectId,
      revisionAdvice: suggestion,
    }).then((item) => mapChapter(item, chapter.projectId)),
  streamGenerateChapterContent: (
    chapter: Chapter,
    suggestion: string | undefined,
    onEvent: (event: ChapterStreamEvent) => void,
  ) =>
    streamRequest<ChapterStreamEventWire>(
      `/chapters/${chapter.id}/generate-content/stream`,
      {
        projectId: chapter.projectId,
        revisionAdvice: suggestion,
      },
      (event) => onEvent(mapChapterStreamEvent(event, chapter.projectId)),
    ),
  updateChapter: (chapterId: number, content: string, expectedVersion: number) =>
    patch<ChapterWire>(`/chapters/${chapterId}`, { content, expectedVersion }).then((item) => mapChapter(item)),
  rewriteChapterContent: (chapterId: number, suggestion: string) =>
    post<ChapterWire>(`/chapters/${chapterId}/rewrite-content`, {
      instruction: suggestion || 'Rewrite the chapter with clearer action, conflict, and dialogue.',
    }).then((item) => mapChapter(item)),
  streamRewriteChapterContent: (
    chapterId: number,
    suggestion: string,
    onEvent: (event: ChapterStreamEvent) => void,
  ) =>
    streamRequest<ChapterStreamEventWire>(
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
  enqueueStoryRebuild: (projectId: number, startChapterNo?: number, modelConfigId?: number) =>
    post<StoryRebuildRun>(`/projects/${projectId}/story-rebuild-jobs`, {
      startChapterNo,
      modelConfigId,
    }),
  getStoryRebuildRun: (projectId: number, runId: number) =>
    request<StoryRebuildRun>(`/projects/${projectId}/story-rebuild-jobs/${runId}`),
  getLatestStoryRebuildRun: (projectId: number) =>
    request<StoryRebuildRun | null>(`/projects/${projectId}/story-rebuild-jobs/latest`),

  createCheck: (projectId: number, chapterId: number) =>
    post<CheckWire>('/checks', { projectId, chapterId, checkType: 'continuity' }).then((result) =>
      result.issues.map((issue, index): CheckResult => ({
        id: index + 1,
        projectId,
        type: issue.type,
        severity: ['critical', 'high'].includes(issue.severity)
          ? '\u9ad8'
          : ['info', 'low'].includes(issue.severity) ? '\u4f4e' : '\u4e2d',
        summary: issue.reference ? `${issue.reference}：${issue.description}` : issue.description,
        suggestion: issue.suggestion,
      })),
    ),
  createQualityCheckBatch: (projectId: number, modelConfigId?: number) =>
    post<ChapterGenerationBatchWire>('/checks/batches', { projectId, modelConfigId, checkType: 'all' })
      .then(mapChapterGenerationBatch),
  getQualityCheckBatch: (batchId: number) =>
    request<ChapterGenerationBatchWire>(`/checks/batches/${batchId}`).then(mapChapterGenerationBatch),
  getLatestQualityCheckBatch: (projectId: number) =>
    request<ChapterGenerationBatchWire>(`/checks/projects/${projectId}/batches/latest`)
      .then(mapChapterGenerationBatch),
  createExport: (projectId: number, format: ExportRecord['format'], scope: string, scopeEntityId?: number) =>
    post<ExportWire>('/exports', { projectId, format, scope, scopeEntityId }).then(mapExport),
  listVersions: (projectId: number) =>
    request<VersionWire[]>(`/versions?projectId=${projectId}`).then((items) =>
      items.map((item): ContentVersion => ({
        id: item.id,
        projectId: item.projectId,
        targetType: item.entityType,
        targetId: item.entityId,
        versionNo: item.versionNo,
        actionType: item.operationType,
        summary: item.changeNote,
        revisionInstruction: item.revisionInstruction ?? null,
        createdAt: item.createdAt,
      })),
    ),
};
