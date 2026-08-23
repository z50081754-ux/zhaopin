<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import JobCard from "../components/JobCard.vue";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
import { jobSummary, jobTitle } from "../data/jobI18n";
const props = defineProps<{ category: string }>();
const { language } = useLanguage();
const { jobs, loadJobs } = useJobs();
const keyword = ref("");
const functionCount = computed(()=>jobs.value.filter(j => j.category === "职能岗位").length);
const techCount = computed(()=>jobs.value.filter(j => j.category === "技术岗位").length);
const visible = computed(() => jobs.value.filter(j =>
  (props.category === "全部职位" || j.category === props.category) &&
  (!keyword.value || `${j.title}${jobTitle(j,language.value)}${j.unit}${jobSummary(j,language.value)}`.toLowerCase().includes(keyword.value.toLowerCase()))
));
onMounted(loadJobs);
</script>

<template>
  <main class="inner-page jobs-page section">
    <section class="jobs-intro">
      <div class="jobs-intro-copy">
        <div class="page-kicker">XW CAREERS / OPEN POSITIONS</div>
        <h1>{{ language==='zh' ? category : category==='技术岗位' ? 'Technology Roles' : category==='职能岗位' ? 'Business Functions' : 'Find Your Role' }}</h1>
        <p>{{ language==='zh' ? '选择与你的经验和目标匹配的岗位，与中国、印度、越南及全球团队一起创造下一阶段。' : 'Find a role that matches your experience and ambition, and build what comes next with teams across China, India, Vietnam and beyond.' }}</p>
      </div>
      <div class="jobs-stats">
        <div class="jobs-total"><small>{{ language==='zh' ? '开放岗位' : 'OPEN ROLES' }}</small><b>{{ jobs.length }}</b><span>POSITIONS</span></div>
        <RouterLink to="/jobs/functions"><small>{{ language==='zh' ? '职能岗' : 'BUSINESS' }}</small><b>{{ functionCount }}</b></RouterLink>
        <RouterLink to="/jobs/tech"><small>{{ language==='zh' ? '技术岗' : 'TECHNOLOGY' }}</small><b>{{ techCount }}</b></RouterLink>
      </div>
    </section>
    <div class="jobs-toolbar">
      <div class="job-tabs">
        <RouterLink to="/jobs">{{ language==='zh' ? '全部职位' : 'All roles' }}</RouterLink>
        <RouterLink to="/jobs/functions">{{ language==='zh' ? '职能岗位' : 'Business functions' }}</RouterLink>
        <RouterLink to="/jobs/tech">{{ language==='zh' ? '技术岗位' : 'Technology roles' }}</RouterLink>
      </div>
      <label class="jobs-search"><span>⌕</span><input v-model="keyword" :placeholder="language==='zh' ? '搜索岗位或关键词' : 'Search roles or keywords'" /></label>
    </div>
    <div class="results-count">{{ language==='zh' ? `显示 ${visible.length} 个岗位` : `${visible.length} roles shown` }}</div>
    <div class="jobs-grid"><JobCard v-for="job in visible" :key="job.slug" :job="job" /></div>
    <div v-if="!visible.length" class="empty">{{ language==='zh' ? '未找到匹配岗位，请尝试其他关键词。' : 'No matching roles. Try another keyword.' }}</div>
  </main>
</template>
