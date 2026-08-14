# AI Novel Studio Product Spec: Mid/Late Stage Roadmap

## 1. Purpose

This document is a handoff spec for the next development conversation.

The project has reached an early closed loop:

1. Create/select a project.
2. Generate ideas.
3. Build and confirm structured settings.
4. Generate and confirm global outline.
5. Generate chapter outlines.
6. Generate chapter content with streaming output.
7. Post-process chapter content into summaries, memories, facts, states, relations, and foreshadowing.
8. Export generated content.

The next goal is to move from a single-chapter tool into a long-form novel production system.

## 2. Current Product Position

AI Novel Studio is a long-form fiction production workspace. It should not only generate isolated chapters. It should manage the whole lifecycle of a serial novel:

- planning
- worldbuilding
- outline expansion
- chapter drafting
- continuity maintenance
- fact/state projection
- memory compression
- quality checking
- batch generation
- export and version control

The product should eventually support authors generating dozens or hundreds of chapters while keeping character state, plot threads, foreshadowing, and long-range continuity coherent.

## 3. Current Capabilities

### 3.1 Completed Core Loop

The current system already supports:

- project management
- model configuration
- idea generation
- setting workflow
- structured setting entities:
  - characters
  - organizations
  - locations
  - items
  - world rules
  - events
  - relations
  - state records
- global outline workflow
- chapter outline creation
- streaming chapter generation using Flux/SSE
- chapter rewrite/regeneration
- chapter summary generation
- hierarchical memory compression:
  - recent window
  - middle memory
  - high memory
  - global memory
- chapter fact extraction
- story event projection
- entity state projection
- relation projection
- foreshadowing thread updates
- dirty mark and rebuild support
- Redis cache integration for reusable chapter context data
- project-level serial queue for streaming chapter generation/rewrite
- Markdown/TXT browser download export

### 3.2 Important Recent Architecture Changes

Chapter streaming has moved toward the mainline approach:

- backend streaming uses `Flux<ChapterStreamEvent>`
- frontend consumes incremental stream events
- `SseEmitter` bridge is no longer the preferred path

Chapter generation is now serialized per project for streaming endpoints:

- same `projectId`: one chapter generation/rewrite at a time
- different `projectId`: can run concurrently
- queued requests receive a `queued` event
- after content streaming finishes, backend emits `post_processing`
- queue releases only after content save and chapter post-processing finish

This matters because later chapters depend on the previous chapter's summary, memory, facts, active states, and foreshadowing updates.

## 4. Current Main Problems

### 4.1 Chapter Outline Generation Is Only Initial Batch

Current chapter outline generation has two limitations:

- the workflow commit path creates only an initial batch, currently checked around 5-10 chapters
- the old `generateChapterOutlines` path is mock-like and may delete existing chapters before recreating outlines

This is not suitable for long-form novels.

The product needs an append-only "continue chapter outline" feature.

### 4.2 Batch Chapter Content Generation Is Missing

The user wants to generate many chapters at once, for example 50 chapters.

The current per-project queue is useful but not enough by itself. A production batch generation feature needs:

- persistent batch record
- child item progress
- retry
- cancel
- pause/resume
- progress query
- failure isolation
- restart recovery

Frontend should not keep one HTTP request open for hours.

### 4.3 Task System Is Still Too Lightweight

`generation_jobs` already has useful fields such as:

- status
- priority
- attempt count
- lock fields
- started/finished timestamps

But long-running production workflows need a stronger batch abstraction.

Recommended new tables:

- `generation_batches`
- `generation_batch_items`

These can reference `generation_jobs` for detailed per-step logs.

### 4.4 Long-Form Consistency Needs More Product Surface

The backend already stores facts, state records, relations, memories, and foreshadowing. The frontend still does not expose enough control over:

- current character states
- active plot threads
- unresolved foreshadowing
- dirty ranges
- rebuild status
- continuity warnings

For enterprise-grade long-form production, the user must be able to inspect and repair these.

## 5. Next Product Milestone

The next milestone should be:

> Support long-form outline continuation and batch chapter drafting.

This should include two user-visible features:

1. Continue generating chapter outlines after the existing last chapter.
2. Batch generate chapter content for a selected chapter range.

## 6. Feature: Continue Chapter Outlines

### 6.1 User Story

As an author, after generating the first batch of chapter outlines, I want to continue generating the next 10, 20, or 50 chapter outlines without overwriting existing chapters.

### 6.2 Recommended API

```text
POST /api/v1/projects/{projectId}/chapters/continue-outline
```

Request:

```json
{
  "count": 20,
  "modelConfigId": 1,
  "instruction": "Continue the first volume middle arc and raise antagonist pressure."
}
```

Response:

```json
[
  {
    "id": 101,
    "projectId": 1,
    "chapterNo": 11,
    "title": "Chapter 11 ...",
    "outline": "...",
    "scenePlan": ["...", "..."],
    "status": "outline_ready"
  }
]
```

### 6.3 Backend Flow

1. Validate project exists.
2. Validate confirmed global outline exists.
3. Query existing chapters by `projectId`.
4. Determine `startChapterNo = max(chapterNo) + 1`.
5. Build continuation context:
   - project profile
   - confirmed setting summary
   - global outline
   - existing volumes and arcs
   - last 10-20 chapter outlines
   - recent chapter summaries if content already exists
   - active foreshadowing threads if available
6. Ask model to generate the next `count` chapter outlines.
7. Parse JSON.
8. Validate:
   - chapter numbers are continuous
   - no duplicate chapter numbers
   - title and outline are not blank
   - scene plan is a list
   - volume/arc references are valid or can be created
9. Insert new volumes/arcs if needed.
10. Insert new chapters only. Do not delete old chapters.
11. Record generation jobs and content versions.
12. Evict outline/context caches.

### 6.4 Important Rule

Do not generate too many chapter outlines in one model call.

Recommended chunking:

- 10 chapters per model call for high quality
- 20 chapters per model call as an upper normal limit
- 50 chapters should be split into multiple continuation calls internally

## 7. Feature: Batch Chapter Content Generation

### 7.1 User Story

As an author, I want to select a chapter range such as 1-50 and let the system generate chapter content sequentially, while I can watch progress and recover from failures.

### 7.2 Recommended API

Create batch:

```text
POST /api/v1/projects/{projectId}/chapter-generation-batches
```

Request:

```json
{
  "startChapterNo": 1,
  "count": 50,
  "modelConfigId": 1,
  "skipExistingContent": true,
  "instruction": "Keep each chapter around 3000 words."
}
```

Response:

```json
{
  "batchId": 9001,
  "projectId": 1,
  "status": "queued",
  "totalCount": 50,
  "succeededCount": 0,
  "failedCount": 0
}
```

Query progress:

```text
GET /api/v1/chapter-generation-batches/{batchId}
```

Cancel:

```text
POST /api/v1/chapter-generation-batches/{batchId}/cancel
```

Retry failed items:

```text
POST /api/v1/chapter-generation-batches/{batchId}/retry-failed
```

### 7.3 Execution Rule

For the same project, chapters must run serially:

```text
chapter N content generation
-> save content
-> fact extraction
-> fact/state/relation projection
-> foreshadowing update
-> summary and memory update
-> chapter N+1 content generation
```

The queue must not release before post-processing finishes.

### 7.4 Why Not Frontend Loop

The frontend should not send 50 independent generation requests because:

- page refresh loses progress
- network interruption loses control
- retry is hard
- failure tracking is poor
- no clean pause/cancel
- backend cannot reliably recover after restart

Batch generation should be a backend-owned long-running workflow.

## 8. Suggested Data Model

### 8.1 generation_batches

Purpose: one user-created long-running generation task.

Suggested fields:

```text
id
project_id
batch_type
model_config_id
status
total_count
pending_count
running_count
succeeded_count
failed_count
skipped_count
request_snapshot
error_message
created_by
started_at
finished_at
created_at
updated_at
```

Suggested status values:

```text
queued
running
paused
cancel_requested
cancelled
completed
failed
partial_failed
```

### 8.2 generation_batch_items

Purpose: one chapter-level item inside a batch.

Suggested fields:

```text
id
batch_id
project_id
chapter_id
chapter_no
item_type
status
attempt_count
generation_job_id
error_message
started_at
finished_at
created_at
updated_at
```

Suggested item status values:

```text
pending
running
succeeded
failed
skipped
cancelled
```

### 8.3 Indexes

Recommended indexes:

```text
generation_batches(project_id, status, created_at)
generation_batch_items(batch_id, status, chapter_no)
generation_batch_items(project_id, status, chapter_no)
```

## 9. Frontend Changes

### 9.1 Outline Page

Add controls:

- continue outline count selector: 10 / 20 / 50
- instruction textarea
- "Continue Chapter Outlines" button
- generated result list appended to existing chapters

Important UI behavior:

- never imply old chapters will be overwritten
- show start chapter number, such as "Will generate chapters 11-30"
- if existing later chapters have content, warn before adding outlines beyond them

### 9.2 Chapter Page

Add batch generation panel:

- start chapter
- count
- skip existing content
- model config
- start batch button
- batch progress list
- pause/cancel/retry failed

### 9.3 Task Center

Eventually add a unified task center for:

- outline continuation batches
- chapter content batches
- exports
- rebuild jobs
- checks

## 10. Enterprise Version Gap

To become a complete enterprise-grade long-form fiction generation system, the project still needs:

1. Persistent task orchestration
   - queue
   - retry
   - cancel
   - pause/resume
   - progress
   - restart recovery

2. Long-horizon outline planning
   - append-only chapter outline continuation
   - volume and arc expansion
   - controlled chapter number ranges
   - outline quality checks

3. Production-grade chapter drafting
   - range generation
   - per-project serial execution
   - cost estimation
   - token budgeting
   - model fallback

4. Continuity and state management
   - current state snapshot
   - fact projection
   - dirty range propagation
   - rebuild from chapter
   - contradiction detection

5. Quality control
   - chapter review
   - outline compliance check
   - character consistency check
   - foreshadowing check
   - duplicate plot check
   - pacing check

6. Collaboration and audit
   - users and roles
   - project permissions
   - operation logs
   - model usage logs
   - version diff and rollback

7. Observability
   - task metrics
   - model latency
   - token usage
   - failure rate
   - queue length
   - slow chapter diagnostics

8. Export and publishing
   - full novel export
   - volume export
   - chapter export
   - Markdown/TXT/DOCX/PDF
   - publish-ready formatting

## 11. Recommended Implementation Order

### Phase 1: Outline Continuation

Goal:

Add append-only chapter outline continuation.

Tasks:

1. Add `ChapterOutlineContinueRequest`.
2. Add outline continuation prompt.
3. Add service method.
4. Add controller endpoint.
5. Add frontend button and count selector.
6. Validate append-only behavior.

Acceptance:

- existing chapters are not deleted
- new chapter numbers are continuous
- 10-20 new chapter outlines can be appended
- frontend list updates after generation

### Phase 2: Batch Content Generation

Goal:

Generate many chapter contents sequentially through a persistent backend batch.

Tasks:

1. Add batch tables.
2. Add batch DTOs.
3. Add batch controller.
4. Add batch service.
5. Reuse project-level serial execution.
6. Add frontend progress panel.

Acceptance:

- creating a 50-chapter batch returns immediately
- backend generates chapters in order
- progress can be queried
- failure on one chapter is recorded
- retry failed works

### Phase 3: Task Center

Goal:

Expose all long-running operations in one place.

Tasks:

1. List active and historical tasks.
2. Show batch items.
3. Add cancel/retry actions.
4. Add failure detail.

### Phase 4: Quality and Rebuild

Goal:

Make long-form generation maintainable after edits.

Tasks:

1. Improve dirty mark UI.
2. Rebuild from chapter UI.
3. Add continuity check after each generated chapter.
4. Add batch-level quality report.

## 12. Suggested Next Conversation Prompt

Use this as the first message in the next development conversation:

```text
请读取 docs/novel-studio-product-spec-next-stage.md。
我们要开始 Phase 1：实现“继续生成章节大纲”。
要求：
1. 新增追加式章节大纲生成接口，不覆盖已有章节。
2. 根据当前最大 chapterNo 继续生成。
3. 支持 count 参数，建议 10/20/50，其中 50 内部拆批。
4. 接入大模型 JSON 输出，并做基本校验。
5. 前端大纲页增加继续生成按钮、数量选择和进度提示。
6. 最后运行后端编译和前端类型检查。
```

