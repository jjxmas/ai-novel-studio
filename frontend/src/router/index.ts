import { createRouter, createWebHistory, type RouteRecordRaw } from 'vue-router';

import AppLayout from '@/layouts/AppLayout.vue';
import CheckPage from '@/pages/CheckPage.vue';
import ChapterPage from '@/pages/ChapterPage.vue';
import ExportPage from '@/pages/ExportPage.vue';
import IdeaPage from '@/pages/IdeaPage.vue';
import ModelConfigPage from '@/pages/ModelConfigPage.vue';
import OutlinePage from '@/pages/OutlinePage.vue';
import ProjectCreatePage from '@/pages/ProjectCreatePage.vue';
import SettingLibraryPage from '@/pages/SettingLibraryPage.vue';
import WorkbenchPage from '@/pages/WorkbenchPage.vue';

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    component: AppLayout,
    children: [
      {
        path: '',
        name: 'workbench',
        component: WorkbenchPage,
        meta: {
          title: '工作台',
          description: '查看最近作品、待确认事项和快速入口。',
        },
      },
      {
        path: 'projects/new',
        name: 'project-create',
        component: ProjectCreatePage,
        meta: {
          title: '新建作品',
          description: '创建小说项目，填写类型、简介和基础偏好。',
        },
      },
      {
        path: 'models',
        name: 'model-config',
        component: ModelConfigPage,
        meta: {
          title: '模型配置',
          description: '预留多模型配置和用途分配入口。',
        },
      },
      {
        path: 'ideas',
        name: 'ideas',
        component: IdeaPage,
        meta: {
          title: '创意页',
          description: '展示创意列表、卖点和长篇潜力评估。',
        },
      },
      {
        path: 'settings',
        name: 'settings',
        component: SettingLibraryPage,
        meta: {
          title: '设定库',
          description: '集中管理人物、地点、势力、规则和伏笔。',
        },
      },
      {
        path: 'outlines',
        name: 'outlines',
        component: OutlinePage,
        meta: {
          title: '大纲页',
          description: '承载全局、分卷、剧情单元和章节大纲。',
        },
      },
      {
        path: 'chapters',
        name: 'chapters',
        component: ChapterPage,
        meta: {
          title: '章节页',
          description: '预留章节大纲、正文、上下文和编辑区。',
        },
      },
      {
        path: 'checks',
        name: 'checks',
        component: CheckPage,
        meta: {
          title: '检查页',
          description: '预留连续性、风格和设定冲突检查入口。',
        },
      },
      {
        path: 'exports',
        name: 'exports',
        component: ExportPage,
        meta: {
          title: '导出页',
          description: '预留 Markdown 与 TXT 导出入口。',
        },
      },
    ],
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/',
  },
];

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { top: 0 };
  },
});

export default router;
