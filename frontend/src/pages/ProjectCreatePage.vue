<script setup lang="ts">
import { reactive } from 'vue';
import { useRouter } from 'vue-router';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const router = useRouter();
const { createProject, state } = useNovelWorkspace();

const form = reactive({
  title: '',
  genres: '修仙、都市',
  projectBrief: '',
  targetWordCountMin: 1200000,
  targetWordCountMax: 2000000,
  targetChapterWordCount: 3000,
  platformTarget: '番茄',
  stylePreference: '节奏快、冲突清楚、语气自然',
});

async function submitProject() {
  if (!form.title.trim()) {
    state.lastMessage = '请先填写作品名称。';
    return;
  }

  await createProject({
    title: form.title.trim(),
    genres: form.genres
      .split(/[、,，]/)
      .map((item) => item.trim())
      .filter(Boolean),
    projectBrief: form.projectBrief.trim(),
    targetWordCountMin: Number(form.targetWordCountMin),
    targetWordCountMax: Number(form.targetWordCountMax),
    targetChapterWordCount: Number(form.targetChapterWordCount),
    platformTarget: form.platformTarget.trim(),
    stylePreference: form.stylePreference.trim(),
  });
  router.push('/ideas');
}
</script>

<template>
  <PageShell
    title="新建作品"
    description="填写题材、模糊描述和平台倾向，保存后进入创意生成。"
  >
    <div class="grid grid--two">
      <section class="card">
        <div class="card__title">基础信息</div>
        <form class="form-grid" @submit.prevent="submitProject">
          <label class="field">
            <span>作品名称</span>
            <input v-model="form.title" type="text" placeholder="例如：都市修仙试写" />
          </label>
          <label class="field">
            <span>小说类型</span>
            <input v-model="form.genres" type="text" placeholder="例如：修仙、都市、悬疑" />
          </label>
          <label class="field field--full">
            <span>模糊描述</span>
            <textarea v-model="form.projectBrief" rows="5" placeholder="写下你现在想到的零散点子"></textarea>
          </label>
          <label class="field">
            <span>最少字数</span>
            <input v-model.number="form.targetWordCountMin" type="number" min="10000" />
          </label>
          <label class="field">
            <span>最多字数</span>
            <input v-model.number="form.targetWordCountMax" type="number" min="10000" />
          </label>
          <label class="field">
            <span>每章目标字数</span>
            <input v-model.number="form.targetChapterWordCount" type="number" min="500" step="100" />
          </label>
          <label class="field field--full">
            <span>平台倾向</span>
            <input v-model="form.platformTarget" type="text" placeholder="通用 / 番茄 / 其他" />
          </label>
          <label class="field field--full">
            <span>风格偏好</span>
            <input v-model="form.stylePreference" type="text" placeholder="例如：节奏快、冲突强、对白自然" />
          </label>
          <div class="field field--full">
            <button class="toolbar__button" type="submit">保存作品并进入创意</button>
          </div>
        </form>
      </section>

      <section class="card">
        <div class="card__title">创建后流程</div>
        <ol class="number-list">
          <li>后端保存作品基础信息。</li>
          <li>进入创意页，使用当前默认模型生成多个创意。</li>
          <li>用户选定创意后，才能生成设定库。</li>
        </ol>
        <p class="helper-text">{{ state.lastMessage }}</p>
      </section>
    </div>
  </PageShell>
</template>
