# Model Config 模块重构说明

## 本轮目标

`model-config` 作为第一个低耦合重构样板，主要解决两类问题：

1. 写接口无意义地返回完整对象。
2. Service 中存在大量手工 DTO、Entity、Response 字段拷贝。

本轮不扩展业务功能，只调整接口边界和代码职责。

## 接口约定

### 查询

`GET /api/v1/model-configs` 仍然返回完整的 `ModelConfigResponse` 列表，因为查询接口需要返回前端展示数据。

### 创建

`POST /api/v1/model-configs` 不再返回完整模型配置，只返回：

```json
{
  "id": 1
}
```

创建后的完整数据由前端重新查询列表获得。

### 更新和状态操作

以下接口只返回成功标识，`data` 为 `null`：

- `PATCH /api/v1/model-configs/{id}`
- `POST /api/v1/model-configs/{id}/default`
- `DELETE /api/v1/model-configs/{id}`

这些操作不再构造 `ModelConfigResponse`。

## 代码职责

### Controller

Controller 只负责：

- 接收前端请求 DTO。
- 调用 Service。
- 包装统一 API 响应。

Controller 不负责 Entity 创建和字段拷贝。

### Service

Service 只保留业务规则：

- 清理旧的默认模型。
- 禁止禁用默认模型。
- 更新时保留空白 API Key 对应的旧值。
- 规范化 API Key。

Service 不再手工组装 Response DTO。

### MapStruct Converter

`ModelConfigConverter` 负责仍然有价值的转换：

- `ModelConfigCreateRequest -> ModelConfig`
- `ModelConfigUpdateRequest -> 已存在的 ModelConfig`
- `ModelConfig -> ModelConfigResponse`
- `List<ModelConfig> -> List<ModelConfigResponse>`

更新使用 `@MappingTarget`，直接修改已经从数据库读取的实体，不再创建中间对象。

## 为什么不是所有接口都返回对象

写接口返回完整对象只有在以下情况有价值：

- 服务端生成了前端必须立即使用的新字段。
- 服务端进行了复杂计算，前端无法通过查询获得一致结果。
- 接口本身就是命令和查询的组合。

`model-config` 的更新、设置默认、禁用操作都不满足这些条件，因此只返回成功标识更合适。

## 前端适配方式

前端在写操作成功后重新调用 `GET /model-configs`，用查询结果刷新本地状态。这样可以：

- 保持前端状态以服务端为准。
- 避免前端依赖写接口回显对象。
- 让写接口和读模型解耦。

## 后续模块复用规则

后续重构模块时，按以下顺序判断：

1. 先确认写接口是否真的需要返回完整对象。
2. 不需要时，创建接口只返回 `id`，更新和删除接口只返回成功。
3. 必须返回对象时，再使用 MapStruct 统一转换。
4. 业务规则保留在 Service，不能隐藏到 Controller 或通用转换器中。
5. 每次只改一个低耦合模块，并在前后端验证通过后再推广。
