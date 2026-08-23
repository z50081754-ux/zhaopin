<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink } from "vue-router";
import ApplyModal from "../components/ApplyModal.vue";
import XwLogo from "../components/XwLogo.vue";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
import { jobCategory, jobLocation, jobTitle } from "../data/jobI18n";

type Variant = "tech" | "performance" | "global" | "apple";
const props = defineProps<{ variant: Variant }>();
const applicationOpen = ref(false);
const isDev = import.meta.env.DEV;
const { jobs, loadJobs } = useJobs();
const { language, setLanguage } = useLanguage();
onMounted(loadJobs);

const content = computed(() => ({
  tech: {
    code: "A / TECHNOLOGY",
    eyebrow: "GLOBAL TECHNOLOGY TEAM · NOW HIRING",
    title: "把复杂问题，\n做成下一代产品。",
    enTitle: "Build what comes next.",
    leadZh: "加入快速成长的国际化团队，与优秀的产品、技术和业务伙伴一起，把想法推向真实世界。",
    leadEn: "Join a fast-growing global team and turn ambitious ideas into products that matter.",
    accent: "ENGINEER THE FUTURE",
    image: "/images/xw-asia-collaboration-v2.png"
  },
  performance: {
    code: "B / CONVERSION",
    eyebrow: "REMOTE OPPORTUNITIES · FAST PROCESS",
    title: "好机会不绕路，\n直接聊岗位与回报。",
    enTitle: "Your next move starts here.",
    leadZh: "远程与居家岗位持续开放，13–14 个月年度薪酬、季度奖金及多项海外福利，流程清晰高效。",
    leadEn: "Explore remote opportunities, competitive annual compensation and a clear, efficient hiring process.",
    accent: "APPLY IN 3 MINUTES",
    image: "/images/xw-asia-global-team-v2.png"
  },
  global: {
    code: "C / PEOPLE",
    eyebrow: "CHINA · INDIA · VIETNAM · GLOBAL",
    title: "不同文化，\n共同创造同一个未来。",
    enTitle: "Different cultures. One future.",
    leadZh: "从亚洲出发，连接全球人才。我们相信开放、尊重和清晰的成长路径，能让每个人发挥真正价值。",
    leadEn: "Built in Asia and connected worldwide. Grow in a team shaped by openness, respect and opportunity.",
    accent: "GROW WITHOUT BORDERS",
    image: "/images/xw-asia-team-life-v2.png"
  },
  apple: {
    code: "D / MINIMAL",
    eyebrow: "XW CAREERS",
    title: "让出色的工作，\n成为日常。",
    enTitle: "Do the best work of your life.",
    leadZh: "和聪明、坦诚、热爱创造的人一起工作。少一点边界，多一点可能，把真正重要的想法做出来。",
    leadEn: "Work with thoughtful people, challenge what is possible and create things that genuinely matter.",
    accent: "MAKE SOMETHING WONDERFUL",
    image: "/images/xw-asia-global-team-v2.png"
  }
}[props.variant]));

const featured = computed(() => jobs.value.slice(0, 6));
const label = (zh: string, en: string) => language.value === "zh" ? zh : en;
</script>

<template>
  <main class="landing" :class="`landing-${variant}`">
    <header class="landing-nav">
      <RouterLink to="/"><XwLogo /></RouterLink>
      <div class="landing-tools">
        <template v-if="variant==='apple'">
          <button class="apple-language" type="button" :aria-label="label('切换为英文','Switch to Chinese')" @click="setLanguage(language==='zh'?'en':'zh')">
            <svg viewBox="0 0 24 24" aria-hidden="true"><circle cx="12" cy="12" r="9"/><path d="M3 12h18M12 3c2.3 2.5 3.5 5.5 3.5 9s-1.2 6.5-3.5 9c-2.3-2.5-3.5-5.5-3.5-9S9.7 5.5 12 3Z"/></svg>
            {{ language==='zh' ? 'English' : '中文' }}
          </button>
          <RouterLink class="apple-nav-jobs" to="/jobs">{{ label('职位','Jobs') }}</RouterLink>
        </template>
        <template v-else>
          <div class="landing-language"><button :class="{active:language==='zh'}" @click="setLanguage('zh')">中</button><button :class="{active:language==='en'}" @click="setLanguage('en')">EN</button></div>
          <a href="https://t.me/XWcompany123" target="_blank" rel="noopener">@XWcompany123</a>
        </template>
        <button class="landing-nav-cta" @click="applicationOpen=true">{{ label('立即投递','Apply now') }}</button>
      </div>
    </header>

    <section class="landing-hero">
      <div class="landing-copy">
        <span class="landing-eyebrow">{{ content.eyebrow }}</span>
        <h1>{{ language==='zh' ? content.title : content.enTitle }}</h1>
        <p>{{ language==='zh' ? content.leadZh : content.leadEn }}</p>
        <div class="landing-actions"><button @click="applicationOpen=true">{{ label('投递简历','Apply now') }} <b>→</b></button><RouterLink to="/jobs">{{ label(`浏览全部 ${jobs.length} 个岗位`,`Explore all ${jobs.length} roles`) }}</RouterLink></div>
        <div class="landing-proof"><div><b>13–14</b><span>{{ label('个月年度薪酬','months annual pay') }}</span></div><div><b>{{ jobs.length }}+</b><span>{{ label('开放岗位','open roles') }}</span></div><div><b>GLOBAL</b><span>{{ label('远程协作','remote collaboration') }}</span></div></div>
      </div>
      <div class="landing-visual">
        <img :src="content.image" alt="XW global team" />
        <i></i><span>{{ content.accent }}</span><small>{{ content.code }}</small>
      </div>
    </section>

    <section class="landing-benefits">
      <article><span>01</span><h2>{{ label('有竞争力的回报','Competitive rewards') }}</h2><p>{{ label('13–14 个月薪资、季度奖金，以及餐补、住房、医疗和交通支持。','13–14 months annual pay, quarterly bonuses and practical support for life abroad.') }}</p></article>
      <article><span>02</span><h2>{{ label('清晰的成长路径','Clear growth paths') }}</h2><p>{{ label('专业与管理双通道，目标透明，关键岗位获得持续指导与成长反馈。','Grow through expert or leadership tracks with transparent goals and meaningful feedback.') }}</p></article>
      <article><span>03</span><h2>{{ label('全球化协作体验','Global collaboration') }}</h2><p>{{ label('连接中国、印度、越南及更多地区，在多元团队中建立国际化能力。','Work across China, India, Vietnam and beyond in a genuinely multicultural team.') }}</p></article>
    </section>

    <section class="landing-roles">
      <div class="landing-section-title"><span>OPEN POSITIONS</span><h2>{{ label('此刻，寻找同行者','Find your next mission') }}</h2><button @click="applicationOpen=true">{{ label('没有合适岗位？直接投递 →','Can’t find your role? Apply anyway →') }}</button></div>
      <div class="landing-role-list"><RouterLink v-for="job in featured" :key="job.slug" :to="`/jobs/${job.slug}`"><small>{{ jobCategory(job,language) }}</small><h3>{{ jobTitle(job,language) }}</h3><p>{{ jobLocation(job,language) }}</p><b>{{ label('查看岗位','View role') }} →</b></RouterLink></div>
    </section>

    <section class="landing-final"><span>{{ content.accent }}</span><h2>{{ label('下一段职业旅程，','Your next chapter') }}<br>{{ label('从一次对话开始。','starts with a conversation.') }}</h2><button @click="applicationOpen=true">{{ label('现在加入 XW　→','Join XW now　→') }}</button></section>

    <nav v-if="isDev" class="variant-picker" aria-label="落地页方案切换"><span>方案预览</span><RouterLink to="/landing/tech">A 科技感</RouterLink><RouterLink to="/landing/performance">B 高转化</RouterLink><RouterLink to="/landing/global">C 全球团队</RouterLink><RouterLink to="/landing/apple">D 极简</RouterLink></nav>
    <ApplyModal :open="applicationOpen" @close="applicationOpen=false" />
  </main>
</template>

<style scoped>
.landing{--accent:#67f5b5;--accent2:#2aa8ff;min-height:100vh;background:#061724;color:#eaf7ff;overflow:hidden}.landing-performance{--accent:#c8ff4d;--accent2:#ffb23f;background:#10140b}.landing-global{--accent:#ffbf88;--accent2:#7fa8ff;background:#101625}.landing-nav{height:82px;padding:0 clamp(22px,5vw,76px);display:flex;align-items:center;justify-content:space-between;border-bottom:1px solid rgba(147,203,224,.2);position:relative;z-index:4}.landing-nav :deep(.xw-logo){width:86px}.landing-tools,.landing-language{display:flex;align-items:center;gap:20px}.landing-tools>a{color:var(--accent);font:12px "DM Mono";text-decoration:none}.landing-language{gap:0;border:1px solid rgba(255,255,255,.18)}.landing-language button{padding:8px 10px;border:0;background:transparent;color:#8aa3af}.landing-language button.active{background:var(--accent);color:#061724}.landing-nav-cta,.landing-actions button,.landing-final button{border:0;border-radius:4px;background:var(--accent);color:#07151d;font-weight:800;cursor:pointer}.landing-nav-cta{padding:13px 20px}.landing-hero{min-height:720px;padding:clamp(55px,8vw,115px) clamp(22px,7vw,110px);display:grid;grid-template-columns:1.05fr .95fr;align-items:center;gap:6vw;position:relative}.landing-hero:before{content:"";position:absolute;inset:0;background:linear-gradient(rgba(73,177,215,.07) 1px,transparent 1px),linear-gradient(90deg,rgba(73,177,215,.07) 1px,transparent 1px);background-size:52px 52px;mask-image:linear-gradient(to right,#000,transparent 70%)}.landing-copy,.landing-visual{position:relative;z-index:1}.landing-eyebrow{color:var(--accent);font:12px "DM Mono";letter-spacing:.18em}.landing h1{white-space:pre-line;font-size:clamp(50px,6vw,92px);line-height:.98;letter-spacing:-.055em;margin:28px 0}.landing-copy>p{max-width:620px;color:#91aab7;font-size:18px;line-height:1.9}.landing-actions{display:flex;align-items:center;gap:28px;margin-top:38px}.landing-actions button{padding:20px 30px;font-size:16px}.landing-actions button b{margin-left:30px}.landing-actions a{color:#d8e7ee;text-decoration:none;border-bottom:1px solid #6e8995;padding:10px 0}.landing-proof{display:flex;gap:36px;margin-top:68px}.landing-proof div{display:flex;flex-direction:column;gap:6px}.landing-proof b{color:var(--accent);font:24px "DM Mono"}.landing-proof span{color:#718b98;font-size:11px}.landing-visual{min-height:580px}.landing-visual img{width:100%;height:580px;object-fit:cover;filter:saturate(.72) contrast(1.08)}.landing-visual:after{content:"";position:absolute;inset:0;background:linear-gradient(145deg,transparent 45%,rgba(4,16,25,.82))}.landing-visual i{position:absolute;inset:-18px 18px 18px -18px;border:1px solid color-mix(in srgb,var(--accent) 52%,transparent);z-index:-1}.landing-visual>span{position:absolute;right:-15px;bottom:46px;z-index:2;padding:17px 20px;background:var(--accent);color:#06131c;font:700 12px "DM Mono";letter-spacing:.1em}.landing-visual>small{position:absolute;top:22px;left:22px;z-index:2;font:11px "DM Mono";letter-spacing:.15em}.landing-benefits{display:grid;grid-template-columns:repeat(3,1fr);border-top:1px solid rgba(147,203,224,.2);border-bottom:1px solid rgba(147,203,224,.2)}.landing-benefits article{padding:55px clamp(26px,4vw,60px);border-right:1px solid rgba(147,203,224,.2)}.landing-benefits span,.landing-section-title>span{color:var(--accent);font:11px "DM Mono"}.landing-benefits h2{font-size:23px;margin:38px 0 16px}.landing-benefits p{color:#829ba7;line-height:1.8}.landing-roles{padding:110px clamp(22px,7vw,110px)}.landing-section-title{display:flex;align-items:end;gap:35px;margin-bottom:48px}.landing-section-title h2{font-size:44px;margin:0 auto 0 0}.landing-section-title button{border:0;background:none;color:var(--accent);cursor:pointer}.landing-role-list{display:grid;grid-template-columns:repeat(3,1fr);gap:12px}.landing-role-list a{padding:30px;min-height:205px;border:1px solid rgba(147,203,224,.22);color:inherit;text-decoration:none;transition:.2s}.landing-role-list a:hover{border-color:var(--accent);transform:translateY(-4px)}.landing-role-list small{color:var(--accent)}.landing-role-list h3{font-size:21px;margin:35px 0 12px}.landing-role-list p{color:#7f98a4}.landing-role-list b{display:block;margin-top:28px;color:var(--accent);font-size:12px}.landing-final{text-align:center;padding:120px 22px;background:radial-gradient(circle at 50% 100%,color-mix(in srgb,var(--accent2) 24%,transparent),transparent 55%)}.landing-final>span{color:var(--accent);font:11px "DM Mono";letter-spacing:.2em}.landing-final h2{font-size:clamp(42px,5vw,72px);line-height:1.15;margin:25px 0 38px}.landing-final button{padding:20px 32px;font-size:16px}.variant-picker{position:fixed;z-index:20;left:50%;bottom:18px;transform:translateX(-50%);display:flex;align-items:center;gap:5px;padding:6px;border:1px solid rgba(255,255,255,.18);border-radius:6px;background:rgba(3,13,20,.9);backdrop-filter:blur(15px);box-shadow:0 10px 40px #0008}.variant-picker span{padding:0 10px;color:#718b98;font-size:10px}.variant-picker a{padding:10px 13px;border-radius:3px;color:#b9cbd3;text-decoration:none;font-size:11px}.variant-picker a.router-link-active{background:var(--accent);color:#07151d}.landing-performance .landing-visual img{filter:saturate(.8) sepia(.12)}.landing-global .landing-hero:before{background:radial-gradient(circle at 22% 30%,rgba(127,168,255,.2),transparent 35%)}
.landing-apple{--accent:#0071e3;--accent2:#8ab4ff;background:#fff;color:#1d1d1f;font-family:-apple-system,BlinkMacSystemFont,"SF Pro Display","Helvetica Neue",Arial,sans-serif}.landing-apple .landing-nav{height:64px;border-color:#e8e8ed;background:rgba(255,255,255,.82);backdrop-filter:saturate(180%) blur(20px)}.landing-apple .landing-nav :deep(.xw-logo){filter:brightness(0);opacity:.88}.landing-apple .landing-tools{gap:28px}.landing-apple .apple-language{display:flex;align-items:center;gap:7px;padding:8px 0;border:0;background:transparent;color:#424245;font:500 13px -apple-system,BlinkMacSystemFont,"SF Pro Text","Helvetica Neue",Arial,sans-serif;cursor:pointer}.landing-apple .apple-language svg{width:16px;height:16px;fill:none;stroke:currentColor;stroke-width:1.5}.landing-apple .apple-language:hover,.landing-apple .apple-nav-jobs:hover{color:#0071e3}.landing-apple .apple-nav-jobs{color:#424245;font:500 13px -apple-system,BlinkMacSystemFont,"SF Pro Text","Helvetica Neue",Arial,sans-serif;text-decoration:none}.landing-apple .landing-nav-cta,.landing-apple .landing-actions button,.landing-apple .landing-final button{border-radius:999px;background:#0071e3;color:#fff;font-weight:600}.landing-apple .landing-nav-cta{padding:10px 18px;font-size:13px;transition:background .2s,transform .2s}.landing-apple .landing-nav-cta:hover{background:#0077ed;transform:translateY(-1px)}.landing-apple .landing-hero{min-height:810px;grid-template-columns:1fr;padding-top:95px;text-align:center}.landing-apple .landing-hero:before{background:radial-gradient(circle at 50% 22%,#e7f1ff 0,rgba(241,246,255,.85) 25%,transparent 57%);mask-image:none}.landing-apple .landing-copy{max-width:1050px;margin:auto}.landing-apple .landing-eyebrow{color:#6e6e73;font-family:inherit;font-weight:600;letter-spacing:.08em}.landing-apple h1{font-size:clamp(64px,8vw,118px);line-height:.92;letter-spacing:-.065em;background:linear-gradient(110deg,#1d1d1f 25%,#396fbd 62%,#774ca8);-webkit-background-clip:text;color:transparent;margin:32px 0}.landing-apple .landing-copy>p{max-width:720px;margin:auto;color:#6e6e73;font-size:22px;line-height:1.55}.landing-apple .landing-actions{justify-content:center}.landing-apple .landing-actions button{padding:16px 26px}.landing-apple .landing-actions a{color:#06c;border:0}.landing-apple .landing-proof{justify-content:center}.landing-apple .landing-proof b{color:#1d1d1f;font-family:inherit;font-size:28px}.landing-apple .landing-proof span{color:#86868b;font-size:12px}.landing-apple .landing-visual{min-height:0;width:min(1120px,86vw);margin:72px auto 0}.landing-apple .landing-visual img{height:min(620px,60vw);border-radius:34px;filter:saturate(.85);box-shadow:0 35px 90px rgba(32,62,105,.2)}.landing-apple .landing-visual:after,.landing-apple .landing-visual i,.landing-apple .landing-visual>small{display:none}.landing-apple .landing-visual>span{right:28px;bottom:28px;border-radius:999px;background:rgba(255,255,255,.82);color:#1d1d1f;font-family:inherit;backdrop-filter:blur(18px)}.landing-apple .landing-benefits{margin-top:170px;padding:0 clamp(22px,7vw,110px);gap:22px;border:0}.landing-apple .landing-benefits article{border:0;border-radius:28px;background:#f5f5f7;padding:50px}.landing-apple .landing-benefits span,.landing-apple .landing-section-title>span{color:#86868b;font-family:inherit;font-weight:600}.landing-apple .landing-benefits h2{font-size:28px;letter-spacing:-.03em}.landing-apple .landing-benefits p{color:#6e6e73}.landing-apple .landing-roles{background:#f5f5f7;margin-top:120px}.landing-apple .landing-section-title h2{font-size:52px;letter-spacing:-.045em}.landing-apple .landing-section-title button{color:#06c}.landing-apple .landing-role-list{gap:18px}.landing-apple .landing-role-list a{border:0;border-radius:24px;background:#fff;box-shadow:0 1px 0 #00000008}.landing-apple .landing-role-list a:hover{border:0;transform:translateY(-5px);box-shadow:0 20px 45px #00000010}.landing-apple .landing-role-list small,.landing-apple .landing-role-list b{color:#06c}.landing-apple .landing-role-list p{color:#86868b}.landing-apple .landing-final{background:#fff;padding:160px 22px}.landing-apple .landing-final>span{color:#86868b;font-family:inherit}.landing-apple .landing-final h2{letter-spacing:-.05em}.landing-apple .variant-picker{border-color:#d2d2d7;background:rgba(255,255,255,.88);box-shadow:0 10px 40px #0002}.landing-apple .variant-picker a{color:#515154}.landing-apple .variant-picker a.router-link-active{background:#1d1d1f;color:#fff}
@media(max-width:900px){.landing-tools>a{display:none}.landing-hero{grid-template-columns:1fr;padding-bottom:75px}.landing-visual{min-height:420px}.landing-visual img{height:420px}.landing-benefits{grid-template-columns:1fr}.landing-benefits article{border-right:0;border-bottom:1px solid rgba(147,203,224,.2)}.landing-role-list{grid-template-columns:1fr 1fr}}
@media(max-width:600px){.landing-nav{height:68px}.landing-tools{gap:8px}.landing-nav-cta{padding:10px 12px}.landing h1{font-size:48px}.landing-copy>p{font-size:15px}.landing-actions{align-items:flex-start;flex-direction:column;gap:14px}.landing-proof{gap:15px;justify-content:space-between;margin-top:45px}.landing-proof b{font-size:18px}.landing-visual{min-height:330px}.landing-visual img{height:330px}.landing-section-title{align-items:flex-start;flex-direction:column}.landing-section-title h2{font-size:36px}.landing-role-list{grid-template-columns:1fr}.variant-picker span{display:none}.variant-picker{width:calc(100% - 24px);justify-content:center;overflow-x:auto}.variant-picker a{flex:1;text-align:center;white-space:nowrap}.landing-language{display:none}.landing-apple .landing-hero{padding-top:75px}.landing-apple h1{font-size:54px}.landing-apple .landing-copy>p{font-size:18px}.landing-apple .landing-actions{align-items:center}.landing-apple .landing-proof{justify-content:space-between}.landing-apple .landing-visual{width:100%;margin-top:48px;min-height:0}.landing-apple .landing-visual img{height:330px;border-radius:22px}.landing-apple .landing-benefits{margin-top:90px}.landing-apple .landing-benefits article{padding:35px 28px}.landing-apple .landing-roles{margin-top:80px}.landing-apple .landing-final{padding:110px 22px}}
</style>
