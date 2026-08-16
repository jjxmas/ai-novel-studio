<script setup lang="ts">
import { computed, ref } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const exportFormats = [
  { label: 'Markdown', value: 'markdown' as const },
  { label: 'TXT', value: 'txt' as const },
];
const exportScopes = [
  { label: '全书', value: 'full_project' },
  { label: '某一卷', value: 'volume' },
  { label: '指定章节', value: 'chapter' },
];
const { state, activeProject, createExport } = useNovelWorkspace();
const format = ref<'markdown' | 'txt'>('markdown');
const scope = ref('full_project');
const scopeEntityId = ref<number | null>(null);
const scopeOptions = computed(() => scope.value === 'volume'
  ? (state.outline?.volumes ?? []).map((volume) => ({ id: volume.id, label: `第 ${volume.volumeNo} 卷 ${volume.title}` }))
  : state.chapters.map((chapter) => ({ id: chapter.id, label: `第 ${chapter.chapterNo ?? ''} 章 ${chapter.title}` })));
const canExport = computed(() => scope.value === 'full_project' || scopeEntityId.value !== null);

function changeScope(value: string) {
  scope.value = value;
  scopeEntityId.value = null;
}
</script>

<template>
  <PageShell
    title="导出页"
    description="选择 Markdown 或 TXT，直接在浏览器中下载文件。"
  >
    <template #actions>
      <button
        class="toolbar__button"
        type="button"
        :disabled="!activeProject || !canExport"
        @click="createExport(format, scope, scopeEntityId ?? undefined)"
      >
        立即导出
      </button>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再进行导出。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">导出范围</div>
        <label class="field">
          <span>范围</span>
          <select :value="scope" @change="changeScope(($event.target as HTMLSelectElement).value)">
            <option v-for="item in exportScopes" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
        <label v-if="scope !== 'full_project'" class="field">
          <span>{{ scope === 'volume' ? '卷' : '章节' }}</span>
          <select v-model="scopeEntityId">
            <option :value="null">请选择范围</option>
            <option v-for="item in scopeOptions" :key="item.id" :value="item.id">{{ item.label }}</option>
          </select>
        </label>
      </section>

      <section class="card">
        <div class="card__title">导出格式</div>
        <label class="field">
          <span>格式</span>
          <select v-model="format">
            <option v-for="item in exportFormats" :key="item.value" :value="item.value">{{ item.label }}</option>
          </select>
        </label>
      </section>
    </div>

    <section class="card">
      <div class="card__title">导出记录</div>
      <div v-if="state.exports.length === 0" class="empty-state">
        <div class="empty-state__title">暂无导出记录</div>
        <p class="empty-state__description">选择范围和格式后点击导出。</p>
      </div>
      <div v-else class="stack">
        <article v-for="item in state.exports" :key="item.id" class="list-item">
          <div>
            <div class="list-item__title">{{ item.fileName }}</div>
            <div class="list-item__text">{{ item.scope }} / {{ item.format.toUpperCase() }}</div>
          </div>
          <span class="badge badge--ok">{{ item.status === 'created' ? '已完成' : '失败' }}</span>
        </article>
      </div>
      <p class="helper-text">{{ state.lastMessage }}</p>
    </section>
  </PageShell>
</template>
