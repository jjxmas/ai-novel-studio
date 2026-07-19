<script setup lang="ts">
import { reactive } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const ideaMetrics = ['长篇承载力', '主线驱动力', '冲突密度', '新手可写性', '设定复杂度风险', '差异化'];
const {
  state,
  activeProject,
  selectedIdea,
  generateIdeas,
  selectIdea,
  rewriteIdea,
  updateIdea,
} = useNovelWorkspace();

const rewriteSuggestions = reactive<Record<number, string>>({});

async function submitGenerate() {
  await generateIdeas();
}

async function submitRewrite(ideaId: number) {
  await rewriteIdea(ideaId, rewriteSuggestions[ideaId] ?? '');
  rewriteSuggestions[ideaId] = '';
}
</script>

<template>
  <PageShell
    title="创意页"
    description="生成多个创意，查看卖点、世界观、主线冲突和长篇潜力，选定后进入设定库。"
  >
    <template #actions>
      <div class="toolbar">
        <button class="toolbar__button" type="button" :disabled="!activeProject" @click="submitGenerate">
          生成创意
        </button>
      </div>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和管理创意。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">创意列表</div>
        <div v-if="state.ideas.length === 0" class="empty-state">
          <div class="empty-state__title">暂无创意</div>
          <p class="empty-state__description">点击“生成创意”，会按当前作品题材生成多个方案。</p>
        </div>
        <div v-else class="stack">
          <article
            v-for="idea in state.ideas"
            :key="idea.id"
            class="card"
            :class="{ 'card--selected': idea.selected }"
          >
            <div class="card__row">
              <div>
                <div class="card__title">{{ idea.title }}</div>
                <p class="helper-text">{{ idea.sellingPoint }}</p>
              </div>
              <span class="badge" :class="{ 'badge--ok': idea.selected }">
                {{ idea.selected ? '已选定' : `${idea.score} 分` }}
              </span>
            </div>

            <div class="metrics">
              <div class="metric">
                <div class="metric__label">字数预估</div>
                <div class="metric__value">{{ idea.estimatedWords }}</div>
              </div>
              <div class="metric">
                <div class="metric__label">世界观</div>
                <div class="metric__value">可扩展</div>
              </div>
              <div class="metric">
                <div class="metric__label">主线冲突</div>
                <div class="metric__value">明确</div>
              </div>
            </div>

            <p class="section-title">主线冲突</p>
            <p class="helper-text">{{ idea.mainConflict }}</p>

            <label class="field">
              <span>创意内容</span>
              <textarea
                v-model="idea.content"
                rows="5"
                @blur="updateIdea(idea.id, idea.content)"
              ></textarea>
            </label>

            <label class="field">
              <span>修改意见</span>
              <textarea
                v-model="rewriteSuggestions[idea.id]"
                rows="3"
                placeholder="例如：降低设定复杂度，加强主角现实目标"
              ></textarea>
            </label>

            <div class="toolbar">
              <button class="toolbar__button" type="button" @click="selectIdea(idea.id)">选定创意</button>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="submitRewrite(idea.id)">
                按意见重生成
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card__title">评估维度</div>
        <ul class="tag-list">
          <li v-for="metric in ideaMetrics" :key="metric" class="tag-list__item">{{ metric }}</li>
        </ul>
        <p class="helper-text">
          当前选定：{{ selectedIdea?.title ?? '尚未选定。选定后才能生成设定库。' }}
        </p>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>
    </div>
  </PageShell>
</template>
