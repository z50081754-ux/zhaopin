<script setup lang="ts">
import type { Job } from "../data/jobs";
import { useLanguage } from "../composables/useLanguage";
import { jobCategory, jobLocation, jobSummary, jobTitle } from "../data/jobI18n";
defineProps<{ job: Job }>();
const { language } = useLanguage();
</script>

<template>
  <RouterLink class="job-card" :to="`/jobs/${job.slug}`">
    <div class="job-top"><span>DEPARTMENT</span><i>{{ jobCategory(job,language) }}</i></div>
    <h3>{{ jobTitle(job,language) }}</h3>
    <p v-if="job.summary">{{ jobSummary(job,language) }}</p>
    <div class="job-meta"><span>{{ jobLocation(job,language) }}<em v-if="job.compensation"> · {{ job.compensation }}</em></span><b>{{ language==='zh' ? '查看详情' : 'View role' }} ↗</b></div>
  </RouterLink>
</template>
