import { computed, reactive } from 'vue';

import { novelApi } from '@/api/novelApi';
import type {
  Chapter,
  CheckResult,
  ContentVersion,
  EntityStateRecord,
  EntityStateRecordRequest,
  ExportRecord,
  GlobalOutline,
  Idea,
  ModelConfig,
  ModelConfigRequest,
  EntityRelation,
  EntityRelationRequest,
  Project,
  ProjectCreateRequest,
  ProjectUpdateRequest,
  ProjectMemory,
  Organization,
  OrganizationRequest,
  OutlineWorkflow,
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
  WorkflowStage,
  WorldRule,
  WorldRuleRequest,
} from '@/api/types';

interface WorkspaceState {
  projects: Project[];
  activeProjectId: number | null;
  modelConfigs: ModelConfig[];
  ideas: Idea[];
  settingLibrary: SettingLibrary | null;
  settingWorkflow: SettingWorkflow | null;
  characters: StoryCharacter[];
  organizations: Organization[];
  locations: StoryLocation[];
  items: StoryItem[];
  worldRules: WorldRule[];
  relations: EntityRelation[];
  events: StoryEvent[];
  stateRecords: EntityStateRecord[];
  outline: GlobalOutline | null;
  outlineWorkflow: OutlineWorkflow | null;
  chapters: Chapter[];
  projectMemory: ProjectMemory | null;
  checks: CheckResult[];
  exports: ExportRecord[];
  versions: ContentVersion[];
  lastMessage: string;
}

const nowText = () => new Date().toLocaleString('zh-CN', { hour12: false });

const state = reactive<WorkspaceState>({
  projects: [],
  activeProjectId: null,
  modelConfigs: [
    {
      id: 1,
      provider: 'OpenAI 兼容',
      displayName: '默认 mock 模型',
      baseUrl: '',
      modelName: 'mock-novel-model',
      usageType: '通用生成',
      hasApiKey: true,
      defaultModel: true,
      enabled: true,
    },
  ],
  ideas: [],
  settingLibrary: null,
  settingWorkflow: null,
  characters: [],
  organizations: [],
  locations: [],
  items: [],
  worldRules: [],
  relations: [],
  events: [],
  stateRecords: [],
  outline: null,
  outlineWorkflow: null,
  chapters: [],
  projectMemory: null,
  checks: [],
  exports: [],
  versions: [],
  lastMessage: '请先在工作台选择或创建作品。',
});

let nextId = 100;

function next() {
  nextId += 1;
  return nextId;
}

function withFallback<T>(
  action: Promise<T>,
  fallback: () => T,
  message: string,
  isValid: (data: T) => boolean = () => true,
): Promise<T> {
  const useFallback = () => {
    const data = fallback();
    state.lastMessage = `${message}（当前使用前端 mock 数据）`;
    return data;
  };

  return action
    .then((data) => {
      if (!isValid(data)) {
        return useFallback();
      }
      state.lastMessage = message;
      return data;
    })
    .catch((error) => {
      if (error instanceof Error && error.message === 'NETWORK_UNAVAILABLE') {
        return useFallback();
      }
      state.lastMessage = error instanceof Error
        ? error.message.replace('BUSINESS_ERROR:', '')
        : '请求失败';
      throw error;
    });
}

function addVersion(targetType: string, targetId: number, actionType: string, summary: string) {
  state.versions.unshift({
    id: next(),
    projectId: state.activeProjectId ?? 0,
    targetType,
    targetId,
    actionType,
    summary,
    createdAt: nowText(),
  });
}

function setProjectStage(stage: WorkflowStage) {
  const project = state.projects.find((item) => item.id === state.activeProjectId);
  if (project) {
    project.stage = stage;
    project.updatedAt = nowText();
  }
}

function syncSettingLibraryMetrics() {
  if (!state.settingLibrary) {
    return;
  }
  state.settingLibrary.characterCount = state.characters.length;
  state.settingLibrary.organizationCount = state.organizations.length;
  state.settingLibrary.locationCount = state.locations.length;
  state.settingLibrary.itemCount = state.items.length;
  state.settingLibrary.ruleCount = state.worldRules.length;
  state.settingLibrary.relationCount = state.relations.length;
  state.settingLibrary.eventCount = state.events.length;
  state.settingLibrary.stateRecordCount = state.stateRecords.length;

  const counts = [
    state.settingLibrary.characterCount,
    state.settingLibrary.organizationCount,
    state.settingLibrary.locationCount,
    state.settingLibrary.itemCount,
    state.settingLibrary.ruleCount,
    state.settingLibrary.relationCount,
    state.settingLibrary.eventCount,
    state.settingLibrary.stateRecordCount,
  ];
  const filled = counts.filter((count) => count > 0).length;
  state.settingLibrary.completenessScore = Math.round((filled * 100) / 8);
}

function appendCreated<T extends { id: number }>(items: T[], created: T) {
  items.push(created);
  return created;
}

function replaceById<T extends { id: number }>(items: T[], updated: T) {
  const index = items.findIndex((item) => item.id === updated.id);
  if (index >= 0) {
    items[index] = updated;
  }
  return updated;
}

const activeProject = computed(() =>
  state.projects.find((project) => project.id === state.activeProjectId) ?? null,
);

const selectedIdea = computed(() => state.ideas.find((idea) => idea.selected) ?? null);

const canGenerateSetting = computed(() => Boolean(selectedIdea.value));
const canGenerateOutline = computed(() => Boolean(state.settingLibrary?.confirmed));
const canGenerateChapters = computed(() => Boolean(state.outline?.confirmed));
const canCheck = computed(() => state.chapters.some((chapter) => chapter.content.trim().length > 0));

const isIdeaList = (data: Idea[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.title && item.sellingPoint));
const isChapterList = (data: Chapter[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.title && item.outline));
const isCheckList = (data: CheckResult[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.type && item.summary));

export function useNovelWorkspace() {
  function resetProjectData() {
    state.ideas = [];
    state.settingLibrary = null;
    state.characters = [];
    state.organizations = [];
    state.locations = [];
    state.items = [];
    state.worldRules = [];
    state.relations = [];
    state.events = [];
    state.stateRecords = [];
    state.outline = null;
    state.chapters = [];
    state.projectMemory = null;
    state.checks = [];
    state.exports = [];
    state.versions = [];
  }

  async function loadProjects() {
    const projects = await withFallback(novelApi.listProjects(), () => [], '作品列表已加载');
    state.projects = projects;
    return projects;
  }

  function selectProject(projectId: number) {
    state.activeProjectId = projectId;
    resetProjectData();
    state.lastMessage = '作品已选中，请从侧边栏进入对应流程。';
  }

  async function loadIdeas() {
    if (!state.activeProjectId) {
      return [];
    }
    const ideas = await withFallback(novelApi.listIdeas(state.activeProjectId), () => state.ideas, '创意列表已加载', isIdeaList);
    state.ideas = ideas;
    return ideas;
  }

  async function loadSettingLibrary() {
    if (!state.activeProjectId) {
      return null;
    }
    const setting = await novelApi.getSettingLibrary(state.activeProjectId).catch(() => null);
    state.settingLibrary = setting;
    return setting;
  }

  async function loadLatestSettingWorkflow() {
    if (!state.activeProjectId) {
      return null;
    }
    const workflow = await novelApi.getLatestSettingWorkflow(state.activeProjectId).catch(() => null);
    state.settingWorkflow = workflow;
    return workflow;
  }

  async function loadSettingSnapshot() {
    if (!state.activeProjectId) {
      return null;
    }
    const snapshot = await novelApi.getSettingLibrarySnapshot(state.activeProjectId).catch(() => null);
    if (!snapshot) {
      return null;
    }
    state.settingLibrary = snapshot.settingLibrary;
    state.characters = snapshot.characters;
    state.organizations = snapshot.organizations;
    state.locations = snapshot.locations;
    state.items = snapshot.items;
    state.worldRules = snapshot.worldRules;
    state.relations = snapshot.relations;
    state.events = snapshot.events;
    state.stateRecords = snapshot.stateRecords;
    syncSettingLibraryMetrics();
    return snapshot;
  }

  async function loadCharacters() {
    if (!state.activeProjectId) {
      return [];
    }
    const characters = await withFallback(novelApi.listCharacters(state.activeProjectId), () => state.characters, '角色列表已加载');
    state.characters = characters;
    syncSettingLibraryMetrics();
    return characters;
  }

  async function loadOrganizations() {
    if (!state.activeProjectId) {
      return [];
    }
    const organizations = await withFallback(
      novelApi.listOrganizations(state.activeProjectId),
      () => state.organizations,
      '组织列表已加载',
    );
    state.organizations = organizations;
    syncSettingLibraryMetrics();
    return organizations;
  }

  async function loadLocations() {
    if (!state.activeProjectId) {
      return [];
    }
    const locations = await withFallback(novelApi.listLocations(state.activeProjectId), () => state.locations, '地点列表已加载');
    state.locations = locations;
    syncSettingLibraryMetrics();
    return locations;
  }

  async function loadItems() {
    if (!state.activeProjectId) {
      return [];
    }
    const items = await withFallback(novelApi.listItems(state.activeProjectId), () => state.items, '物品列表已加载');
    state.items = items;
    syncSettingLibraryMetrics();
    return items;
  }

  async function loadWorldRules() {
    if (!state.activeProjectId) {
      return [];
    }
    const worldRules = await withFallback(novelApi.listWorldRules(state.activeProjectId), () => state.worldRules, '规则列表已加载');
    state.worldRules = worldRules;
    syncSettingLibraryMetrics();
    return worldRules;
  }

  async function loadRelations() {
    if (!state.activeProjectId) {
      return [];
    }
    const relations = await withFallback(novelApi.listRelations(state.activeProjectId), () => state.relations, '关系列表已加载');
    state.relations = relations;
    syncSettingLibraryMetrics();
    return relations;
  }

  async function loadEvents() {
    if (!state.activeProjectId) {
      return [];
    }
    const events = await withFallback(novelApi.listEvents(state.activeProjectId), () => state.events, '事件列表已加载');
    state.events = events;
    syncSettingLibraryMetrics();
    return events;
  }

  async function loadStateRecords() {
    if (!state.activeProjectId) {
      return [];
    }
    const stateRecords = await withFallback(
      novelApi.listStateRecords(state.activeProjectId),
      () => state.stateRecords,
      '状态记录已加载',
    );
    state.stateRecords = stateRecords;
    syncSettingLibraryMetrics();
    return stateRecords;
  }

  async function createCharacter(payload: StoryCharacterRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const character = await withFallback(
      novelApi.createCharacter(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
          gender: payload.gender ?? '',
          ageText: payload.ageText ?? '',
          motivation: payload.motivation ?? '',
          relationshipSummary: payload.relationshipSummary ?? '',
          notes: payload.notes ?? '',
        };
      },
      '角色已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.characters, character);
  }

  async function updateCharacter(characterId: number, payload: StoryCharacterRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const character = await withFallback(
      novelApi.updateCharacter(state.activeProjectId, characterId, payload),
      () => {
        return {
          id: characterId,
          projectId: state.activeProjectId!,
          ...payload,
          gender: payload.gender ?? '',
          ageText: payload.ageText ?? '',
          motivation: payload.motivation ?? '',
          relationshipSummary: payload.relationshipSummary ?? '',
          notes: payload.notes ?? '',
        };
      },
      '角色已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.characters, character);
  }

  async function deleteCharacter(characterId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteCharacter(state.activeProjectId, characterId), () => undefined, '角色已删除');
    state.characters = state.characters.filter((item) => item.id !== characterId);
    syncSettingLibraryMetrics();
  }

  async function createOrganization(payload: OrganizationRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const organization = await withFallback(
      novelApi.createOrganization(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
          baseLocationId: payload.baseLocationId ?? null,
        };
      },
      '组织已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.organizations, organization);
  }

  async function updateOrganization(organizationId: number, payload: OrganizationRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const organization = await withFallback(
      novelApi.updateOrganization(state.activeProjectId, organizationId, payload),
      () => {
        return {
          id: organizationId,
          projectId: state.activeProjectId!,
          ...payload,
          baseLocationId: payload.baseLocationId ?? null,
        };
      },
      '组织已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.organizations, organization);
  }

  async function deleteOrganization(organizationId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteOrganization(state.activeProjectId, organizationId), () => undefined, '组织已删除');
    state.organizations = state.organizations.filter((item) => item.id !== organizationId);
    syncSettingLibraryMetrics();
  }

  async function createLocation(payload: StoryLocationRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const location = await withFallback(
      novelApi.createLocation(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
          parentLocationId: payload.parentLocationId ?? null,
          controllingOrgId: payload.controllingOrgId ?? null,
        };
      },
      '地点已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.locations, location);
  }

  async function updateLocation(locationId: number, payload: StoryLocationRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const location = await withFallback(
      novelApi.updateLocation(state.activeProjectId, locationId, payload),
      () => {
        return {
          id: locationId,
          projectId: state.activeProjectId!,
          ...payload,
          parentLocationId: payload.parentLocationId ?? null,
          controllingOrgId: payload.controllingOrgId ?? null,
        };
      },
      '地点已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.locations, location);
  }

  async function deleteLocation(locationId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteLocation(state.activeProjectId, locationId), () => undefined, '地点已删除');
    state.locations = state.locations.filter((item) => item.id !== locationId);
    syncSettingLibraryMetrics();
  }

  async function createWorldRule(payload: WorldRuleRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const worldRule = await withFallback(
      novelApi.createWorldRule(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
        };
      },
      '规则已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.worldRules, worldRule);
  }

  async function createItem(payload: StoryItemRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const item = await withFallback(
      novelApi.createItem(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
          ownerCharacterId: payload.ownerCharacterId ?? null,
          ownerOrgId: payload.ownerOrgId ?? null,
        };
      },
      '物品已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.items, item);
  }

  async function updateItem(itemId: number, payload: StoryItemRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const item = await withFallback(
      novelApi.updateItem(state.activeProjectId, itemId, payload),
      () => {
        return {
          id: itemId,
          projectId: state.activeProjectId!,
          ...payload,
          ownerCharacterId: payload.ownerCharacterId ?? null,
          ownerOrgId: payload.ownerOrgId ?? null,
        };
      },
      '物品已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.items, item);
  }

  async function deleteItem(itemId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteItem(state.activeProjectId, itemId), () => undefined, '物品已删除');
    state.items = state.items.filter((entry) => entry.id !== itemId);
    syncSettingLibraryMetrics();
  }

  async function updateWorldRule(ruleId: number, payload: WorldRuleRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const worldRule = await withFallback(
      novelApi.updateWorldRule(state.activeProjectId, ruleId, payload),
      () => {
        return {
          id: ruleId,
          projectId: state.activeProjectId!,
          ...payload,
        };
      },
      '规则已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.worldRules, worldRule);
  }

  async function deleteWorldRule(ruleId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteWorldRule(state.activeProjectId, ruleId), () => undefined, '规则已删除');
    state.worldRules = state.worldRules.filter((item) => item.id !== ruleId);
    syncSettingLibraryMetrics();
  }

  async function createRelation(payload: EntityRelationRequest) {
    if (!state.activeProjectId || payload.sourceId == null || payload.targetId == null) {
      return;
    }
    const sourceId = payload.sourceId;
    const targetId = payload.targetId;
    const relation = await withFallback(
      novelApi.createRelation(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          sourceType: payload.sourceType,
          sourceId,
          targetType: payload.targetType,
          targetId,
          relationType: payload.relationType,
          relationStatus: payload.relationStatus,
          strengthValue: payload.strengthValue ?? null,
          visibilityLevel: payload.visibilityLevel,
          note: payload.note,
          startEventId: payload.startEventId ?? null,
          endEventId: payload.endEventId ?? null,
        };
      },
      '关系已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.relations, relation);
  }

  async function createEvent(payload: StoryEventRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const event = await withFallback(
      novelApi.createEvent(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          ...payload,
          locationId: payload.locationId ?? null,
          chapterId: payload.chapterId ?? null,
        };
      },
      '事件已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.events, event);
  }

  async function createStateRecord(payload: EntityStateRecordRequest) {
    if (!state.activeProjectId || payload.entityId == null || payload.newValue == null) {
      return;
    }
    const entityId = payload.entityId;
    const newValue = payload.newValue;
    const stateRecord = await withFallback(
      novelApi.createStateRecord(state.activeProjectId, payload),
      () => {
        return {
          id: next(),
          projectId: state.activeProjectId!,
          entityType: payload.entityType,
          entityId,
          stateType: payload.stateType,
          oldValue: payload.oldValue ?? null,
          newValue,
          eventId: payload.eventId ?? null,
          chapterId: payload.chapterId ?? null,
          effectiveAt: payload.effectiveAt ?? null,
        };
      },
      '状态记录已创建',
    );
    syncSettingLibraryMetrics();
    return appendCreated(state.stateRecords, stateRecord);
  }

  async function updateRelation(relationId: number, payload: EntityRelationRequest) {
    if (!state.activeProjectId || payload.sourceId == null || payload.targetId == null) {
      return;
    }
    const sourceId = payload.sourceId;
    const targetId = payload.targetId;
    const relation = await withFallback(
      novelApi.updateRelation(state.activeProjectId, relationId, payload),
      () => {
        return {
          id: relationId,
          projectId: state.activeProjectId!,
          sourceType: payload.sourceType,
          sourceId,
          targetType: payload.targetType,
          targetId,
          relationType: payload.relationType,
          relationStatus: payload.relationStatus,
          strengthValue: payload.strengthValue ?? null,
          visibilityLevel: payload.visibilityLevel,
          note: payload.note,
          startEventId: payload.startEventId ?? null,
          endEventId: payload.endEventId ?? null,
        };
      },
      '关系已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.relations, relation);
  }

  async function updateEvent(eventId: number, payload: StoryEventRequest) {
    if (!state.activeProjectId) {
      return;
    }
    const event = await withFallback(
      novelApi.updateEvent(state.activeProjectId, eventId, payload),
      () => {
        return {
          id: eventId,
          projectId: state.activeProjectId!,
          ...payload,
          locationId: payload.locationId ?? null,
          chapterId: payload.chapterId ?? null,
        };
      },
      '事件已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.events, event);
  }

  async function updateStateRecord(recordId: number, payload: EntityStateRecordRequest) {
    if (!state.activeProjectId || payload.entityId == null || payload.newValue == null) {
      return;
    }
    const entityId = payload.entityId;
    const newValue = payload.newValue;
    const stateRecord = await withFallback(
      novelApi.updateStateRecord(state.activeProjectId, recordId, payload),
      () => {
        return {
          id: recordId,
          projectId: state.activeProjectId!,
          entityType: payload.entityType,
          entityId,
          stateType: payload.stateType,
          oldValue: payload.oldValue ?? null,
          newValue,
          eventId: payload.eventId ?? null,
          chapterId: payload.chapterId ?? null,
          effectiveAt: payload.effectiveAt ?? null,
        };
      },
      '状态记录已保存',
    );
    syncSettingLibraryMetrics();
    return replaceById(state.stateRecords, stateRecord);
  }

  async function deleteRelation(relationId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteRelation(state.activeProjectId, relationId), () => undefined, '关系已删除');
    state.relations = state.relations.filter((entry) => entry.id !== relationId);
    syncSettingLibraryMetrics();
  }

  async function deleteEvent(eventId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteEvent(state.activeProjectId, eventId), () => undefined, '事件已删除');
    state.events = state.events.filter((entry) => entry.id !== eventId);
    syncSettingLibraryMetrics();
  }

  async function deleteStateRecord(recordId: number) {
    if (!state.activeProjectId) {
      return;
    }
    await withFallback(novelApi.deleteStateRecord(state.activeProjectId, recordId), () => undefined, '状态记录已删除');
    state.stateRecords = state.stateRecords.filter((entry) => entry.id !== recordId);
    syncSettingLibraryMetrics();
  }

  async function loadOutline() {
    if (!state.activeProjectId) {
      return null;
    }
    const outline = await novelApi.getGlobalOutline(state.activeProjectId).catch(() => null);
    state.outline = outline;
    return outline;
  }

  async function loadLatestOutlineWorkflow() {
    if (!state.activeProjectId) {
      return null;
    }
    const workflow = await novelApi.getLatestOutlineWorkflow(state.activeProjectId).catch(() => null);
    state.outlineWorkflow = workflow;
    return workflow;
  }

  async function loadChapters() {
    if (!state.activeProjectId) {
      return [];
    }
    const chapters = await withFallback(novelApi.listChapters(state.activeProjectId), () => state.chapters, '章节列表已加载', isChapterList);
    state.chapters = chapters;
    return chapters;
  }

  async function loadProjectMemory() {
    if (!state.activeProjectId) {
      return null;
    }
    const memory = await novelApi.getProjectMemory(state.activeProjectId).catch(() => null);
    state.projectMemory = memory;
    return memory;
  }

  async function loadVersions() {
    if (!state.activeProjectId) {
      return [];
    }
    const versions = await withFallback(novelApi.listVersions(state.activeProjectId), () => state.versions, '版本记录已加载');
    state.versions = versions;
    return versions;
  }

  async function createProject(payload: ProjectCreateRequest) {
    const projectId = await withFallback(
      novelApi.createProject(payload),
      () => next(),
      '作品已创建',
    );
    await loadProjects().catch(() => undefined);
    const project = state.projects.find((item) => item.id === projectId) ?? {
      id: projectId,
      ...payload,
      stage: 'idea' as const,
      updatedAt: nowText(),
    };
    if (!state.projects.some((item) => item.id === project.id)) {
      state.projects.unshift(project);
    }
    state.activeProjectId = project.id;
    resetProjectData();
    addVersion('project', project.id, 'create', `创建作品《${project.title}》`);
    return project;
  }

  async function updateProject(projectId: number, payload: ProjectUpdateRequest) {
    await withFallback(
      novelApi.updateProject(projectId, payload),
      () => undefined,
      '作品信息已保存',
    );
    const index = state.projects.findIndex((item) => item.id === projectId);
    if (index >= 0) {
      state.projects[index] = {
        ...state.projects[index],
        ...payload,
        updatedAt: nowText(),
      };
    }
    addVersion('project', projectId, 'edit', '用户修改作品基础信息');
  }

  async function createModelConfig(payload: ModelConfigRequest) {
    const createdId = await withFallback(
      novelApi.createModelConfig(payload),
      () => next(),
      '模型配置已保存',
    );
    await loadModelConfigs().catch(() => undefined);
    return state.modelConfigs.find((item) => item.id === createdId);
  }

  async function loadModelConfigs() {
    const models = await withFallback(novelApi.listModelConfigs(), () => state.modelConfigs, '模型配置列表已加载');
    state.modelConfigs = models;
    return models;
  }

  async function updateModelConfig(id: number, payload: ModelConfigRequest) {
    await withFallback(
      novelApi.updateModelConfig(id, payload),
      () => undefined,
      '模型配置已修改',
    );
    await loadModelConfigs().catch(() => undefined);
    return state.modelConfigs.find((item) => item.id === id);
  }

  async function setDefaultModel(id: number) {
    const localModel = state.modelConfigs.find((item) => item.id === id);
    if (!localModel) {
      state.lastMessage = '未找到模型配置。';
      return;
    }
    await withFallback(
      novelApi.setDefaultModel(id),
      () => undefined,
      '默认模型已更新',
    );
    await loadModelConfigs().catch(() => undefined);
  }

  async function disableModelConfig(id: number) {
    await withFallback(
      novelApi.disableModelConfig(id),
      () => undefined,
      '模型配置已禁用',
    );
    await loadModelConfigs().catch(() => undefined);
    return state.modelConfigs.find((item) => item.id === id);
  }

  async function generateIdeas(suggestion = '', ideaCount = 3) {
    if (!state.activeProjectId) {
      return [];
    }
    const project = activeProject.value;
    const ideas = await withFallback(
      novelApi.generateIdeas(state.activeProjectId, suggestion, ideaCount),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: `${project?.genres.join('+') || '长篇'}方案 A：低门槛成长线`,
          sellingPoint: '主角目标清晰，从生活困境切入超凡世界，适合新手持续推进。',
          worldview: '现代都市表层正常，暗线存在资源、传承和势力分层。',
          mainConflict: '主角要保护现实生活，同时被迫参与隐藏秩序的资源争夺。',
          estimatedWords: '120万-180万字',
          score: 86,
          selected: false,
          content: '前期用都市事件建立代入感，中期展开修炼体系和势力矛盾，后期解决城市背后的传承危机。',
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: `${project?.genres.join('+') || '长篇'}方案 B：强冲突逆袭线`,
          sellingPoint: '冲突密度高，章节钩子明显，适合平台连载节奏。',
          worldview: '城市被多个隐秘组织切分，普通人只看到表面秩序。',
          mainConflict: '主角从被误解和压制开始，逐步揭开自己身世与旧盟约。',
          estimatedWords: '150万-220万字',
          score: 82,
          selected: false,
          content: '每卷围绕一个现实麻烦和一个修真危机并行推进，保持长篇扩展空间。',
        },
      ],
      '创意已生成',
      isIdeaList,
    );
    state.ideas = ideas;
    addVersion('idea', ideas[0]?.id ?? 0, 'generate', '生成多个创意方案');
    setProjectStage('idea');
    return ideas;
  }

  async function selectIdea(id: number) {
    await withFallback(novelApi.selectIdea(id), () => undefined, '创意已选定');
    await loadIdeas().catch(() => undefined);
    addVersion('idea', id, 'confirm', '选定创意方案');
    setProjectStage('setting');
  }

  async function rewriteIdea(id: number, suggestion: string) {
    const idea = state.ideas.find((item) => item.id === id);
    if (!idea) {
      return;
    }
    const projectId = idea.projectId;
    const rewritten = await withFallback(
      novelApi.rewriteIdea(id, suggestion),
      () => ({
        ...idea,
        sellingPoint: `${idea.sellingPoint} 已按意见强化：${suggestion || '增强长篇延展性'}`,
        score: Math.min(99, idea.score + 2),
      }),
      '创意已重生成',
    );
    Object.assign(idea, rewritten, { projectId });
    addVersion('idea', id, 'rewrite', `根据意见重生成创意：${suggestion || '未填写具体意见'}`);
  }

  async function updateIdea(idea: Idea) {
    const localIdea = state.ideas.find((item) => item.id === idea.id);
    if (!localIdea) {
      return;
    }
    localIdea.title = idea.title;
    localIdea.sellingPoint = idea.sellingPoint;
    localIdea.worldview = idea.worldview;
    localIdea.mainConflict = idea.mainConflict;
    localIdea.estimatedWords = idea.estimatedWords;
    localIdea.content = idea.content;
    await withFallback(novelApi.updateIdea(localIdea), () => undefined, '创意修改已保存');
    await loadIdeas().catch(() => undefined);
    addVersion('idea', idea.id, 'edit', '用户直接修改创意内容');
  }

  async function deleteIdea(id: number) {
    const idea = state.ideas.find((item) => item.id === id);
    if (!idea) {
      return;
    }
    await withFallback(novelApi.deleteIdea(id), () => undefined, '创意已删除');
    state.ideas = state.ideas.filter((item) => item.id !== id);
    addVersion('idea', id, 'delete', '删除创意方案');
  }

  async function generateSettingLibrary() {
    if (!canGenerateSetting.value || !state.activeProjectId) {
      state.lastMessage = '请先选定创意，再生成设定库。';
      return;
    }
    const setting = await withFallback(
      novelApi.generateSettingLibrary(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        sourceIdeaId: selectedIdea.value?.id ?? null,
        summary: '基于已选创意生成的结构化设定总览。',
        overview:
          '基于已选创意生成的结构化设定总览。\n\n建议继续补全角色、组织、地点、物品、规则、关系、事件与状态记录后，再确认设定库。',
        genreTemplate: activeProject.value?.platformTarget ?? '通用',
        status: 'generated',
        confirmed: false,
        confirmedAt: null,
        characterCount: 0,
        organizationCount: 0,
        locationCount: 0,
        itemCount: 0,
        ruleCount: 0,
        relationCount: 0,
        eventCount: 0,
        stateRecordCount: 0,
        completenessScore: 0,
      }),
      '设定库已生成',
      (data) => Boolean(data.overview || data.summary),
    );
    state.settingLibrary = setting;
    addVersion('setting_library', setting.id, 'generate', '生成设定库');
    return setting;
  }

  async function startSettingWorkflow() {
    if (!canGenerateSetting.value || !state.activeProjectId) {
      state.lastMessage = '请先选定创意，再启动设定生成流程。';
      return null;
    }
    const workflow = await withFallback(
      novelApi.startSettingWorkflow(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        sourceIdeaId: selectedIdea.value?.id ?? 0,
        status: 'blueprint_ready',
        blueprint: {
          corePremise: '基于已选创意生成设定蓝图。',
          mainConflict: selectedIdea.value?.mainConflict ?? '',
          worldPremise: selectedIdea.value?.worldview ?? '',
          immutableRules: ['核心前提确认前不进入正式落库'],
        },
        draft: {},
        checks: {},
        blueprintConfirmedAt: null,
        committedAt: null,
      }),
      '设定蓝图已生成',
    );
    state.settingWorkflow = workflow;
    return workflow;
  }

  async function approveSettingWorkflowBlueprint() {
    if (!state.settingWorkflow) {
      state.lastMessage = '请先生成设定蓝图。';
      return null;
    }
    const workflow = await withFallback(
      novelApi.approveSettingWorkflowBlueprint(state.settingWorkflow.id),
      () => ({
        ...state.settingWorkflow!,
        status: 'draft_ready',
        draft: { overview: '基于蓝图生成的设定草案。' },
        checks: { passed: true, issues: [] },
        blueprintConfirmedAt: new Date().toISOString(),
      }),
      '设定草案已生成',
    );
    state.settingWorkflow = workflow;
    return workflow;
  }

  async function regenerateSettingWorkflowModule(moduleKey: string) {
    if (!state.settingWorkflow) {
      state.lastMessage = '请先生成设定草案。';
      return null;
    }
    const workflow = await withFallback(
      novelApi.regenerateSettingWorkflowModule(state.settingWorkflow.id, moduleKey),
      () => state.settingWorkflow!,
      '设定模块已重生成',
    );
    state.settingWorkflow = workflow;
    return workflow;
  }

  async function commitSettingWorkflow() {
    if (!state.settingWorkflow) {
      state.lastMessage = '请先生成设定草案。';
      return null;
    }
    const setting = await withFallback(
      novelApi.commitSettingWorkflow(state.settingWorkflow.id),
      () => state.settingLibrary ?? ({
        id: next(),
        projectId: state.activeProjectId!,
        sourceIdeaId: state.settingWorkflow?.sourceIdeaId ?? null,
        summary: '设定生成流程已提交。',
        overview: '设定生成流程已提交。',
        genreTemplate: activeProject.value?.platformTarget ?? '通用',
        status: 'generated',
        confirmed: false,
        confirmedAt: null,
        characterCount: state.characters.length,
        organizationCount: state.organizations.length,
        locationCount: state.locations.length,
        itemCount: state.items.length,
        ruleCount: state.worldRules.length,
        relationCount: state.relations.length,
        eventCount: state.events.length,
        stateRecordCount: state.stateRecords.length,
        completenessScore: 0,
      }),
      '设定草案已写入设定库',
    );
    state.settingLibrary = setting;
    if (state.settingWorkflow) {
      state.settingWorkflow.status = 'committed';
      state.settingWorkflow.committedAt = new Date().toISOString();
    }
    await loadSettingSnapshot().catch(() => undefined);
    return setting;
  }

  async function updateSettingLibrary(overview: string) {
    if (!state.settingLibrary) {
      return;
    }
    state.settingLibrary.overview = overview;
    state.settingLibrary.summary = overview;
    state.settingLibrary.status = 'edited';
    const updated = await withFallback(
      novelApi.updateSettingLibrary(state.settingLibrary.id, overview),
      () => state.settingLibrary!,
      '设定库修改已保存',
    );
    Object.assign(state.settingLibrary, updated);
    addVersion('setting_library', state.settingLibrary.id, 'edit', '用户直接修改设定库');
  }

  async function confirmSettingLibrary() {
    if (!state.settingLibrary) {
      return;
    }
    await withFallback(
      novelApi.confirmSettingLibrary(state.settingLibrary.id),
      () => ({ ...state.settingLibrary!, confirmed: true, status: 'confirmed' }),
      '设定库已确认',
    );
    state.settingLibrary.confirmed = true;
    state.settingLibrary.status = 'confirmed';
    addVersion('setting_library', state.settingLibrary.id, 'confirm', '确认设定库');
    setProjectStage('outline');
  }

  async function generateOutline() {
    if (!canGenerateOutline.value || !state.activeProjectId) {
      state.lastMessage = '请先确认设定库，再生成全局大纲。';
      return;
    }
    const outline = await withFallback(
      novelApi.generateGlobalOutline(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        confirmed: false,
        content:
          '全局主线：主角从城市小事件切入，逐步发现修真秩序正在影响普通生活。\n第一卷：建立主角目标、能力代价和第一个敌对势力。\n第二卷：扩大地点和关系网，揭露资源争夺。\n第三卷：回收旧物伏笔，完成阶段性胜利并打开更大冲突。',
      }),
      '全局大纲已生成',
      (data) => Boolean(data.content),
    );
    state.outline = outline;
    addVersion('global_outline', outline.id, 'generate', '生成全局大纲');
    return outline;
  }

  async function startOutlineWorkflow() {
    if (!canGenerateOutline.value || !state.activeProjectId) {
      state.lastMessage = '请先确认设定库，再启动大纲生成流程。';
      return null;
    }
    const workflow = await withFallback(
      novelApi.startOutlineWorkflow(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        settingLibraryId: state.settingLibrary?.id ?? 0,
        status: 'draft_ready',
        draft: {
          globalOutline: { title: '全局大纲', content: '基于设定库生成的大纲草案。' },
          volumes: [],
          arcs: [],
          chapters: [],
        },
        checks: { passed: true, issues: [] },
        committedAt: null,
      }),
      '大纲草案已生成',
    );
    state.outlineWorkflow = workflow;
    return workflow;
  }

  async function commitOutlineWorkflow() {
    if (!state.outlineWorkflow) {
      state.lastMessage = '请先生成大纲草案。';
      return null;
    }
    const outline = await withFallback(
      novelApi.commitOutlineWorkflow(state.outlineWorkflow.id),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        confirmed: true,
        content: String((state.outlineWorkflow?.draft?.globalOutline as any)?.content ?? '大纲草案已提交。'),
      }),
      '大纲草案已提交',
    );
    state.outline = outline;
    state.outlineWorkflow.status = 'committed';
    state.outlineWorkflow.committedAt = new Date().toISOString();
    await loadChapters().catch(() => undefined);
    setProjectStage('chapter');
    return outline;
  }

  async function updateOutline(content: string) {
    if (!state.outline) {
      return;
    }
    state.outline.content = content;
    await withFallback(
      novelApi.saveGlobalOutline(state.outline.id, content),
      () => undefined,
      '全局大纲修改已保存',
    );
    await loadOutline().catch(() => undefined);
    addVersion('global_outline', state.outline.id, 'edit', '用户直接修改全局大纲');
  }

  async function confirmOutline() {
    if (!state.outline) {
      return;
    }
    await withFallback(
      novelApi.confirmGlobalOutline(state.outline.id),
      () => undefined,
      '全局大纲已确认',
    );
    state.outline.confirmed = true;
    await loadOutline().catch(() => undefined);
    addVersion('global_outline', state.outline.id, 'confirm', '确认全局大纲');
    setProjectStage('chapter');
  }

  async function generateChapterOutlines() {
    if (!canGenerateChapters.value || !state.activeProjectId) {
      state.lastMessage = '请先确认全局大纲，再生成章节。';
      return [];
    }
    const chapters = await withFallback(
      novelApi.generateChapterOutlines(state.activeProjectId),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 1 章 旧物',
          outline: '主角在现实麻烦中发现家中旧物异常，结尾留下超凡线索。',
          content: '',
          status: 'outline_ready' as const,
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 2 章 第一次代价',
          outline: '主角尝试使用能力解决问题，却发现每次使用都会带来现实代价。',
          content: '',
          status: 'outline_ready' as const,
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 3 章 城市背面',
          outline: '隐藏势力露出一角，主角意识到自己不是唯一接触传承的人。',
          content: '',
          status: 'outline_ready' as const,
        },
      ],
      '章节大纲已生成',
      isChapterList,
    );
    state.chapters = chapters;
    addVersion('chapter_outline', chapters[0]?.id ?? 0, 'generate', '生成章节大纲');
    setProjectStage('chapter');
    return chapters;
  }

  async function generateChapterContent(chapterId: number, suggestion = '') {
    const chapter = state.chapters.find((item) => item.id === chapterId);
    if (!chapter) {
      return;
    }
    const rewriting = Boolean(chapter.content);
    const fallback = () => ({
      ...chapter,
      content: `${chapter.title}\n\n${chapter.outline}\n\n主角按照大纲推进当前事件，场景中保留人物目标、冲突升级和章末钩子。${suggestion ? `\n\n重生成意见：${suggestion}` : ''}`,
      status: 'content_ready' as const,
    });
    const handleProgressEvent = (event: { type: string; message?: string }) => {
      if (event.type === 'queued') {
        const match = event.message?.match(/\d+/);
        state.lastMessage = match ? `前方还有 ${match[0]} 个章节任务，正在等待上一章完成...` : '正在等待上一章完成...';
      }
      if (event.type === 'started') {
        state.lastMessage = '章节正文生成中...';
      }
      if (event.type === 'post_processing') {
        state.lastMessage = '正文已生成，正在整理章节摘要、事实和记忆...';
      }
    };
    try {
      chapter.content = '';
      chapter.status = 'content_ready';
      state.lastMessage = '章节正文生成中...';
      await (rewriting
        ? novelApi.streamRewriteChapterContent(chapterId, suggestion, (event) => {
          handleProgressEvent(event);
          if (event.type === 'chunk') {
            chapter.content += event.content ?? '';
          }
          if (event.type === 'done' && event.chapter) {
            Object.assign(chapter, event.chapter);
          }
          if (event.type === 'error') {
            throw new Error(event.message || '章节流式生成失败');
          }
        })
        : novelApi.streamGenerateChapterContent(chapter, suggestion, (event) => {
          handleProgressEvent(event);
          if (event.type === 'chunk') {
            chapter.content += event.content ?? '';
          }
          if (event.type === 'done' && event.chapter) {
            Object.assign(chapter, event.chapter);
          }
          if (event.type === 'error') {
            throw new Error(event.message || '章节流式生成失败');
          }
        }));
      if (!chapter.content) {
        Object.assign(chapter, fallback());
        state.lastMessage = '章节正文已生成（当前使用前端 mock 数据）';
      } else {
        state.lastMessage = '章节正文已生成';
      }
    } catch (error) {
      if (error instanceof Error && error.message === 'NETWORK_UNAVAILABLE') {
        Object.assign(chapter, fallback());
        state.lastMessage = '章节正文已生成（当前使用前端 mock 数据）';
      } else {
        state.lastMessage = error instanceof Error
          ? error.message.replace('BUSINESS_ERROR:', '')
          : '请求失败';
        throw error;
      }
    }
    state.projectMemory = await novelApi.getProjectMemory(chapter.projectId).catch(() => state.projectMemory);
    addVersion('chapter', chapterId, rewriting ? 'rewrite' : 'generate', suggestion || (rewriting ? '重生成章节正文' : '生成章节正文'));
    setProjectStage('check');
  }

  async function updateChapterContent(chapterId: number, content: string) {
    const chapter = state.chapters.find((item) => item.id === chapterId);
    if (!chapter) {
      return;
    }
    chapter.content = content;
    chapter.status = 'edited';
    const updated = await withFallback(
      novelApi.updateChapter(chapterId, content),
      () => chapter,
      '章节正文修改已保存',
    );
    Object.assign(chapter, updated);
    state.projectMemory = await novelApi.getProjectMemory(chapter.projectId).catch(() => state.projectMemory);
    addVersion('chapter', chapterId, 'edit', '用户直接修改章节正文');
  }

  async function createCheck() {
    if (!canCheck.value || !state.activeProjectId) {
      state.lastMessage = '请先生成至少一章正文，再创建检查。';
      return [];
    }
    const checks = await withFallback(
      novelApi.createCheck(state.activeProjectId),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          type: '人物状态',
          severity: '中' as const,
          summary: '主角能力代价需要在后续章节持续记录，避免忽然消失。',
          suggestion: '在章节编辑时补一句身体或现实成本。',
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          type: 'AI 痕迹提示',
          severity: '中' as const,
          summary: '部分句子像概述，缺少具体动作和场景细节。',
          suggestion: '把抽象总结改成可见动作、对白或环境反馈。',
        },
      ],
      '检查结果已生成',
      isCheckList,
    );
    state.checks = checks;
    addVersion('check_result', checks[0]?.id ?? 0, 'generate', '生成连续性和风格检查结果');
    setProjectStage('export');
    return checks;
  }

  async function createExport(format: ExportRecord['format'], scope: string) {
    if (!state.activeProjectId) {
      return;
    }
    try {
      const result = await novelApi.createExport(state.activeProjectId, format, scope);
      const blob = new Blob([result.content], {
        type: format === 'markdown' ? 'text/markdown;charset=utf-8' : 'text/plain;charset=utf-8',
      });
      const url = URL.createObjectURL(blob);
      const anchor = document.createElement('a');
      anchor.href = url;
      anchor.download = result.fileName;
      anchor.click();
      URL.revokeObjectURL(url);
      const record = {
        id: next(),
        projectId: state.activeProjectId,
        format,
        scope,
        fileName: result.fileName,
        status: 'created' as const,
      };
      state.exports.unshift(record);
      state.lastMessage = '导出文件已下载';
      addVersion('export', record.id, 'export', `导出 ${format.toUpperCase()} 文件`);
      return record;
    } catch (error) {
      state.lastMessage = error instanceof Error ? error.message : '导出失败';
      throw error;
    }
  }

  return {
    state,
    activeProject,
    selectedIdea,
    canGenerateSetting,
    canGenerateOutline,
    canGenerateChapters,
    canCheck,
    loadProjects,
    selectProject,
    loadIdeas,
    loadSettingLibrary,
    loadLatestSettingWorkflow,
    loadSettingSnapshot,
    loadCharacters,
    loadOrganizations,
    loadLocations,
    loadItems,
    loadWorldRules,
    loadRelations,
    loadEvents,
    loadStateRecords,
    loadOutline,
    loadLatestOutlineWorkflow,
    loadChapters,
    loadProjectMemory,
    loadVersions,
    createProject,
    updateProject,
    loadModelConfigs,
    createModelConfig,
    updateModelConfig,
    setDefaultModel,
    disableModelConfig,
    generateIdeas,
    selectIdea,
    rewriteIdea,
    updateIdea,
    deleteIdea,
    generateSettingLibrary,
    startSettingWorkflow,
    approveSettingWorkflowBlueprint,
    regenerateSettingWorkflowModule,
    commitSettingWorkflow,
    updateSettingLibrary,
    confirmSettingLibrary,
    createCharacter,
    updateCharacter,
    deleteCharacter,
    createOrganization,
    updateOrganization,
    deleteOrganization,
    createLocation,
    updateLocation,
    deleteLocation,
    createItem,
    updateItem,
    deleteItem,
    createWorldRule,
    updateWorldRule,
    deleteWorldRule,
    createRelation,
    updateRelation,
    deleteRelation,
    createEvent,
    updateEvent,
    deleteEvent,
    createStateRecord,
    updateStateRecord,
    deleteStateRecord,
    generateOutline,
    startOutlineWorkflow,
    commitOutlineWorkflow,
    updateOutline,
    confirmOutline,
    generateChapterOutlines,
    generateChapterContent,
    updateChapterContent,
    createCheck,
    createExport,
  };
}
