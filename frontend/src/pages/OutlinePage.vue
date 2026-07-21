<script setup lang="ts">
import { onMounted, ref, watch } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const outlineLevels = [
  { key: 'global', title: '全局大纲', hint: '总方向、总冲突、总节奏' },
  { key: 'volume', title: '分卷大纲', hint: '每一卷的目标和转折' },
  { key: 'arc', title: '剧情单元', hint: '中段推进单元，方便长篇控制' },
  { key: 'chapter', title: '章节大纲', hint: '每章目标、冲突和结尾钩子' },
];
const {
  state,
  activeProject,
  canGenerateOutline,
  canGenerateChapters,
  loadSettingLibrary,
  loadOutline,
  loadChapters,
  generateOutline,
  generateChapterOutlines,
  updateOutline,
  confirmOutline,
} = useNovelWorkspace();

const activeLevel = ref('global');

function editOutline(event: Event) {
  if (!state.outline) {
    return;
  }
  state.outline.content = (event.target as HTMLTextAreaElement).value;
}

function saveOutline() {
  if (state.outline) {
    updateOutline(state.outline.content);
  }
}

async function submitGenerateChapterOutlines() {
  await generateChapterOutlines();
}

onMounted(() => {
  void loadSettingLibrary().catch(() => undefined);
  void loadOutline().catch(() => undefined);
});

watch(activeLevel, (level) => {
  if (level === 'chapter') {
    void loadChapters().catch(() => undefined);
  }
});
</script>

<template>
  <PageShell
    title="大纲页"
    description="第二版先实现全局大纲生成、编辑和确认，确认后解锁章节生成。"
  >
    <template #actions>
      <div class="toolbar">
        <button
          v-if="activeLevel === 'global'"
          class="toolbar__button"
          type="button"
          :disabled="!activeProject || !canGenerateOutline"
          @click="generateOutline"
        >
          生成全局大纲
        </button>
        <button
          v-else-if="activeLevel === 'chapter'"
          class="toolbar__button"
          type="button"
          :disabled="!activeProject || !canGenerateChapters"
          @click="submitGenerateChapterOutlines"
        >
          生成章节大纲
        </button>
      </div>
    </template>

    <div v-if="!activeProject" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和确认大纲。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">大纲层级</div>
        <div class="stack">
          <article
            v-for="item in outlineLevels"
            :key="item.key"
            class="list-item list-item--clickable"
            :class="{ 'card--selected': activeLevel === item.key }"
            @click="activeLevel = item.key"
          >
            <div>
              <div class="list-item__title">{{ item.title }}</div>
              <div class="list-item__text">{{ item.hint }}</div>
            </div>
            <span
              class="badge"
              :class="{
                'badge--ok': item.key === 'global' && state.outline?.confirmed,
                'badge--warn': item.key === 'volume' || item.key === 'arc',
              }"
            >
              {{
                item.key === 'global' && state.outline?.confirmed
                  ? '已确认'
                  : item.key === 'chapter' && state.chapters.length > 0
                    ? '已生成'
                    : item.key === 'volume' || item.key === 'arc'
                      ? '预留'
                      : '可操作'
              }}
            </span>
          </article>
        </div>
      </section>

      <section v-if="activeLevel === 'global'" class="card">
        <div class="card__title">全局大纲</div>
        <div v-if="!state.outline" class="empty-state">
          <div class="empty-state__title">尚未生成全局大纲</div>
          <p class="empty-state__description">请先确认设定库，再生成全局大纲。</p>
        </div>
        <div v-else class="stack">
          <label class="field">
            <span>可直接编辑</span>
            <textarea
              :value="state.outline.content"
              class="text-editor"
              rows="10"
              @input="editOutline"
              @blur="saveOutline"
            ></textarea>
          </label>
          <div class="toolbar">
            <button class="toolbar__button" type="button" @click="confirmOutline">确认全局大纲</button>
          </div>
        </div>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>

      <section v-else-if="activeLevel === 'chapter'" class="card">
        <div class="card__title">章节大纲</div>
        <div v-if="state.chapters.length === 0" class="empty-state">
          <div class="empty-state__title">尚未生成章节大纲</div>
          <p class="empty-state__description">确认全局大纲后，可以在这里生成章节大纲。</p>
        </div>
        <div v-else class="stack">
          <article v-for="chapter in state.chapters" :key="chapter.id" class="list-item">
            <div>
              <div class="list-item__title">{{ chapter.title }}</div>
              <div class="list-item__text">{{ chapter.outline }}</div>
            </div>
            <span class="badge">{{ chapter.content ? '已有正文' : '待正文' }}</span>
          </article>
        </div>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>

      <section v-else class="card">
        <div class="card__title">{{ outlineLevels.find((item) => item.key === activeLevel)?.title }}</div>
        <div class="empty-state">
          <div class="empty-state__title">后续版本扩展</div>
          <p class="empty-state__description">第二版先跑通全局大纲和章节大纲，分卷与剧情单元会在后续版本细化。</p>
        </div>
      </section>
    </div>
  </PageShell>
</template>
