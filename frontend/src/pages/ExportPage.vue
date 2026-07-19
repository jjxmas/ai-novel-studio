<script setup lang="ts">
import { ref } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const exportFormats = [
  { label: 'Markdown', value: 'markdown' as const },
  { label: 'TXT', value: 'txt' as const },
];
const exportScopes = ['全书', '某一卷', '指定章节'];
const { state, activeProject, createExport } = useNovelWorkspace();
const format = ref<'markdown' | 'txt'>('markdown');
const scope = ref('全书');
</script>

<template>
  <PageShell
    title="导出页"
    description="选择 Markdown 或 TXT，由后端创建导出任务，后续再扩展 docx/epub。"
  >
    <template #actions>
      <button
        class="toolbar__button"
        type="button"
        :disabled="!activeProject"
        @click="createExport(format, scope)"
      >
        创建导出
      </button>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再创建导出。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">导出范围</div>
        <label class="field">
          <span>范围</span>
          <select v-model="scope">
            <option v-for="item in exportScopes" :key="item" :value="item">{{ item }}</option>
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
        <p class="empty-state__description">选择范围和格式后创建导出。</p>
      </div>
      <div v-else class="stack">
        <article v-for="item in state.exports" :key="item.id" class="list-item">
          <div>
            <div class="list-item__title">{{ item.fileName }}</div>
            <div class="list-item__text">{{ item.scope }} · {{ item.format.toUpperCase() }}</div>
          </div>
          <span class="badge badge--ok">{{ item.status === 'created' ? '已创建' : '失败' }}</span>
        </article>
      </div>
      <p class="helper-text">{{ state.lastMessage }}</p>
    </section>
  </PageShell>
</template>
