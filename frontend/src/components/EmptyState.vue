<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <div class="empty-state">
    <div class="empty-state__title">{{ title }}</div>
    <p class="empty-state__description">{{ description }}</p>
    <slot />
  </div>
</template>
