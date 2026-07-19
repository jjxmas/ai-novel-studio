<script setup lang="ts">
import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const checks = ['人物状态', '时间线', '地点移动', '设定冲突', '语气风格', 'AI 腔/低质感'];
const { state, canCheck, createCheck } = useNovelWorkspace();
</script>

<template>
  <PageShell
    title="检查页"
    description="对章节正文做基础 mock 检查，提示人物、时间线、地点、设定冲突和 AI 痕迹风险。"
  >
    <template #actions>
      <button class="toolbar__button" type="button" :disabled="!canCheck" @click="createCheck">
        创建检查
      </button>
    </template>

    <div v-if="!state.activeProjectId" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再进行检查。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">检查类型</div>
        <ul class="tag-list">
          <li v-for="item in checks" :key="item" class="tag-list__item">{{ item }}</li>
        </ul>
      </section>

      <section class="card">
        <div class="card__title">检查结果</div>
        <div v-if="state.checks.length === 0" class="empty-state">
          <div class="empty-state__title">暂无检查结果</div>
          <p class="empty-state__description">生成至少一章正文后，可以创建检查。</p>
        </div>
        <div v-else class="stack">
          <article v-for="item in state.checks" :key="item.id" class="list-item">
            <div>
              <div class="list-item__title">{{ item.type }}：{{ item.summary }}</div>
              <div class="list-item__text">建议：{{ item.suggestion }}</div>
            </div>
            <span class="badge" :class="{ 'badge--warn': item.severity !== '低' }">{{ item.severity }}</span>
          </article>
        </div>
      </section>
    </div>
    <section class="card">
      <div class="card__title">平台风险提示</div>
      <p class="helper-text">
        第二版只做基础提示：避免概述式表达、模板化转折、人物状态断裂和设定自相矛盾。真实判定仍以后端检查服务为准。
      </p>
      <p class="helper-text">{{ state.lastMessage }}</p>
    </section>
  </PageShell>
</template>
