<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';

import PageShell from '@/components/PageShell.vue';
import type {
  EntityRelationRequest,
  EntityStateRecordRequest,
  OrganizationRequest,
  SettingModuleSummary,
  StoryCharacterRequest,
  StoryEventRequest,
  StoryItemRequest,
  StoryLocationRequest,
  WorldRuleRequest,
} from '@/api/types';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const moduleDefinitions: Array<Pick<SettingModuleSummary, 'key' | 'label' | 'description'>> = [
  { key: 'characters', label: '角色', description: '管理主角、配角、反派和群像角色的稳定档案。' },
  { key: 'organizations', label: '组织', description: '管理势力、家族、公司、学校等群体设定。' },
  { key: 'locations', label: '地点', description: '管理地理空间、重要场景、据点和上下级地点关系。' },
  { key: 'items', label: '物品', description: '管理关键物品、装备、资产和归属关系。' },
  { key: 'rules', label: '规则', description: '管理世界、社会、能力或技术规则及其限制。' },
  { key: 'relations', label: '关系', description: '管理人物、组织、地点和物品之间的结构化关系。' },
  { key: 'events', label: '事件', description: '管理关键剧情事件、时间节点和重要转折。' },
  { key: 'states', label: '状态', description: '记录实体状态变化，保证长篇过程中的持续一致性。' },
];

const entityTypeOptions = [
  { value: 'character', label: '角色' },
  { value: 'organization', label: '组织' },
  { value: 'location', label: '地点' },
  { value: 'item', label: '物品' },
];

const {
  state,
  activeProject,
  canGenerateSetting,
  loadIdeas,
  loadLatestSettingWorkflow,
  loadSettingSnapshot,
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
} = useNovelWorkspace();

const activeModuleKey = ref<'overview' | SettingModuleSummary['key']>('overview');
const activeWorkflowModuleKey = ref('overview');
const overviewDraft = ref('');
const isSaving = ref(false);
const workflowBusy = ref(false);

const selectedCharacterId = ref<number | null>(null);
const selectedOrganizationId = ref<number | null>(null);
const selectedLocationId = ref<number | null>(null);
const selectedItemId = ref<number | null>(null);
const selectedWorldRuleId = ref<number | null>(null);
const selectedRelationId = ref<number | null>(null);
const selectedEventId = ref<number | null>(null);
const selectedStateRecordId = ref<number | null>(null);

const stateOldValueText = ref('{}');
const stateNewValueText = ref('{}');

const emptyCharacter = (): StoryCharacterRequest => ({
  name: '',
  aliases: [],
  roleType: 'supporting',
  narrativeRole: 'supporting',
  identity: '',
  publicIdentity: '',
  gender: '',
  ageText: '',
  personality: '',
  motivation: '',
  background: '',
  coreGoal: '',
  innerNeed: '',
  coreFlaw: '',
  bottomLine: '',
  skillsSummary: '',
  secretNotes: '',
  relationshipSummary: '',
  importance: 0,
  status: 'active',
  firstAppearedChapterId: null,
  notes: '',
});

const emptyOrganization = (): OrganizationRequest => ({
  name: '',
  organizationType: 'faction',
  publicMission: '',
  realGoal: '',
  controlledResources: '',
  powerScope: '',
  baseLocationId: null,
  entryRules: '',
  status: 'active',
  notes: '',
});

const emptyLocation = (): StoryLocationRequest => ({
  name: '',
  locationType: 'place',
  parentLocationId: null,
  description: '',
  keyFeatures: '',
  entryConditions: '',
  availableResources: '',
  controllingOrgId: null,
  riskLevel: 'medium',
  rules: '',
  notes: '',
});

const emptyItem = (): StoryItemRequest => ({
  name: '',
  itemType: 'item',
  description: '',
  usageRules: '',
  limitations: '',
  rarity: '',
  ownerCharacterId: null,
  ownerOrgId: null,
  status: 'available',
  notes: '',
});

const emptyWorldRule = (): WorldRuleRequest => ({
  name: '',
  ruleType: 'general',
  description: '',
  triggerCondition: '',
  effectResult: '',
  limitations: '',
  cost: '',
  exceptions: '',
  visibilityLevel: 'public',
  importance: 0,
  examples: '',
  notes: '',
});

const emptyRelation = (): EntityRelationRequest => ({
  sourceType: 'character',
  sourceId: null,
  targetType: 'character',
  targetId: null,
  relationType: 'knows',
  relationStatus: 'active',
  strengthValue: null,
  visibilityLevel: 'public',
  note: '',
  startEventId: null,
  endEventId: null,
});

const emptyEvent = (): StoryEventRequest => ({
  name: '',
  eventType: 'story',
  description: '',
  eventTimeText: '',
  locationId: null,
  chapterId: null,
  planned: true,
  importance: 0,
});

const emptyStateRecord = (): EntityStateRecordRequest => ({
  entityType: 'character',
  entityId: null,
  stateType: '',
  oldValue: {},
  newValue: {},
  eventId: null,
  chapterId: null,
  effectiveAt: null,
});

const characterDraft = reactive<StoryCharacterRequest>(emptyCharacter());
const organizationDraft = reactive<OrganizationRequest>(emptyOrganization());
const locationDraft = reactive<StoryLocationRequest>(emptyLocation());
const itemDraft = reactive<StoryItemRequest>(emptyItem());
const worldRuleDraft = reactive<WorldRuleRequest>(emptyWorldRule());
const relationDraft = reactive<EntityRelationRequest>(emptyRelation());
const eventDraft = reactive<StoryEventRequest>(emptyEvent());
const stateRecordDraft = reactive<EntityStateRecordRequest>(emptyStateRecord());

const countByModule = computed(() => ({
  characters: state.characters.length || state.settingLibrary?.characterCount || 0,
  organizations: state.organizations.length || state.settingLibrary?.organizationCount || 0,
  locations: state.locations.length || state.settingLibrary?.locationCount || 0,
  items: state.items.length || state.settingLibrary?.itemCount || 0,
  rules: state.worldRules.length || state.settingLibrary?.ruleCount || 0,
  relations: state.relations.length || state.settingLibrary?.relationCount || 0,
  events: state.events.length || state.settingLibrary?.eventCount || 0,
  states: state.stateRecords.length || state.settingLibrary?.stateRecordCount || 0,
}));

const workflowIssueText = computed(() => {
  const issues = state.settingWorkflow?.checks?.issues ?? [];
  return issues.length ? issues.join('；') : '暂无阻断问题';
});

const workflowBlueprintText = computed(() => {
  if (!state.settingWorkflow?.blueprint) {
    return '暂无蓝图';
  }
  return JSON.stringify(state.settingWorkflow.blueprint, null, 2);
});

const workflowDraftOverview = computed(() => {
  const overview = state.settingWorkflow?.draft?.overview;
  return typeof overview === 'string' && overview.trim() ? overview : '草案尚未生成';
});

const workflowStatusText = computed(() => {
  switch (state.settingWorkflow?.status) {
    case 'blueprint_ready':
      return '蓝图待确认';
    case 'draft_ready':
      return '草案待提交';
    case 'check_failed':
      return '检查未通过';
    case 'committed':
      return '已提交';
    default:
      return '未启动';
  }
});

const canRegenerateWorkflowModule = computed(() =>
  Boolean(state.settingWorkflow && ['draft_ready', 'check_failed', 'committed'].includes(state.settingWorkflow.status)),
);

const workflowModules = [
  { key: 'overview', label: '总览' },
  { key: 'rules', label: '规则' },
  { key: 'characters', label: '角色' },
  { key: 'organizations', label: '组织' },
  { key: 'locations', label: '地点' },
  { key: 'items', label: '物品' },
  { key: 'relations', label: '关系' },
  { key: 'events', label: '事件' },
  { key: 'states', label: '状态' },
];

const activeWorkflowModuleLabel = computed(() =>
  workflowModules.find((item) => item.key === activeWorkflowModuleKey.value)?.label ?? '总览',
);

const activeWorkflowModulePreview = computed(() => {
  const draft = state.settingWorkflow?.draft ?? {};
  const value = draft[activeWorkflowModuleKey.value];
  if (value == null || value === '') {
    return '草案尚未生成';
  }
  return typeof value === 'string' ? value : JSON.stringify(value, null, 2);
});

const settingModules = computed<SettingModuleSummary[]>(() =>
  moduleDefinitions.map((module) => ({
    ...module,
    count: countByModule.value[module.key],
  })),
);

const selectedCharacter = computed(() => state.characters.find((item) => item.id === selectedCharacterId.value) ?? null);
const selectedOrganization = computed(() => state.organizations.find((item) => item.id === selectedOrganizationId.value) ?? null);
const selectedLocation = computed(() => state.locations.find((item) => item.id === selectedLocationId.value) ?? null);
const selectedItem = computed(() => state.items.find((item) => item.id === selectedItemId.value) ?? null);
const selectedWorldRule = computed(() => state.worldRules.find((item) => item.id === selectedWorldRuleId.value) ?? null);
const selectedRelation = computed(() => state.relations.find((item) => item.id === selectedRelationId.value) ?? null);
const selectedEvent = computed(() => state.events.find((item) => item.id === selectedEventId.value) ?? null);
const selectedStateRecord = computed(() => state.stateRecords.find((item) => item.id === selectedStateRecordId.value) ?? null);

const availableParentLocations = computed(() =>
  state.locations.filter((item) => item.id !== selectedLocationId.value),
);

function entityOptions(type: string) {
  if (type === 'character') {
    return state.characters.map((item) => ({ id: item.id, name: item.name }));
  }
  if (type === 'organization') {
    return state.organizations.map((item) => ({ id: item.id, name: item.name }));
  }
  if (type === 'location') {
    return state.locations.map((item) => ({ id: item.id, name: item.name }));
  }
  return state.items.map((item) => ({ id: item.id, name: item.name }));
}

function entityLabel(type: string, id: number | null | undefined) {
  if (id == null) {
    return '未设置';
  }
  const match = entityOptions(type).find((item) => item.id === id);
  return match?.name ?? `${type}#${id}`;
}

function parseJson(text: string) {
  if (!text.trim()) {
    return {};
  }
  return JSON.parse(text) as Record<string, unknown>;
}

watch(
  () => state.settingLibrary?.overview,
  (overview) => {
    overviewDraft.value = overview ?? '';
  },
  { immediate: true },
);

watch(selectedCharacter, (value) => {
  Object.assign(characterDraft, value ? { ...emptyCharacter(), ...value } : emptyCharacter());
}, { immediate: true });

watch(selectedOrganization, (value) => {
  Object.assign(organizationDraft, value ? { ...emptyOrganization(), ...value } : emptyOrganization());
}, { immediate: true });

watch(selectedLocation, (value) => {
  Object.assign(locationDraft, value ? { ...emptyLocation(), ...value } : emptyLocation());
}, { immediate: true });

watch(selectedItem, (value) => {
  Object.assign(itemDraft, value ? { ...emptyItem(), ...value } : emptyItem());
}, { immediate: true });

watch(selectedWorldRule, (value) => {
  Object.assign(worldRuleDraft, value ? { ...emptyWorldRule(), ...value } : emptyWorldRule());
}, { immediate: true });

watch(selectedRelation, (value) => {
  Object.assign(relationDraft, value ? { ...emptyRelation(), ...value } : emptyRelation());
}, { immediate: true });

watch(selectedEvent, (value) => {
  Object.assign(eventDraft, value ? { ...emptyEvent(), ...value } : emptyEvent());
}, { immediate: true });

watch(selectedStateRecord, (value) => {
  Object.assign(stateRecordDraft, value ? { ...emptyStateRecord(), ...value } : emptyStateRecord());
  stateOldValueText.value = JSON.stringify(value?.oldValue ?? {}, null, 2);
  stateNewValueText.value = JSON.stringify(value?.newValue ?? {}, null, 2);
}, { immediate: true });

onMounted(() => {
  void loadIdeas().catch(() => undefined);
  void loadLatestSettingWorkflow().catch(() => undefined);
  void loadSettingSnapshot().catch(() => undefined);
});

async function withWorkflow(action: () => Promise<unknown>) {
  workflowBusy.value = true;
  try {
    await action();
  } finally {
    workflowBusy.value = false;
  }
}

function startWorkflow() {
  void withWorkflow(startSettingWorkflow);
}

function approveWorkflowBlueprint() {
  void withWorkflow(approveSettingWorkflowBlueprint);
}

function commitWorkflowDraft() {
  void withWorkflow(commitSettingWorkflow);
}

function regenerateWorkflowModule(moduleKey: string) {
  void withWorkflow(() => regenerateSettingWorkflowModule(moduleKey));
}

function saveOverview() {
  if (state.settingLibrary) {
    void updateSettingLibrary(overviewDraft.value);
  }
}

function startCharacter() {
  selectedCharacterId.value = null;
  Object.assign(characterDraft, emptyCharacter());
}

function startOrganization() {
  selectedOrganizationId.value = null;
  Object.assign(organizationDraft, emptyOrganization());
}

function startLocation() {
  selectedLocationId.value = null;
  Object.assign(locationDraft, emptyLocation());
}

function startItem() {
  selectedItemId.value = null;
  Object.assign(itemDraft, emptyItem());
}

function startWorldRule() {
  selectedWorldRuleId.value = null;
  Object.assign(worldRuleDraft, emptyWorldRule());
}

function startRelation() {
  selectedRelationId.value = null;
  Object.assign(relationDraft, emptyRelation());
}

function startEvent() {
  selectedEventId.value = null;
  Object.assign(eventDraft, emptyEvent());
}

function startStateRecord() {
  selectedStateRecordId.value = null;
  Object.assign(stateRecordDraft, emptyStateRecord());
  stateOldValueText.value = '{}';
  stateNewValueText.value = '{}';
}

async function withSaving(action: () => Promise<void>) {
  isSaving.value = true;
  try {
    await action();
  } finally {
    isSaving.value = false;
  }
}

async function saveCharacterDraft() {
  if (!characterDraft.name.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedCharacterId.value) {
      await updateCharacter(selectedCharacterId.value, { ...characterDraft });
    } else {
      const created = await createCharacter({ ...characterDraft });
      selectedCharacterId.value = created?.id ?? null;
    }
  });
}

async function saveOrganizationDraft() {
  if (!organizationDraft.name.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedOrganizationId.value) {
      await updateOrganization(selectedOrganizationId.value, { ...organizationDraft });
    } else {
      const created = await createOrganization({ ...organizationDraft });
      selectedOrganizationId.value = created?.id ?? null;
    }
  });
}

async function saveLocationDraft() {
  if (!locationDraft.name.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedLocationId.value) {
      await updateLocation(selectedLocationId.value, { ...locationDraft });
    } else {
      const created = await createLocation({ ...locationDraft });
      selectedLocationId.value = created?.id ?? null;
    }
  });
}

async function saveItemDraft() {
  if (!itemDraft.name.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedItemId.value) {
      await updateItem(selectedItemId.value, { ...itemDraft });
    } else {
      const created = await createItem({ ...itemDraft });
      selectedItemId.value = created?.id ?? null;
    }
  });
}

async function saveWorldRuleDraft() {
  if (!worldRuleDraft.name.trim() || !worldRuleDraft.description.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedWorldRuleId.value) {
      await updateWorldRule(selectedWorldRuleId.value, { ...worldRuleDraft });
    } else {
      const created = await createWorldRule({ ...worldRuleDraft });
      selectedWorldRuleId.value = created?.id ?? null;
    }
  });
}

async function saveRelationDraft() {
  if (relationDraft.sourceId == null || relationDraft.targetId == null || !relationDraft.relationType.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedRelationId.value) {
      await updateRelation(selectedRelationId.value, { ...relationDraft });
    } else {
      const created = await createRelation({ ...relationDraft });
      selectedRelationId.value = created?.id ?? null;
    }
  });
}

async function saveEventDraft() {
  if (!eventDraft.name.trim()) {
    return;
  }
  await withSaving(async () => {
    if (selectedEventId.value) {
      await updateEvent(selectedEventId.value, { ...eventDraft });
    } else {
      const created = await createEvent({ ...eventDraft });
      selectedEventId.value = created?.id ?? null;
    }
  });
}

async function saveStateRecordDraft() {
  if (stateRecordDraft.entityId == null || !stateRecordDraft.stateType.trim()) {
    return;
  }

  let oldValue: Record<string, unknown>;
  let newValue: Record<string, unknown>;
  try {
    oldValue = parseJson(stateOldValueText.value);
    newValue = parseJson(stateNewValueText.value);
  } catch {
    state.lastMessage = '状态快照 JSON 格式不正确，请检查后再保存。';
    return;
  }

  await withSaving(async () => {
    const payload: EntityStateRecordRequest = {
      ...stateRecordDraft,
      oldValue,
      newValue,
    };
    if (selectedStateRecordId.value) {
      await updateStateRecord(selectedStateRecordId.value, payload);
    } else {
      const created = await createStateRecord(payload);
      selectedStateRecordId.value = created?.id ?? null;
    }
  });
}

async function removeCharacter() {
  if (selectedCharacterId.value) {
    await deleteCharacter(selectedCharacterId.value);
    startCharacter();
  }
}

async function removeOrganization() {
  if (selectedOrganizationId.value) {
    await deleteOrganization(selectedOrganizationId.value);
    startOrganization();
  }
}

async function removeLocation() {
  if (selectedLocationId.value) {
    await deleteLocation(selectedLocationId.value);
    startLocation();
  }
}

async function removeItem() {
  if (selectedItemId.value) {
    await deleteItem(selectedItemId.value);
    startItem();
  }
}

async function removeWorldRule() {
  if (selectedWorldRuleId.value) {
    await deleteWorldRule(selectedWorldRuleId.value);
    startWorldRule();
  }
}

async function removeRelation() {
  if (selectedRelationId.value) {
    await deleteRelation(selectedRelationId.value);
    startRelation();
  }
}

async function removeEvent() {
  if (selectedEventId.value) {
    await deleteEvent(selectedEventId.value);
    startEvent();
  }
}

async function removeStateRecord() {
  if (selectedStateRecordId.value) {
    await deleteStateRecord(selectedStateRecordId.value);
    startStateRecord();
  }
}
</script>

<template>
  <PageShell title="设定库" description="把创意拆成可维护的结构化事实，先固定稳定档案，再追踪事件和状态变化。">
    <template #actions>
      <div class="toolbar">
        <button
          class="toolbar__button toolbar__button--ghost"
          type="button"
          :disabled="!state.settingLibrary || state.settingLibrary.confirmed"
          @click="confirmSettingLibrary"
        >
          确认设定库
        </button>
      </div>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和编辑设定库。</p>
    </div>

    <template v-else>
      <section class="card stack">
        <div class="card__row">
          <div>
            <h3 class="section-title">设定生成流程</h3>
            <p class="helper-text">先生成设定蓝图，确认后生成结构化草案，检查通过后再提交到设定库。</p>
          </div>
          <span class="badge meta-pill--soft">{{ workflowStatusText }}</span>
        </div>

        <div class="toolbar">
          <button class="toolbar__button" type="button" :disabled="!canGenerateSetting || workflowBusy" @click="startWorkflow">
            生成设定蓝图
          </button>
          <button
            class="toolbar__button toolbar__button--ghost"
            type="button"
            :disabled="workflowBusy || state.settingWorkflow?.status !== 'blueprint_ready'"
            @click="approveWorkflowBlueprint"
          >
            确认蓝图并生成草案
          </button>
          <button
            class="toolbar__button toolbar__button--ghost"
            type="button"
            :disabled="workflowBusy || state.settingWorkflow?.status !== 'draft_ready'"
            @click="commitWorkflowDraft"
          >
            提交草案到设定库
          </button>
        </div>

        <div class="grid grid--three">
          <div class="metric">
            <div class="metric__label">设定蓝图</div>
            <pre class="workflow-preview">{{ workflowBlueprintText }}</pre>
          </div>
          <div class="metric">
            <div class="metric__label">草案总览</div>
            <p class="helper-text">{{ workflowDraftOverview }}</p>
          </div>
          <div class="metric">
            <div class="metric__label">检查结果</div>
            <p class="helper-text">{{ workflowIssueText }}</p>
          </div>
        </div>

        <div class="stack">
          <div class="card__row">
            <div>
              <h3 class="section-title">流程详情</h3>
              <p class="helper-text">选择模块查看当前草案，也可以只重生成这个模块。</p>
            </div>
            <span class="badge meta-pill--soft">{{ activeWorkflowModuleLabel }}</span>
          </div>

          <div class="toolbar">
            <button
              v-for="module in workflowModules"
              :key="module.key"
              class="toolbar__button toolbar__button--ghost"
              :class="{ 'toolbar__button--active': activeWorkflowModuleKey === module.key }"
              type="button"
              @click="activeWorkflowModuleKey = module.key"
            >
              {{ module.label }}
            </button>
          </div>

          <div class="card__row">
            <button
              class="toolbar__button"
              type="button"
              :disabled="workflowBusy || !canRegenerateWorkflowModule"
              @click="regenerateWorkflowModule(activeWorkflowModuleKey)"
            >
              重生成当前模块
            </button>
          </div>

          <pre class="workflow-preview workflow-preview--detail">{{ activeWorkflowModulePreview }}</pre>
        </div>
      </section>

      <div class="grid grid--three">
        <section class="card">
          <div class="card__title">当前状态</div>
          <div class="status-line">
            <span class="badge" :class="{ 'badge--ok': state.settingLibrary?.confirmed }">
              {{ state.settingLibrary?.confirmed ? '已确认' : '待确认' }}
            </span>
            <span>{{ state.settingLibrary?.status || 'draft' }}</span>
          </div>
          <p class="helper-text">{{ state.lastMessage }}</p>
        </section>

        <section class="card">
          <div class="card__title">完整度</div>
          <div class="metric">
            <div class="metric__label">模块覆盖率</div>
            <div class="metric__value">{{ state.settingLibrary?.completenessScore ?? 0 }}%</div>
          </div>
        </section>

        <section class="card">
          <div class="card__title">来源创意</div>
          <p class="helper-text">
            {{ state.settingLibrary?.sourceIdeaId ? `来源创意 ID：${state.settingLibrary.sourceIdeaId}` : '当前还没有记录来源创意。' }}
          </p>
          <p class="helper-text">题材模板：{{ state.settingLibrary?.genreTemplate || activeProject.platformTarget || '通用' }}</p>
        </section>
      </div>

      <div class="grid setting-workspace">
        <section class="card setting-workspace__nav">
          <div class="card__title">模块导航</div>
          <div class="stack">
            <button
              class="setting-nav-button"
              :class="{ 'setting-nav-button--active': activeModuleKey === 'overview' }"
              type="button"
              @click="activeModuleKey = 'overview'"
            >
              <span>总览</span>
              <span class="badge meta-pill--soft">核心</span>
            </button>

            <button
              v-for="module in settingModules"
              :key="module.key"
              class="setting-nav-button"
              :class="{ 'setting-nav-button--active': activeModuleKey === module.key }"
              type="button"
              @click="activeModuleKey = module.key"
            >
              <span>{{ module.label }}</span>
              <span class="badge meta-pill--soft">{{ module.count }}</span>
            </button>
          </div>
        </section>

        <section class="card setting-workspace__detail">
          <div v-if="activeModuleKey === 'overview'" class="stack">
            <div class="card__row">
              <div>
                <h3 class="section-title">设定总览</h3>
                <p class="helper-text">先写清作品的大前提，再把稳定事实拆进下方模块。</p>
              </div>
              <span class="badge meta-pill--soft">{{ state.settingLibrary?.status || 'draft' }}</span>
            </div>

            <label class="field">
              <span>总览说明</span>
              <textarea
                v-model="overviewDraft"
                class="text-editor"
                rows="12"
                placeholder="写下故事的时代、核心冲突、不可违背的前提，以及后续要补全的设定。"
                @blur="saveOverview"
              ></textarea>
            </label>

            <div class="grid grid--three">
              <div v-for="module in settingModules" :key="module.key" class="metric">
                <div class="metric__label">{{ module.label }}</div>
                <div class="metric__value">{{ module.count }}</div>
                <div class="helper-text">{{ module.description }}</div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'characters'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">角色档案</h3>
                <p class="helper-text">静态档案只记录角色底色，动态变化留给后续状态记录。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startCharacter">新建角色</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="character in state.characters"
                  :key="character.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedCharacterId === character.id }"
                  type="button"
                  @click="selectedCharacterId = character.id"
                >
                  <strong>{{ character.name }}</strong>
                  <span>{{ character.narrativeRole || 'supporting' }}</span>
                </button>
                <div v-if="!state.characters.length" class="empty-state empty-state--compact">还没有角色，先创建一个核心人物。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>角色名称</span><input v-model="characterDraft.name" /></label>
                  <label class="field"><span>叙事角色</span><select v-model="characterDraft.narrativeRole"><option value="protagonist">主角</option><option value="supporting">配角</option><option value="antagonist">对手</option><option value="ensemble">群像</option></select></label>
                  <label class="field"><span>核心身份</span><input v-model="characterDraft.identity" /></label>
                  <label class="field"><span>对外身份</span><input v-model="characterDraft.publicIdentity" /></label>
                  <label class="field"><span>性格底色</span><textarea v-model="characterDraft.personality" rows="3" /></label>
                  <label class="field"><span>外在目标</span><textarea v-model="characterDraft.coreGoal" rows="3" /></label>
                  <label class="field"><span>内在需求</span><textarea v-model="characterDraft.innerNeed" rows="3" /></label>
                  <label class="field"><span>核心缺陷</span><textarea v-model="characterDraft.coreFlaw" rows="3" /></label>
                  <label class="field"><span>底线</span><textarea v-model="characterDraft.bottomLine" rows="3" /></label>
                  <label class="field"><span>背景身世</span><textarea v-model="characterDraft.background" rows="3" /></label>
                  <label class="field"><span>能力或技能</span><textarea v-model="characterDraft.skillsSummary" rows="3" /></label>
                  <label class="field"><span>作者秘密</span><textarea v-model="characterDraft.secretNotes" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !characterDraft.name.trim()" @click="saveCharacterDraft">保存角色</button>
                  <button v-if="selectedCharacter" class="toolbar__button toolbar__button--danger" type="button" @click="removeCharacter">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'organizations'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">组织档案</h3>
                <p class="helper-text">把公开目标、真实目标和资源边界拆开，后续冲突会更耐写。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startOrganization">新建组织</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="organization in state.organizations"
                  :key="organization.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedOrganizationId === organization.id }"
                  type="button"
                  @click="selectedOrganizationId = organization.id"
                >
                  <strong>{{ organization.name }}</strong>
                  <span>{{ organization.organizationType || 'faction' }}</span>
                </button>
                <div v-if="!state.organizations.length" class="empty-state empty-state--compact">还没有组织，先创建一个主要势力。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>组织名称</span><input v-model="organizationDraft.name" /></label>
                  <label class="field"><span>组织类型</span><input v-model="organizationDraft.organizationType" placeholder="势力、公司、家族、学校" /></label>
                  <label class="field"><span>公开使命</span><textarea v-model="organizationDraft.publicMission" rows="3" /></label>
                  <label class="field"><span>真实目标</span><textarea v-model="organizationDraft.realGoal" rows="3" /></label>
                  <label class="field"><span>控制资源</span><textarea v-model="organizationDraft.controlledResources" rows="3" /></label>
                  <label class="field"><span>影响范围</span><textarea v-model="organizationDraft.powerScope" rows="3" /></label>
                  <label class="field"><span>加入条件</span><textarea v-model="organizationDraft.entryRules" rows="3" /></label>
                  <label class="field"><span>备注</span><textarea v-model="organizationDraft.notes" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !organizationDraft.name.trim()" @click="saveOrganizationDraft">保存组织</button>
                  <button v-if="selectedOrganization" class="toolbar__button toolbar__button--danger" type="button" @click="removeOrganization">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'locations'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">地点档案</h3>
                <p class="helper-text">地点不只是地图点位，更要写清它能提供什么、限制什么、谁在控制它。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startLocation">新建地点</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="location in state.locations"
                  :key="location.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedLocationId === location.id }"
                  type="button"
                  @click="selectedLocationId = location.id"
                >
                  <strong>{{ location.name }}</strong>
                  <span>{{ location.locationType || 'place' }}</span>
                </button>
                <div v-if="!state.locations.length" class="empty-state empty-state--compact">还没有地点，先创建一个关键场景。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>地点名称</span><input v-model="locationDraft.name" /></label>
                  <label class="field"><span>地点类型</span><input v-model="locationDraft.locationType" placeholder="城市、宗门、学校、据点" /></label>
                  <label class="field">
                    <span>上级地点</span>
                    <select v-model="locationDraft.parentLocationId">
                      <option :value="null">无</option>
                      <option v-for="location in availableParentLocations" :key="location.id" :value="location.id">{{ location.name }}</option>
                    </select>
                  </label>
                  <label class="field">
                    <span>控制组织</span>
                    <select v-model="locationDraft.controllingOrgId">
                      <option :value="null">未指定</option>
                      <option v-for="organization in state.organizations" :key="organization.id" :value="organization.id">{{ organization.name }}</option>
                    </select>
                  </label>
                  <label class="field"><span>地点描述</span><textarea v-model="locationDraft.description" rows="3" /></label>
                  <label class="field"><span>关键特征</span><textarea v-model="locationDraft.keyFeatures" rows="3" /></label>
                  <label class="field"><span>进入条件</span><textarea v-model="locationDraft.entryConditions" rows="3" /></label>
                  <label class="field"><span>可用资源</span><textarea v-model="locationDraft.availableResources" rows="3" /></label>
                  <label class="field"><span>风险等级</span><select v-model="locationDraft.riskLevel"><option value="low">低</option><option value="medium">中</option><option value="high">高</option></select></label>
                  <label class="field"><span>地点规则</span><textarea v-model="locationDraft.rules" rows="3" /></label>
                  <label class="field"><span>备注</span><textarea v-model="locationDraft.notes" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !locationDraft.name.trim()" @click="saveLocationDraft">保存地点</button>
                  <button v-if="selectedLocation" class="toolbar__button toolbar__button--danger" type="button" @click="removeLocation">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'items'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">物品档案</h3>
                <p class="helper-text">物品最好写清用途、限制、稀有度和当前归属，这样伏笔和资源流转会更稳。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startItem">新建物品</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="item in state.items"
                  :key="item.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedItemId === item.id }"
                  type="button"
                  @click="selectedItemId = item.id"
                >
                  <strong>{{ item.name }}</strong>
                  <span>{{ item.itemType || 'item' }}</span>
                </button>
                <div v-if="!state.items.length" class="empty-state empty-state--compact">还没有物品，先创建一个关键道具或资源。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>物品名称</span><input v-model="itemDraft.name" /></label>
                  <label class="field"><span>物品类型</span><input v-model="itemDraft.itemType" placeholder="道具、神器、车辆、文件" /></label>
                  <label class="field"><span>当前持有人物</span><select v-model="itemDraft.ownerCharacterId"><option :value="null">未指定</option><option v-for="character in state.characters" :key="character.id" :value="character.id">{{ character.name }}</option></select></label>
                  <label class="field"><span>当前持有组织</span><select v-model="itemDraft.ownerOrgId"><option :value="null">未指定</option><option v-for="organization in state.organizations" :key="organization.id" :value="organization.id">{{ organization.name }}</option></select></label>
                  <label class="field"><span>物品描述</span><textarea v-model="itemDraft.description" rows="3" /></label>
                  <label class="field"><span>使用规则</span><textarea v-model="itemDraft.usageRules" rows="3" /></label>
                  <label class="field"><span>限制条件</span><textarea v-model="itemDraft.limitations" rows="3" /></label>
                  <label class="field"><span>稀有度</span><input v-model="itemDraft.rarity" placeholder="普通、稀有、传说" /></label>
                  <label class="field"><span>状态</span><select v-model="itemDraft.status"><option value="available">可用</option><option value="lost">遗失</option><option value="destroyed">损毁</option><option value="sealed">封存</option></select></label>
                  <label class="field"><span>备注</span><textarea v-model="itemDraft.notes" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !itemDraft.name.trim()" @click="saveItemDraft">保存物品</button>
                  <button v-if="selectedItem" class="toolbar__button toolbar__button--danger" type="button" @click="removeItem">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'rules'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">规则档案</h3>
                <p class="helper-text">规则要同时说明触发条件、效果、限制和代价，这样写到长篇后段也不容易崩。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startWorldRule">新建规则</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="worldRule in state.worldRules"
                  :key="worldRule.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedWorldRuleId === worldRule.id }"
                  type="button"
                  @click="selectedWorldRuleId = worldRule.id"
                >
                  <strong>{{ worldRule.name }}</strong>
                  <span>{{ worldRule.ruleType || 'general' }}</span>
                </button>
                <div v-if="!state.worldRules.length" class="empty-state empty-state--compact">还没有规则，先写一条最不可动摇的世界前提。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>规则名称</span><input v-model="worldRuleDraft.name" /></label>
                  <label class="field"><span>规则类型</span><input v-model="worldRuleDraft.ruleType" placeholder="力量、社会、科技、禁忌" /></label>
                  <label class="field"><span>可见性</span><select v-model="worldRuleDraft.visibilityLevel"><option value="public">公开</option><option value="hidden">隐藏</option><option value="secret">秘密</option></select></label>
                  <label class="field"><span>重要度</span><input v-model.number="worldRuleDraft.importance" type="number" min="0" /></label>
                  <label class="field"><span>规则内容</span><textarea v-model="worldRuleDraft.description" rows="3" /></label>
                  <label class="field"><span>触发条件</span><textarea v-model="worldRuleDraft.triggerCondition" rows="3" /></label>
                  <label class="field"><span>作用结果</span><textarea v-model="worldRuleDraft.effectResult" rows="3" /></label>
                  <label class="field"><span>限制条件</span><textarea v-model="worldRuleDraft.limitations" rows="3" /></label>
                  <label class="field"><span>代价或副作用</span><textarea v-model="worldRuleDraft.cost" rows="3" /></label>
                  <label class="field"><span>例外情况</span><textarea v-model="worldRuleDraft.exceptions" rows="3" /></label>
                  <label class="field"><span>示例</span><textarea v-model="worldRuleDraft.examples" rows="3" /></label>
                  <label class="field"><span>备注</span><textarea v-model="worldRuleDraft.notes" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !worldRuleDraft.name.trim() || !worldRuleDraft.description.trim()" @click="saveWorldRuleDraft">保存规则</button>
                  <button v-if="selectedWorldRule" class="toolbar__button toolbar__button--danger" type="button" @click="removeWorldRule">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'relations'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">关系档案</h3>
                <p class="helper-text">关系单独结构化后，人物网、势力网和物品归属都能持续演化而不混乱。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startRelation">新建关系</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="relation in state.relations"
                  :key="relation.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedRelationId === relation.id }"
                  type="button"
                  @click="selectedRelationId = relation.id"
                >
                  <strong>{{ entityLabel(relation.sourceType, relation.sourceId) }} → {{ entityLabel(relation.targetType, relation.targetId) }}</strong>
                  <span>{{ relation.relationType }}</span>
                </button>
                <div v-if="!state.relations.length" class="empty-state empty-state--compact">还没有关系，先创建一条最重要的人物或势力连接。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>源实体类型</span><select v-model="relationDraft.sourceType"><option v-for="option in entityTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
                  <label class="field"><span>源实体</span><select v-model="relationDraft.sourceId"><option :value="null">未指定</option><option v-for="option in entityOptions(relationDraft.sourceType)" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
                  <label class="field"><span>目标实体类型</span><select v-model="relationDraft.targetType"><option v-for="option in entityTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
                  <label class="field"><span>目标实体</span><select v-model="relationDraft.targetId"><option :value="null">未指定</option><option v-for="option in entityOptions(relationDraft.targetType)" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
                  <label class="field"><span>关系类型</span><input v-model="relationDraft.relationType" placeholder="ally、enemy、owns、member_of、located_in" /></label>
                  <label class="field"><span>关系状态</span><select v-model="relationDraft.relationStatus"><option value="active">进行中</option><option value="ended">已结束</option><option value="hidden">隐藏</option></select></label>
                  <label class="field"><span>可见性</span><select v-model="relationDraft.visibilityLevel"><option value="public">公开</option><option value="hidden">隐藏</option><option value="secret">秘密</option></select></label>
                  <label class="field"><span>强度值</span><input v-model.number="relationDraft.strengthValue" type="number" /></label>
                  <label class="field field--full"><span>说明</span><textarea v-model="relationDraft.note" rows="3" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || relationDraft.sourceId == null || relationDraft.targetId == null || !relationDraft.relationType.trim()" @click="saveRelationDraft">保存关系</button>
                  <button v-if="selectedRelation" class="toolbar__button toolbar__button--danger" type="button" @click="removeRelation">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'events'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">事件档案</h3>
                <p class="helper-text">关键转折、联盟破裂、身份揭露、夺宝、出逃，都应单独记成事件。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startEvent">新建事件</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="event in state.events"
                  :key="event.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedEventId === event.id }"
                  type="button"
                  @click="selectedEventId = event.id"
                >
                  <strong>{{ event.name }}</strong>
                  <span>{{ event.eventType || 'story' }}</span>
                </button>
                <div v-if="!state.events.length" class="empty-state empty-state--compact">还没有事件，先创建一个关键转折点。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>事件名称</span><input v-model="eventDraft.name" /></label>
                  <label class="field"><span>事件类型</span><input v-model="eventDraft.eventType" placeholder="reveal、alliance、betrayal、death、transfer" /></label>
                  <label class="field"><span>时间文本</span><input v-model="eventDraft.eventTimeText" placeholder="第一卷末、第三卷中期、历法X年" /></label>
                  <label class="field"><span>重要度</span><input v-model.number="eventDraft.importance" type="number" min="0" /></label>
                  <label class="field"><span>关联地点</span><select v-model="eventDraft.locationId"><option :value="null">未指定</option><option v-for="location in state.locations" :key="location.id" :value="location.id">{{ location.name }}</option></select></label>
                  <label class="field"><span>章节 ID</span><input v-model.number="eventDraft.chapterId" type="number" min="1" /></label>
                  <label class="field"><span>是否计划中</span><select v-model="eventDraft.planned"><option :value="true">是</option><option :value="false">否</option></select></label>
                  <label class="field field--full"><span>事件描述</span><textarea v-model="eventDraft.description" rows="4" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || !eventDraft.name.trim()" @click="saveEventDraft">保存事件</button>
                  <button v-if="selectedEvent" class="toolbar__button toolbar__button--danger" type="button" @click="removeEvent">删除</button>
                </div>
              </div>
            </div>
          </div>

          <div v-else-if="activeModuleKey === 'states'" class="setting-editor">
            <div class="card__row">
              <div>
                <h3 class="section-title">状态记录</h3>
                <p class="helper-text">这里不改底层档案，只记录过程中的状态变化，例如位置、伤势、所有权、身份暴露。</p>
              </div>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="startStateRecord">新建状态记录</button>
            </div>
            <div class="setting-editor__body">
              <div class="setting-list">
                <button
                  v-for="record in state.stateRecords"
                  :key="record.id"
                  class="setting-list__item"
                  :class="{ 'setting-list__item--active': selectedStateRecordId === record.id }"
                  type="button"
                  @click="selectedStateRecordId = record.id"
                >
                  <strong>{{ entityLabel(record.entityType, record.entityId) }}</strong>
                  <span>{{ record.stateType }}</span>
                </button>
                <div v-if="!state.stateRecords.length" class="empty-state empty-state--compact">还没有状态记录，先记一条最重要的变化。</div>
              </div>
              <div class="setting-form">
                <div class="form-grid">
                  <label class="field"><span>实体类型</span><select v-model="stateRecordDraft.entityType"><option v-for="option in entityTypeOptions" :key="option.value" :value="option.value">{{ option.label }}</option></select></label>
                  <label class="field"><span>实体</span><select v-model="stateRecordDraft.entityId"><option :value="null">未指定</option><option v-for="option in entityOptions(stateRecordDraft.entityType)" :key="option.id" :value="option.id">{{ option.name }}</option></select></label>
                  <label class="field"><span>状态类型</span><input v-model="stateRecordDraft.stateType" placeholder="location、injury、ownership、identity、control" /></label>
                  <label class="field"><span>关联事件</span><select v-model="stateRecordDraft.eventId"><option :value="null">未指定</option><option v-for="event in state.events" :key="event.id" :value="event.id">{{ event.name }}</option></select></label>
                  <label class="field"><span>章节 ID</span><input v-model.number="stateRecordDraft.chapterId" type="number" min="1" /></label>
                  <label class="field"><span>生效时间</span><input v-model="stateRecordDraft.effectiveAt" type="datetime-local" /></label>
                  <label class="field field--full"><span>旧状态 JSON</span><textarea v-model="stateOldValueText" rows="5" /></label>
                  <label class="field field--full"><span>新状态 JSON</span><textarea v-model="stateNewValueText" rows="5" /></label>
                </div>
                <div class="card__row">
                  <button class="toolbar__button" type="button" :disabled="isSaving || stateRecordDraft.entityId == null || !stateRecordDraft.stateType.trim()" @click="saveStateRecordDraft">保存状态记录</button>
                  <button v-if="selectedStateRecord" class="toolbar__button toolbar__button--danger" type="button" @click="removeStateRecord">删除</button>
                </div>
              </div>
            </div>
          </div>
        </section>
      </div>
    </template>
  </PageShell>
</template>
