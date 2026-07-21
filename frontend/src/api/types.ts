export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  success: boolean;
  timestamp: number;
  requestId: string;
}

export interface Project {
  id: number;
  title: string;
  genres: string[];
  projectBrief: string;
  targetWordCountMin: number;
  targetWordCountMax: number;
  targetChapterWordCount: number;
  platformTarget: string;
  stylePreference: string;
  stage: WorkflowStage;
  updatedAt: string;
}

export type WorkflowStage =
  | 'idea'
  | 'setting'
  | 'outline'
  | 'chapter'
  | 'check'
  | 'export';

export interface ProjectCreateRequest {
  title: string;
  genres: string[];
  projectBrief: string;
  targetWordCountMin: number;
  targetWordCountMax: number;
  targetChapterWordCount: number;
  platformTarget: string;
  stylePreference: string;
}

export interface ModelConfig {
  id: number;
  provider: string;
  displayName: string;
  baseUrl: string;
  modelName: string;
  usageType: string;
  hasApiKey: boolean;
  defaultModel: boolean;
  enabled: boolean;
}

export interface ModelConfigRequest {
  provider: string;
  displayName: string;
  baseUrl: string;
  modelName: string;
  usageType: string;
  apiKey: string;
  defaultModel: boolean;
  enabled: boolean;
}

export interface Idea {
  id: number;
  projectId: number;
  title: string;
  sellingPoint: string;
  worldview: string;
  mainConflict: string;
  estimatedWords: string;
  score: number;
  selected: boolean;
  content: string;
}

export interface SettingLibrary {
  id: number;
  projectId: number;
  content: string;
  confirmed: boolean;
}

export interface GlobalOutline {
  id: number;
  projectId: number;
  content: string;
  confirmed: boolean;
}

export interface Chapter {
  id: number;
  projectId: number;
  chapterNo?: number;
  title: string;
  outline: string;
  content: string;
  status: 'outline_ready' | 'content_ready' | 'edited';
}

export interface ChapterSummary {
  id: number;
  chapterId?: number;
  chapterNo?: number;
  summary: string;
}

export type MemoryType = 'recent_window' | 'middle' | 'high' | 'global';

export interface StoryMemory {
  id: number;
  memoryType: MemoryType;
  memoryKey: string;
  sequenceNo: number;
  startChapterNo?: number;
  endChapterNo?: number;
  content: string;
  status: 'active' | 'compressed' | 'superseded';
  current: boolean;
}

export interface ProjectMemory {
  projectId: number;
  globalMemory: StoryMemory | null;
  highMemories: StoryMemory[];
  middleMemories: StoryMemory[];
  recentWindows: StoryMemory[];
  recentChapterSummaries: ChapterSummary[];
}

export interface CheckResult {
  id: number;
  projectId: number;
  type: string;
  severity: '低' | '中' | '高';
  summary: string;
  suggestion: string;
}

export interface ExportRecord {
  id: number;
  projectId: number;
  format: 'markdown' | 'txt';
  scope: string;
  fileName: string;
  status: 'created' | 'failed';
}

export interface ContentVersion {
  id: number;
  projectId: number;
  targetType: string;
  targetId: number;
  actionType: string;
  summary: string;
  createdAt: string;
}
