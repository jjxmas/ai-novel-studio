<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';

import PageShell from '@/components/PageShell.vue';
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
  const fallback = state.ideas[0] ?? null;
  return state.ideas.find((idea) => idea.id === activeIdeaId.value) ?? fallback;
});

watch(
  () => state.ideas.map((idea) => idea.id).join(','),
  () => {
    if (!activeIdea.value) {
      activeIdeaId.value = null;
      return;
    }
    activeIdeaId.value = activeIdea.value.id;
  },
);

onMounted(() => {
  void loadIdeas().then((ideas) => {
    activeIdeaId.value = ideas[0]?.id ?? null;
  }).catch(() => undefined);
});

async function submitGenerate() {
  const ideas = await generateIdeas(generationForm.suggestion, generationForm.ideaCount);
  activeIdeaId.value = ideas[0]?.id ?? activeIdeaId.value;
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
</script>

<template>
  <PageShell
    title="创意页"
    description="生成多个创意，左侧选择方案，右侧查看和编辑详情。"
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

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">创意列表</div>
        <div v-if="state.ideas.length === 0" class="empty-state">
          <div class="empty-state__title">暂无创意</div>
          <p class="empty-state__description">点击生成创意后，方案标题会显示在这里。</p>
        </div>
        <div v-else class="stack">
          <article
            v-for="idea in state.ideas"
            :key="idea.id"
            class="list-item list-item--clickable"
            :class="{ 'card--selected': idea.id === activeIdea?.id }"
            @click="activeIdeaId = idea.id"
          >
            <div>
              <div class="list-item__title">{{ idea.title }}</div>
              <div class="list-item__text">{{ idea.estimatedWords }} · {{ idea.score }} 分</div>
            </div>
            <span class="badge" :class="{ 'badge--ok': idea.selected }">
              {{ idea.selected ? '已选定' : '候选' }}
            </span>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card__title">创意详情</div>
        <div v-if="!activeIdea" class="empty-state">
          <div class="empty-state__title">尚未选择创意</div>
          <p class="empty-state__description">从左侧列表选择一个创意查看详情。</p>
        </div>
        <div v-else class="stack">
          <div class="card__row">
            <div>
              <h3 class="section-title">{{ activeIdea.title }}</h3>
              <p class="helper-text">当前选定：{{ selectedIdea?.title ?? '尚未选定' }}</p>
            </div>
            <span class="badge" :class="{ 'badge--ok': activeIdea.selected }">
              {{ activeIdea.selected ? '已选定' : `${activeIdea.score} 分` }}
            </span>
          </div>

          <div class="metrics">
            <div class="metric">
              <div class="metric__label">字数预估</div>
              <div class="metric__value">{{ activeIdea.estimatedWords }}</div>
            </div>
            <div class="metric">
              <div class="metric__label">卖点</div>
              <div class="metric__value">{{ activeIdea.sellingPoint || '待补充' }}</div>
            </div>
          </div>

          <label class="field">
            <span>世界观</span>
            <textarea v-model="activeIdea.worldview" rows="4" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>主线冲突</span>
            <textarea v-model="activeIdea.mainConflict" rows="4" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>创意正文</span>
            <textarea v-model="activeIdea.content" class="text-editor" rows="8" @blur="updateIdea(activeIdea)"></textarea>
          </label>
          <label class="field">
            <span>修改意见</span>
            <textarea v-model="rewriteSuggestion" rows="3" placeholder="例如：降低设定复杂度，加强主角现实目标"></textarea>
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
