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

export type ProjectUpdateRequest = ProjectCreateRequest;

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
  longFormPotentialScore?: number | null;
  conflictScore?: number | null;
  noveltyScore?: number | null;
  beginnerFriendlinessScore?: number | null;
  platformFitScore?: number | null;
  riskLevel?: string | null;
  strengths?: string[];
  risks?: string[];
  suggestions?: string[];
  overallComment?: string;
}

export interface IdeaGenerateRequest {
  projectId: number;
  modelType: '创意生成';
  briefDescription: string;
  ideaCount: number;
}

export interface SettingLibrary {
  id: number;
  projectId: number;
  sourceIdeaId?: number | null;
  summary: string;
  overview: string;
  genreTemplate?: string | null;
  status?: string;
  confirmed: boolean;
  confirmedAt?: string | null;
  characterCount: number;
  organizationCount: number;
  locationCount: number;
  itemCount: number;
  ruleCount: number;
  relationCount: number;
  eventCount: number;
  stateRecordCount: number;
  completenessScore: number;
}

export interface SettingLibrarySnapshot {
  settingLibrary: SettingLibrary;
  characters: StoryCharacter[];
  organizations: Organization[];
  locations: StoryLocation[];
  items: StoryItem[];
  worldRules: WorldRule[];
  relations: EntityRelation[];
  events: StoryEvent[];
  stateRecords: EntityStateRecord[];
}

export interface SettingWorkflow {
  id: number;
  projectId: number;
  sourceIdeaId: number;
  status: 'blueprint_ready' | 'draft_ready' | 'check_failed' | 'committed' | string;
  blueprint: Record<string, unknown>;
  draft: Record<string, unknown>;
  checks: {
    passed?: boolean;
    issues?: string[];
  };
  blueprintConfirmedAt?: string | null;
  committedAt?: string | null;
}

export interface SettingModuleSummary {
  key: 'characters' | 'organizations' | 'locations' | 'items' | 'rules' | 'relations' | 'events' | 'states';
  label: string;
  count: number;
  description: string;
}

export interface StoryCharacter {
  id: number;
  projectId: number;
  name: string;
  aliases: string[];
  roleType: string;
  narrativeRole: string;
  identity: string;
  publicIdentity: string;
  gender: string;
  ageText: string;
  personality: string;
  motivation: string;
  background: string;
  coreGoal: string;
  innerNeed: string;
  coreFlaw: string;
  bottomLine: string;
  skillsSummary: string;
  secretNotes: string;
  relationshipSummary: string;
  importance: number;
  status: string;
  firstAppearedChapterId?: number | null;
  notes?: string;
}

export interface StoryCharacterRequest {
  name: string;
  aliases: string[];
  roleType: string;
  narrativeRole: string;
  identity: string;
  publicIdentity: string;
  gender?: string;
  ageText?: string;
  personality: string;
  motivation?: string;
  background: string;
  coreGoal: string;
  innerNeed: string;
  coreFlaw: string;
  bottomLine: string;
  skillsSummary: string;
  secretNotes: string;
  relationshipSummary?: string;
  importance: number;
  status: string;
  firstAppearedChapterId?: number | null;
  notes?: string;
}

export interface Organization {
  id: number;
  projectId: number;
  name: string;
  organizationType: string;
  publicMission: string;
  realGoal: string;
  controlledResources: string;
  powerScope: string;
  baseLocationId?: number | null;
  entryRules: string;
  status: string;
  notes: string;
}

export interface OrganizationRequest {
  name: string;
  organizationType: string;
  publicMission: string;
  realGoal: string;
  controlledResources: string;
  powerScope: string;
  baseLocationId?: number | null;
  entryRules: string;
  status: string;
  notes: string;
}

export interface StoryLocation {
  id: number;
  projectId: number;
  name: string;
  locationType: string;
  parentLocationId?: number | null;
  description: string;
  keyFeatures: string;
  entryConditions: string;
  availableResources: string;
  controllingOrgId?: number | null;
  riskLevel: string;
  rules: string;
  notes: string;
}

export interface StoryLocationRequest {
  name: string;
  locationType: string;
  parentLocationId?: number | null;
  description: string;
  keyFeatures: string;
  entryConditions: string;
  availableResources: string;
  controllingOrgId?: number | null;
  riskLevel: string;
  rules: string;
  notes: string;
}

export interface StoryItem {
  id: number;
  projectId: number;
  name: string;
  itemType: string;
  description: string;
  usageRules: string;
  limitations: string;
  rarity: string;
  ownerCharacterId?: number | null;
  ownerOrgId?: number | null;
  status: string;
  notes: string;
}

export interface StoryItemRequest {
  name: string;
  itemType: string;
  description: string;
  usageRules: string;
  limitations: string;
  rarity: string;
  ownerCharacterId?: number | null;
  ownerOrgId?: number | null;
  status: string;
  notes: string;
}

export interface WorldRule {
  id: number;
  projectId: number;
  name: string;
  ruleType: string;
  description: string;
  triggerCondition: string;
  effectResult: string;
  limitations: string;
  cost: string;
  exceptions: string;
  visibilityLevel: string;
  importance: number;
  examples: string;
  notes: string;
}

export interface WorldRuleRequest {
  name: string;
  ruleType: string;
  description: string;
  triggerCondition: string;
  effectResult: string;
  limitations: string;
  cost: string;
  exceptions: string;
  visibilityLevel: string;
  importance: number;
  examples: string;
  notes: string;
}

export interface EntityRelation {
  id: number;
  projectId: number;
  sourceType: string;
  sourceId: number;
  targetType: string;
  targetId: number;
  relationType: string;
  relationStatus: string;
  strengthValue?: number | null;
  visibilityLevel: string;
  note: string;
  startEventId?: number | null;
  endEventId?: number | null;
}

export interface EntityRelationRequest {
  sourceType: string;
  sourceId: number | null;
  targetType: string;
  targetId: number | null;
  relationType: string;
  relationStatus: string;
  strengthValue?: number | null;
  visibilityLevel: string;
  note: string;
  startEventId?: number | null;
  endEventId?: number | null;
}

export interface StoryEvent {
  id: number;
  projectId: number;
  name: string;
  eventType: string;
  description: string;
  eventTimeText: string;
  locationId?: number | null;
  chapterId?: number | null;
  planned: boolean;
  importance: number;
}

export interface StoryEventRequest {
  name: string;
  eventType: string;
  description: string;
  eventTimeText: string;
  locationId?: number | null;
  chapterId?: number | null;
  planned: boolean;
  importance: number;
}

export interface EntityStateRecord {
  id: number;
  projectId: number;
  entityType: string;
  entityId: number;
  stateType: string;
  oldValue?: Record<string, unknown> | null;
  newValue: Record<string, unknown>;
  eventId?: number | null;
  chapterId?: number | null;
  effectiveAt?: string | null;
}

export interface EntityStateRecordRequest {
  entityType: string;
  entityId: number | null;
  stateType: string;
  oldValue?: Record<string, unknown> | null;
  newValue: Record<string, unknown> | null;
  eventId?: number | null;
  chapterId?: number | null;
  effectiveAt?: string | null;
}

export interface GlobalOutline {
  id: number;
  projectId: number;
  content: string;
  confirmed: boolean;
  volumes?: Array<{
    id: number;
    volumeNo: number;
    title: string;
    summary: string;
    goal: string;
    estimatedWordCount: number;
  }>;
}

export interface OutlineWorkflow {
  id: number;
  projectId: number;
  settingLibraryId: number;
  status: 'draft_ready' | 'check_failed' | 'committed' | string;
  draft: Record<string, unknown>;
  checks: {
    passed?: boolean;
    issues?: string[];
  };
  committedAt?: string | null;
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
