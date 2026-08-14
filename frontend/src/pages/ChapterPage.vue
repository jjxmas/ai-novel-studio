<script setup lang="ts">
import { computed, onMounted, ref } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const chapterBlocks = ['章节列表', '章节大纲', '场景拆分', '正文编辑器', '上一章摘要', '相关设定'];
const {
  state,
  activeProject,
  loadChapters,
  loadProjectMemory,
  generateChapterContent,
  updateChapterContent,
  createCheck,
} = useNovelWorkspace();

const activeChapterId = ref<number | null>(null);
const rewriteSuggestion = ref('');
const isGeneratingContent = ref(false);

const activeChapter = computed(() => {
  const fallback = state.chapters[0] ?? null;
  return state.chapters.find((chapter) => chapter.id === activeChapterId.value) ?? fallback;
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
});
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

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">章节结构</div>
        <div class="stack">
          <ul class="tag-list">
            <li v-for="item in chapterBlocks" :key="item" class="tag-list__item">{{ item }}</li>
          </ul>
          <div v-if="state.chapters.length === 0" class="empty-state">
            <div class="empty-state__title">尚未生成章节大纲</div>
            <p class="empty-state__description">请到大纲页生成章节大纲后，再回到这里写正文。</p>
          </div>
          <article
            v-for="chapter in state.chapters"
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
