<script setup lang="ts">
import { onMounted } from "vue";
import HomeView from "./HomeView.vue";
import LandingView from "./LandingView.vue";
import { useSiteTemplate } from "../composables/useSiteTemplate";

const { activeTemplate, templateReady, loadSiteTemplate } = useSiteTemplate();
onMounted(loadSiteTemplate);
</script>

<template>
  <div v-if="!templateReady" class="template-loading" aria-label="Loading"></div>
  <LandingView v-else-if="activeTemplate==='apple'" variant="apple" />
  <HomeView v-else />
</template>

<style scoped>
.template-loading{min-height:100vh;background:#061724}
</style>
