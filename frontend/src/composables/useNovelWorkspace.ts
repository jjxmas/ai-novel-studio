import { computed, reactive } from 'vue';

import { novelApi } from '@/api/novelApi';
import type {
  Chapter,
  CheckResult,
  ContentVersion,
  ExportRecord,
  GlobalOutline,
  Idea,
  ModelConfig,
  ModelConfigRequest,
  Project,
  ProjectCreateRequest,
  ProjectMemory,
  SettingLibrary,
  WorkflowStage,
} from '@/api/types';

interface WorkspaceState {
  projects: Project[];
  activeProjectId: number | null;
  modelConfigs: ModelConfig[];
  ideas: Idea[];
  settingLibrary: SettingLibrary | null;
  outline: GlobalOutline | null;
  chapters: Chapter[];
  projectMemory: ProjectMemory | null;
  checks: CheckResult[];
  exports: ExportRecord[];
  versions: ContentVersion[];
  lastMessage: string;
}

const nowText = () => new Date().toLocaleString('zh-CN', { hour12: false });

const state = reactive<WorkspaceState>({
  projects: [],
  activeProjectId: null,
  modelConfigs: [
    {
      id: 1,
      provider: 'OpenAI 兼容',
      displayName: '默认 mock 模型',
      baseUrl: '',
      modelName: 'mock-novel-model',
      usageType: '通用生成',
      hasApiKey: true,
      defaultModel: true,
      enabled: true,
    },
  ],
  ideas: [],
  settingLibrary: null,
  outline: null,
  chapters: [],
  projectMemory: null,
  checks: [],
  exports: [],
  versions: [],
  lastMessage: '请先在工作台选择或创建作品。',
});

let nextId = 100;

function next() {
  nextId += 1;
  return nextId;
}

function withFallback<T>(
  action: Promise<T>,
  fallback: () => T,
  message: string,
  isValid: (data: T) => boolean = () => true,
): Promise<T> {
  const useFallback = () => {
    const data = fallback();
    state.lastMessage = `${message}（当前使用前端 mock 数据）`;
    return data;
  };

  return action
    .then((data) => {
      if (!isValid(data)) {
        return useFallback();
      }
      state.lastMessage = message;
      return data;
    })
    .catch((error) => {
      if (error instanceof Error && error.message === 'NETWORK_UNAVAILABLE') {
        return useFallback();
      }
      state.lastMessage = error instanceof Error
        ? error.message.replace('BUSINESS_ERROR:', '')
        : '请求失败';
      throw error;
    });
}

function addVersion(targetType: string, targetId: number, actionType: string, summary: string) {
  state.versions.unshift({
    id: next(),
    projectId: state.activeProjectId ?? 0,
    targetType,
    targetId,
    actionType,
    summary,
    createdAt: nowText(),
  });
}

function setProjectStage(stage: WorkflowStage) {
  const project = state.projects.find((item) => item.id === state.activeProjectId);
  if (project) {
    project.stage = stage;
    project.updatedAt = nowText();
  }
}

const activeProject = computed(() =>
  state.projects.find((project) => project.id === state.activeProjectId) ?? null,
);

const selectedIdea = computed(() => state.ideas.find((idea) => idea.selected) ?? null);

const canGenerateSetting = computed(() => Boolean(selectedIdea.value));
const canGenerateOutline = computed(() => Boolean(state.settingLibrary?.confirmed));
const canGenerateChapters = computed(() => Boolean(state.outline?.confirmed));
const canCheck = computed(() => state.chapters.some((chapter) => chapter.content.trim().length > 0));

const isIdeaList = (data: Idea[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.title && item.sellingPoint));
const isChapterList = (data: Chapter[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.title && item.outline));
const isCheckList = (data: CheckResult[]) =>
  Array.isArray(data) && data.every((item) => Boolean(item.type && item.summary));

export function useNovelWorkspace() {
  function resetProjectData() {
    state.ideas = [];
    state.settingLibrary = null;
    state.outline = null;
    state.chapters = [];
    state.projectMemory = null;
    state.checks = [];
    state.exports = [];
    state.versions = [];
  }

  async function loadProjects() {
    const projects = await withFallback(novelApi.listProjects(), () => [], '作品列表已加载');
    state.projects = projects;
    return projects;
  }

  function selectProject(projectId: number) {
    state.activeProjectId = projectId;
    resetProjectData();
    state.lastMessage = '作品已选中，请从侧边栏进入对应流程。';
  }

  async function loadIdeas() {
    if (!state.activeProjectId) {
      return [];
    }
    const ideas = await withFallback(novelApi.listIdeas(state.activeProjectId), () => state.ideas, '创意列表已加载', isIdeaList);
    state.ideas = ideas;
    return ideas;
  }

  async function loadSettingLibrary() {
    if (!state.activeProjectId) {
      return null;
    }
    const setting = await novelApi.getSettingLibrary(state.activeProjectId).catch(() => null);
    state.settingLibrary = setting;
    return setting;
  }

  async function loadOutline() {
    if (!state.activeProjectId) {
      return null;
    }
    const outline = await novelApi.getGlobalOutline(state.activeProjectId).catch(() => null);
    state.outline = outline;
    return outline;
  }

  async function loadChapters() {
    if (!state.activeProjectId) {
      return [];
    }
    const chapters = await withFallback(novelApi.listChapters(state.activeProjectId), () => state.chapters, '章节列表已加载', isChapterList);
    state.chapters = chapters;
    return chapters;
  }

  async function loadProjectMemory() {
    if (!state.activeProjectId) {
      return null;
    }
    const memory = await novelApi.getProjectMemory(state.activeProjectId).catch(() => null);
    state.projectMemory = memory;
    return memory;
  }

  async function loadVersions() {
    if (!state.activeProjectId) {
      return [];
    }
    const versions = await withFallback(novelApi.listVersions(state.activeProjectId), () => state.versions, '版本记录已加载');
    state.versions = versions;
    return versions;
  }

  async function createProject(payload: ProjectCreateRequest) {
    const project = await withFallback(
      novelApi.createProject(payload),
      () => ({
        id: next(),
        ...payload,
        stage: 'idea' as const,
        updatedAt: nowText(),
      }),
      '作品已创建',
    );
    state.projects.unshift(project);
    state.activeProjectId = project.id;
    resetProjectData();
    addVersion('project', project.id, 'create', `创建作品《${project.title}》`);
    return project;
  }

  async function createModelConfig(payload: ModelConfigRequest) {
    const model = await withFallback(
      novelApi.createModelConfig(payload),
      () => ({
        id: next(),
        provider: payload.provider,
        displayName: payload.displayName,
        baseUrl: payload.baseUrl,
        modelName: payload.modelName,
        usageType: payload.usageType,
        hasApiKey: payload.apiKey.trim().length > 0,
        defaultModel: payload.defaultModel,
        enabled: payload.enabled,
      }),
      '模型配置已保存',
    );
    if (model.defaultModel) {
      state.modelConfigs.forEach((item) => {
        item.defaultModel = false;
      });
    }
    state.modelConfigs.unshift(model);
    return model;
  }

  async function loadModelConfigs() {
    const models = await withFallback(novelApi.listModelConfigs(), () => state.modelConfigs, '模型配置列表已加载');
    state.modelConfigs = models;
    return models;
  }

  async function updateModelConfig(id: number, payload: ModelConfigRequest) {
    const model = await withFallback(novelApi.updateModelConfig(id, payload), () => ({
      id,
      provider: payload.provider,
      displayName: payload.displayName,
      baseUrl: payload.baseUrl,
      modelName: payload.modelName,
      usageType: payload.usageType,
      hasApiKey: payload.apiKey.trim().length > 0,
      defaultModel: payload.defaultModel,
      enabled: payload.enabled,
    }), '模型配置已修改');
    const index = state.modelConfigs.findIndex((item) => item.id === id);
    if (index >= 0) {
      state.modelConfigs[index] = model;
    }
    await loadModelConfigs().catch(() => undefined);
    return model;
  }

  async function setDefaultModel(id: number) {
    const localModel = state.modelConfigs.find((item) => item.id === id);
    if (!localModel) {
      state.lastMessage = '未找到模型配置。';
      return;
    }
    await withFallback(novelApi.setDefaultModel(id), () => localModel, '默认模型已更新');
    state.modelConfigs.forEach((item) => {
      item.defaultModel = item.id === id;
    });
    await loadModelConfigs().catch(() => undefined);
  }

  async function disableModelConfig(id: number) {
    const localModel = state.modelConfigs.find((item) => item.id === id);
    const model = await withFallback(novelApi.disableModelConfig(id), () => ({
      ...(localModel!),
      enabled: false,
      defaultModel: false,
    }), '模型配置已禁用');
    const index = state.modelConfigs.findIndex((item) => item.id === id);
    if (index >= 0) {
      state.modelConfigs[index] = model;
    }
    await loadModelConfigs().catch(() => undefined);
    return model;
  }

  async function generateIdeas(suggestion = '', ideaCount = 3) {
    if (!state.activeProjectId) {
      return [];
    }
    const project = activeProject.value;
    const ideas = await withFallback(
      novelApi.generateIdeas(state.activeProjectId, suggestion, ideaCount),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: `${project?.genres.join('+') || '长篇'}方案 A：低门槛成长线`,
          sellingPoint: '主角目标清晰，从生活困境切入超凡世界，适合新手持续推进。',
          worldview: '现代都市表层正常，暗线存在资源、传承和势力分层。',
          mainConflict: '主角要保护现实生活，同时被迫参与隐藏秩序的资源争夺。',
          estimatedWords: '120万-180万字',
          score: 86,
          selected: false,
          content: '前期用都市事件建立代入感，中期展开修炼体系和势力矛盾，后期解决城市背后的传承危机。',
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: `${project?.genres.join('+') || '长篇'}方案 B：强冲突逆袭线`,
          sellingPoint: '冲突密度高，章节钩子明显，适合平台连载节奏。',
          worldview: '城市被多个隐秘组织切分，普通人只看到表面秩序。',
          mainConflict: '主角从被误解和压制开始，逐步揭开自己身世与旧盟约。',
          estimatedWords: '150万-220万字',
          score: 82,
          selected: false,
          content: '每卷围绕一个现实麻烦和一个修真危机并行推进，保持长篇扩展空间。',
        },
      ],
      '创意已生成',
      isIdeaList,
    );
    state.ideas = ideas;
    addVersion('idea', ideas[0]?.id ?? 0, 'generate', '生成多个创意方案');
    setProjectStage('idea');
    return ideas;
  }

  async function selectIdea(id: number) {
    await withFallback(novelApi.selectIdea(id), () => state.ideas.find((item) => item.id === id)!, '创意已选定');
    state.ideas.forEach((idea) => {
      idea.selected = idea.id === id;
    });
    addVersion('idea', id, 'confirm', '选定创意方案');
    setProjectStage('setting');
  }

  async function rewriteIdea(id: number, suggestion: string) {
    const idea = state.ideas.find((item) => item.id === id);
    if (!idea) {
      return;
    }
    const projectId = idea.projectId;
    const rewritten = await withFallback(
      novelApi.rewriteIdea(id, suggestion),
      () => ({
        ...idea,
        sellingPoint: `${idea.sellingPoint} 已按意见强化：${suggestion || '增强长篇延展性'}`,
        score: Math.min(99, idea.score + 2),
      }),
      '创意已重生成',
    );
    Object.assign(idea, rewritten, { projectId });
    addVersion('idea', id, 'rewrite', `根据意见重生成创意：${suggestion || '未填写具体意见'}`);
  }

  async function updateIdea(idea: Idea) {
    const localIdea = state.ideas.find((item) => item.id === idea.id);
    if (!localIdea) {
      return;
    }
    localIdea.title = idea.title;
    localIdea.sellingPoint = idea.sellingPoint;
    localIdea.worldview = idea.worldview;
    localIdea.mainConflict = idea.mainConflict;
    localIdea.estimatedWords = idea.estimatedWords;
    localIdea.content = idea.content;
    await withFallback(novelApi.updateIdea(localIdea), () => localIdea, '创意修改已保存');
    addVersion('idea', idea.id, 'edit', '用户直接修改创意内容');
  }

  async function deleteIdea(id: number) {
    const idea = state.ideas.find((item) => item.id === id);
    if (!idea) {
      return;
    }
    await withFallback(novelApi.deleteIdea(id), () => undefined, '创意已删除');
    state.ideas = state.ideas.filter((item) => item.id !== id);
    addVersion('idea', id, 'delete', '删除创意方案');
  }

  async function generateSettingLibrary() {
    if (!canGenerateSetting.value || !state.activeProjectId) {
      state.lastMessage = '请先选定创意，再生成设定库。';
      return;
    }
    const setting = await withFallback(
      novelApi.generateSettingLibrary(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        confirmed: false,
        content:
          '人物：主角是刚入行的普通新人，核心弱点是经验不足但行动力强。\n地点：主舞台为一线城市和城郊旧城区。\n规则：修炼资源稀缺，所有能力必须付出代价。\n伏笔：主角家中旧物与隐藏传承有关。',
      }),
      '设定库已生成',
      (data) => Boolean(data.content),
    );
    state.settingLibrary = setting;
    addVersion('setting_library', setting.id, 'generate', '生成设定库');
    return setting;
  }

  async function updateSettingLibrary(content: string) {
    if (!state.settingLibrary) {
      return;
    }
    state.settingLibrary.content = content;
    const updated = await withFallback(
      novelApi.updateSettingLibrary(state.settingLibrary.id, content),
      () => state.settingLibrary!,
      '设定库修改已保存',
    );
    Object.assign(state.settingLibrary, updated);
    addVersion('setting_library', state.settingLibrary.id, 'edit', '用户直接修改设定库');
  }

  async function confirmSettingLibrary() {
    if (!state.settingLibrary) {
      return;
    }
    await withFallback(
      novelApi.confirmSettingLibrary(state.settingLibrary.id),
      () => ({ ...state.settingLibrary!, confirmed: true }),
      '设定库已确认',
    );
    state.settingLibrary.confirmed = true;
    addVersion('setting_library', state.settingLibrary.id, 'confirm', '确认设定库');
    setProjectStage('outline');
  }

  async function generateOutline() {
    if (!canGenerateOutline.value || !state.activeProjectId) {
      state.lastMessage = '请先确认设定库，再生成全局大纲。';
      return;
    }
    const outline = await withFallback(
      novelApi.generateGlobalOutline(state.activeProjectId),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        confirmed: false,
        content:
          '全局主线：主角从城市小事件切入，逐步发现修真秩序正在影响普通生活。\n第一卷：建立主角目标、能力代价和第一个敌对势力。\n第二卷：扩大地点和关系网，揭露资源争夺。\n第三卷：回收旧物伏笔，完成阶段性胜利并打开更大冲突。',
      }),
      '全局大纲已生成',
      (data) => Boolean(data.content),
    );
    state.outline = outline;
    addVersion('global_outline', outline.id, 'generate', '生成全局大纲');
    return outline;
  }

  async function updateOutline(content: string) {
    if (!state.outline) {
      return;
    }
    state.outline.content = content;
    const updated = await withFallback(
      novelApi.updateGlobalOutline(state.outline.id, content),
      () => state.outline!,
      '全局大纲修改已保存',
    );
    Object.assign(state.outline, updated);
    addVersion('global_outline', state.outline.id, 'edit', '用户直接修改全局大纲');
  }

  async function confirmOutline() {
    if (!state.outline) {
      return;
    }
    await withFallback(
      novelApi.confirmGlobalOutline(state.outline.id),
      () => ({ ...state.outline!, confirmed: true }),
      '全局大纲已确认',
    );
    state.outline.confirmed = true;
    addVersion('global_outline', state.outline.id, 'confirm', '确认全局大纲');
    setProjectStage('chapter');
  }

  async function generateChapterOutlines() {
    if (!canGenerateChapters.value || !state.activeProjectId) {
      state.lastMessage = '请先确认全局大纲，再生成章节。';
      return [];
    }
    const chapters = await withFallback(
      novelApi.generateChapterOutlines(state.activeProjectId),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 1 章 旧物',
          outline: '主角在现实麻烦中发现家中旧物异常，结尾留下超凡线索。',
          content: '',
          status: 'outline_ready' as const,
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 2 章 第一次代价',
          outline: '主角尝试使用能力解决问题，却发现每次使用都会带来现实代价。',
          content: '',
          status: 'outline_ready' as const,
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          title: '第 3 章 城市背面',
          outline: '隐藏势力露出一角，主角意识到自己不是唯一接触传承的人。',
          content: '',
          status: 'outline_ready' as const,
        },
      ],
      '章节大纲已生成',
      isChapterList,
    );
    state.chapters = chapters;
    addVersion('chapter_outline', chapters[0]?.id ?? 0, 'generate', '生成章节大纲');
    setProjectStage('chapter');
    return chapters;
  }

  async function generateChapterContent(chapterId: number, suggestion = '') {
    const chapter = state.chapters.find((item) => item.id === chapterId);
    if (!chapter) {
      return;
    }
    const generated = await withFallback(
      chapter.content && suggestion
        ? novelApi.rewriteChapterContent(chapterId, suggestion)
        : novelApi.generateChapterContent(chapter, suggestion),
      () => ({
        ...chapter,
        content: `${chapter.title}\n\n${chapter.outline}\n\n主角按照大纲推进当前事件，场景中保留人物目标、冲突升级和章末钩子。${suggestion ? `\n\n重生成意见：${suggestion}` : ''}`,
        status: 'content_ready' as const,
      }),
      '章节正文已生成',
      (data) => Boolean(data.content),
    );
    Object.assign(chapter, generated);
    state.projectMemory = await novelApi.getProjectMemory(chapter.projectId).catch(() => state.projectMemory);
    addVersion('chapter', chapterId, suggestion ? 'rewrite' : 'generate', suggestion || '生成章节正文');
    setProjectStage('check');
  }

  async function updateChapterContent(chapterId: number, content: string) {
    const chapter = state.chapters.find((item) => item.id === chapterId);
    if (!chapter) {
      return;
    }
    chapter.content = content;
    chapter.status = 'edited';
    const updated = await withFallback(
      novelApi.updateChapter(chapterId, content),
      () => chapter,
      '章节正文修改已保存',
    );
    Object.assign(chapter, updated, { status: 'edited' as const });
    state.projectMemory = await novelApi.getProjectMemory(chapter.projectId).catch(() => state.projectMemory);
    addVersion('chapter', chapterId, 'edit', '用户直接修改章节正文');
  }

  async function createCheck() {
    if (!canCheck.value || !state.activeProjectId) {
      state.lastMessage = '请先生成至少一章正文，再创建检查。';
      return [];
    }
    const checks = await withFallback(
      novelApi.createCheck(state.activeProjectId),
      () => [
        {
          id: next(),
          projectId: state.activeProjectId!,
          type: '人物状态',
          severity: '中' as const,
          summary: '主角能力代价需要在后续章节持续记录，避免忽然消失。',
          suggestion: '在章节编辑时补一句身体或现实成本。',
        },
        {
          id: next(),
          projectId: state.activeProjectId!,
          type: 'AI 痕迹提示',
          severity: '中' as const,
          summary: '部分句子像概述，缺少具体动作和场景细节。',
          suggestion: '把抽象总结改成可见动作、对白或环境反馈。',
        },
      ],
      '检查结果已生成',
      isCheckList,
    );
    state.checks = checks;
    addVersion('check_result', checks[0]?.id ?? 0, 'generate', '生成连续性和风格检查结果');
    setProjectStage('export');
    return checks;
  }

  async function createExport(format: ExportRecord['format'], scope: string) {
    if (!state.activeProjectId) {
      return;
    }
    const record = await withFallback(
      novelApi.createExport(state.activeProjectId, format, scope),
      () => ({
        id: next(),
        projectId: state.activeProjectId!,
        format,
        scope,
        fileName: `${activeProject.value?.title || 'novel'}.${format === 'markdown' ? 'md' : 'txt'}`,
        status: 'created' as const,
      }),
      '导出任务已创建',
      (data) => Boolean(data.fileName),
    );
    state.exports.unshift(record);
    addVersion('export', record.id, 'export', `导出 ${format.toUpperCase()} 文件`);
    return record;
  }

  return {
    state,
    activeProject,
    selectedIdea,
    canGenerateSetting,
    canGenerateOutline,
    canGenerateChapters,
    canCheck,
    loadProjects,
    selectProject,
    loadIdeas,
    loadSettingLibrary,
    loadOutline,
    loadChapters,
    loadProjectMemory,
    loadVersions,
    createProject,
    loadModelConfigs,
    createModelConfig,
    updateModelConfig,
    setDefaultModel,
    disableModelConfig,
    generateIdeas,
    selectIdea,
    rewriteIdea,
    updateIdea,
    deleteIdea,
    generateSettingLibrary,
    updateSettingLibrary,
    confirmSettingLibrary,
    generateOutline,
    updateOutline,
    confirmOutline,
    generateChapterOutlines,
    generateChapterContent,
    updateChapterContent,
    createCheck,
    createExport,
  };
}
