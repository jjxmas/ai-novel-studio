<script setup lang="ts">
defineProps<{
  title: string;
  description: string;
}>();
</script>

<template>
  <section class="page-shell">
    <header class="page-shell__header">
      <div>
        <h2 class="page-shell__title">{{ title }}</h2>
        <p class="page-shell__description">{{ description }}</p>
      </div>
      <slot name="actions" />
    </header>

    <div class="page-shell__body">
      <slot />
    </div>
  </section>
</template>
