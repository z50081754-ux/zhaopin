<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useRoute } from "vue-router";
import type { Job } from "../data/jobs";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
import { englishBonus, englishDuties, englishRequirements, jobCategory, jobLocation, jobSummary, jobTitle } from "../data/jobI18n";
const route = useRoute();
const { jobs, loadJob } = useJobs();
const job = ref<Job|undefined>(jobs.value.find(j => j.slug === route.params.slug));
onMounted(async()=>{if(!job.value)job.value=await loadJob(String(route.params.slug))});
const { language } = useLanguage();
</script>

<template>
  <main v-if="job" class="inner-page section job-detail">
    <RouterLink class="back" :to="job.category === '技术岗位' ? '/jobs/tech' : '/jobs/functions'">← {{ language==='zh' ? '返回职位列表' : 'Back to roles' }}</RouterLink>
    <div class="detail-hero">
      <div><div class="page-kicker">POSITION / XW TECHNOLOGY</div><h1>{{ jobTitle(job,language) }}</h1><p v-if="job.summary">{{ jobSummary(job,language) }}</p></div>
      <div class="detail-meta" :class="{two:!job.compensation}"><span>{{ language==='zh' ? '部门' : 'Department' }}<b>{{ jobCategory(job,language) }}</b></span><span>{{ language==='zh' ? '地点' : 'Location' }}<b>{{ jobLocation(job,language) }}</b></span><span v-if="job.compensation">{{ language==='zh' ? '薪资' : 'Compensation' }}<b>{{ job.compensation }}</b></span></div>
    </div>
    <div class="detail-grid" :class="{'details-empty':!job.duties.length&&!job.requirements.length&&!job.bonus.length}">
      <article v-if="job.duties.length||job.requirements.length||job.bonus.length"><section v-if="job.duties.length"><span>01 / RESPONSIBILITIES</span><h2>{{ language==='zh' ? '岗位职责' : 'What you will do' }}</h2><ol><li v-for="item in (language==='zh' ? job.duties : englishDuties(job))" :key="item">{{ item }}</li></ol></section><section v-if="job.requirements.length"><span>02 / REQUIREMENTS</span><h2>{{ language==='zh' ? '任职要求' : 'What you bring' }}</h2><ol><li v-for="item in (language==='zh' ? job.requirements : englishRequirements(job))" :key="item">{{ item }}</li></ol></section><section v-if="job.bonus.length"><span>03 / BONUS</span><h2>{{ language==='zh' ? '加分项' : 'Nice to have' }}</h2><ol><li v-for="item in (language==='zh' ? job.bonus : englishBonus)" :key="item">{{ item }}</li></ol></section></article>
      <aside><div class="apply-box"><small>COMPENSATION</small><h3>{{ language==='zh' ? '每日 9 小时' : '9-hour workday' }}<br>{{ language==='zh' ? '全年 13–14 薪' : '13–14 months annual pay' }}</h3><p>{{ language==='zh' ? '班次由部门安排；季度奖金及年度薪资依据公司效益与个人绩效发放。' : 'Shifts are arranged by department. Quarterly bonuses and annual pay depend on company results and individual performance.' }}</p><RouterLink class="primary-btn" to="/contact">{{ language==='zh' ? '立即投递' : 'Apply now' }}　→</RouterLink></div><div class="notice">{{ language==='zh' ? '招聘流程' : 'Hiring process' }}<br><b>{{ language==='zh' ? '简历筛选 → 专业面试 → 综合面试 → Offer' : 'Review → Professional interview → Final interview → Offer' }}</b></div></aside>
    </div>
  </main>
  <main v-else class="inner-page section empty"><h1>岗位不存在</h1><RouterLink to="/jobs">返回全部职位</RouterLink></main>
</template>
