<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import ApplyModal from "../components/ApplyModal.vue";
import XwLogo from "../components/XwLogo.vue";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
import { jobCategory, jobLocation, jobTitle } from "../data/jobI18n";

const applicationOpen = ref(false);
const { jobs, loadJobs } = useJobs();
const { language, setLanguage } = useLanguage();
const label = (zh: string, en: string) => language.value === "zh" ? zh : en;
const featured = computed(() => jobs.value.slice(0, 4));

onMounted(loadJobs);
</script>

<template>
  <main class="campaign-page">
    <header class="campaign-nav">
      <RouterLink to="/" aria-label="XW home"><XwLogo /></RouterLink>
      <div>
        <button class="language-button" type="button" @click="setLanguage(language === 'zh' ? 'en' : 'zh')">
          {{ language === "zh" ? "EN" : "中文" }}
        </button>
        <button class="nav-apply" type="button" @click="applicationOpen = true">
          {{ label("立即投递", "Apply now") }}
        </button>
      </div>
    </header>

    <section class="campaign-hero">
      <div class="hero-copy">
        <p class="eyebrow">XW GLOBAL CAREERS · 2026</p>
        <h1>{{ label("下一份好工作，\n不必受地点限制。", "Great work,\nwithout borders.") }}</h1>
        <p class="hero-lead">
          {{ label("面向全球招聘职能与技术人才。远程协作、清晰晋升、13–14 个月年度薪酬，让你的下一步更值得。", "Join a global team hiring across business and technology. Work remotely, grow clearly and earn competitive annual rewards.") }}
        </p>
        <div class="hero-actions">
          <button type="button" @click="applicationOpen = true">{{ label("3 分钟投递简历", "Apply in 3 minutes") }} <span>→</span></button>
          <a href="#open-roles">{{ label("先看看热招岗位", "Explore open roles") }}</a>
        </div>
        <div class="trust-row">
          <span><b>13–14</b>{{ label("个月薪资", "months pay") }}</span>
          <span><b>9H</b>{{ label("每日工时", "workday") }}</span>
          <span><b>GLOBAL</b>{{ label("远程协作", "remote team") }}</span>
        </div>
      </div>
      <div class="hero-media">
        <img src="/images/xw-asia-collaboration-v2.png" alt="XW global team collaboration" />
        <div class="media-card">
          <i></i>
          <span>{{ label("正在全球招募", "Now hiring globally") }}</span>
          <strong>{{ jobs.length || 33 }}+</strong>
          <small>{{ label("开放岗位", "open roles") }}</small>
        </div>
      </div>
    </section>

    <section class="value-strip">
      <article><span>01</span><div><h2>{{ label("有竞争力的回报", "Competitive rewards") }}</h2><p>{{ label("年度薪酬、季度奖金，以及住房、餐饮、医疗和交通支持。", "Annual pay, quarterly bonuses and practical support for life abroad.") }}</p></div></article>
      <article><span>02</span><div><h2>{{ label("真正清晰的成长", "Clear career growth") }}</h2><p>{{ label("扁平沟通、透明晋升与独立申诉机制，让贡献被公平看见。", "Open communication, transparent promotion and fair employee support.") }}</p></div></article>
      <article><span>03</span><div><h2>{{ label("跨区域协作", "Work across borders") }}</h2><p>{{ label("连接中国、印度、越南及更多市场，积累真正国际化的经验。", "Collaborate across China, India, Vietnam and emerging global markets.") }}</p></div></article>
    </section>

    <section id="open-roles" class="featured-roles">
      <div class="section-heading">
        <div><span>SELECTED OPPORTUNITIES</span><h2>{{ label("现在，找到适合你的机会。", "Find the role that moves you forward.") }}</h2></div>
        <RouterLink to="/jobs">{{ label("查看全部岗位", "View all roles") }} →</RouterLink>
      </div>
      <div class="role-grid">
        <RouterLink v-for="job in featured" :key="job.slug" :to="`/jobs/${job.slug}`">
          <div><small>{{ jobCategory(job, language) }}</small><b>↗</b></div>
          <h3>{{ jobTitle(job, language) }}</h3>
          <p>{{ jobLocation(job, language) }}</p>
          <strong>{{ job.compensation || label("薪资面议", "Competitive salary") }}</strong>
        </RouterLink>
      </div>
    </section>

    <section class="campaign-steps">
      <span>FAST APPLICATION</span>
      <h2>{{ label("从看到机会，到开始对话。", "From opportunity to conversation.") }}</h2>
      <div><p><b>1</b>{{ label("填写基本信息", "Share your profile") }}</p><i></i><p><b>2</b>{{ label("上传个人简历", "Upload your resume") }}</p><i></i><p><b>3</b>{{ label("招聘团队联系", "Hear from our team") }}</p></div>
    </section>

    <section class="campaign-final">
      <p>XW · CHINA · INDIA · VIETNAM · GLOBAL</p>
      <h2>{{ label("你的下一段职业旅程，\n可以从今天开始。", "Your next chapter\ncan start today.") }}</h2>
      <button type="button" @click="applicationOpen = true">{{ label("立即投递简历", "Apply now") }} →</button>
      <a href="https://t.me/XWcompany123" target="_blank" rel="noopener">Telegram · @XWcompany123</a>
    </section>

    <ApplyModal :open="applicationOpen" @close="applicationOpen = false" />
  </main>
</template>

<style scoped>
.campaign-page{--ink:#101114;--blue:#0668e8;--soft:#f3f6fb;min-height:100vh;background:#fff;color:var(--ink);font-family:-apple-system,BlinkMacSystemFont,"SF Pro Display","Helvetica Neue",Arial,sans-serif}.campaign-nav{height:68px;padding:0 clamp(20px,5vw,72px);display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid #e8e8ed;background:rgba(255,255,255,.9);backdrop-filter:blur(20px);position:relative;z-index:5}.campaign-nav :deep(.xw-logo){width:82px;filter:brightness(0)}.campaign-nav>div{display:flex;align-items:center;gap:16px}.campaign-nav button{border:0;cursor:pointer}.language-button{padding:9px;background:transparent;color:#515154}.nav-apply,.hero-actions button,.campaign-final button{border-radius:999px;background:var(--blue);color:#fff;font-weight:700}.nav-apply{padding:11px 18px}.campaign-hero{min-height:760px;padding:90px clamp(22px,7vw,112px);display:grid;grid-template-columns:1fr .9fr;align-items:center;gap:7vw;background:radial-gradient(circle at 18% 20%,#e8f2ff,transparent 34%),#fbfcff}.eyebrow,.featured-roles span,.campaign-steps>span{color:var(--blue);font-size:12px;font-weight:700;letter-spacing:.14em}.hero-copy h1{white-space:pre-line;margin:28px 0;font-size:clamp(54px,6.7vw,98px);line-height:.96;letter-spacing:-.065em}.hero-lead{max-width:650px;color:#626268;font-size:20px;line-height:1.65}.hero-actions{display:flex;align-items:center;gap:28px;margin-top:38px}.hero-actions button{padding:17px 24px;font-size:16px;border:0}.hero-actions button span{margin-left:28px}.hero-actions a{color:var(--blue);text-decoration:none}.trust-row{display:flex;gap:34px;margin-top:64px}.trust-row span{display:flex;flex-direction:column;gap:4px;color:#85858a;font-size:11px}.trust-row b{color:#1d1d1f;font-size:23px}.hero-media{position:relative}.hero-media>img{width:100%;height:590px;object-fit:cover;border-radius:36px;box-shadow:0 35px 80px #274e8129}.media-card{position:absolute;left:-38px;bottom:30px;min-width:190px;padding:24px;border-radius:22px;background:rgba(255,255,255,.9);box-shadow:0 16px 48px #1630522b;backdrop-filter:blur(18px);display:grid}.media-card i{width:8px;height:8px;border-radius:50%;background:#30d17d;box-shadow:0 0 0 7px #30d17d1d}.media-card span{margin:19px 0 5px;color:#66666b;font-size:12px}.media-card strong{font-size:42px}.media-card small{color:#8a8a8f}.value-strip{display:grid;grid-template-columns:repeat(3,1fr);padding:0 clamp(22px,6vw,96px)}.value-strip article{display:flex;gap:24px;padding:70px 38px;border-right:1px solid #e6e6ea}.value-strip article:last-child{border:0}.value-strip article>span{color:var(--blue);font-weight:700}.value-strip h2{margin:0 0 14px;font-size:24px;letter-spacing:-.03em}.value-strip p{margin:0;color:#737378;line-height:1.7}.featured-roles{padding:110px clamp(22px,7vw,112px);background:var(--soft)}.section-heading{display:flex;align-items:end;justify-content:space-between;gap:30px;margin-bottom:48px}.section-heading h2{max-width:720px;margin:18px 0 0;font-size:clamp(40px,5vw,64px);line-height:1.05;letter-spacing:-.055em}.section-heading>a{color:var(--blue);text-decoration:none}.role-grid{display:grid;grid-template-columns:repeat(4,1fr);gap:16px}.role-grid>a{min-height:250px;padding:28px;border-radius:24px;background:#fff;color:inherit;text-decoration:none;transition:.25s}.role-grid>a:hover{transform:translateY(-6px);box-shadow:0 24px 50px #17375f12}.role-grid>a>div{display:flex;justify-content:space-between}.role-grid small,.role-grid>a>div b{color:var(--blue)}.role-grid h3{margin:45px 0 14px;font-size:22px}.role-grid p{color:#85858a}.role-grid strong{display:block;margin-top:30px;font-size:13px}.campaign-steps{padding:120px 22px;text-align:center}.campaign-steps h2{margin:20px auto 55px;font-size:clamp(38px,5vw,62px);letter-spacing:-.05em}.campaign-steps>div{display:flex;align-items:center;justify-content:center;gap:24px}.campaign-steps p{display:flex;align-items:center;gap:14px;color:#515154}.campaign-steps p b{display:grid;place-items:center;width:36px;height:36px;border-radius:50%;background:#e8f2ff;color:var(--blue)}.campaign-steps i{width:70px;height:1px;background:#d7d7dc}.campaign-final{padding:130px 22px;text-align:center;background:#101114;color:#fff}.campaign-final p{color:#7baef4;font-size:11px;letter-spacing:.16em}.campaign-final h2{white-space:pre-line;margin:28px 0 42px;font-size:clamp(48px,6vw,76px);line-height:1.05;letter-spacing:-.055em}.campaign-final button{display:block;margin:auto;padding:18px 30px;border:0;font-size:16px}.campaign-final>a{display:inline-block;margin-top:30px;color:#a9a9af;text-decoration:none;font-size:13px}
@media(max-width:980px){.campaign-hero{grid-template-columns:1fr}.hero-media>img{height:480px}.role-grid{grid-template-columns:1fr 1fr}.value-strip{grid-template-columns:1fr}.value-strip article{border-right:0;border-bottom:1px solid #e6e6ea}.value-strip article:last-child{border-bottom:0}}
@media(max-width:620px){.campaign-nav{height:58px;padding:0 16px}.campaign-nav :deep(.xw-logo){width:66px}.campaign-nav>div{gap:7px}.language-button{padding:8px 6px;font-size:12px}.nav-apply{padding:9px 13px;font-size:12px}.campaign-hero{min-height:auto;padding:52px 18px 62px;gap:46px}.eyebrow{font-size:10px;letter-spacing:.1em}.hero-copy h1{margin:20px 0;font-size:42px;line-height:1.02}.hero-lead{font-size:15px;line-height:1.65}.hero-actions{align-items:stretch;flex-direction:column;gap:18px;margin-top:28px}.hero-actions button{width:100%;padding:14px 18px}.hero-actions a{text-align:center;font-size:13px}.trust-row{justify-content:space-between;gap:8px;margin-top:38px}.trust-row b{font-size:19px}.trust-row span{font-size:9px}.hero-media>img{height:300px;border-radius:20px}.media-card{left:12px;bottom:12px;min-width:150px;padding:16px;border-radius:16px}.media-card span{margin:12px 0 3px}.media-card strong{font-size:32px}.value-strip{padding:0 18px}.value-strip article{gap:15px;padding:32px 0}.value-strip h2{font-size:20px}.value-strip p{font-size:14px}.featured-roles{padding:68px 18px}.section-heading{align-items:flex-start;flex-direction:column;gap:15px;margin-bottom:28px}.section-heading h2{font-size:36px}.role-grid{grid-template-columns:1fr}.role-grid>a{min-height:0;padding:22px;border-radius:18px}.role-grid h3{margin:28px 0 10px}.campaign-steps{padding:72px 18px}.campaign-steps h2{margin-bottom:38px;font-size:36px}.campaign-steps>div{align-items:flex-start;flex-direction:column;width:100%;margin:auto;gap:10px}.campaign-steps p{margin:0}.campaign-steps i{width:1px;height:22px;margin-left:18px}.campaign-final{padding:78px 18px}.campaign-final h2{font-size:38px}.campaign-final button{width:100%;max-width:340px}}
</style>
