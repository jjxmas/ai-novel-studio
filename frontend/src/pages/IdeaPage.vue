<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';

import PageShell from '@/components/PageShell.vue';
import type { Idea } from '@/api/types';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const {
  state,
  activeProject,
  selectedIdea,
  loadIdeas,
  generateIdeas,
  selectIdea,
  rewriteIdea,
  updateIdea,
  deleteIdea,
} = useNovelWorkspace();

const activeIdeaId = ref<number | null>(null);
const generationForm = reactive({
  ideaCount: 3,
  suggestion: '',
});
const rewriteSuggestion = ref('');

const activeIdea = computed(() => {
  return state.ideas.find((idea) => idea.id === activeIdeaId.value) ?? null;
});

const evaluationMetrics = computed(() => {
  if (!activeIdea.value) {
    return [];
  }
  return [
    { label: '长篇潜力', value: activeIdea.value.longFormPotentialScore },
    { label: '冲突强度', value: activeIdea.value.conflictScore },
    { label: '新意程度', value: activeIdea.value.noveltyScore },
    { label: '新手友好', value: activeIdea.value.beginnerFriendlinessScore },
    { label: '平台适配', value: activeIdea.value.platformFitScore },
  ];
});

watch(
  () => state.ideas.map((idea) => idea.id).join(','),
  () => {
    if (!activeIdea.value) {
      activeIdeaId.value = null;
    }
  },
);

onMounted(() => {
  void loadIdeas().catch(() => undefined);
});

async function submitGenerate() {
  const ideas = await generateIdeas(generationForm.suggestion, generationForm.ideaCount);
  activeIdeaId.value = ideas[0]?.id ?? null;
}

async function submitRewrite() {
  if (!activeIdea.value) {
    return;
  }
  await rewriteIdea(activeIdea.value.id, rewriteSuggestion.value);
  rewriteSuggestion.value = '';
}

async function submitDelete() {
  if (!activeIdea.value || activeIdea.value.selected) {
    return;
  }
  const deletedId = activeIdea.value.id;
  await deleteIdea(deletedId);
  activeIdeaId.value = state.ideas[0]?.id ?? null;
}

function hasEvaluation(idea: Idea | null) {
  if (!idea) {
    return false;
  }
  return Boolean(
    idea.longFormPotentialScore != null
    || idea.conflictScore != null
    || idea.noveltyScore != null
    || idea.beginnerFriendlinessScore != null
    || idea.platformFitScore != null
    || idea.overallComment
    || idea.strengths?.length
    || idea.risks?.length
    || idea.suggestions?.length,
  );
}

function formatRiskLevel(riskLevel?: string | null) {
  if (riskLevel === 'high') {
    return '高风险';
  }
  if (riskLevel === 'low') {
    return '低风险';
  }
  return '中风险';
}

function riskBadgeClass(riskLevel?: string | null) {
  return riskLevel === 'high' ? 'badge--warn' : '';
}

function sellingPoints(text: string) {
  return text
    .split(/[;；\n]+/)
    .map((item) => item.trim().replace(/^卖点\s*\d+[.。:：、]?\s*/, ''))
    .filter(Boolean);
}
</script>

<template>
  <PageShell
    title="创意页"
    description="生成多个创意方案，先比较列表，点击候选项后进入详情。"
  >
    <template #actions>
      <div class="toolbar">
        <label class="field field--compact">
          <span>数量</span>
          <input v-model.number="generationForm.ideaCount" type="number" min="1" max="5" />
        </label>
        <input
          v-model="generationForm.suggestion"
          class="toolbar__input"
          type="text"
          placeholder="可选：补充创意方向"
        />
        <button class="toolbar__button" type="button" :disabled="!activeProject" @click="submitGenerate">
          生成创意
        </button>
      </div>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和管理创意。</p>
    </div>

    <section v-else-if="!activeIdea" class="card">
      <div class="card__title">创意列表</div>
      <div v-if="state.ideas.length === 0" class="empty-state">
        <div class="empty-state__title">暂无创意</div>
        <p class="empty-state__description">点击生成创意后，候选方案会显示在这里。</p>
      </div>
      <div v-else class="stack">
        <article
          v-for="idea in state.ideas"
          :key="idea.id"
          class="list-item list-item--clickable"
          @click="activeIdeaId = idea.id"
        >
          <div>
            <div class="list-item__title">{{ idea.title }}</div>
            <div class="list-item__text">{{ idea.estimatedWords }} · 长篇潜力 {{ idea.score }} 分</div>
          </div>
          <div class="idea-list-meta">
            <span v-if="idea.riskLevel" class="badge" :class="riskBadgeClass(idea.riskLevel)">
              {{ formatRiskLevel(idea.riskLevel) }}
            </span>
            <span class="badge" :class="{ 'badge--ok': idea.selected }">
              {{ idea.selected ? '已选定' : '候选' }}
            </span>
          </div>
        </article>
      </div>
    </section>

    <div v-else class="idea-detail-layout">
      <section class="card">
        <div class="card__row">
          <div class="card__title">创意详情</div>
          <button class="toolbar__button toolbar__button--ghost" type="button" @click="activeIdeaId = null">
            返回列表
          </button>
        </div>
        <div v-if="!activeIdea" class="empty-state">
          <div class="empty-state__title">尚未选择创意</div>
          <p class="empty-state__description">从左侧列表选择一个创意，即可查看内容和评价。</p>
        </div>
        <div v-else class="stack">
          <div class="card__row">
            <div>
              <h3 class="section-title">{{ activeIdea.title }}</h3>
              <p class="helper-text">当前选定：{{ selectedIdea?.title ?? '尚未选定' }}</p>
            </div>
            <div class="badge-group">
              <span v-if="activeIdea.riskLevel" class="badge" :class="riskBadgeClass(activeIdea.riskLevel)">
                {{ formatRiskLevel(activeIdea.riskLevel) }}
              </span>
              <span class="badge" :class="{ 'badge--ok': activeIdea.selected }">
                {{ activeIdea.selected ? '已选定' : `${activeIdea.score} 分` }}
              </span>
            </div>
          </div>

          <div class="metrics metrics--stacked">
            <div class="metric">
              <div class="metric__label">字数预估</div>
              <div class="metric__value">{{ activeIdea.estimatedWords }}</div>
            </div>
            <div class="metric">
              <div class="metric__label">卖点</div>
              <ol v-if="sellingPoints(activeIdea.sellingPoint).length" class="plain-list">
                <li v-for="item in sellingPoints(activeIdea.sellingPoint)" :key="item">{{ item }}</li>
              </ol>
              <div v-else class="metric__value metric__value--text">待补充</div>
            </div>
          </div>

          <section class="stack">
            <div class="section-title">AI 评价</div>
            <div v-if="hasEvaluation(activeIdea)" class="stack">
              <div class="hint-box hint-box--accent">
                {{ activeIdea.overallComment || '当前创意已生成评分，但暂时没有总体评价文本。' }}
              </div>

              <div class="metrics idea-score-grid">
                <div v-for="metric in evaluationMetrics" :key="metric.label" class="metric">
                  <div class="metric__label">{{ metric.label }}</div>
                  <div class="metric__value">{{ metric.value ?? '--' }}</div>
                </div>
              </div>

              <div class="grid grid--three">
                <section class="info-panel">
                  <h4 class="info-panel__title">优势</h4>
                  <ul v-if="activeIdea.strengths?.length" class="plain-list">
                    <li v-for="item in activeIdea.strengths" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else class="helper-text">暂无优势分析。</p>
                </section>

                <section class="info-panel">
                  <h4 class="info-panel__title">风险</h4>
                  <ul v-if="activeIdea.risks?.length" class="plain-list">
                    <li v-for="item in activeIdea.risks" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else class="helper-text">暂无风险提示。</p>
                </section>

                <section class="info-panel">
                  <h4 class="info-panel__title">建议</h4>
                  <ul v-if="activeIdea.suggestions?.length" class="plain-list">
                    <li v-for="item in activeIdea.suggestions" :key="item">{{ item }}</li>
                  </ul>
                  <p v-else class="helper-text">暂无修改建议。</p>
                </section>
              </div>
            </div>
            <div v-else class="empty-state">
              <div class="empty-state__title">暂无评价数据</div>
              <p class="empty-state__description">这个创意还没有可展示的 AI 评价内容。</p>
            </div>
          </section>

          <label class="field">
            <span>世界观</span>
            <textarea v-model="activeIdea.worldview" class="idea-textarea" rows="8" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>主线冲突</span>
            <textarea v-model="activeIdea.mainConflict" class="idea-textarea" rows="8" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>创意正文</span>
            <textarea v-model="activeIdea.content" class="text-editor" rows="8" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>修改意见</span>
            <textarea
              v-model="rewriteSuggestion"
              rows="3"
              placeholder="例如：降低设定复杂度，加强主角现实目标"
            ></textarea>
          </label>

          <div class="toolbar">
            <button class="toolbar__button" type="button" @click="selectIdea(activeIdea.id)">选定创意</button>
            <button class="toolbar__button toolbar__button--ghost" type="button" @click="submitRewrite">
              按意见重生成
            </button>
            <button
              class="toolbar__button toolbar__button--ghost"
              type="button"
              :disabled="activeIdea.selected"
              @click="submitDelete"
            >
              删除
            </button>
          </div>
          <p class="helper-text">{{ state.lastMessage }}</p>
        </div>
      </section>
    </div>
  </PageShell>
</template>

<style scoped>
.idea-detail-layout {
  display: grid;
  gap: 16px;
}

.idea-textarea {
  min-height: 180px;
}
</style>
