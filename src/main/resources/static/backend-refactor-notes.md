# Backend Refactor Notes

更新日期：2026-08-02

## 扫描基线

- 模块耦合排序（低到高）：
  - `model-config`
  - `version`
  - `check`
  - `export`
  - `setting-library`
  - `memory`
  - `outline`
  - `idea`
  - `chapter`
  - `setting-workflow`
  - `outline-workflow`
  - `project`
- 已识别的高频问题：
  - Controller -> Service -> Entity -> Response 的重复字段搬运很多。
  - 多个模块对写接口默认返回完整对象，前端再直接覆盖本地状态。
  - `setting-library`、`idea`、`chapter` 存在明显的手工转换和重复查询。
  - `ProjectServiceImpl` 当前工作区已有未提交修改，本轮继续跳过，避免覆盖。

## 已完成模块

### `model-config`

- 仅做检查，不重复改造。
- 当前状态：
  - 查询返回 `ModelConfigResponse`
  - 创建返回新建 ID 包装对象 `ModelConfigCreatedResponse`
  - 更新、设为默认、禁用返回 `Void`
  - 已接入 `ModelConfigConverter`
  - 前端已改为写操作后重新加载列表

### `version`

- 新增 `VersionConverter`
- `VersionServiceImpl` 去掉手工 `VersionResponse` builder
- `getVersion`、`listVersions` 改为统一通过 MapStruct 转换
- API 契约无变化

### `chapter`

- 新增 `ChapterConverter`
- `ChapterServiceImpl` 去掉手工 `ChapterResponse` builder
- `confirmChapterOutline`、`updateChapterContent` 改为返回 `Void`
- 前端改为写入后重新拉取章节列表
- 章节生成、重写仍返回完整章节对象，保留原有业务语义

### `idea`

- 新增 `IdeaConverter`
- `IdeaServiceImpl` 改为批量查询最新 `IdeaEvaluation`，消除逐条创意的重复查询
- `updateIdea`、`selectIdea` 改为返回 `Void`
- 前端改为写入后重新拉取创意列表
- 生成、重写仍返回完整创意对象

### `setting-library`

- 新增 `SettingLibraryConverter`
- 八类资料读取统一走 MapStruct：
  - `character`
  - `organization`
  - `location`
  - `item`
  - `world-rule`
  - `relation`
  - `event`
  - `state-record`
- `applyX` 仍保留在 Service 内，负责默认值归一化；字段拷贝改为 `@MappingTarget` 更新已有实体
- 八类资料写接口契约统一为：
  - 创建返回 `Long` ID
  - 更新返回 `Void`
  - 删除返回 `Void`
- 前端 API 与 `useNovelWorkspace` 已同步：
  - 在线请求成功后重新拉取对应列表
  - 网络不可用时仍保留原有前端 mock fallback 行为
- 仍待后续继续处理：
  - `SettingLibraryResponse` 的统计计数仍是 8 次独立 `count` 查询，尚未收敛

### `memory`

- 新增 `ChapterMemoryConverter`
- `getProjectMemory` 的实体 -> 响应链已切到 MapStruct
- 当前保留原有两个私有 helper 方法，避免源码编码问题导致清理补丁误伤；它们已不再参与主查询链
- API 契约无变化

### `outline`

- 新增 `OutlineConverter`
- `OutlineServiceImpl` 的大纲、分卷、章节 Response builder 已移除
- 章节大纲生成复用已有 `ChapterConverter`
- `OutlineResponse` 补充 `projectId`，修复前端长期兜底为 `0` 的字段缺口
- 更新、确认大纲接口改为返回 `Void`
- 前端保存、确认后重新加载大纲
- 生成、重写、工作流提交仍返回完整大纲对象

### `setting-workflow`

- 新增 `SettingWorkflowConverter`
- `SettingWorkflowRun -> SettingWorkflowResponse` 的基础字段改为 MapStruct
- blueprint、draft、checks 仍由 Service 解析 JSON，保留动态结构
- 工作流状态机和提交逻辑未改变

### `outline-workflow`

- 新增 `OutlineWorkflowConverter`
- 工作流 Response 基础字段改为 MapStruct
- 复用 `OutlineConverter` 转换提交后的大纲和分卷
- 工作流提交顺序、章节覆盖保护和状态推进逻辑未改变

## 已检查但暂不改动

### `check`

- 当前接口是命令式查询结果接口，不属于 CRUD 写回型接口
- `runCheck` 需要直接返回检查结果，暂不收敛成成功标识
- 暂未发现必须立即调整的前后端契约问题

### `export`

- 当前接口需要直接返回导出内容与路径，不能压缩成成功标识
- 暂未发现必须立即调整的前后端契约问题

## 尚未完成模块

- `project`
  - 当前工作区已有未提交修改，按约束跳过
- `setting-library`
  - `SettingLibraryResponse` 的统计计数仍是 8 次独立 `count` 查询，尚未收敛
- `memory`
  - 旧的两个私有 helper 仍保留，但已不再参与主查询链

## API 契约变化汇总

- `model-config`
  - 创建：返回新建 ID
  - 更新/默认/禁用：返回 `Void`
- `chapter`
  - 确认章节大纲、更新章节正文：返回 `Void`
- `idea`
  - 更新创意、选择创意：返回 `Void`
- `setting-library`
  - 八类资料创建：返回 `Long`
  - 八类资料更新：返回 `Void`
  - 八类资料删除：返回 `Void`
- `outline`
  - 更新大纲：返回 `Void`
  - 确认大纲：返回 `Void`

## 静态检查

- 已执行：
  - 源码扫描与引用检查
  - `git diff --check`
- 结果：
  - 未发现空白符或补丁格式错误
  - 仅存在工作区换行符提示（LF/CRLF warning），未做格式性回退

## 下一步顺序

1. `outline`
2. `setting-workflow`
3. `outline-workflow`
4. `project`（若与现有修改不冲突，再单独处理）

## Current Batch (2026-08-02)

### `project`

- Added `ProjectConverter` for `Project`/`ProjectResponse` mapping and request-to-entity updates.
- Kept project defaults, JSON genre serialization, project status initialization, and version snapshot logic in the service.
- Project creation now returns the new project ID.
- Project update now returns `Void`; the frontend updates its local project state from the submitted payload.
- Project list conversion now uses the converter in one batch instead of a per-item response builder.
- Existing annotation-order changes in `ProjectServiceImpl` were preserved.

### Remaining Deliberate Exceptions

- `check` keeps returning the complete check result because callers need issue details.
- `export` keeps returning export metadata and content because callers need the generated result.
- `generation-job` is an internal persistence service and has no Controller/DTO conversion chain.
- `setting-library` still performs eight independent resource-count queries; merging them would require a new union query and is not justified without a measured bottleneck.
- `ChapterMemoryServiceImpl` retains two unused legacy helper methods because the file contains historical encoding corruption; the active response chain already uses `ChapterMemoryConverter`.
- No frontend, backend, database, or browser process was started. Only source scanning and static checks were performed.

## Follow-up Adjustment (2026-08-02)

### `project.genres`

- Kept the database column as JSON; no table split or schema migration was introduced.
- Changed `Project.genres` from `String` to `List<String>` and added `JacksonTypeHandler` with `autoResultMap`.
- Removed project-level JSON serialization/parsing from `ProjectServiceImpl` and `ProjectConverter`.
- Updated downstream project context builders to join genre labels explicitly with `" + "`.
- Static checks confirmed no remaining source usage of `JsonUtils.toJson(request.getGenres())`, `JsonUtils.toStringList(project.getGenres())`, `blankToEmpty(project.getGenres())`, or `text(project.getGenres())`.
- Per user instruction, Maven/runtime validation was not run for this follow-up; only source-level static checks and `git diff --check` were performed.
