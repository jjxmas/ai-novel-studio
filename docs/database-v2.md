# 第二版 MVP 数据库说明

## 设计假设

- V1 表结构已经视为已发布结构，第二版只通过 `V2__mvp_workflow_state_and_exports.sql` 追加字段和新表。
- 数据库继续使用 MySQL 8，字符集为 `utf8mb4`，排序规则保持 `utf8mb4_0900_ai_ci`。
- 第二版不接真实模型，生成内容可以由后端 mock 服务写入业务表和版本表。
- 阶段门禁主要由后端服务校验，数据库提供可查询的阶段和确认状态。

## 作品流程阶段

`projects.workflow_stage` 表示当前作品所处阶段：

| 值 | 含义 |
| --- | --- |
| `idea` | 创意阶段 |
| `setting` | 设定库阶段 |
| `outline` | 大纲阶段 |
| `chapter` | 章节阶段 |
| `check` | 检查阶段 |
| `export` | 导出阶段 |

后端仍需要结合具体表判断门禁，例如选中创意后才允许生成设定库，全局大纲确认后才允许生成章节正文。

## 确认状态

以下表通过 `status` 和 `confirmed_at` 共同表达确认状态：

| 表 | 状态字段 | 确认时间字段 |
| --- | --- | --- |
| `setting_libraries` | `status` | `confirmed_at` |
| `global_outlines` | `status` | `confirmed_at` |
| `volumes` | `status` | `confirmed_at` |
| `story_arcs` | `status` | `confirmed_at` |

通用状态值：

| 值 | 含义 |
| --- | --- |
| `draft` | 草稿 |
| `generated` | 已生成 |
| `edited` | 已编辑 |
| `confirmed` | 已确认 |

## 章节正文状态

`chapters.content_status` 表示章节正文状态：

| 值 | 含义 |
| --- | --- |
| `not_generated` | 未生成 |
| `generating` | 生成中 |
| `generated` | 已生成 |
| `edited` | 已编辑 |
| `checked` | 已检查 |

`chapters.last_generation_job_id` 记录最近一次正文生成任务，`chapters.last_content_version_no` 记录当前正文最新版本号。

## 版本记录

所有生成、用户直接编辑、根据意见重生成、确认保存都应该写入 `content_versions`。

`content_versions.operation_type` 建议值：

| 值 | 含义 |
| --- | --- |
| `generate` | AI 或 mock 首次生成 |
| `edit` | 用户直接编辑 |
| `rewrite` | 根据用户修改意见重生成 |
| `confirm` | 用户确认保存 |
| `export` | 导出时的内容快照 |

`revision_instruction` 保存用户修改意见或生成指令。`snapshot` 保存该版本的完整内容快照，后端负责递增同一对象的 `version_no`。

## 导出任务

`export_tasks` 记录 Markdown/TXT 导出：

| 字段 | 说明 |
| --- | --- |
| `format` | `markdown` 或 `txt` |
| `scope` | `full_project`、`volume`、`chapter` |
| `scope_entity_id` | 分卷或章节导出时的对象 ID |
| `status` | `pending`、`running`、`succeeded`、`failed` |
| `request_snapshot` | 导出请求快照 |
| `file_name` / `file_path` / `file_size` | 导出文件信息 |

导出成功后，后端可以同步更新 `projects.last_exported_at`。
