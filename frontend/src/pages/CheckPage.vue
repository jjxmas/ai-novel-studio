<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref } from 'vue';

import { novelApi } from '@/api/novelApi';
import type { ChapterGenerationBatch } from '@/api/types';
import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const checkDimensions = ['人物状态', '时间线', '地点移动', '设定冲突', '语气风格', 'AI 腔/低质感'];
const { state, canCheck, loadChapters } = useNovelWorkspace();
const activeBatch = ref<ChapterGenerationBatch | null>(null);
const busy = ref(false);
let pollTimer: number | null = null;

const terminalStatuses = new Set(['completed', 'failed', 'partial_failed', 'cancelled']);
const batchActive = computed(() => Boolean(activeBatch.value && !terminalStatuses.has(activeBatch.value.status)));
const finishedCount = computed(() => activeBatch.value
  ? activeBatch.value.succeededCount + activeBatch.value.failedCount + activeBatch.value.skippedCount
  : 0);
const progress = computed(() => {
  if (!activeBatch.value || activeBatch.value.totalCount === 0) {
    return 0;
  }
  return Math.round((finishedCount.value / activeBatch.value.totalCount) * 100);
});
const issueItems = computed(() => activeBatch.value?.items.flatMap((item) =>
  (item.qualityReport?.issues ?? []).map((issue, index) => ({
    key: `${item.id}-${index}`,
    chapterNo: item.chapterNo,
    ...issue,
  }))) ?? []);

function statusText(status: string) {
  const labels: Record<string, string> = {
    queued: '排队中',
    running: '检查中',
    paused: '已暂停',
    cancel_requested: '正在取消',
    cancelled: '已取消',
    completed: '已完成',
    failed: '失败',
    partial_failed: '部分失败',
  };
  return labels[status] ?? status;
}

function severityText(severity: string) {
  if (severity === 'high' || severity === 'critical') return '高';
  if (severity === 'low' || severity === 'info') return '低';
  return '中';
}

function stopPolling() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
}

async function refreshBatch() {
  if (!activeBatch.value) return;
  const batchId = activeBatch.value.batchId;
  const batch = await novelApi.getQualityCheckBatch(batchId);
  if (activeBatch.value?.batchId !== batchId) return;
  activeBatch.value = batch;
  if (terminalStatuses.has(batch.status)) {
    stopPolling();
    state.lastMessage = batch.status === 'completed'
      ? `全书检查完成，共发现 ${batch.qualityIssueCount} 项问题。`
      : '全书检查已结束，请查看失败章节并在任务中心重试。';
  }
}

function startPolling() {
  stopPolling();
  if (!batchActive.value) return;
  pollTimer = window.setInterval(() => {
    void refreshBatch().catch((error) => {
      state.lastMessage = error instanceof Error ? error.message : '检查进度刷新失败';
    });
  }, 1500);
}

async function createBatch() {
  if (!state.activeProjectId || !canCheck.value || busy.value || batchActive.value) return;
  busy.value = true;
  try {
    activeBatch.value = await novelApi.createQualityCheckBatch(state.activeProjectId);
    state.lastMessage = '全书检查任务已进入数据库队列。';
    startPolling();
  } catch (error) {
    state.lastMessage = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '检查任务创建失败';
  } finally {
    busy.value = false;
  }
}

async function retryFailed() {
  if (!activeBatch.value || busy.value) return;
  busy.value = true;
  try {
    activeBatch.value = await novelApi.retryFailedChapterGenerationBatch(activeBatch.value.batchId);
    state.lastMessage = '失败章节已重新进入队列。';
    startPolling();
  } catch (error) {
    state.lastMessage = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '重试失败';
  } finally {
    busy.value = false;
  }
}

onMounted(async () => {
  await loadChapters().catch(() => undefined);
  if (!state.activeProjectId) return;
  const projectId = state.activeProjectId;
  const batch = await novelApi.getLatestQualityCheckBatch(projectId).catch(() => null);
  if (state.activeProjectId !== projectId) return;
  activeBatch.value = batch;
  startPolling();
});

onUnmounted(stopPolling);
</script>

<template>
  <PageShell
    title="检查页"
    description="全书检查由后端数据库队列逐章执行，可在任务中心暂停、取消或重试失败章节。"
  >
    <template #actions>
      <button
        class="toolbar__button"
        type="button"
        :disabled="!canCheck || busy || batchActive"
        @click="createBatch"
      >
        {{ batchActive ? '检查进行中' : activeBatch ? '重新检查全书' : '检查全书' }}
      </button>
      <button
        v-if="activeBatch && ['failed', 'partial_failed'].includes(activeBatch.status) && activeBatch.failedCount > 0"
        class="toolbar__button toolbar__button--ghost"
        type="button"
        :disabled="busy"
        @click="retryFailed"
      >
        重试失败章节
      </button>
    </template>

    <div v-if="!state.activeProjectId" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再进行检查。</p>
    </div>

    <template v-else>
      <section v-if="activeBatch" class="card stack">
        <div class="card__row">
          <div>
            <div class="card__title">全书检查 #{{ activeBatch.batchId }}</div>
            <p class="helper-text">
              {{ finishedCount }}/{{ activeBatch.totalCount }} 章完成，发现 {{ activeBatch.qualityIssueCount }} 项问题
            </p>
          </div>
          <span class="badge" :class="{ 'badge--ok': activeBatch.status === 'completed', 'badge--warn': ['failed', 'partial_failed'].includes(activeBatch.status) }">
            {{ statusText(activeBatch.status) }}
          </span>
        </div>
        <div class="task-progress task-progress--large"><span :style="{ width: `${progress}%` }"></span></div>
        <p v-if="activeBatch.failedCount" class="helper-text">{{ activeBatch.failedCount }} 章检查失败，可在本页或任务中心重试。</p>
      </section>

      <div class="grid grid--two">
        <section class="card">
          <div class="card__title">检查类型</div>
          <ul class="tag-list">
            <li v-for="item in checkDimensions" :key="item" class="tag-list__item">{{ item }}</li>
          </ul>
        </section>

        <section class="card">
          <div class="card__title">检查结果</div>
          <div v-if="issueItems.length === 0" class="empty-state">
            <div class="empty-state__title">暂无质量问题</div>
            <p class="empty-state__description">检查尚未完成，或已完成章节暂未发现明确问题。</p>
          </div>
          <div v-else class="stack">
            <article v-for="item in issueItems" :key="item.key" class="list-item">
              <div>
                <div class="list-item__title">第 {{ item.chapterNo }} 章 · {{ item.type }}：{{ item.description }}</div>
                <div class="list-item__text">建议：{{ item.suggestion }}</div>
              </div>
              <span class="badge" :class="{ 'badge--warn': severityText(item.severity) !== '低' }">
                {{ severityText(item.severity) }}
              </span>
            </article>
          </div>
        </section>
      </div>
    </template>

    <section class="card">
      <div class="card__title">平台风险提示</div>
      <p class="helper-text">检查结果由后端模型生成，只列出能够从章节正文定位的问题；发布前仍需人工复核。</p>
      <p class="helper-text">{{ state.lastMessage }}</p>
    </section>
  </PageShell>
</template>

<style scoped>
.task-progress {
  display: block;
  width: 100%;
  height: 8px;
  overflow: hidden;
  background: #e2e8f0;
}

.task-progress span {
  display: block;
  height: 100%;
  background: #2563eb;
  transition: width 180ms ease;
}
</style>
