<script setup lang="ts">
defineProps<{ compact?: boolean }>();
</script>

<template>
  <span class="xw-logo" :class="{compact}">
    <span class="xw-wordmark" aria-hidden="true"><b>X</b><i>W</i><em></em></span>
    <small v-if="!compact" class="xw-subline">TECHNOLOGY<br><span>GLOBAL</span></small>
  </span>
</template>
