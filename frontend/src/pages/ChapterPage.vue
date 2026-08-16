<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';

import { novelApi } from '@/api/novelApi';
import type { ChapterGenerationBatch, StoryDirtyMarkSnapshot, StoryRebuildResult } from '@/api/types';
import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const chapterBlocks = ['章节列表', '章节大纲', '场景拆分', '正文编辑器', '上一章摘要', '相关设定'];
const {
  state,
  activeProject,
  loadChapters,
  loadModelConfigs,
  loadProjectMemory,
  generateChapterContent,
  updateChapterContent,
  createCheck,
} = useNovelWorkspace();

const activeChapterId = ref<number | null>(null);
const chapterSearch = ref('');
const chapterPage = ref(1);
const chapterPageSize = ref(20);
const rewriteSuggestion = ref('');
const isGeneratingContent = ref(false);
const batchStartChapterNo = ref(1);
const batchCount = ref(10);
const batchModelConfigId = ref<number | null>(null);
const batchSkipExistingContent = ref(true);
const batchInstruction = ref('');
const batchBusy = ref(false);
const activeBatch = ref<ChapterGenerationBatch | null>(null);
const dirtySnapshot = ref<StoryDirtyMarkSnapshot | null>(null);
const dirtyLoading = ref(false);
const rebuildBusy = ref(false);
const rebuildStartChapterNo = ref<number | null>(null);
const rebuildResult = ref<StoryRebuildResult | null>(null);
let batchPollTimer: number | null = null;

const terminalBatchStatuses = new Set(['cancelled', 'completed', 'failed', 'partial_failed']);
const enabledModels = computed(() => state.modelConfigs.filter((model) => model.enabled));
const rebuildableChapters = computed(() => state.chapters.filter((chapter) => chapter.content.trim().length > 0));
const batchControlsLocked = computed(() => batchBusy.value
  || Boolean(activeBatch.value && !terminalBatchStatuses.has(activeBatch.value.status)));
const batchEndChapterNo = computed(() => batchStartChapterNo.value + Math.max(batchCount.value, 1) - 1);
const batchProgressPercent = computed(() => {
  if (!activeBatch.value || activeBatch.value.totalCount === 0) {
    return 0;
  }
  const finished = activeBatch.value.succeededCount
    + activeBatch.value.failedCount
    + activeBatch.value.skippedCount
    + activeBatch.value.items.filter((item) => item.status === 'cancelled').length;
  return Math.round((finished / activeBatch.value.totalCount) * 100);
});
const batchStatusText = computed(() => {
  const labels: Record<string, string> = {
    queued: '排队中',
    running: '生成中',
    paused: '已暂停',
    cancel_requested: '正在取消',
    cancelled: '已取消',
    completed: '已完成',
    failed: '失败',
    partial_failed: '部分失败',
  };
  return activeBatch.value ? labels[activeBatch.value.status] ?? activeBatch.value.status : '尚未创建';
});

function batchItemStatusText(status: string) {
  const labels: Record<string, string> = {
    pending: '等待中',
    running: '生成中',
    succeeded: '已完成',
    failed: '失败',
    skipped: '已跳过',
    cancelled: '已取消',
  };
  return labels[status] ?? status;
}

const activeChapter = computed(() => {
  const fallback = state.chapters[0] ?? null;
  return state.chapters.find((chapter) => chapter.id === activeChapterId.value) ?? fallback;
});

const filteredChapters = computed(() => {
  const keyword = chapterSearch.value.trim().toLowerCase();
  if (!keyword) {
    return state.chapters;
  }
  return state.chapters.filter((chapter) => {
    const chapterNo = String(chapter.chapterNo ?? '');
    return chapterNo.includes(keyword)
      || chapter.title.toLowerCase().includes(keyword)
      || chapter.outline.toLowerCase().includes(keyword);
  });
});

const chapterTotalPages = computed(() => Math.max(1, Math.ceil(filteredChapters.value.length / chapterPageSize.value)));
const pagedChapters = computed(() => {
  const start = (chapterPage.value - 1) * chapterPageSize.value;
  return filteredChapters.value.slice(start, start + chapterPageSize.value);
});

const activeChapterSummary = computed(() => {
  if (!activeChapter.value || !state.projectMemory) {
    return null;
  }
  return state.projectMemory.recentChapterSummaries.find((summary) => summary.chapterId === activeChapter.value?.id) ?? null;
});

const memoryCounts = computed(() => ({
  recent: state.projectMemory?.recentChapterSummaries.length ?? 0,
  middle: state.projectMemory?.middleMemories.length ?? 0,
  high: state.projectMemory?.highMemories.length ?? 0,
  hasGlobal: Boolean(state.projectMemory?.globalMemory),
}));

async function submitGenerateContent() {
  if (!activeChapter.value || isGeneratingContent.value) {
    return;
  }
  isGeneratingContent.value = true;
  try {
    await generateChapterContent(activeChapter.value.id, rewriteSuggestion.value);
    rewriteSuggestion.value = '';
  } finally {
    isGeneratingContent.value = false;
  }
}

async function submitCheck() {
  await createCheck();
}

function stopBatchPolling() {
  if (batchPollTimer != null) {
    window.clearInterval(batchPollTimer);
    batchPollTimer = null;
  }
}

async function refreshActiveBatch() {
  if (!activeBatch.value) {
    return;
  }
  const batch = await novelApi.getChapterGenerationBatch(activeBatch.value.batchId);
  activeBatch.value = batch;
  if (terminalBatchStatuses.has(batch.status)) {
    stopBatchPolling();
    await Promise.all([
      loadChapters().catch(() => undefined),
      loadProjectMemory().catch(() => undefined),
      loadDirtyMarks().catch(() => undefined),
    ]);
  }
}

function startBatchPolling() {
  stopBatchPolling();
  batchPollTimer = window.setInterval(() => {
    void refreshActiveBatch().catch((error) => {
      state.lastMessage = error instanceof Error ? error.message : '批次进度查询失败';
    });
  }, 1500);
}

async function loadLatestBatch(projectId: number) {
  const batch = await novelApi.getLatestChapterGenerationBatch(projectId).catch(() => null);
  activeBatch.value = batch;
  if (batch && !terminalBatchStatuses.has(batch.status) && batch.status !== 'paused') {
    startBatchPolling();
  }
}

async function loadDirtyMarks() {
  if (!activeProject.value || dirtyLoading.value) {
    return;
  }
  const projectId = activeProject.value.id;
  dirtyLoading.value = true;
  try {
    const snapshot = await novelApi.getStoryDirtyMarks(projectId);
    if (activeProject.value?.id === projectId) {
      dirtySnapshot.value = snapshot;
      const earliest = snapshot.earliestDirtyChapterNo;
      if (rebuildStartChapterNo.value == null && earliest != null) {
        rebuildStartChapterNo.value = earliest;
      }
    }
  } catch (error) {
    state.lastMessage = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '脏标记加载失败';
  } finally {
    if (activeProject.value?.id === projectId) {
      dirtyLoading.value = false;
    }
  }
}

async function submitRebuild() {
  if (!activeProject.value || rebuildStartChapterNo.value == null || batchModelConfigId.value == null || rebuildBusy.value) {
    return;
  }
  rebuildBusy.value = true;
  rebuildResult.value = null;
  try {
    rebuildResult.value = await novelApi.rebuildStoryState(
      activeProject.value.id,
      rebuildStartChapterNo.value,
      batchModelConfigId.value,
    );
    state.lastMessage = rebuildResult.value.note;
    await Promise.all([
      loadDirtyMarks(),
      loadProjectMemory().catch(() => undefined),
    ]);
  } catch (error) {
    state.lastMessage = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '故事状态回算失败';
  } finally {
    rebuildBusy.value = false;
  }
}

async function submitBatch() {
  if (!activeProject.value || batchModelConfigId.value == null || batchBusy.value) {
    return;
  }
  batchBusy.value = true;
  try {
    activeBatch.value = await novelApi.createChapterGenerationBatch(activeProject.value.id, {
      startChapterNo: batchStartChapterNo.value,
      count: batchCount.value,
      modelConfigId: batchModelConfigId.value,
      skipExistingContent: batchSkipExistingContent.value,
      instruction: batchInstruction.value.trim() || undefined,
    });
    state.lastMessage = `批次 #${activeBatch.value.batchId} 已创建。`;
    startBatchPolling();
  } finally {
    batchBusy.value = false;
  }
}

async function controlBatch(action: 'pause' | 'resume' | 'cancel' | 'retry') {
  if (!activeBatch.value || batchBusy.value) {
    return;
  }
  batchBusy.value = true;
  try {
    const batchId = activeBatch.value.batchId;
    if (action === 'pause') {
      activeBatch.value = await novelApi.pauseChapterGenerationBatch(batchId);
      stopBatchPolling();
    } else if (action === 'resume') {
      activeBatch.value = await novelApi.resumeChapterGenerationBatch(batchId);
      startBatchPolling();
    } else if (action === 'cancel') {
      activeBatch.value = await novelApi.cancelChapterGenerationBatch(batchId);
      if (!terminalBatchStatuses.has(activeBatch.value.status)) {
        startBatchPolling();
      }
    } else {
      activeBatch.value = await novelApi.retryFailedChapterGenerationBatch(batchId);
      startBatchPolling();
    }
  } finally {
    batchBusy.value = false;
  }
}

function editActiveChapterContent(event: Event) {
  if (!activeChapter.value) {
    return;
  }
  activeChapter.value.content = (event.target as HTMLTextAreaElement).value;
}

function saveActiveChapterContent() {
  if (activeChapter.value) {
    updateChapterContent(activeChapter.value.id, activeChapter.value.content);
  }
}

onMounted(() => {
  void loadChapters().catch(() => undefined);
  void loadProjectMemory().catch(() => undefined);
  void loadModelConfigs().catch(() => undefined);
});

watch(enabledModels, (models) => {
  if (models.length === 0) {
    batchModelConfigId.value = null;
    return;
  }
  if (!models.some((model) => model.id === batchModelConfigId.value)) {
    batchModelConfigId.value = (models.find((model) => model.defaultModel) ?? models[0]).id;
  }
}, { immediate: true });

watch(() => activeProject.value?.id, (projectId) => {
  stopBatchPolling();
  activeBatch.value = null;
  dirtySnapshot.value = null;
  dirtyLoading.value = false;
  rebuildResult.value = null;
  rebuildStartChapterNo.value = null;
  if (projectId) {
    void loadLatestBatch(projectId);
    void loadDirtyMarks().catch(() => undefined);
  }
}, { immediate: true });

watch(() => state.chapters, (chapters) => {
  if (chapters.length > 0 && !chapters.some((chapter) => chapter.chapterNo === batchStartChapterNo.value)) {
    batchStartChapterNo.value = chapters[0].chapterNo ?? 1;
  }
  if (rebuildableChapters.value.length > 0
    && !rebuildableChapters.value.some((chapter) => chapter.chapterNo === rebuildStartChapterNo.value)) {
    rebuildStartChapterNo.value = rebuildableChapters.value[0].chapterNo ?? null;
  }
}, { deep: true, immediate: true });

watch([chapterSearch, chapterPageSize], () => {
  chapterPage.value = 1;
});

watch(chapterTotalPages, (totalPages) => {
  if (chapterPage.value > totalPages) {
    chapterPage.value = totalPages;
  }
});

onUnmounted(stopBatchPolling);
</script>

<template>
  <PageShell
    title="章节页"
    description="在大纲页生成好章节大纲后，这里只负责章节正文生成、编辑和重生成。"
  >
    <template #actions>
      <div class="toolbar">
        <span class="badge" :class="{ 'badge--ok': Boolean(activeProject) }">
          {{ activeProject ? '已选作品' : '请先选择作品' }}
        </span>
      </div>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再处理章节正文。</p>
    </div>

    <section v-if="activeProject" class="card batch-panel">
      <div class="card__row">
        <div>
          <div class="card__title">批量生成正文</div>
          <p class="helper-text">第 {{ batchStartChapterNo }}-{{ batchEndChapterNo }} 章将按顺序生成并完成后处理。</p>
        </div>
        <span class="badge" :class="{ 'badge--ok': activeBatch?.status === 'completed', 'badge--warn': activeBatch?.status === 'partial_failed' || activeBatch?.status === 'failed' }">
          {{ batchStatusText }}
        </span>
      </div>
      <div class="form-grid">
        <label class="field">
          <span>起始章节</span>
          <select v-model.number="batchStartChapterNo" :disabled="batchControlsLocked">
            <option v-for="chapter in state.chapters" :key="chapter.id" :value="chapter.chapterNo">
              第 {{ chapter.chapterNo }} 章 · {{ chapter.title }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>章节数量</span>
          <input v-model.number="batchCount" type="number" min="1" max="50" :disabled="batchControlsLocked" />
        </label>
        <label class="field">
          <span>生成模型</span>
          <select v-model.number="batchModelConfigId" :disabled="batchControlsLocked || enabledModels.length === 0">
            <option v-for="model in enabledModels" :key="model.id" :value="model.id">{{ model.displayName }}</option>
          </select>
        </label>
        <label class="batch-checkbox">
          <input v-model="batchSkipExistingContent" type="checkbox" :disabled="batchControlsLocked" />
          <span>跳过已有正文</span>
        </label>
        <label class="field field--full">
          <span>批次要求</span>
          <textarea v-model="batchInstruction" rows="3" placeholder="例如：每章控制在 3000 字左右。" :disabled="batchControlsLocked"></textarea>
        </label>
      </div>
      <div class="toolbar">
        <button class="toolbar__button" type="button" :disabled="batchControlsLocked || state.chapters.length === 0 || batchModelConfigId == null" @click="submitBatch">
          开始批量生成
        </button>
        <button v-if="activeBatch?.status === 'running' || activeBatch?.status === 'queued'" class="toolbar__button toolbar__button--ghost" type="button" :disabled="batchBusy" @click="controlBatch('pause')">暂停</button>
        <button v-if="activeBatch?.status === 'paused'" class="toolbar__button toolbar__button--ghost" type="button" :disabled="batchBusy" @click="controlBatch('resume')">继续</button>
        <button v-if="activeBatch && !terminalBatchStatuses.has(activeBatch.status)" class="toolbar__button toolbar__button--danger" type="button" :disabled="batchBusy" @click="controlBatch('cancel')">取消</button>
        <button v-if="activeBatch && activeBatch.failedCount > 0 && terminalBatchStatuses.has(activeBatch.status)" class="toolbar__button toolbar__button--ghost" type="button" :disabled="batchBusy" @click="controlBatch('retry')">重试失败章节</button>
      </div>

      <div v-if="activeBatch" class="stack batch-progress" aria-live="polite">
        <div class="batch-progress__track"><div class="batch-progress__value" :style="{ width: `${batchProgressPercent}%` }"></div></div>
        <div class="batch-counts">
          <span>完成 {{ activeBatch.succeededCount }}</span>
          <span>失败 {{ activeBatch.failedCount }}</span>
          <span>跳过 {{ activeBatch.skippedCount }}</span>
          <span>待处理 {{ activeBatch.pendingCount + activeBatch.runningCount }}</span>
        </div>
        <div class="batch-items">
          <div v-for="item in activeBatch.items" :key="item.id" class="batch-item">
            <span>第 {{ item.chapterNo }} 章</span>
            <span class="badge" :class="{ 'badge--ok': item.status === 'succeeded', 'badge--warn': item.status === 'failed' }">{{ batchItemStatusText(item.status) }}</span>
            <span v-if="item.errorMessage" class="batch-item__error">{{ item.errorMessage }}</span>
          </div>
        </div>
      </div>
    </section>

    <section v-if="activeProject" class="card integrity-panel">
      <div class="card__row">
        <div>
          <div class="card__title">数据一致性</div>
          <p class="helper-text">
            {{ dirtySnapshot?.earliestDirtyChapterNo
              ? `第 ${dirtySnapshot.earliestDirtyChapterNo} 章起需要重新计算事实、状态和记忆。`
              : '当前没有待回算的章节区间。' }}
          </p>
        </div>
        <span class="badge" :class="{ 'badge--warn': Boolean(dirtySnapshot?.activeDirtyMarkCount), 'badge--ok': dirtySnapshot?.activeDirtyMarkCount === 0 }">
          {{ dirtySnapshot?.activeDirtyMarkCount ?? 0 }} 个脏标记
        </span>
      </div>

      <div class="integrity-controls">
        <label class="field">
          <span>从章节开始回算</span>
          <select v-model.number="rebuildStartChapterNo" :disabled="rebuildBusy || batchControlsLocked || rebuildableChapters.length === 0">
            <option v-for="chapter in rebuildableChapters" :key="chapter.id" :value="chapter.chapterNo">
              第 {{ chapter.chapterNo }} 章 · {{ chapter.title }}
            </option>
          </select>
        </label>
        <label class="field">
          <span>处理模型</span>
          <select v-model.number="batchModelConfigId" :disabled="rebuildBusy || batchControlsLocked || enabledModels.length === 0">
            <option v-for="model in enabledModels" :key="model.id" :value="model.id">{{ model.displayName }}</option>
          </select>
        </label>
        <div class="toolbar integrity-actions">
          <button class="toolbar__button toolbar__button--ghost" type="button" :disabled="dirtyLoading" @click="loadDirtyMarks">
            {{ dirtyLoading ? '刷新中' : '刷新标记' }}
          </button>
          <button class="toolbar__button" type="button" :disabled="rebuildBusy || batchControlsLocked || rebuildStartChapterNo == null || batchModelConfigId == null" @click="submitRebuild">
            {{ rebuildBusy ? '回算中' : '开始回算' }}
          </button>
        </div>
      </div>

      <div v-if="dirtySnapshot?.activeDirtyMarks.length" class="dirty-list">
        <div v-for="mark in dirtySnapshot.activeDirtyMarks" :key="mark.id" class="dirty-row">
          <strong>第 {{ mark.sourceChapterNo ?? '?' }} 章变更</strong>
          <span>影响第 {{ mark.dirtyFromChapterNo }} 章起</span>
          <span>{{ mark.reasonNote || mark.reasonType }}</span>
        </div>
      </div>

      <div v-if="rebuildResult" class="rebuild-result" :class="{ 'rebuild-result--ok': rebuildResult.status === 'completed' }" aria-live="polite">
        <strong>{{ rebuildResult.status === 'completed' ? '回算完成' : '无需回算' }}</strong>
        <span>{{ rebuildResult.note }}</span>
        <span>处理 {{ rebuildResult.processedChapterCount }} 章，跳过 {{ rebuildResult.skippedChapterCount }} 章，解决 {{ rebuildResult.resolvedDirtyMarkCount }} 个标记。</span>
      </div>
    </section>

    <div v-if="activeProject" class="grid grid--two">
      <section class="card">
        <div class="card__title">章节结构</div>
        <div class="stack">
          <ul class="tag-list">
            <li v-for="item in chapterBlocks" :key="item" class="tag-list__item">{{ item }}</li>
          </ul>
          <div class="chapter-list-controls">
            <input
              v-model="chapterSearch"
              class="toolbar__input"
              type="text"
              placeholder="搜索章节号、标题或大纲"
            />
            <label class="field">
              <span>每页</span>
              <select v-model.number="chapterPageSize">
                <option :value="10">10 章</option>
                <option :value="20">20 章</option>
                <option :value="50">50 章</option>
              </select>
            </label>
          </div>
          <div v-if="state.chapters.length === 0" class="empty-state">
            <div class="empty-state__title">尚未生成章节大纲</div>
            <p class="empty-state__description">请到大纲页生成章节大纲后，再回到这里写正文。</p>
          </div>
          <div v-else-if="filteredChapters.length === 0" class="empty-state">
            <div class="empty-state__title">没有匹配章节</div>
            <p class="empty-state__description">换一个章节号、标题或关键词再试。</p>
          </div>
          <article
            v-for="chapter in pagedChapters"
            :key="chapter.id"
            class="list-item list-item--clickable"
            :class="{ 'card--selected': chapter.id === activeChapter?.id }"
            @click="activeChapterId = chapter.id"
          >
            <div>
              <div class="list-item__title">{{ chapter.title }}</div>
              <div class="list-item__text">{{ chapter.outline }}</div>
            </div>
            <span class="badge">{{ chapter.status === 'outline_ready' ? '待正文' : '有正文' }}</span>
          </article>
          <div v-if="filteredChapters.length > chapterPageSize" class="pagination">
            <button
              class="toolbar__button toolbar__button--ghost toolbar__button--small"
              type="button"
              :disabled="chapterPage === 1"
              @click="chapterPage -= 1"
            >
              上一页
            </button>
            <span class="helper-text">第 {{ chapterPage }} / {{ chapterTotalPages }} 页，共 {{ filteredChapters.length }} 章</span>
            <button
              class="toolbar__button toolbar__button--ghost toolbar__button--small"
              type="button"
              :disabled="chapterPage === chapterTotalPages"
              @click="chapterPage += 1"
            >
              下一页
            </button>
          </div>
        </div>
      </section>

      <section class="card">
        <div class="card__title">正文状态</div>
        <div v-if="!activeChapter" class="empty-state">
          <div class="empty-state__title">尚未生成章节</div>
          <p class="empty-state__description">请先到大纲页生成章节大纲。</p>
        </div>
        <div v-else class="stack">
          <div class="hint-box">{{ activeChapter.outline }}</div>
          <label class="field">
            <span>章节正文</span>
            <textarea
              :value="activeChapter.content"
              class="text-editor"
              rows="12"
              placeholder="生成后可以直接编辑正文"
              @input="editActiveChapterContent"
            ></textarea>
          </label>
          <label class="field">
            <span>修改意见</span>
            <textarea
              v-model="rewriteSuggestion"
              rows="3"
              placeholder="例如：减少总结感，增加人物动作和对白"
            ></textarea>
          </label>
          <div class="toolbar">
            <button class="toolbar__button toolbar__button--ghost" type="button" :disabled="isGeneratingContent" @click="saveActiveChapterContent">
              保存正文
            </button>
            <button class="toolbar__button" type="button" :disabled="isGeneratingContent" @click="submitGenerateContent">
              {{ activeChapter.content ? '按意见重生成' : '生成正文' }}
            </button>
            <button class="toolbar__button toolbar__button--ghost" type="button" :disabled="isGeneratingContent || !activeChapter.content" @click="submitCheck">
              创建检查
            </button>
          </div>
          <div v-if="state.checks.length" class="hint-box">
            <strong>检查结果</strong>
            <p v-for="check in state.checks.slice(0, 3)" :key="check.id" class="helper-text">
              {{ check.type }}：{{ check.summary }}
            </p>
          </div>
          <div class="hint-box">
            <strong>章节记忆</strong>
            <p class="helper-text">
              {{ activeChapterSummary?.summary || '生成正文后，后端会刷新单章摘要并进入近窗。' }}
            </p>
            <ul class="tag-list">
              <li class="tag-list__item">近窗 {{ memoryCounts.recent }}/6</li>
              <li class="tag-list__item">中层 {{ memoryCounts.middle }}/8</li>
              <li class="tag-list__item">高层 {{ memoryCounts.high }}</li>
              <li class="tag-list__item">{{ memoryCounts.hasGlobal ? '已有全局摘要' : '暂无全局摘要' }}</li>
            </ul>
            <p class="helper-text">近窗满 6 章会压缩为中层；中层满 8 条会压缩为高层，并同步更新全局总摘要。</p>
          </div>
        </div>
      </section>
    </div>
    <section class="card">
      <div class="card__title">版本提示</div>
      <p class="helper-text">{{ state.lastMessage }}</p>
    </section>
  </PageShell>
</template>

<style scoped>
.batch-panel {
  margin-bottom: 16px;
}

.integrity-panel {
  margin-bottom: 16px;
}

.integrity-controls {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr)) auto;
  gap: 16px;
  align-items: end;
  margin-top: 16px;
}

.integrity-actions {
  padding-bottom: 1px;
}

.dirty-list {
  display: grid;
  margin-top: 16px;
  border-top: 1px solid #e5e7eb;
}

.dirty-row {
  display: grid;
  grid-template-columns: minmax(120px, 0.7fr) minmax(140px, 0.8fr) minmax(0, 2fr);
  gap: 12px;
  padding: 10px 0;
  border-bottom: 1px solid #e5e7eb;
  color: #475569;
  font-size: 13px;
}

.rebuild-result {
  display: grid;
  gap: 4px;
  margin-top: 16px;
  padding: 12px 14px;
  border-left: 3px solid #d97706;
  background: #fffbeb;
  color: #92400e;
  font-size: 13px;
}

.rebuild-result--ok {
  border-left-color: #16a34a;
  background: #f0fdf4;
  color: #166534;
}

.batch-checkbox {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 42px;
  color: #374151;
  font-weight: 600;
}

.chapter-list-controls {
  display: grid;
  grid-template-columns: minmax(0, 1fr) 120px;
  gap: 12px;
  align-items: end;
}

.pagination {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
}

.batch-progress {
  margin-top: 16px;
}

.batch-progress__track {
  overflow: hidden;
  height: 8px;
  border-radius: 4px;
  background: #e5e7eb;
}

.batch-progress__value {
  height: 100%;
  border-radius: inherit;
  background: #2563eb;
  transition: width 180ms ease;
}

.batch-counts {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  color: #475569;
  font-size: 14px;
}

.batch-items {
  display: grid;
  max-height: 280px;
  overflow: auto;
  border: 1px solid #e5e7eb;
  border-radius: 8px;
}

.batch-item {
  display: grid;
  grid-template-columns: minmax(100px, 1fr) auto minmax(0, 2fr);
  gap: 12px;
  align-items: center;
  min-height: 44px;
  padding: 8px 12px;
  border-bottom: 1px solid #e5e7eb;
}

.batch-item:last-child {
  border-bottom: 0;
}

.batch-item__error {
  overflow-wrap: anywhere;
  color: #b91c1c;
  font-size: 13px;
}

@media (max-width: 720px) {
  .integrity-controls,
  .dirty-row,
  .chapter-list-controls {
    grid-template-columns: 1fr;
  }

  .batch-item {
    grid-template-columns: 1fr auto;
  }

  .batch-item__error {
    grid-column: 1 / -1;
  }
}
</style>
