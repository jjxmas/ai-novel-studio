<script setup lang="ts">
import { computed, reactive, ref } from 'vue';
import { useRouter } from 'vue-router';

import PageShell from '@/components/PageShell.vue';
import { useNovelWorkspace } from '@/composables/useNovelWorkspace';

const router = useRouter();
const { createProject, state } = useNovelWorkspace();

const tomatoGenreTagGroups = [
  {
    name: '热门标签',
    tags: [
      '历史',
      '都市',
      '悬疑',
      '衍生',
      '玄幻',
      '奇幻仙侠',
      '种田',
      '科幻末世',
      '灾难',
      '游戏体育',
      '同人',
      '扮猪吃虎',
      '山海经',
      '风水秘术',
      '高手下山',
      '大佬',
      '反派',
      '迪化',
      '海岛',
      '皇帝',
      '捉鬼',
      '绝地逃生',
      '都市修真',
      '盗墓',
      '穿书',
      '1v1',
      '惊悚游戏',
    ],
  },
  {
    name: '主题标签',
    tags: [
      '国运',
      '异世大陆',
      '异能',
      '都市异能',
      '宋朝',
      '第一人称',
      '都市高武',
      '仕途',
      '武将',
      '开局',
      '东方仙侠',
      '克苏鲁',
      '西方奇幻',
      '抗战谍战',
      '清朝',
      '玄幻脑洞',
      '都市日常',
      '规则怪谈',
      '东方玄幻',
      '灵气复苏',
      '都市生活',
      '断层',
      '高武世界',
      '第四天灾',
      '影视小说',
      '搞笑轻松',
      '都市种田',
      '武侠',
      '男频衍生',
    ],
  },
  {
    name: '角色标签',
    tags: [
      '天才',
      '宫廷侯爵',
      '女帝',
      '校花',
      '赘婿',
      '神医',
      '腹黑',
      '特种兵',
      '学霸',
      '群像',
      '大小姐',
      '多女主',
      '单女主',
      '奶爸',
      '全能',
      '神探',
      '战神赘婿',
      '游戏主播',
      '无女主',
      '特工',
    ],
  },
  {
    name: '情节标签',
    tags: [
      '星际',
      '基建',
      '魂穿',
      '大唐',
      '无敌',
      '双重生',
      '美食',
      '修仙',
      '双系统',
      '囤物资',
      '废土',
      '诸天万界',
      '打脸',
      '推理',
      '系统',
      '升级流',
      '宠物',
      '发家致富',
      '传统玄幻',
      '十日衍生',
      '外卖',
      '家庭',
      '黑科技',
      '黑化',
      '封神',
      '钓鱼',
      '无限流',
      '都市江湖',
      '神豪',
      '三国',
    ],
  },
];

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

const selectedGenreTag = ref('');

const selectedGenres = computed(() =>
  form.genres
    .split(/[、,，]/)
    .map((item) => item.trim())
    .filter(Boolean),
);

function setGenres(genres: string[]) {
  form.genres = Array.from(new Set(genres)).join('、');
}

function addSelectedGenre() {
  if (!selectedGenreTag.value) {
    return;
  }
  setGenres([...selectedGenres.value, selectedGenreTag.value]);
  selectedGenreTag.value = '';
}

function removeGenre(tag: string) {
  setGenres(selectedGenres.value.filter((item) => item !== tag));
}

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
          <div class="field">
            <span>小说类型</span>
            <select v-model="selectedGenreTag" @change="addSelectedGenre">
              <option value="" disabled>选择番茄标签</option>
              <optgroup v-for="group in tomatoGenreTagGroups" :key="group.name" :label="group.name">
                <option v-for="tag in group.tags" :key="`${group.name}-${tag}`" :value="tag">
                  {{ tag }}
                </option>
              </optgroup>
            </select>
            <div class="tag-list selected-genre-list" aria-label="已选择小说类型">
              <button
                v-for="tag in selectedGenres"
                :key="tag"
                class="tag-list__item tag-list__item--button"
                type="button"
                @click="removeGenre(tag)"
              >
                {{ tag }} ×
              </button>
            </div>
          </div>
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

<style scoped>
/* 外层两栏卡片布局 */
.grid--two {
  display: grid;
  grid-template-columns: 1fr 380px;
  gap: 24px;
}

.card {
  background: #fff;
  border-radius: 10px;
  padding: 20px;
}
.card__title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 16px;
  padding-bottom: 10px;
  border-bottom: 1px solid #eee;
}

/* 表单网格核心：两列表单对齐 */
.form-grid {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px 20px;
  align-items: start; /* 顶部对齐，解决下拉框下方标签导致错位 */
}

/* 整行占满两列的输入项 */
.field--full {
  grid-column: 1 / -1;
}

/* 统一表单字段布局 */
.field {
  display: flex;
  flex-direction: column;
  gap: 6px;
}
.field > span {
  font-size: 14px;
  color: #333;
}

/* 统一输入框、下拉框高度，保证对齐 */
.field input,
.field select,
.field textarea {
  border: 1px solid #dcdcdc;
  border-radius: 6px;
  padding: 0 12px;
  font-size: 14px;
  box-sizing: border-box;
}
.field input,
.field select {
  height: 44px;
}
.field textarea {
  padding: 10px 12px;
  resize: vertical;
}

/* 已选标签列表 */
.tag-list {
  margin-top: 8px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}
.tag-list__item {
  padding: 4px 10px;
  background: #f0f7ff;
  border-radius: 4px;
  font-size: 13px;
  border: none;
  cursor: pointer;
}

/* 按钮、辅助文字 */
.toolbar__button {
  height: 46px;
  background: #4080ff;
  color: white;
  border: none;
  border-radius: 6px;
  font-size: 15px;
  cursor: pointer;
}
.number-list {
  padding-left: 20px;
  line-height: 1.7;
}
.helper-text {
  color: #f53f3f;
  margin-top: 12px;
}
</style>