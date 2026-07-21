<script setup lang="ts">
import { onMounted } from 'vue';
import { RouterLink } from 'vue-router';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const { state, activeProject, loadProjects, selectProject, loadVersions } = useNovelWorkspace();

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
  void loadVersions().catch(() => undefined);
}

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
            <span class="badge">{{ stageLabels[item.stage] }}</span>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card__title">待确认事项</div>
        <ol class="number-list">
          <li v-for="item in pendingItems" :key="item">{{ item }}</li>
        </ol>
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
  </PageShell>
</template>
