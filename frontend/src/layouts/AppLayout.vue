<script setup lang="ts">
import { computed, ref, watch } from 'vue';
import { RouterLink, useRoute } from 'vue-router';

const route = useRoute();
const sidebarCollapsed = ref(localStorage.getItem('sidebarCollapsed') === 'true');

const currentTitle = computed(() => String(route.meta.title ?? '工作台'));
const currentDescription = computed(() => String(route.meta.description ?? ''));

const navItems = [
  { label: '工作台', to: '/' },
  { label: '新建作品', to: '/projects/new' },
  { label: '模型配置', to: '/models' },
  { label: '创意页', to: '/ideas' },
  { label: '设定库', to: '/settings' },
  { label: '大纲页', to: '/outlines' },
  { label: '章节页', to: '/chapters' },
  { label: '任务中心', to: '/tasks' },
  { label: '检查页', to: '/checks' },
  { label: '导出页', to: '/exports' },
];

const isActive = (path: string) => {
  if (path === '/') {
    return route.path === '/';
  }

  return route.path.startsWith(path);
};

watch(sidebarCollapsed, (collapsed) => {
  localStorage.setItem('sidebarCollapsed', String(collapsed));
});
</script>

<template>
  <div class="app-shell" :class="{ 'app-shell--sidebar-collapsed': sidebarCollapsed }">
    <aside class="sidebar" :aria-hidden="sidebarCollapsed">
      <button
        class="sidebar-toggle"
        type="button"
        :aria-label="sidebarCollapsed ? '展开侧边栏' : '收起侧边栏'"
        @click="sidebarCollapsed = !sidebarCollapsed"
      >
        {{ sidebarCollapsed ? '>' : '<' }}
      </button>
      <div class="brand">
        <div class="brand__title">AI 长篇小说工作台</div>
        <div class="brand__subtitle">Vue3 / Spring Boot / MySQL</div>
      </div>

      <RouterLink class="primary-action" to="/projects/new">+ 新建作品</RouterLink>

      <nav class="nav">
        <RouterLink
          v-for="item in navItems"
          :key="item.to"
          :to="item.to"
          class="nav-link"
          :class="{ 'is-active': isActive(item.to) }"
        >
          {{ item.label }}
        </RouterLink>
      </nav>
    </aside>

    <button
      v-if="sidebarCollapsed"
      class="sidebar-restore"
      type="button"
      aria-label="展开侧边栏"
      @click="sidebarCollapsed = false"
    >
      >
    </button>

    <div class="content">
      <header class="topbar">
        <div>
          <h1 class="topbar__title">{{ currentTitle }}</h1>
          <p class="topbar__description">{{ currentDescription }}</p>
        </div>
        <div class="topbar__meta">
          <span class="meta-pill">第二版 MVP</span>
          <span class="meta-pill meta-pill--soft">REST API + mock fallback</span>
        </div>
      </header>

      <main class="main">
        <router-view />
      </main>
    </div>
  </div>
</template>
