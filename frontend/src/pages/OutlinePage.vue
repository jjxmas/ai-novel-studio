<script setup lang="ts">
import { computed, onMounted, ref, watch } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const outlineLevels = [
  { key: 'global', title: '全局大纲', hint: '总方向、总冲突、总节奏' },
  { key: 'volume', title: '分卷大纲', hint: '每一卷的目标和转折' },
  { key: 'arc', title: '剧情单元', hint: '中段推进单元，方便长篇控制' },
  { key: 'chapter', title: '章节大纲', hint: '每章目标、冲突和结尾钩子' },
];
const {
  state,
  activeProject,
  canGenerateOutline,
  canGenerateChapters,
  loadSettingLibrary,
  loadOutline,
  loadLatestOutlineWorkflow,
  loadChapters,
  loadModelConfigs,
  startOutlineWorkflow,
  commitOutlineWorkflow,
  continueChapterOutlines,
  updateOutline,
  confirmOutline,
} = useNovelWorkspace();

const activeLevel = ref<string | null>(null);
const workflowBusy = ref(false);
const continuationBusy = ref(false);
const continuationCount = ref<10 | 20 | 50>(10);
const continuationInstruction = ref('');
const continuationModelConfigId = ref<number | null>(null);

const enabledModels = computed(() => state.modelConfigs.filter((model) => model.enabled));
const lastChapterNo = computed(() => Math.max(0, ...state.chapters.map((chapter) => chapter.chapterNo ?? 0)));
const continuationStartChapterNo = computed(() => lastChapterNo.value + 1);
const continuationEndChapterNo = computed(() => lastChapterNo.value + continuationCount.value);
const continuationProgressText = computed(() => {
  if (!continuationBusy.value) {
    return `将追加第 ${continuationStartChapterNo.value}-${continuationEndChapterNo.value} 章，已有章节不会被覆盖。`;
  }
  return `正在生成第 ${continuationStartChapterNo.value}-${continuationEndChapterNo.value} 章，共 ${Math.ceil(continuationCount.value / 10)} 批。`;
});

const outlineWorkflowStatusText = computed(() => {
  switch (state.outlineWorkflow?.status) {
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

const outlineWorkflowGlobalText = computed(() => {
  const globalOutline = state.outlineWorkflow?.draft?.globalOutline as { content?: string } | undefined;
  return globalOutline?.content || state.outline?.content || '暂无全局大纲草案';
});

const outlineWorkflowVolumes = computed<Array<Record<string, any>>>(() => {
  const volumes = state.outlineWorkflow?.draft?.volumes;
  if (Array.isArray(volumes)) {
    return volumes as Array<Record<string, any>>;
  }
  return state.outline?.volumes ?? [];
});

const outlineWorkflowArcs = computed<Array<Record<string, any>>>(() => {
  const arcs = state.outlineWorkflow?.draft?.arcs;
  return Array.isArray(arcs) ? arcs as Array<Record<string, any>> : [];
});

const outlineWorkflowChapters = computed<Array<Record<string, any>>>(() => {
  const chapters = state.outlineWorkflow?.draft?.chapters;
  return Array.isArray(chapters) ? chapters as Array<Record<string, any>> : [];
});

const outlineWorkflowIssueText = computed(() => {
  const issues = state.outlineWorkflow?.checks?.issues ?? [];
  return issues.length ? issues.join('；') : '暂无阻断问题';
});

function editOutline(event: Event) {
  if (!state.outline) {
    return;
  }
  state.outline.content = (event.target as HTMLTextAreaElement).value;
}

function saveOutline() {
  if (state.outline) {
    updateOutline(state.outline.content);
  }
}

async function submitContinueChapterOutlines() {
  if (continuationModelConfigId.value == null) {
    state.lastMessage = '请选择用于续写大纲的模型。';
    return;
  }
  continuationBusy.value = true;
  try {
    await continueChapterOutlines(
      continuationCount.value,
      continuationModelConfigId.value,
      continuationInstruction.value,
    );
    continuationInstruction.value = '';
  } finally {
    continuationBusy.value = false;
  }
}

async function withWorkflow(action: () => Promise<unknown>) {
  workflowBusy.value = true;
  try {
    await action();
  } finally {
    workflowBusy.value = false;
  }
}

function startWorkflow() {
  void withWorkflow(startOutlineWorkflow);
}

function commitWorkflow() {
  void withWorkflow(commitOutlineWorkflow);
}

onMounted(() => {
  void loadSettingLibrary().catch(() => undefined);
  void loadOutline().catch(() => undefined);
  void loadLatestOutlineWorkflow().catch(() => undefined);
  void loadModelConfigs().catch(() => undefined);
});

watch(enabledModels, (models) => {
  if (models.length === 0) {
    continuationModelConfigId.value = null;
    return;
  }
  if (!models.some((model) => model.id === continuationModelConfigId.value)) {
    continuationModelConfigId.value = (models.find((model) => model.defaultModel) ?? models[0]).id;
  }
}, { immediate: true });

watch(activeLevel, (level) => {
  if (level === 'chapter') {
    void loadChapters().catch(() => undefined);
  }
});
</script>

<template>
  <PageShell
    title="大纲页"
    description="从全局结构持续推进到可写作的章节计划。"
  >
    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和确认大纲。</p>
    </div>

    <section v-else-if="!activeLevel" class="card">
      <div class="card__title">大纲层级</div>
      <div class="stack">
        <article
          v-for="item in outlineLevels"
          :key="item.key"
          class="list-item list-item--clickable"
          @click="activeLevel = item.key"
        >
          <div>
            <div class="list-item__title">{{ item.title }}</div>
            <div class="list-item__text">{{ item.hint }}</div>
          </div>
          <span
            class="badge"
            :class="{
              'badge--ok':
                (item.key === 'global' && state.outline?.confirmed)
                || (item.key === 'volume' && outlineWorkflowVolumes.length > 0)
                || (item.key === 'arc' && outlineWorkflowArcs.length > 0),
              'badge--warn': (item.key === 'volume' && outlineWorkflowVolumes.length === 0) || (item.key === 'arc' && outlineWorkflowArcs.length === 0),
            }"
          >
            {{
              item.key === 'global' && state.outline?.confirmed
                ? '已确认'
                : item.key === 'volume' && outlineWorkflowVolumes.length > 0
                  ? '已生成'
                  : item.key === 'arc' && outlineWorkflowArcs.length > 0
                    ? '已生成'
                    : item.key === 'chapter' && state.chapters.length > 0
                  ? '已生成'
                  : item.key === 'volume' || item.key === 'arc'
                    ? '预留'
                    : '可操作'
            }}
          </span>
        </article>
      </div>
    </section>

    <div v-else class="outline-detail-layout">
      <div class="card detail-header">
        <div>
          <div class="card__title">{{ outlineLevels.find((item) => item.key === activeLevel)?.title }}</div>
          <p class="helper-text">{{ outlineLevels.find((item) => item.key === activeLevel)?.hint }}</p>
        </div>
        <button class="toolbar__button toolbar__button--ghost" type="button" @click="activeLevel = null">
          返回列表
        </button>
      </div>

      <section v-if="activeLevel === 'global'" class="card">
        <div class="stack">
          <div class="card__row">
            <div>
              <h3 class="section-title">大纲生成流程</h3>
              <p class="helper-text">基于已确认设定库生成全局大纲、分卷大纲、第一卷剧情单元和首批章节大纲。</p>
            </div>
            <span class="badge meta-pill--soft">{{ outlineWorkflowStatusText }}</span>
          </div>

          <div class="toolbar">
            <button
              class="toolbar__button"
              type="button"
              :disabled="!activeProject || !canGenerateOutline || workflowBusy"
              @click="startWorkflow"
            >
              生成大纲草案
            </button>
            <button
              class="toolbar__button toolbar__button--ghost"
              type="button"
              :disabled="workflowBusy || state.outlineWorkflow?.status !== 'draft_ready'"
              @click="commitWorkflow"
            >
              提交草案到大纲
            </button>
          </div>

          <div class="grid grid--two">
            <div class="metric">
              <div class="metric__label">全局主线</div>
              <pre class="workflow-preview workflow-preview--detail">{{ outlineWorkflowGlobalText }}</pre>
            </div>
            <div class="metric">
              <div class="metric__label">检查结果</div>
              <p class="helper-text">{{ outlineWorkflowIssueText }}</p>
            </div>
          </div>

          <div class="grid grid--three">
            <div class="metric">
              <div class="metric__label">分卷</div>
              <div class="metric__value">{{ outlineWorkflowVolumes.length }}</div>
            </div>
            <div class="metric">
              <div class="metric__label">剧情单元</div>
              <div class="metric__value">{{ outlineWorkflowArcs.length }}</div>
            </div>
            <div class="metric">
              <div class="metric__label">首批章节</div>
              <div class="metric__value">{{ outlineWorkflowChapters.length }}</div>
            </div>
          </div>

          <div v-if="outlineWorkflowChapters.length" class="stack">
            <article v-for="chapter in outlineWorkflowChapters" :key="String(chapter.chapterNo)" class="list-item">
              <div>
                <div class="list-item__title">第{{ chapter.chapterNo }}章 {{ chapter.title }}</div>
                <div class="list-item__text">{{ chapter.outline }}</div>
              </div>
              <span class="badge">草案</span>
            </article>
          </div>
        </div>

        <div class="card__title">全局大纲</div>
        <div v-if="!state.outline" class="empty-state">
          <div class="empty-state__title">尚未生成全局大纲</div>
          <p class="empty-state__description">请先确认设定库，再生成全局大纲。</p>
        </div>
        <div v-else class="stack">
          <label class="field">
            <span>可直接编辑</span>
            <textarea
              :value="state.outline.content"
              class="text-editor"
              rows="10"
              @input="editOutline"
              @blur="saveOutline"
            ></textarea>
          </label>
          <div class="toolbar">
            <button class="toolbar__button" type="button" @click="confirmOutline">确认全局大纲</button>
          </div>
        </div>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>

      <section v-else-if="activeLevel === 'chapter'" class="card">
        <div class="card__title">章节大纲</div>
        <div v-if="canGenerateChapters" class="stack continuation-controls">
          <div class="form-grid">
            <label class="field">
              <span>追加数量</span>
              <select v-model.number="continuationCount" :disabled="continuationBusy">
                <option :value="10">10 章</option>
                <option :value="20">20 章</option>
                <option :value="50">50 章</option>
              </select>
            </label>
            <label class="field">
              <span>生成模型</span>
              <select v-model.number="continuationModelConfigId" :disabled="continuationBusy || enabledModels.length === 0">
                <option v-for="model in enabledModels" :key="model.id" :value="model.id">
                  {{ model.displayName }}
                </option>
              </select>
            </label>
            <label class="field field--full">
              <span>补充要求</span>
              <textarea
                v-model="continuationInstruction"
                rows="3"
                placeholder="例如：继续第一卷中段，提高反派压力。"
                :disabled="continuationBusy"
              ></textarea>
            </label>
          </div>
          <div class="card__row">
            <p class="helper-text" aria-live="polite">{{ continuationProgressText }}</p>
            <button
              class="toolbar__button"
              type="button"
              :disabled="continuationBusy || continuationModelConfigId == null"
              @click="submitContinueChapterOutlines"
            >
              {{ continuationBusy ? '正在追加…' : '继续生成章节大纲' }}
            </button>
          </div>
        </div>
        <div v-if="state.chapters.length === 0" class="empty-state">
          <div class="empty-state__title">尚未生成章节大纲</div>
          <p class="empty-state__description">首次追加将从第 1 章开始。</p>
        </div>
        <div v-else class="stack">
          <article v-for="chapter in state.chapters" :key="chapter.id" class="list-item">
            <div>
              <div class="list-item__title">{{ chapter.title }}</div>
              <div class="list-item__text">{{ chapter.outline }}</div>
            </div>
            <span class="badge">{{ chapter.hasContent ? '已有正文' : '待正文' }}</span>
          </article>
        </div>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>

      <section v-else-if="activeLevel === 'volume'" class="card">
        <div class="card__title">分卷大纲</div>
        <div v-if="outlineWorkflowVolumes.length === 0" class="empty-state">
          <div class="empty-state__title">尚未生成分卷大纲</div>
          <p class="empty-state__description">请先生成或提交大纲草案。</p>
        </div>
        <div v-else class="stack">
          <article v-for="volume in outlineWorkflowVolumes" :key="String(volume.volumeNo)" class="list-item">
            <div>
              <div class="list-item__title">第{{ volume.volumeNo }}卷：{{ volume.title }}</div>
              <div class="list-item__text">{{ volume.summary }}</div>
              <div class="list-item__text">目标：{{ volume.goal }}</div>
            </div>
            <span class="badge">{{ volume.estimatedWordCount || 0 }}字</span>
          </article>
        </div>
      </section>

      <section v-else-if="activeLevel === 'arc'" class="card">
        <div class="card__title">剧情单元</div>
        <div v-if="outlineWorkflowArcs.length === 0" class="empty-state">
          <div class="empty-state__title">尚未生成剧情单元</div>
          <p class="empty-state__description">第一版会先展开第一卷剧情单元。</p>
        </div>
        <div v-else class="stack">
          <article v-for="arc in outlineWorkflowArcs" :key="`${arc.volumeNo}-${arc.arcNo}`" class="list-item">
            <div>
              <div class="list-item__title">第{{ arc.volumeNo }}卷 / 单元{{ arc.arcNo }}：{{ arc.title }}</div>
              <div class="list-item__text">{{ arc.summary }}</div>
              <div class="list-item__text">目标：{{ arc.goal }}</div>
              <div class="list-item__text">冲突：{{ arc.conflict }}</div>
            </div>
            <span class="badge">{{ arc.estimatedChapterCount || 0 }}章</span>
          </article>
        </div>
      </section>
    </div>
  </PageShell>
</template>

<style scoped>
.continuation-controls {
  margin-bottom: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e5e7eb;
}

.outline-detail-layout {
  display: grid;
  gap: 16px;
}

.detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
}
</style>
