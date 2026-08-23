<script setup lang="ts">
import { computed, onMounted } from "vue";
import JobCard from "../components/JobCard.vue";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
const { jobs, loadJobs } = useJobs();
const featured = computed(() => jobs.value.filter(j => ["Java 架构师","AI 应用开发岗","Golang 开发工程师","游戏策划"].includes(j.title)).slice(0,4));
const { language, t } = useLanguage();
onMounted(loadJobs);
</script>

<template>
  <main>
    <section class="hero section">
      <div class="hero-copy">
        <div class="system-label">[ SYSTEM 01 ] // CAREER PORTAL</div>
        <h1>{{ t.headlineA }}<br><span>{{ t.headlineB }}</span></h1>
        <p>{{ t.heroLead }}<br>{{ t.heroSub }}</p>
        <div class="benefits"><span>{{ language==='zh' ? '9 小时 / 班次' : '9H / SHIFT' }}</span><span>13–14 MONTHS</span><span>{{ language==='zh' ? '季度奖金' : 'QUARTERLY BONUS' }}</span></div>
        <RouterLink class="primary-btn" to="/jobs">{{ t.enterJobs }}　→</RouterLink>
      </div>
      <div class="digital-core" aria-hidden="true">
        <div class="core-frame"><div class="globe"><i></i></div></div>
        <div class="hud hud-a">ACTIVE POSITIONS<b>{{ jobs.length }}</b></div>
        <div class="hud hud-b">GLOBAL NODE<b>REMOTE</b></div>
        <div class="scan-line"></div>
      </div>
    </section>

    <section class="section portal-section">
      <div class="section-head"><div><span>DISCOVER / 02</span><h2>{{ t.discoverTitle }}</h2></div><p>{{ t.discoverLead }}</p></div>
      <div class="portal-grid">
        <RouterLink to="/life"><small>NODE / 01</small><h3>{{ t.teamLife }}</h3><p>{{ t.teamLifeDesc }}</p><b>EXPLORE ↗</b></RouterLink>
        <RouterLink to="/growth"><small>NODE / 02</small><h3>{{ t.growth }}</h3><p>{{ t.growthDesc }}</p><b>EXPLORE ↗</b></RouterLink>
        <RouterLink to="/business"><small>NODE / 03</small><h3>{{ t.business }}</h3><p>{{ t.businessDesc }}</p><b>EXPLORE ↗</b></RouterLink>
      </div>
    </section>

    <section class="section image-story">
      <img src="/images/xw-asia-global-team-v2.png" alt="Chinese, Indian and Vietnamese professionals collaborating at XW" />
      <div class="image-copy"><span>CHINA · INDIA · VIETNAM · GLOBAL</span><h2>{{ t.globalTeam }}</h2><p>{{ t.globalDesc }}</p><div class="image-facts"><b>CHINA</b><b>INDIA</b><b>VIETNAM</b><b>GLOBAL NEXT</b></div></div>
    </section>

    <section class="section">
      <div class="section-head"><div><span>OPEN ROLES / 04</span><h2>{{ t.openRoles }}</h2></div><RouterLink class="text-link" to="/jobs">{{ t.viewAll }} ({{ jobs.length }}) →</RouterLink></div>
      <div class="jobs-grid"><JobCard v-for="job in featured" :key="job.slug" :job="job" /></div>
    </section>

    <section class="section life-strip">
      <div class="metric"><b>13–14</b><span>{{ t.salary }}</span></div>
      <div class="metric"><b>30+</b><span>{{ t.roles }}</span></div>
      <div class="metric"><b>2</b><span>{{ t.tracks }}</span></div>
      <div class="metric"><b>∞</b><span>{{ t.possibility }}</span></div>
    </section>

    <section class="section photo-grid">
      <RouterLink to="/about"><img src="/images/xw-asia-collaboration-v2.png" alt="Chinese, Indian and Vietnamese technology team collaborating" /><div><span>PEOPLE / IDEAS</span><h3>{{ language==='zh' ? '从中国、印度、越南出发，与全球优秀人才一起创造' : 'Built across China, India and Vietnam — growing with talent worldwide' }}</h3></div></RouterLink>
      <RouterLink to="/life"><img src="/images/xw-asia-team-life-v2.png" alt="Chinese, Indian and Vietnamese colleagues at a team dinner" /><div><span>LIFE / MOMENTS</span><h3>{{ language==='zh' ? '不同文化，同一个团队' : 'Different cultures. One team.' }}</h3></div></RouterLink>
    </section>
  </main>
</template>
