import { computed, ref } from "vue";
import { apiUrl } from "../utils/api";

export type Language = "zh" | "en";

const saved = typeof localStorage !== "undefined" ? localStorage.getItem("xw-language") : null;
const hasSavedLanguage = saved === "zh" || saved === "en";
const language = ref<Language>(saved === "zh" || saved === "en" ? saved : "en");
let regionInitialization: Promise<void> | null = null;
let manuallyChangedThisVisit = false;

async function initializeLanguageByRegion() {
  if (typeof window === "undefined") return;
  if (hasSavedLanguage) return;
  if (regionInitialization) return regionInitialization;

  regionInitialization = fetch(apiUrl("/api/locale"), { headers: { Accept: "application/json" } })
    .then(async (response) => {
      if (!response.ok) throw new Error(`Locale request failed: ${response.status}`);
      const result = await response.json() as { language?: Language };
      // A manual language switch made while the request was running always wins.
      if (!manuallyChangedThisVisit && (result.language === "zh" || result.language === "en")) {
        language.value = result.language;
        document.documentElement.lang = result.language === "zh" ? "zh-CN" : "en";
      }
    })
    .catch(() => {
      // English is the safe default when the visitor's country cannot be resolved.
      if (!manuallyChangedThisVisit) language.value = "en";
    });

  return regionInitialization;
}

const messages = {
  zh: {
    navJobs:"职位",navAbout:"关于我们",navBusiness:"业务版图",navLife:"员工生活",navGrowth:"成长福利",
    apply:"投递简历",headlineA:"加入 XW，让技术",headlineB:"与热爱彼此成就",heroLead:"面向全球的科技团队，职能与技术双通道，30+ 热招岗位。",heroSub:"登陆数字总部，与下一代产品共同进化。",
    enterJobs:"进入职位中心",discoverTitle:"不止一份工作，更是一张新世界地图",discoverLead:"我们关注结果，也认真建设让人才长期成长的环境。",
    teamLife:"员工生活",teamLifeDesc:"真实记录聚餐、团建、节日现场与团队故事。",
    growth:"成长体系",growthDesc:"专业与管理双通道，关键岗位导师陪跑。",
    business:"业务版图",businessDesc:"连接游戏、平台、AI 与全球化技术产品。",
    openRoles:"此刻，寻找同行者",viewAll:"查看全部职位",globalTeam:"从亚洲出发，与全球优秀人才一起工作",globalDesc:"XW 当前深耕中国、印度和越南市场，并持续走向全球。我们希望更多不同背景的人加入，共同创造下一代产品。",
    salary:"个月年度薪酬",roles:"开放岗位",tracks:"职能与技术通道",possibility:"成长可能",
    footer:"让技术与热爱彼此成就",company:"公司介绍",contact:"联系我们",telegram:"Telegram 联系"
  },
  en: {
    navJobs:"Jobs",navAbout:"About",navBusiness:"Business",navLife:"Life at XW",navGrowth:"Growth",
    apply:"Apply Now",headlineA:"Build the future with XW.",headlineB:"Where technology meets passion.",heroLead:"A global technology team with 30+ opportunities across business and engineering.",heroSub:"Enter our digital headquarters and evolve with the next generation of products.",
    enterJobs:"Explore open roles",discoverTitle:"More than a job. A map to what comes next.",discoverLead:"We care about results and build an environment where great people can grow for the long term.",
    teamLife:"Life at XW",teamLifeDesc:"Real moments from team dinners, gatherings, celebrations and everyday collaboration.",
    growth:"Growth paths",growthDesc:"Dual expert and leadership tracks, with mentorship for key roles.",
    business:"What we build",businessDesc:"Connecting gaming, platforms, AI and global technology products.",
    openRoles:"Find your next mission",viewAll:"View all roles",globalTeam:"Built in Asia. Growing with talent worldwide.",globalDesc:"XW currently serves China, India and Vietnam and is expanding globally. We welcome people from every background to build the next generation of products with us.",
    salary:"months annual compensation",roles:"open positions",tracks:"career tracks",possibility:"possibilities",
    footer:"Where technology meets passion",company:"Company",contact:"Contact",telegram:"Contact on Telegram"
  }
} as const;

export function useLanguage() {
  const t = computed(() => messages[language.value]);
  const setLanguage = (next: Language) => {
    manuallyChangedThisVisit = true;
    language.value = next;
    localStorage.setItem("xw-language", next);
    document.documentElement.lang = next === "zh" ? "zh-CN" : "en";
  };
  if (typeof document !== "undefined") document.documentElement.lang = language.value === "zh" ? "zh-CN" : "en";
  return { language, t, setLanguage, initializeLanguageByRegion };
}
