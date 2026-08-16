<script setup lang="ts">
import { computed, onMounted, onUnmounted, ref, watch } from 'vue';

import { novelApi } from '@/api/novelApi';
import type { ChapterGenerationBatch, ChapterGenerationBatchSummary } from '@/api/types';
import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

type TaskFilter = 'all' | 'active' | 'failed' | 'finished';
type BatchAction = 'pause' | 'resume' | 'cancel' | 'retry';

const { state, activeProject, loadProjects, selectProject } = useNovelWorkspace();
const batches = ref<ChapterGenerationBatchSummary[]>([]);
const selectedBatchId = ref<number | null>(null);
const selectedBatch = ref<ChapterGenerationBatch | null>(null);
const statusFilter = ref<TaskFilter>('all');
const refreshing = ref(false);
const detailLoading = ref(false);
const controlBusy = ref(false);
const errorMessage = ref('');
let pollTimer: number | null = null;

const activeStatuses = new Set(['queued', 'running', 'paused', 'cancel_requested']);
const pollingStatuses = new Set(['queued', 'running', 'cancel_requested']);
const failedStatuses = new Set(['failed', 'partial_failed']);
const finishedStatuses = new Set(['completed', 'cancelled']);

const filteredBatches = computed(() => batches.value.filter((batch) => {
  if (statusFilter.value === 'active') {
    return activeStatuses.has(batch.status);
  }
  if (statusFilter.value === 'failed') {
    return failedStatuses.has(batch.status);
  }
  if (statusFilter.value === 'finished') {
    return finishedStatuses.has(batch.status);
  }
  return true;
}));

const selectedSummary = computed(() => batches.value.find((batch) => batch.batchId === selectedBatchId.value) ?? null);
const activeCount = computed(() => batches.value.filter((batch) => activeStatuses.has(batch.status)).length);
const failedCount = computed(() => batches.value.filter((batch) => failedStatuses.has(batch.status)).length);
const cancelledItemCount = computed(() => selectedBatch.value?.items.filter((item) => item.status === 'cancelled').length ?? 0);
const selectedProgress = computed(() => selectedBatch.value ? progressPercent({
  ...selectedBatch.value,
  skippedCount: selectedBatch.value.skippedCount + cancelledItemCount.value,
}) : 0);

function statusText(status: string) {
  const labels: Record<string, string> = {
    queued: '排队中',
    running: '执行中',
    paused: '已暂停',
    cancel_requested: '正在取消',
    cancelled: '已取消',
    completed: '已完成',
    failed: '失败',
    partial_failed: '部分失败',
    pending: '等待中',
    succeeded: '已完成',
    skipped: '已跳过',
  };
  return labels[status] ?? status;
}

function batchTypeText(batchType: string) {
  const labels: Record<string, string> = {
    chapter_content: '章节正文生成',
    quality_check: '全书质量检查',
  };
  return labels[batchType] ?? batchType;
}

function qualityStatusText(status: string, issueCount: number) {
  if (status === 'completed') {
    return issueCount > 0 ? `${issueCount} 项提示` : '检查通过';
  }
  const labels: Record<string, string> = {
    pending: '待检查',
    failed: '检查失败',
    skipped: '已跳过',
    not_run: '未检查',
  };
  return labels[status] ?? status;
}

function formatTime(value?: string | null) {
  if (!value) {
    return '尚未记录';
  }
  return new Date(value).toLocaleString('zh-CN', { hour12: false });
}

function progressPercent(batch: ChapterGenerationBatchSummary) {
  if (batch.totalCount <= 0) {
    return 0;
  }
  if (batch.status === 'cancelled') {
    return 100;
  }
  const finished = batch.succeededCount + batch.failedCount + batch.skippedCount;
  return Math.min(100, Math.round((finished / batch.totalCount) * 100));
}

function stopPolling() {
  if (pollTimer != null) {
    window.clearInterval(pollTimer);
    pollTimer = null;
  }
}

function syncPolling() {
  const shouldPoll = batches.value.some((batch) => pollingStatuses.has(batch.status));
  if (!shouldPoll) {
    stopPolling();
    return;
  }
  if (pollTimer == null) {
    pollTimer = window.setInterval(() => {
      void refreshTaskCenter();
    }, 2000);
  }
}

async function loadSelectedBatch() {
  if (selectedBatchId.value == null) {
    selectedBatch.value = null;
    detailLoading.value = false;
    return;
  }
  const batchId = selectedBatchId.value;
  detailLoading.value = true;
  try {
    const batch = await novelApi.getChapterGenerationBatch(batchId);
    if (selectedBatchId.value === batchId) {
      selectedBatch.value = batch;
    }
  } finally {
    if (selectedBatchId.value === batchId) {
      detailLoading.value = false;
    }
  }
}

async function loadBatchSummaries() {
  if (!activeProject.value) {
    batches.value = [];
    selectedBatchId.value = null;
    selectedBatch.value = null;
    return;
  }
  batches.value = await novelApi.listChapterGenerationBatches(activeProject.value.id);
  const visibleIds = new Set(filteredBatches.value.map((batch) => batch.batchId));
  if (selectedBatchId.value == null || !visibleIds.has(selectedBatchId.value)) {
    selectedBatchId.value = filteredBatches.value[0]?.batchId ?? null;
  }
}

async function refreshTaskCenter() {
  if (refreshing.value || controlBusy.value || !activeProject.value) {
    return;
  }
  refreshing.value = true;
  errorMessage.value = '';
  try {
    await loadBatchSummaries();
    await loadSelectedBatch();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '任务加载失败';
  } finally {
    refreshing.value = false;
    syncPolling();
  }
}

async function openBatch(batchId: number) {
  if (batchId === selectedBatchId.value && selectedBatch.value) {
    return;
  }
  selectedBatchId.value = batchId;
  errorMessage.value = '';
  try {
    await loadSelectedBatch();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '任务详情加载失败';
  }
}

async function controlBatch(action: BatchAction) {
  if (!selectedBatch.value || controlBusy.value) {
    return;
  }
  controlBusy.value = true;
  errorMessage.value = '';
  try {
    const batchId = selectedBatch.value.batchId;
    if (action === 'pause') {
      selectedBatch.value = await novelApi.pauseChapterGenerationBatch(batchId);
    } else if (action === 'resume') {
      selectedBatch.value = await novelApi.resumeChapterGenerationBatch(batchId);
    } else if (action === 'cancel') {
      selectedBatch.value = await novelApi.cancelChapterGenerationBatch(batchId);
    } else {
      selectedBatch.value = await novelApi.retryFailedChapterGenerationBatch(batchId);
    }
    await loadBatchSummaries();
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '任务操作失败';
  } finally {
    controlBusy.value = false;
    syncPolling();
  }
}

function changeProject(event: Event) {
  const projectId = Number((event.target as HTMLSelectElement).value);
  if (!projectId || projectId === state.activeProjectId) {
    return;
  }
  localStorage.setItem('novel-task-center-project-id', String(projectId));
  selectProject(projectId);
}

watch(() => activeProject.value?.id, (projectId) => {
  stopPolling();
  batches.value = [];
  selectedBatchId.value = null;
  selectedBatch.value = null;
  if (projectId) {
    localStorage.setItem('novel-task-center-project-id', String(projectId));
    void refreshTaskCenter();
  }
}, { immediate: true });

watch(statusFilter, () => {
  const visibleIds = new Set(filteredBatches.value.map((batch) => batch.batchId));
  if (selectedBatchId.value == null || !visibleIds.has(selectedBatchId.value)) {
    selectedBatchId.value = filteredBatches.value[0]?.batchId ?? null;
    selectedBatch.value = null;
    void loadSelectedBatch().catch((error) => {
      errorMessage.value = error instanceof Error ? error.message.replace('BUSINESS_ERROR:', '') : '任务详情加载失败';
    });
  }
});

onMounted(async () => {
  const projects = await loadProjects().catch(() => []);
  if (state.activeProjectId == null && projects.length > 0) {
    const savedProjectId = Number(localStorage.getItem('novel-task-center-project-id'));
    const projectId = projects.some((project) => project.id === savedProjectId)
      ? savedProjectId
      : projects[0].id;
    selectProject(projectId);
  }
});

onUnmounted(stopPolling);
</script>

<template>
  <PageShell title="任务中心" description="查看章节生成批次的实时状态、执行记录和失败详情。">
    <template #actions>
      <button class="toolbar__button toolbar__button--ghost" type="button" :disabled="refreshing || !activeProject" @click="refreshTaskCenter">
        {{ refreshing ? '刷新中' : '刷新任务' }}
      </button>
    </template>

    <div class="task-toolbar">
      <label class="field task-toolbar__field">
        <span>作品</span>
        <select :value="state.activeProjectId ?? ''" @change="changeProject">
          <option value="" disabled>选择作品</option>
          <option v-for="project in state.projects" :key="project.id" :value="project.id">{{ project.title }}</option>
        </select>
      </label>
      <label class="field task-toolbar__field">
        <span>状态</span>
        <select v-model="statusFilter">
          <option value="all">全部任务</option>
          <option value="active">活动任务</option>
          <option value="failed">失败任务</option>
          <option value="finished">已结束任务</option>
        </select>
      </label>
      <div class="task-stats" aria-label="任务统计">
        <span>共 {{ batches.length }}</span>
        <span>活动 {{ activeCount }}</span>
        <span>失败 {{ failedCount }}</span>
      </div>
    </div>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">暂无可查看的作品</div>
      <p class="empty-state__description">请先创建或选择作品。</p>
    </div>

    <p v-if="errorMessage" class="task-error" role="alert">{{ errorMessage }}</p>

    <div v-if="activeProject" class="task-workspace">
      <section class="task-list-panel" aria-label="任务列表">
        <div class="task-panel-heading">
          <strong>批次记录</strong>
          <span>{{ filteredBatches.length }} 条</span>
        </div>
        <div v-if="!refreshing && filteredBatches.length === 0" class="empty-state task-empty">
          <div class="empty-state__title">没有符合条件的任务</div>
        </div>
        <button
          v-for="batch in filteredBatches"
          :key="batch.batchId"
          class="task-row"
          :class="{ 'task-row--selected': batch.batchId === selectedBatchId }"
          type="button"
          @click="openBatch(batch.batchId)"
        >
          <span class="task-row__header">
            <strong>{{ batchTypeText(batch.batchType) }} #{{ batch.batchId }}</strong>
            <span class="badge" :class="{ 'badge--ok': batch.status === 'completed', 'badge--warn': failedStatuses.has(batch.status) }">
              {{ statusText(batch.status) }}
            </span>
          </span>
          <span class="task-row__meta">
            {{ formatTime(batch.createdAt) }} · {{ batch.succeededCount }}/{{ batch.totalCount }} 章 · 质量提示 {{ batch.qualityIssueCount }}
          </span>
          <span class="task-progress"><span :style="{ width: `${progressPercent(batch)}%` }"></span></span>
        </button>
      </section>

      <section class="task-detail-panel" aria-label="任务详情">
        <div v-if="detailLoading" class="empty-state task-empty">
          <div class="empty-state__title">正在加载任务详情</div>
        </div>
        <div v-else-if="!selectedBatch || !selectedSummary" class="empty-state task-empty">
          <div class="empty-state__title">请选择任务</div>
        </div>
        <template v-else>
          <div class="task-detail-header">
            <div>
              <h3>{{ batchTypeText(selectedBatch.batchType) }} #{{ selectedBatch.batchId }}</h3>
              <p>创建于 {{ formatTime(selectedBatch.createdAt) }}</p>
            </div>
            <span class="badge" :class="{ 'badge--ok': selectedBatch.status === 'completed', 'badge--warn': failedStatuses.has(selectedBatch.status) }">
              {{ statusText(selectedBatch.status) }}
            </span>
          </div>

          <div class="task-progress task-progress--large"><span :style="{ width: `${selectedProgress}%` }"></span></div>
          <div class="task-metrics">
            <span><strong>{{ selectedBatch.succeededCount }}</strong> 完成</span>
            <span><strong>{{ selectedBatch.runningCount }}</strong> 执行中</span>
            <span><strong>{{ selectedBatch.pendingCount }}</strong> 等待</span>
            <span><strong>{{ selectedBatch.failedCount }}</strong> 失败</span>
            <span><strong>{{ selectedBatch.skippedCount }}</strong> 跳过</span>
          </div>

          <div class="task-quality-summary">
            <div>
              <strong>批次质量报告</strong>
              <span>{{ selectedBatch.batchType === 'quality_check' ? '逐章执行全量质量检查' : '每章正文完成后自动执行连续性检查' }}</span>
            </div>
            <dl>
              <div><dt>已检查</dt><dd>{{ selectedBatch.qualityCheckedCount }}</dd></div>
              <div><dt>检查失败</dt><dd>{{ selectedBatch.qualityFailedCount }}</dd></div>
              <div><dt>质量提示</dt><dd>{{ selectedBatch.qualityIssueCount }}</dd></div>
            </dl>
          </div>

          <div class="toolbar task-actions">
            <button v-if="selectedBatch.status === 'queued' || selectedBatch.status === 'running'" class="toolbar__button toolbar__button--ghost" type="button" :disabled="controlBusy" @click="controlBatch('pause')">暂停</button>
            <button v-if="selectedBatch.status === 'paused'" class="toolbar__button toolbar__button--ghost" type="button" :disabled="controlBusy" @click="controlBatch('resume')">继续</button>
            <button v-if="activeStatuses.has(selectedBatch.status)" class="toolbar__button toolbar__button--danger" type="button" :disabled="controlBusy" @click="controlBatch('cancel')">取消</button>
            <button v-if="failedStatuses.has(selectedBatch.status) && selectedBatch.failedCount > 0" class="toolbar__button toolbar__button--ghost" type="button" :disabled="controlBusy" @click="controlBatch('retry')">重试失败章节</button>
          </div>

          <div v-if="selectedBatch.errorMessage" class="task-failure">
            <strong>批次失败原因</strong>
            <p>{{ selectedBatch.errorMessage }}</p>
          </div>

          <div class="task-items" role="table" aria-label="章节任务明细">
            <div class="task-item task-item--header" role="row">
              <span>章节</span>
              <span>{{ selectedBatch.batchType === 'quality_check' ? '执行' : '生成' }}</span>
              <span>质量</span>
              <span>次数</span>
              <span>完成时间 / 详情</span>
            </div>
            <div v-for="item in selectedBatch.items" :key="item.id" class="task-item" role="row">
              <strong>第 {{ item.chapterNo }} 章</strong>
              <span class="badge" :class="{ 'badge--ok': item.status === 'succeeded', 'badge--warn': item.status === 'failed' }">{{ statusText(item.status) }}</span>
              <span class="badge" :class="{ 'badge--ok': item.qualityStatus === 'completed' && item.qualityIssueCount === 0, 'badge--warn': item.qualityStatus === 'failed' || item.qualityIssueCount > 0 }">
                {{ qualityStatusText(item.qualityStatus, item.qualityIssueCount) }}
              </span>
              <span>{{ item.attemptCount }}</span>
              <div class="task-item__detail" :class="{ 'task-item__error': Boolean(item.errorMessage || item.qualityErrorMessage) }">
                <span>{{ item.errorMessage || item.qualityErrorMessage || formatTime(item.finishedAt) }}</span>
                <details v-if="item.qualityReport?.issues.length">
                  <summary>查看质量提示</summary>
                  <ul>
                    <li v-for="(issue, index) in item.qualityReport.issues" :key="`${item.id}-${index}`">
                      <strong>{{ issue.description }}</strong>
                      <span>{{ issue.suggestion }}</span>
                    </li>
                  </ul>
                </details>
              </div>
            </div>
          </div>
        </template>
      </section>
    </div>
  </PageShell>
</template>

<style scoped>
.task-toolbar {
  display: flex;
  align-items: end;
  gap: 16px;
  padding-bottom: 16px;
  border-bottom: 1px solid #dbe2ea;
}

.task-toolbar__field {
  width: min(280px, 100%);
}

.task-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  margin-left: auto;
  padding-bottom: 10px;
  color: #475569;
  font-size: 13px;
}

.task-error,
.task-failure {
  margin: 0;
  padding: 12px 14px;
  border-left: 3px solid #dc2626;
  background: #fef2f2;
  color: #991b1b;
}

.task-failure p {
  margin: 6px 0 0;
}

.task-workspace {
  display: grid;
  grid-template-columns: minmax(280px, 0.7fr) minmax(0, 1.5fr);
  min-height: 560px;
  overflow: hidden;
  border: 1px solid #dbe2ea;
  border-radius: 8px;
  background: #fff;
}

.task-list-panel {
  overflow: auto;
  border-right: 1px solid #dbe2ea;
  background: #f8fafc;
}

.task-panel-heading {
  display: flex;
  justify-content: space-between;
  padding: 16px;
  border-bottom: 1px solid #dbe2ea;
  color: #334155;
}

.task-panel-heading span {
  color: #64748b;
  font-size: 13px;
}

.task-row {
  display: grid;
  width: 100%;
  gap: 9px;
  padding: 14px 16px;
  border: 0;
  border-bottom: 1px solid #e2e8f0;
  border-radius: 0;
  background: transparent;
  color: #1f2937;
  text-align: left;
}

.task-row:hover,
.task-row--selected {
  background: #eef6ff;
}

.task-row--selected {
  box-shadow: inset 3px 0 0 #2563eb;
}

.task-row__header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 8px;
}

.task-row__meta {
  color: #64748b;
  font-size: 12px;
}

.task-progress {
  display: block;
  width: 100%;
  height: 5px;
  overflow: hidden;
  background: #e2e8f0;
}

.task-progress span {
  display: block;
  height: 100%;
  background: #2563eb;
  transition: width 180ms ease;
}

.task-progress--large {
  height: 8px;
}

.task-detail-panel {
  min-width: 0;
  padding: 20px;
}

.task-detail-header {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 16px;
  margin-bottom: 18px;
}

.task-detail-header h3 {
  margin: 0;
  font-size: 18px;
}

.task-detail-header p {
  margin: 6px 0 0;
  color: #64748b;
  font-size: 13px;
}

.task-metrics {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 1px;
  margin: 16px 0;
  background: #dbe2ea;
}

.task-metrics span {
  display: grid;
  gap: 3px;
  padding: 12px;
  background: #f8fafc;
  color: #64748b;
  font-size: 12px;
}

.task-metrics strong {
  color: #1f2937;
  font-size: 18px;
}

.task-actions {
  margin-bottom: 16px;
}

.task-quality-summary {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  margin-bottom: 16px;
  padding: 14px 0;
  border-block: 1px solid #dbe2ea;
}

.task-quality-summary > div {
  display: grid;
  gap: 4px;
}

.task-quality-summary > div span {
  color: #64748b;
  font-size: 12px;
}

.task-quality-summary dl {
  display: flex;
  gap: 24px;
  margin: 0;
}

.task-quality-summary dl div {
  display: grid;
  gap: 2px;
}

.task-quality-summary dt {
  color: #64748b;
  font-size: 11px;
}

.task-quality-summary dd {
  margin: 0;
  font-size: 18px;
  font-weight: 700;
}

.task-items {
  margin-top: 20px;
  border-top: 1px solid #dbe2ea;
}

.task-item {
  display: grid;
  grid-template-columns: minmax(72px, 0.5fr) minmax(86px, 0.6fr) minmax(100px, 0.7fr) minmax(44px, 0.3fr) minmax(180px, 2fr);
  align-items: center;
  gap: 12px;
  min-height: 52px;
  padding: 9px 4px;
  border-bottom: 1px solid #e5e7eb;
  font-size: 13px;
}

.task-item--header {
  min-height: 42px;
  color: #64748b;
  font-size: 12px;
  font-weight: 600;
}

.task-item .badge {
  justify-self: start;
}

.task-item__error {
  color: #b91c1c;
  overflow-wrap: anywhere;
}

.task-item__detail {
  display: grid;
  gap: 6px;
  min-width: 0;
}

.task-item__detail details {
  color: #334155;
}

.task-item__detail summary {
  cursor: pointer;
  font-weight: 600;
}

.task-item__detail ul {
  display: grid;
  gap: 8px;
  margin: 8px 0 0;
  padding-left: 18px;
}

.task-item__detail li {
  display: grid;
  gap: 3px;
}

.task-item__detail li span {
  color: #64748b;
}

.task-empty {
  margin: 24px;
}

@media (max-width: 900px) {
  .task-toolbar {
    align-items: stretch;
    flex-direction: column;
  }

  .task-toolbar__field {
    width: 100%;
  }

  .task-stats {
    margin-left: 0;
    padding-bottom: 0;
  }

  .task-workspace {
    grid-template-columns: 1fr;
  }

  .task-list-panel {
    max-height: 360px;
    border-right: 0;
    border-bottom: 1px solid #dbe2ea;
  }

  .task-metrics {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .task-item,
  .task-item--header {
    grid-template-columns: minmax(68px, 0.5fr) minmax(82px, 0.6fr) minmax(96px, 0.7fr) minmax(40px, 0.3fr) minmax(130px, 1.5fr);
  }
}

@media (max-width: 620px) {
  .task-detail-panel {
    padding: 16px;
  }

  .task-quality-summary {
    align-items: stretch;
    flex-direction: column;
  }

  .task-quality-summary dl {
    justify-content: space-between;
  }

  .task-item--header {
    display: none;
  }

  .task-item {
    grid-template-columns: 1fr auto;
  }

  .task-item > :nth-child(3),
  .task-item > :nth-child(4),
  .task-item > :nth-child(5) {
    grid-column: 1 / -1;
  }
}
</style>
