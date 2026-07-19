<script setup lang="ts">
import { onMounted, reactive } from 'vue';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';
import type { ModelConfig } from '@/api/types';

const modelTasks = ['创意生成', '大纲生成', '章节正文', '连续性检查', '摘要提取'];
const {
  state,
  loadModelConfigs,
  createModelConfig,
  updateModelConfig,
  setDefaultModel,
  disableModelConfig,
} = useNovelWorkspace();

const form = reactive({
  editingId: null as number | null,
  provider: 'OpenAI 兼容',
  displayName: '',
  baseUrl: '',
  modelName: '',
  usageType: '通用生成',
  apiKey: '',
  defaultModel: true,
  enabled: true,
});

async function submitModel() {
  if (!form.modelName.trim()) {
    state.lastMessage = '请先填写模型名称。';
    return;
  }

  const payload = {
    provider: form.provider,
    displayName: form.displayName.trim() || form.modelName.trim(),
    baseUrl: form.baseUrl,
    modelName: form.modelName,
    usageType: form.usageType,
    apiKey: form.apiKey,
    defaultModel: form.defaultModel,
    enabled: form.enabled,
  };
  if (form.editingId) {
    await updateModelConfig(form.editingId, payload);
  } else {
    await createModelConfig(payload);
  }
  resetForm();
  await loadModelConfigs().catch(() => undefined);
}

function editModel(model: ModelConfig) {
  form.editingId = model.id;
  form.provider = model.provider;
  form.displayName = model.displayName;
  form.baseUrl = model.baseUrl;
  form.modelName = model.modelName;
  form.usageType = model.usageType;
  form.apiKey = '';
  form.defaultModel = model.defaultModel;
  form.enabled = model.enabled;
}

async function submitDisableModel(model: ModelConfig) {
  if (model.defaultModel) {
    state.lastMessage = '默认模型不能直接禁用，请先设置其他默认模型。';
    return;
  }
  await disableModelConfig(model.id);
}

function resetForm() {
  form.editingId = null;
  form.provider = 'OpenAI 兼容';
  form.displayName = '';
  form.baseUrl = '';
  form.modelName = '';
  form.usageType = '通用生成';
  form.apiKey = '';
  form.defaultModel = true;
  form.enabled = true;
}

onMounted(() => {
  void loadModelConfigs().catch(() => undefined);
});
</script>

<template>
  <PageShell
    title="模型配置"
    description="用户自填 API Key，前端只保存配置到后端，不直接调用模型。"
  >
    <div class="grid grid--two">
      <section class="card">
        <div class="card__title">供应商列表</div>
        <div class="stack">
          <div v-if="state.modelConfigs.length === 0" class="empty-state">
            <div class="empty-state__title">暂无模型配置</div>
            <p class="empty-state__description">保存一个模型配置后，会在这里显示。</p>
          </div>
          <article v-for="model in state.modelConfigs" :key="model.id" class="list-item">
            <div>
              <div class="list-item__title">{{ model.displayName || model.modelName }}</div>
              <div class="list-item__text">
                {{ model.provider }} · {{ model.usageType }} · {{ model.hasApiKey ? '已填写 API Key' : '未填写 API Key' }}
              </div>
            </div>
            <div class="toolbar">
              <span class="badge" :class="{ 'badge--ok': model.enabled }">
                {{ model.enabled ? '启用' : '已禁用' }}
              </span>
              <button class="toolbar__button toolbar__button--ghost" type="button" @click="editModel(model)">
                编辑
              </button>
              <button
                class="toolbar__button toolbar__button--ghost"
                type="button"
                :disabled="model.defaultModel || !model.enabled"
                @click="submitDisableModel(model)"
              >
                禁用
              </button>
              <button
                class="toolbar__button toolbar__button--ghost"
                type="button"
                :disabled="model.defaultModel || !model.enabled"
                @click="setDefaultModel(model.id)"
              >
                {{ model.defaultModel ? '默认' : '设为默认' }}
              </button>
            </div>
          </article>
        </div>
      </section>

      <section class="card">
        <div class="card__title">{{ form.editingId ? '编辑模型' : '新增模型' }}</div>
        <form class="form-grid" @submit.prevent="submitModel">
          <label class="field">
            <span>供应商</span>
            <input v-model="form.provider" type="text" />
          </label>
          <label class="field">
            <span>显示名称</span>
            <input v-model="form.displayName" type="text" placeholder="例如：章节生成模型" />
          </label>
          <label class="field">
            <span>模型名称</span>
            <input v-model="form.modelName" type="text" placeholder="例如：gpt-4.1-mini" />
          </label>
          <label class="field">
            <span>Base URL</span>
            <input v-model="form.baseUrl" type="text" placeholder="可选，OpenAI 兼容网关地址" />
          </label>
          <label class="field">
            <span>用途</span>
            <select v-model="form.usageType">
              <option v-for="task in modelTasks" :key="task" :value="task">{{ task }}</option>
              <option value="通用生成">通用生成</option>
            </select>
          </label>
          <label class="field">
            <span>API Key</span>
            <input
              v-model="form.apiKey"
              type="password"
              :placeholder="form.editingId ? '留空则不修改原 API Key' : '只提交到后端保存'"
            />
          </label>
          <label class="field field--full">
            <span>
              <input v-model="form.defaultModel" type="checkbox" />
              设为默认模型
            </span>
          </label>
          <label class="field field--full">
            <span>
              <input v-model="form.enabled" type="checkbox" />
              启用
            </span>
          </label>
          <div class="field field--full">
            <div class="toolbar">
              <button class="toolbar__button" type="submit">
                {{ form.editingId ? '保存修改' : '保存模型配置' }}
              </button>
              <button v-if="form.editingId" class="toolbar__button toolbar__button--ghost" type="button" @click="resetForm">
                取消编辑
              </button>
            </div>
          </div>
        </form>
      </section>
    </div>
  </PageShell>
</template>
