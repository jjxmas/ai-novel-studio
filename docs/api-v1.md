# AI 长篇小说工作台 REST API 文档（第三版 MVP）

## 1. 基本约定

- Base URL：`/api/v1`
- 文档状态：
  - `第二版实现`：本阶段前后端需要实际对接的接口。
  - `第三版实现`：第三版新增或升级的接口，主要覆盖章节生成、重写和章节记忆压缩。
  - `预留`：为后续版本保留，本阶段不要求实现。
- 字段命名：请求和响应字段使用英文驼峰；接口说明、错误信息、提示词说明使用中文。
- 模型调用：前端只传 `modelConfigId` 或使用默认模型，所有模型调用必须经过后端。
- API Key：只允许在模型配置写入接口中提交；查询接口只返回 `hasApiKey`，不返回明文 API Key。
- 生成策略：创意、设定库、大纲等非章节链路可继续使用第二版 mock；章节正文生成、章节重写和摘要压缩优先走后端 AI 编排服务，模型不可用时允许后端回退 mock。
- 模型适配：第三版通过 Spring AI `ChatClient` 接 OpenAI-compatible 模型，并保留 `NovelAiClient` 端口；模型不可用时回退 mock。
- 版本记录：每次 mock 生成、用户直接编辑、根据修改意见重生成、确认内容时，都必须写入 `content_versions`。
- 章节记忆：章节正文生成或重写成功后，后端应刷新单章摘要、近窗记忆、中层记忆、高层记忆和全局总摘要；摘要生成与压缩也必须写入版本记录。
- 时间格式：使用 ISO-8601 字符串，例如 `2026-07-19T20:30:00`。

## 2. 通用响应结构

所有接口统一返回：

```json
{
  "code": 0,
  "message": "ok",
  "data": {},
  "success": true,
  "timestamp": 1784440000000,
  "requestId": "uuid"
}
```

分页列表统一放在 `data` 中：

```json
{
  "items": [],
  "page": 1,
  "pageSize": 20,
  "total": 0
}
```

第二版可以先不做复杂分页；如果接口数据量较小，可以返回数组，并在后续版本补分页。

## 3. 错误码

| code | 含义 | 典型场景 |
| --- | --- | --- |
| 0 | 成功 | 请求处理成功 |
| 40001 | 参数错误 | 必填字段缺失、字段长度非法、格式非法 |
| 40002 | 资源不存在 | `projectId`、`ideaId`、`chapterId` 等不存在 |
| 40003 | 流程门禁未满足 | 未选中创意就生成设定库；未确认全局大纲就生成章节 |
| 40004 | 模型配置无效或未配置 API Key | `modelConfigId` 不存在、未启用、未填写 API Key |
| 40005 | AI 任务未接入或模型不可用 | 真实模型调用尚未接入或模型调用失败 |
| 40006 | 导出失败 | 导出格式不支持、导出内容为空 |
| 40900 | 业务处理失败 | 重复设置、状态冲突、内容已确认但被非法覆盖 |
| 50000 | 系统异常 | 未预期异常 |

错误响应示例：

```json
{
  "code": 40003,
  "message": "请先确认全局大纲，再生成章节正文",
  "data": null,
  "success": false,
  "timestamp": 1784440000000,
  "requestId": "uuid"
}
```

## 4. 阶段门禁

第二版 MVP 使用较短闭环，避免分卷、剧情单元、章节大纲全部确认后才能写正文的复杂流程。

| 当前动作 | 前置条件 | 不满足时 |
| --- | --- | --- |
| 生成创意 | 作品存在 | `40002` |
| 选择创意 | 创意属于当前作品 | `40002` 或 `40900` |
| 生成设定库 | 作品已选中创意 | `40003` |
| 确认设定库 | 设定库存在且属于当前作品 | `40002` |
| 生成全局大纲 | 设定库已确认 | `40003` |
| 确认全局大纲 | 全局大纲存在且属于当前作品 | `40002` |
| 生成分卷大纲 | 全局大纲已确认 | `40003` |
| 生成章节大纲 | 全局大纲已确认 | `40003` |
| 生成章节正文 | 全局大纲已确认，且章节大纲存在 | `40003` |
| 检查章节 | 章节存在且有正文 | `40002` 或 `40003` |
| 导出作品 | 至少存在一个有正文的章节 | `40006` |

预留门禁：分卷确认、剧情单元确认、章节大纲逐章确认。后续版本需要更严格流程时再启用。

## 5. 通用枚举

| 字段 | 可选值 | 说明 |
| --- | --- | --- |
| `project.status` | `draft`、`idea_selected`、`setting_confirmed`、`outline_confirmed`、`writing`、`exported` | 作品阶段 |
| `idea.status` | `draft`、`selected`、`rejected` | 创意状态 |
| `chapter.status` | `outline_ready`、`content_ready`、`edited` | 章节状态 |
| `memoryType` | `recent_window`、`middle`、`high`、`global` | 章节记忆层级 |
| `memory.status` | `active`、`compressed`、`superseded` | 记忆快照状态 |
| `changeSource` | `mock_generate`、`ai_generate`、`user_edit`、`mock_rewrite`、`ai_rewrite`、`confirm`、`export` | 版本来源 |
| `entityType` | `idea`、`setting_library`、`global_outline`、`volume`、`chapter_outline`、`chapter`、`chapter_summary`、`story_memory`、`export` | 版本所属实体 |
| `export.format` | `markdown`、`txt` | 第二版支持格式 |
| `checkType` | `continuity`、`character`、`timeline`、`location`、`setting_conflict`、`style`、`ai_trace`、`all` | 检查类型 |
| `severity` | `info`、`warning`、`critical` | 检查问题级别 |

## 6. 作品接口

### 6.1 创建作品（第二版实现）

`POST /projects`

请求：

```json
{
  "title": "长生从夜市摆摊开始",
  "genres": ["修仙", "都市"],
  "targetWordCountMin": 1500000,
  "targetWordCountMax": 2200000,
  "platformTarget": "番茄小说",
  "stylePreference": "节奏快，人物说话自然，避免过度 AI 腔",
  "projectBrief": "主角在都市夜市获得修仙传承，从小人物慢慢成长。"
}
```

响应 `data`：

```json
{
  "id": 1,
  "title": "长生从夜市摆摊开始",
  "genres": ["修仙", "都市"],
  "targetWordCountMin": 1500000,
  "targetWordCountMax": 2200000,
  "platformTarget": "番茄小说",
  "stylePreference": "节奏快，人物说话自然，避免过度 AI 腔",
  "projectBrief": "主角在都市夜市获得修仙传承，从小人物慢慢成长。",
  "status": "draft",
  "selectedIdeaId": null,
  "createdAt": "2026-07-19T20:30:00",
  "updatedAt": "2026-07-19T20:30:00"
}
```

### 6.2 查询作品列表（第二版实现）

`GET /projects?keyword=&status=`

响应 `data`：

```json
[
  {
    "id": 1,
    "title": "长生从夜市摆摊开始",
    "genres": ["修仙", "都市"],
    "targetWordCountMin": 1500000,
    "targetWordCountMax": 2200000,
    "platformTarget": "番茄小说",
    "status": "draft",
    "selectedIdeaId": null,
    "updatedAt": "2026-07-19T20:30:00"
  }
]
```

### 6.3 查询作品详情（第二版实现）

`GET /projects/{projectId}`

响应同“创建作品”。

### 6.4 修改作品（预留）

`PATCH /projects/{projectId}`

### 6.5 删除作品（预留）

`DELETE /projects/{projectId}`

## 7. 模型配置接口

### 7.1 新增模型配置（第二版实现）

`POST /model-configs`

请求：

```json
{
  "provider": "openai_compatible",
  "displayName": "我的写作模型",
  "baseUrl": "https://api.example.com/v1",
  "modelName": "example-model",
  "apiKey": "sk-***",
  "usageType": "writing",
  "defaultModel": true,
  "enabled": true,
  "contextWindow": 128000,
  "temperature": 0.8,
  "topP": 0.9,
  "supportsJson": true,
  "supportsStream": false,
  "notes": "用户自填 API Key，后端保存密文或本地安全存储。"
}
```

响应 `data`：

```json
{
  "id": 1,
  "provider": "openai_compatible",
  "displayName": "我的写作模型",
  "baseUrl": "https://api.example.com/v1",
  "modelName": "example-model",
  "usageType": "writing",
  "hasApiKey": true,
  "defaultModel": true,
  "enabled": true,
  "contextWindow": 128000,
  "temperature": 0.8,
  "topP": 0.9,
  "supportsJson": true,
  "supportsStream": false,
  "notes": "用户自填 API Key，后端保存密文或本地安全存储。",
  "createdAt": "2026-07-19T20:30:00",
  "updatedAt": "2026-07-19T20:30:00"
}
```

### 7.2 查询模型配置列表（第二版实现）

`GET /model-configs?enabled=`

响应 `data`：`ModelConfigResponse[]`

### 7.3 查询模型配置详情（预留）

`GET /model-configs/{id}`

响应 `data`：`ModelConfigResponse`

### 7.4 修改模型配置（已实现）

`PATCH /model-configs/{id}`

请求字段与新增接口一致；`apiKey` 为空或不传时，不覆盖旧 API Key。

### 7.5 设置默认模型（第二版实现）

`POST /model-configs/{id}/default`

响应 `data`：

```json
{
  "id": 1,
  "defaultModel": true
}
```

### 7.6 禁用模型配置（已实现）

`DELETE /model-configs/{id}`

说明：本阶段不物理删除模型配置，只把 `enabled` 置为 `false`，避免破坏历史版本记录中的 `modelConfigId`。默认模型不能直接禁用，请先设置其他默认模型。

响应 `data`：`ModelConfigResponse`

### 7.7 测试模型连接（预留）

`POST /model-configs/{id}/test`

## 8. 创意接口

### 8.1 生成多个创意（第三版实现，AI + mock fallback）

`POST /projects/{projectId}/ideas/generate`

请求：

```json
{
  "modelConfigId": 1,
  "genres": ["修仙", "都市"],
  "briefDescription": "主角从夜市小摊开始接触修仙世界。",
  "ideaCount": 3
}
```

响应 `data`：

```json
[
  {
    "id": 11,
    "projectId": 1,
    "title": "夜市长生路",
    "sellingPoints": ["都市烟火气", "修仙体系逐步展开", "小人物成长"],
    "worldview": "现代城市表层正常，地下存在低调修行者网络。",
    "mainConflict": "主角想守住普通生活，却不断被修行势力卷入。",
    "estimatedWordCount": 2000000,
    "longFormPotentialScore": 86,
    "summary": "主角在夜市摆摊时得到残缺传承，从解决小麻烦开始，一步步接触城市背后的修行秩序。",
    "status": "draft",
    "selectedAt": null,
    "createdAt": "2026-07-19T20:30:00",
    "updatedAt": "2026-07-19T20:30:00"
  }
]
```

生成后必须写入版本记录：

- `entityType = idea`
- `changeSource = ai_generate`
- `changeNote = AI 生成创意`

### 8.2 查询创意列表（第二版实现）

`GET /projects/{projectId}/ideas`

响应 `data`：`IdeaResponse[]`

### 8.3 查询创意详情（预留）

`GET /ideas/{ideaId}`

响应 `data`：`IdeaResponse`

### 8.4 直接修改创意（第二版实现）

`PATCH /ideas/{ideaId}`

请求：

```json
{
  "title": "夜市长生路",
  "sellingPoints": ["都市烟火气", "修仙体系逐步展开"],
  "worldview": "城市中存在隐秘修行秩序。",
  "mainConflict": "主角想守住家人和摊位，却被迫进入更大的修行冲突。",
  "estimatedWordCount": 2000000,
  "summary": "用户编辑后的创意摘要。",
  "changeNote": "强化主线冲突"
}
```

响应 `data`：`IdeaResponse`

必须写入版本记录：

- `changeSource = user_edit`
- `changeNote` 使用请求中的 `changeNote`

### 8.5 根据修改意见重生成创意（第三版实现，AI + mock fallback）

`POST /ideas/{ideaId}/rewrite`

请求：

```json
{
  "modelConfigId": 1,
  "instruction": "减少套路感，让主角目标更明确，保留都市夜市元素。"
}
```

响应 `data`：`IdeaResponse`

必须写入版本记录：

- `changeSource = ai_rewrite`
- `changeNote = 根据修改意见重生成创意`

### 8.6 选择创意（第二版实现）

`POST /ideas/{ideaId}/select`

请求：

```json
{
  "projectId": 1
}
```

响应 `data`：

```json
{
  "projectId": 1,
  "selectedIdeaId": 11,
  "projectStatus": "idea_selected"
}
```

选择后同作品下其他创意可保持 `draft`，也可以置为 `rejected`；第二版建议保持简单，只更新当前创意为 `selected` 并写入作品 `selectedIdeaId`。

## 9. 设定库接口

### 9.1 生成设定库（第二版实现，mock）

`POST /projects/{projectId}/setting-library/generate`

门禁：作品必须已选中创意。

请求：

```json
{
  "modelConfigId": 1,
  "ideaId": 11,
  "sourceIdeaSummary": "基于选中创意生成设定库。",
  "revisionAdvice": ""
}
```

响应 `data`：

```json
{
  "id": 21,
  "projectId": 1,
  "summary": "现代都市表层正常，暗线存在修行者、灵材交易和隐秘组织。",
  "charactersSummary": "主角：夜市摊主，谨慎但有韧性；女主：城市医院医生，逐渐接触修行事件。",
  "locationsSummary": "夜市、老城区、地下灵材集市、城郊废弃工厂。",
  "rulesSummary": "灵气稀薄，修行资源稀缺，普通人不能大规模知道修行者存在。",
  "confirmed": false,
  "confirmedAt": null,
  "createdAt": "2026-07-19T20:30:00",
  "updatedAt": "2026-07-19T20:30:00"
}
```

必须写入版本记录：`entityType = setting_library`，`changeSource = mock_generate`。

### 9.2 查询设定库（第二版实现）

`GET /projects/{projectId}/setting-library`

响应 `data`：`SettingLibraryResponse`

### 9.3 编辑设定库（第二版实现）

`PATCH /setting-library/{settingLibraryId}`

请求：

```json
{
  "summary": "编辑后的世界设定总览。",
  "charactersSummary": "编辑后的人物摘要。",
  "locationsSummary": "编辑后的地点摘要。",
  "rulesSummary": "编辑后的规则摘要。",
  "changeNote": "补充修行规则限制"
}
```

响应 `data`：`SettingLibraryResponse`

必须写入版本记录：`changeSource = user_edit`。

### 9.4 确认设定库（第二版实现）

`POST /setting-library/{settingLibraryId}/confirm`

响应 `data`：

```json
{
  "id": 21,
  "projectId": 1,
  "confirmed": true,
  "confirmedAt": "2026-07-19T20:30:00",
  "projectStatus": "setting_confirmed"
}
```

必须写入版本记录：`changeSource = confirm`。

## 10. 大纲接口

### 10.1 生成全局大纲（第二版实现，mock）

`POST /projects/{projectId}/global-outline/generate`

门禁：设定库必须已确认。

请求：

```json
{
  "modelConfigId": 1,
  "sourceContent": "可传设定库摘要或用户补充要求。",
  "revisionAdvice": ""
}
```

响应 `data`：

```json
{
  "id": 31,
  "projectId": 1,
  "outlineLevel": "global",
  "title": "全局大纲",
  "content": "第一阶段：主角获得传承并守住夜市；第二阶段：进入地下修行秩序；第三阶段：面对城市级危机。",
  "confirmed": false,
  "confirmedAt": null,
  "createdAt": "2026-07-19T20:30:00",
  "updatedAt": "2026-07-19T20:30:00"
}
```

必须写入版本记录：`entityType = global_outline`，`changeSource = mock_generate`。

### 10.2 查询全局大纲（第二版实现）

`GET /projects/{projectId}/global-outline`

响应 `data`：`OutlineResponse`

### 10.3 编辑全局大纲（第二版实现）

`PATCH /global-outlines/{id}`

请求：

```json
{
  "title": "全局大纲",
  "content": "编辑后的全局大纲正文。",
  "changeNote": "调整中后期主线"
}
```

响应 `data`：`OutlineResponse`

必须写入版本记录：`changeSource = user_edit`。

### 10.4 确认全局大纲（第二版实现）

`POST /global-outlines/{id}/confirm`

响应 `data`：

```json
{
  "id": 31,
  "projectId": 1,
  "confirmed": true,
  "confirmedAt": "2026-07-19T20:30:00",
  "projectStatus": "outline_confirmed"
}
```

必须写入版本记录：`changeSource = confirm`。

### 10.5 生成分卷与章节大纲（第二版实现，mock）

`POST /projects/{projectId}/chapters/generate-outline`

门禁：全局大纲必须已确认。

请求：

```json
{
  "modelConfigId": 1,
  "chapterCount": 6,
  "revisionAdvice": ""
}
```

响应 `data`：

```json
[
  {
    "id": 51,
    "projectId": 1,
    "volumeId": 41,
    "storyArcId": 51,
    "volumeNo": 1,
    "chapterNo": 1,
    "title": "第1章 阶段推进",
    "outline": "围绕一个明确目标、一个阻碍和一个推进结果展开。",
    "content": "",
    "status": "outline_pending",
    "createdAt": "2026-07-19T20:30:00",
    "updatedAt": "2026-07-19T20:30:00"
  }
]
```

说明：第二版后端会在内部创建基础分卷、剧情单元和章节大纲，接口直接返回章节大纲列表。

必须写入版本记录：`entityType = chapter_outline`，`changeSource = ai_generate`。

### 10.6 查询分卷列表（预留）

`GET /projects/{projectId}/volumes`

响应 `data`：`VolumeResponse[]`

### 10.7 独立生成章节大纲参数（预留）

`POST /projects/{projectId}/chapters/generate-outline`

说明：第二版已经通过 10.5 跑通章节大纲生成。后续版本再支持指定 `volumeId`、`chapterCount` 等精细参数。

请求：

```json
{
  "modelConfigId": 1,
  "volumeId": 41,
  "chapterCount": 10,
  "revisionAdvice": ""
}
```

响应 `data`：

```json
[
  {
    "id": 51,
    "projectId": 1,
    "volumeId": 41,
    "storyArcId": null,
    "chapterNo": 1,
    "title": "夜市里的玉牌",
    "outline": "主角收摊时得到奇怪玉牌，第一次感知灵气。",
    "content": "",
    "status": "outline_ready",
    "createdAt": "2026-07-19T20:30:00",
    "updatedAt": "2026-07-19T20:30:00"
  }
]
```

必须写入版本记录：`entityType = chapter_outline`。

### 10.8 分卷确认、剧情单元生成与确认（预留）

- `POST /volumes/{id}/confirm`
- `GET /volumes/{volumeId}/arcs`
- `POST /volumes/{volumeId}/arcs/generate`
- `POST /arcs/{id}/confirm`

## 11. 章节接口

### 11.1 查询章节列表（第二版实现）

`GET /projects/{projectId}/chapters?volumeId=`

响应 `data`：`ChapterResponse[]`

### 11.2 查询章节详情（预留）

`GET /chapters/{id}`

响应 `data`：`ChapterResponse`

### 11.3 生成章节正文（第三版实现，AI + mock fallback）

`POST /chapters/{id}/generate-content`

门禁：全局大纲必须已确认，章节必须已有大纲。

说明：前端仍调用原章节正文接口。后端内部统一经过 AI 编排服务组装上下文，优先调用用户配置的真实模型；模型配置缺失、调用失败或第三方不可用时，可以回退 mock，保证创作流程不中断。

上下文组合建议：作品基础信息、已确认设定库摘要、全局总摘要、高层摘要列表、中层摘要列表、近窗单章摘要、上一章结尾片段、当前章节大纲、相关人物/地点/伏笔状态、用户本次修改意见。

请求：

```json
{
  "projectId": 1,
  "modelConfigId": 1,
  "targetWordCount": 2500,
  "revisionAdvice": ""
}
```

响应 `data`：

```json
{
  "id": 51,
  "chapterNo": 1,
  "title": "夜市里的玉牌",
  "outline": "主角收摊时得到奇怪玉牌，第一次感知灵气。",
  "content": "这是第三版生成的章节正文，用于跑通真实模型接入和记忆压缩闭环。",
  "status": "content_ready",
  "outlineConfirmed": true,
  "wordCount": 1200
}
```

必须写入版本记录：

- 章节正文：`entityType = chapter`，`changeSource = ai_generate` 或 `mock_generate`。
- 单章摘要：`entityType = chapter_summary`，`changeSource = ai_generate`。
- 记忆压缩：如触发近窗到中层、中层到高层、全局总摘要更新，写入 `entityType = story_memory`，`changeSource = ai_generate`。
- 前端需要展示记忆状态时，调用 `GET /projects/{projectId}/memories`。

### 11.4 直接编辑章节（第二版实现）

`PATCH /chapters/{id}`

请求：

```json
{
  "content": "用户直接编辑后的章节正文。",
  "changeNote": "调整开头节奏"
}
```

响应 `data`：`ChapterResponse`

必须写入版本记录：`changeSource = user_edit`。

### 11.5 根据修改意见重生成章节正文（第三版实现，AI + mock fallback）

`POST /chapters/{id}/rewrite-content`

请求：

```json
{
  "projectId": 1,
  "modelConfigId": 1,
  "instruction": "减少解释说明，增加人物行动和对话。"
}
```

响应 `data`：`ChapterResponse`

说明：重生成成功后必须刷新当前章节摘要，并按压缩规则更新故事记忆。响应结构与 11.3 一致。

必须写入版本记录：

- 章节正文：`entityType = chapter`，`changeSource = ai_rewrite` 或 `mock_rewrite`。
- 单章摘要与记忆压缩：同 11.3。

### 11.6 确认章节大纲（预留）

`POST /chapters/{id}/confirm-outline`

### 11.7 查询作品章节记忆（第三版实现）

`GET /projects/{projectId}/memories`

响应 `data`：

```json
{
  "projectId": 1,
  "globalMemory": {
    "id": 201,
    "memoryType": "global",
    "memoryKey": "global",
    "sequenceNo": 1,
    "startChapterNo": null,
    "endChapterNo": null,
    "content": "全局总摘要正文。",
    "status": "active",
    "current": true
  },
  "highMemories": [],
  "middleMemories": [
    {
      "id": 101,
      "memoryType": "middle",
      "memoryKey": "middle-1",
      "sequenceNo": 1,
      "startChapterNo": 1,
      "endChapterNo": 6,
      "content": "第 1-6 章中，主角完成入门、确认玉牌代价，并第一次接触地下修行秩序。",
      "status": "active",
      "current": true
    }
  ],
  "recentWindows": [
    {
      "id": 102,
      "memoryType": "recent_window",
      "memoryKey": "recent-window",
      "sequenceNo": 3,
      "startChapterNo": 7,
      "endChapterNo": 8,
      "content": "第 7-8 章近窗摘要。",
      "status": "active",
      "current": true
    }
  ],
  "recentChapterSummaries": [
    {
      "id": 91,
      "chapterId": 58,
      "chapterNo": 8,
      "summary": "主角在本章推进当前目标，并留下新的伏笔。"
    }
  ]
}
```

说明：第三版只提供项目级记忆查询接口。单章摘要通过 `recentChapterSummaries` 返回，近窗、中层、高层和全局总摘要通过同一个响应返回。

## 12. 检查接口

### 12.1 创建检查并返回 mock 结果（第二版实现，mock）

`POST /checks`

请求：

```json
{
  "projectId": 1,
  "chapterId": 51,
  "checkType": "all",
  "targetText": "需要检查的章节正文，可为空；为空时后端读取章节正文。"
}
```

响应 `data`：

```json
{
  "id": 61,
  "projectId": 1,
  "chapterId": 51,
  "checkType": "all",
  "issueCount": 3,
  "summary": "发现 3 条 mock 提醒：人物状态、时间线和 AI 痕迹表达需要复核。",
  "issues": [
    {
      "type": "character",
      "severity": "warning",
      "description": "人物上一章受伤，本章行动过于轻松。",
      "suggestion": "补一句恢复过程，或降低本章动作强度。",
      "reference": "第 1 章"
    },
    {
      "type": "timeline",
      "severity": "warning",
      "description": "同一天内事件过密。",
      "suggestion": "明确夜市收摊后的具体时间。",
      "reference": "正文第 8 段"
    },
    {
      "type": "ai_trace",
      "severity": "info",
      "description": "部分句式较整齐，可能显得像 AI 生成。",
      "suggestion": "增加口语化表达、动作细节和不完全对称的句式。",
      "reference": "正文第 3-5 段"
    }
  ],
  "createdAt": "2026-07-19T20:30:00"
}
```

说明：`ai_trace` 只做风险提示，不能承诺规避平台检测；前端文案也应避免保证“绕过检测”。

### 12.2 查询检查记录（预留）

- `GET /checks?projectId=&chapterId=`
- `GET /checks/{checkId}`
- `PATCH /check-issues/{issueId}`

## 13. 导出接口

### 13.1 创建导出（第二版实现）

`POST /exports`

请求：

```json
{
  "projectId": 1,
  "scope": "all",
  "format": "markdown"
}
```

响应 `data`：

```json
{
  "id": 71,
  "projectId": 1,
  "format": "markdown",
  "scope": "all",
  "fileName": "长生从夜市摆摊开始.md",
  "filePath": "exports/project-1/长生从夜市摆摊开始.md",
  "content": "# 长生从夜市摆摊开始\n\n## 第1章 夜市里的玉牌\n\n这是章节正文。",
  "createdAt": "2026-07-19T20:30:00"
}
```

`format = txt` 时，`fileName` 使用 `.txt` 后缀，`content` 不包含 Markdown 标题符号。

必须写入版本记录：`entityType = export`，`changeSource = export`。

### 13.2 查询导出记录与下载（预留）

- `GET /exports/{exportId}`
- `GET /exports/{exportId}/download`
- `GET /projects/{projectId}/exports`

## 14. 版本接口

### 14.1 查询实体版本列表（第二版实现）

`GET /versions?projectId=1&entityType=chapter&entityId=51`

请求参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `projectId` | 是 | 作品 ID |
| `entityType` | 否 | 版本实体类型 |
| `entityId` | 否 | 实体 ID |

响应 `data`：

```json
[
  {
    "id": 81,
    "projectId": 1,
    "entityType": "chapter",
    "entityId": 51,
    "versionNo": 1,
    "changeSource": "mock_generate",
    "changeNote": "章节正文 mock 生成",
    "modelConfigId": 1,
    "jobId": null,
    "createdAt": "2026-07-19T20:30:00"
  }
]
```

### 14.2 查询版本详情（第二版实现）

`GET /versions/{versionId}`

响应 `data`：

```json
{
  "id": 81,
  "projectId": 1,
  "entityType": "chapter",
  "entityId": 51,
  "versionNo": 1,
  "snapshot": "{\"title\":\"夜市里的玉牌\",\"content\":\"章节正文快照\"}",
  "changeSource": "mock_generate",
  "changeNote": "章节正文 mock 生成",
  "modelConfigId": 1,
  "jobId": null,
  "createdAt": "2026-07-19T20:30:00"
}
```

### 14.3 版本恢复和对比（预留）

- `POST /versions/{versionId}/restore`
- `POST /versions/compare`

## 15. AI 任务接口（预留）

第二版生成接口采用同步 mock 返回，不要求异步任务。未来接入真实模型或长文本生成时再启用 AI 任务接口。

| 方法 | 路径 | 状态 | 说明 |
| --- | --- | --- | --- |
| POST | `/ai/tasks` | 预留 | 创建 AI 任务 |
| GET | `/ai/tasks/{taskId}` | 预留 | 查询 AI 任务 |

预留任务状态：`queued`、`running`、`succeeded`、`failed`、`canceled`。

## 16. 第三版闭环调用顺序

1. `POST /projects` 创建作品。
2. `POST /model-configs` 配置模型；可选 `POST /model-configs/{id}/default` 设置默认。
3. `POST /projects/{projectId}/ideas/generate` 生成多个 mock 创意。
4. `PATCH /ideas/{ideaId}` 或 `POST /ideas/{ideaId}/rewrite` 修改创意。
5. `POST /ideas/{ideaId}/select` 选择创意。
6. `POST /projects/{projectId}/setting-library/generate` 生成设定库。
7. `PATCH /setting-library/{settingLibraryId}` 编辑设定库。
8. `POST /setting-library/{settingLibraryId}/confirm` 确认设定库。
9. `POST /projects/{projectId}/global-outline/generate` 生成全局大纲。
10. `PATCH /global-outlines/{id}` 编辑全局大纲。
11. `POST /global-outlines/{id}/confirm` 确认全局大纲。
12. `POST /projects/{projectId}/chapters/generate-outline` 生成基础分卷、剧情单元和章节大纲。
13. `POST /chapters/{id}/generate-content` 生成章节正文；后端同步刷新单章摘要和故事记忆。
14. `GET /projects/{projectId}/memories` 查看当前章节摘要、近窗、中层、高层和全局总摘要状态。
15. `PATCH /chapters/{id}` 或 `POST /chapters/{id}/rewrite-content` 修改章节；重生成或编辑后同样刷新摘要和故事记忆。
16. `POST /checks` 做 mock 连续性、人物、时间线、地点、设定冲突、风格和 AI 痕迹检查。
17. `POST /exports` 导出 Markdown 或 TXT。
18. `GET /versions`、`GET /versions/{versionId}` 查看版本记录。

## 17. 前后端对接注意事项

- 前端展示按钮时应根据 `project.status` 和具体资源 `confirmed` 状态控制可点击性；后端仍必须做门禁校验。
- 所有写操作成功后，前端应重新拉取当前资源和版本列表，避免本地状态过旧。
- `modelConfigId` 可选时，后端优先使用默认启用模型；没有可用模型或模型调用失败时，章节生成链路回退 mock。
- 第三版章节生成和重写必须经过后端 AI 编排服务；前端不要直接接触 API Key 或第三方模型地址。
- 章节生成或重写完成后，前端调用 `GET /projects/{projectId}/memories` 展示单章摘要、近窗、中层、高层和全局摘要。
- 文档中标为“预留”的接口，本阶段前端不要依赖。
