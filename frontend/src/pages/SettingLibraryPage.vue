<script setup lang="ts">
import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const categories = ['人物', '地点', '势力', '世界规则', '能力体系', '关键物品', '关系网', '时间线', '伏笔池'];
const {
  state,
  canGenerateSetting,
  generateSettingLibrary,
  updateSettingLibrary,
  confirmSettingLibrary,
} = useNovelWorkspace();

function editSettingLibrary(event: Event) {
  if (!state.settingLibrary) {
    return;
  }
  state.settingLibrary.content = (event.target as HTMLTextAreaElement).value;
}

function saveSettingLibrary() {
  if (state.settingLibrary) {
    updateSettingLibrary(state.settingLibrary.content);
  }
}
</script>

<template>
  <PageShell
    title="设定库"
    description="基于选定创意生成事实库，用户编辑并确认后，才能生成全局大纲。"
  >
    <template #actions>
      <div class="toolbar">
        <button
          class="toolbar__button"
          type="button"
          :disabled="!canGenerateSetting"
          @click="generateSettingLibrary"
        >
          生成设定库
        </button>
      </div>
    </template>

    <div v-if="!state.settingLibrary && !state.activeProjectId" class="empty-state">
      <div class="empty-state__title">请先选择作品</div>
      <p class="empty-state__description">回到工作台选择作品后，再生成和编辑设定库。</p>
    </div>

    <div v-else class="grid grid--two">
      <section class="card">
        <div class="card__title">设定分类</div>
        <ul class="tag-list">
          <li v-for="item in categories" :key="item" class="tag-list__item">{{ item }}</li>
        </ul>
      </section>

      <section class="card">
        <div class="card__title">当前状态</div>
        <div class="status-line">
          <span class="badge" :class="{ 'badge--ok': state.settingLibrary?.confirmed }">
            {{ state.settingLibrary?.confirmed ? '已确认' : '待确认' }}
          </span>
          <span>{{ state.lastMessage }}</span>
        </div>
      </section>
    </div>

    <section class="card">
      <div class="card__title">设定内容</div>
      <div v-if="!state.settingLibrary" class="empty-state">
        <div class="empty-state__title">尚未生成设定库</div>
        <p class="empty-state__description">请先在创意页选定创意，再生成设定库。</p>
      </div>
      <div v-else class="stack">
        <label class="field">
          <span>可直接编辑</span>
          <textarea
            :value="state.settingLibrary.content"
            class="text-editor"
            rows="10"
            @input="editSettingLibrary"
            @blur="saveSettingLibrary"
          ></textarea>
        </label>
        <div class="toolbar">
          <button class="toolbar__button" type="button" @click="confirmSettingLibrary">确认设定库</button>
        </div>
      </div>
    </section>
  </PageShell>
</template>
