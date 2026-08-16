<script setup lang="ts">
import { computed, onMounted, reactive, ref, watch } from 'vue';
import { RouterLink } from 'vue-router';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';
import type { ProjectUpdateRequest } from '@/api/types';
import { tomatoGenreTagGroups } from '@/constants/genreOptions';

const { state, activeProject, loadProjects, selectProject, loadVersions, updateProject, deleteProject } = useNovelWorkspace();

const form = reactive({
  title: '',
  genres: '',
  projectBrief: '',
  targetWordCountMin: 0,
  targetWordCountMax: 0,
  targetChapterWordCount: 3000,
  platformTarget: '',
  stylePreference: '',
});
const selectedGenreTag = ref('');

const selectedGenres = computed(() =>
  form.genres
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean),
);

const stageLabels = {
  idea: '创意阶段',
  setting: '设定库阶段',
  outline: '大纲阶段',
  chapter: '章节阶段',
  check: '检查阶段',
  export: '导出阶段',
};

const pendingItems = [
  '创意选定后才能生成设定库',
  '设定库确认后才能生成全局大纲',
  '全局大纲确认后先去大纲页生成章节大纲，再到章节页写正文',
];

function activateProject(projectId: number) {
  selectProject(projectId);
  fillForm();
  void loadVersions().catch(() => undefined);
}

function fillForm() {
  if (!activeProject.value) {
    form.title = '';
    form.genres = '';
    form.projectBrief = '';
    form.targetWordCountMin = 0;
    form.targetWordCountMax = 0;
    form.targetChapterWordCount = 3000;
    form.platformTarget = '';
    form.stylePreference = '';
    return;
  }
  form.title = activeProject.value.title;
  form.genres = activeProject.value.genres.join('、');
  form.projectBrief = activeProject.value.projectBrief;
  form.targetWordCountMin = activeProject.value.targetWordCountMin;
  form.targetWordCountMax = activeProject.value.targetWordCountMax;
  form.targetChapterWordCount = activeProject.value.targetChapterWordCount;
  form.platformTarget = activeProject.value.platformTarget;
  form.stylePreference = activeProject.value.stylePreference;
}

function setGenres(genres: string[]) {
  form.genres = Array.from(new Set(genres)).join('、');
}

function addSelectedGenre() {
  if (!selectedGenreTag.value) {
    return;
  }
  setGenres([...selectedGenres.value, selectedGenreTag.value]);
  selectedGenreTag.value = '';
}

function removeGenre(tag: string) {
  setGenres(selectedGenres.value.filter((item) => item !== tag));
}

async function submitProject() {
  if (!activeProject.value) {
    return;
  }
  const payload: ProjectUpdateRequest = {
    title: form.title.trim(),
    genres: form.genres
      .split(/[、,，\s]+/)
      .map((item) => item.trim())
      .filter(Boolean),
    projectBrief: form.projectBrief.trim(),
    targetWordCountMin: Number(form.targetWordCountMin),
    targetWordCountMax: Number(form.targetWordCountMax),
    targetChapterWordCount: Number(form.targetChapterWordCount),
    platformTarget: form.platformTarget.trim(),
    stylePreference: form.stylePreference.trim(),
  };
  if (!payload.title || payload.genres.length === 0 || !payload.projectBrief) {
    state.lastMessage = '作品名、类型和模糊描述不能为空。';
    return;
  }
  await updateProject(activeProject.value.id, payload);
  void loadVersions().catch(() => undefined);
}

async function submitDeleteProject(projectId: number, title: string, event?: Event) {
  event?.stopPropagation();
  if (!window.confirm(`确定删除《${title}》吗？删除后作品、创意、大纲和章节都会一起移除。`)) {
    return;
  }
  await deleteProject(projectId);
  void loadProjects().catch(() => undefined);
}

watch(activeProject, fillForm);

onMounted(() => {
  void loadProjects().catch(() => undefined);
});
</script>

<template>
  <PageShell
    title="工作台"
    description="从作品列表进入创作流程，按阶段确认后继续下一步。"
  >
    <template #actions>
      <div class="toolbar">
        <RouterLink class="toolbar__button" to="/projects/new">新建作品</RouterLink>
        <RouterLink class="toolbar__button toolbar__button--ghost" to="/models">模型配置</RouterLink>
      </div>
    </template>

    <div class="grid grid--two">
      <section class="card">
        <div class="card__title">最近作品</div>
        <div v-if="state.projects.length === 0" class="empty-state">
          <div class="empty-state__title">暂无作品</div>
          <p class="empty-state__description">先新建作品，工作台会展示你的作品列表。</p>
        </div>
        <div v-else class="stack">
          <article
            v-for="item in state.projects"
            :key="item.id"
            class="list-item list-item--clickable"
            :class="{ 'card--selected': item.id === state.activeProjectId }"
            @click="activateProject(item.id)"
          >
            <div>
              <div class="list-item__title">{{ item.title }}</div>
              <div class="list-item__text">
                {{ item.genres.join(' + ') }} · {{ item.platformTarget }} · {{ item.updatedAt }}
              </div>
            </div>
            <div class="idea-list-meta">
              <span class="badge">{{ stageLabels[item.stage] }}</span>
              <button
                class="toolbar__button toolbar__button--danger toolbar__button--small"
                type="button"
                @click="submitDeleteProject(item.id, item.title, $event)"
              >
                删除
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card__title">作品详情</div>
        <div v-if="!activeProject" class="empty-state">
          <div class="empty-state__title">请选择作品</div>
          <p class="empty-state__description">点击左侧作品后，这里会显示详情和可修改项。</p>
        </div>
        <form v-else class="form-grid" @submit.prevent="submitProject">
          <label class="field">
            <span>作品名</span>
            <input v-model="form.title" type="text" />
          </label>
          <div class="field">
            <span>类型</span>
            <select v-model="selectedGenreTag" @change="addSelectedGenre">
              <option value="" disabled>选择番茄标签</option>
              <optgroup v-for="group in tomatoGenreTagGroups" :key="group.name" :label="group.name">
                <option v-for="tag in group.tags" :key="`${group.name}-${tag}`" :value="tag">
                  {{ tag }}
                </option>
              </optgroup>
            </select>
            <div class="tag-list selected-genre-list" aria-label="已选择小说类型">
              <button
                v-for="tag in selectedGenres"
                :key="tag"
                class="tag-list__item tag-list__item--button"
                type="button"
                @click="removeGenre(tag)"
              >
                {{ tag }} ×
              </button>
            </div>
          </div>
          <label class="field field--full">
            <span>模糊描述</span>
            <textarea v-model="form.projectBrief" rows="5"></textarea>
          </label>
          <label class="field">
            <span>最少字数</span>
            <input v-model.number="form.targetWordCountMin" type="number" min="0" />
          </label>
          <label class="field">
            <span>最多字数</span>
            <input v-model.number="form.targetWordCountMax" type="number" min="0" />
          </label>
          <label class="field">
            <span>每章目标字数</span>
            <input v-model.number="form.targetChapterWordCount" type="number" min="500" step="100" />
          </label>
          <label class="field">
            <span>目标平台</span>
            <input v-model="form.platformTarget" type="text" />
          </label>
          <label class="field field--full">
            <span>风格偏好</span>
            <input v-model="form.stylePreference" type="text" placeholder="例如：节奏快、对白自然、少解释" />
          </label>
          <div class="field field--full">
            <button class="toolbar__button" type="submit">保存作品信息</button>
          </div>
        </form>
      </section>
    </div>

    <div class="grid grid--two">
      <section class="card">
        <div class="card__title">当前流程</div>
        <div v-if="!activeProject" class="empty-state">
          <div class="empty-state__title">尚未选择作品</div>
          <p class="empty-state__description">请先从作品列表中选择一个作品。</p>
        </div>
        <div v-else class="flow">
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'idea' }">创意</span>
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'setting' }">设定库</span>
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'outline' }">大纲</span>
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'chapter' }">章节</span>
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'check' }">检查</span>
          <span class="flow__step" :class="{ 'flow__step--active': activeProject?.stage === 'export' }">导出</span>
        </div>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>

      <section class="card">
        <div class="card__title">版本记录</div>
        <div class="version-list">
          <div v-if="!activeProject" class="helper-text">选择作品后查看版本记录。</div>
          <div v-else-if="state.versions.length === 0" class="helper-text">当前作品还没有版本记录。</div>
          <div v-for="version in state.versions" :key="version.id" class="version-item">
            <div class="version-item__title">{{ version.summary }}</div>
            <div class="version-item__meta">
              {{ version.targetType }} · {{ version.actionType }} · {{ version.createdAt }}
            </div>
          </div>
        </div>
      </section>
    </div>

    <section class="card">
      <div class="card__title">待确认事项</div>
      <ol class="number-list">
        <li v-for="item in pendingItems" :key="item">{{ item }}</li>
      </ol>
    </section>
  </PageShell>
</template>
