<script setup lang="ts">
import { computed } from "vue";
import { useLanguage } from "../composables/useLanguage";
const props = defineProps<{ page: string }>();
const { language } = useLanguage();
const pagesZh: Record<string, any> = {
  about:{code:"ABOUT / 05",title:"年轻、快速、面向全球的 XW",lead:"XW 成立于 2024 年，在快速发展中已经汇聚千余名在职员工。团队目前分布在泰国、日本、越南和马来西亚，持续连接亚洲市场并为全球化发展积累能力。",stats:[["2024","公司正式成立"],["1,000+","在职员工"],["4 个国家","泰国、日本、越南、马来西亚"]],sections:[["业务方向","目前主要从事娱乐类项目，围绕产品体验、技术研发和市场运营持续发展；未来将逐步拓展金融类市场项目，建立更加多元的业务版图。"],["扁平化管理","我们倡导直接、清晰、高效的沟通方式，减少不必要的管理层级，让专业意见可以更快到达决策和执行环节。"],["晋升与成长","公司建立清晰的晋升制度，以个人能力、工作成果和团队贡献为主要依据，为职能与技术人才提供持续发展的机会。"],["公平与申诉机制","公司设有专门的投诉处理部门。员工在工作中遇到不公平待遇时，可以通过正式渠道反馈；公司将调查真实情况，并依据制度进行处理。"]]},
  business:{code:"BUSINESS / 06",title:"以娱乐体验为起点，持续探索更广阔的市场",lead:"XW 以数字娱乐项目为当前核心，通过产品、技术和运营协同，为不同市场提供稳定而持续的服务能力。随着团队与全球网络不断成熟，我们也在为下一阶段的多元业务发展做好准备。",stats:[["数字娱乐生态","连接内容、产品、技术与运营"],["全球化运营","服务亚洲市场，持续拓展全球"],["未来业务探索","关注金融市场等新方向"]],sections:[["构建优质娱乐体验","围绕用户需求和市场变化，持续打磨产品内容、交互体验与运营方式，让娱乐项目保持活力并形成长期价值。"],["连接区域与全球市场","依托泰国、日本、越南和马来西亚团队，建立跨地区协作能力；在理解不同市场的基础上，逐步扩大全球服务范围。"],["为下一阶段提前布局","在夯实娱乐业务的同时，持续观察金融市场等新领域的发展机会，通过人才、技术和合规能力的积累，为未来的业务拓展建立基础。"]]},
  life:{code:"TEAM LIFE / 07",title:"认真工作，也认真记录生活",lead:"团队氛围不靠口号。它存在于一次顺畅协作、一场真诚复盘，也存在于聚餐、节日、运动与每一个值得庆祝的时刻。",stats:[["团队聚餐","阶段成果，一起庆祝"],["节日活动","让仪式感自然发生"],["兴趣社群","运动、游戏与创作"]],sections:[["日常连接","跨团队下午茶、主题分享与新人见面会，让信息和关系自然流动。"],["团队时刻","项目里程碑、生日、节日与季度团建，都值得被认真对待。"],["真实尊重","鼓励直接沟通和不同意见，让每个人都可以在安全氛围中表达判断。"]]},
  growth:{code:"BENEFITS / 08",title:"让每一份投入，都得到清晰而长期的回报",lead:"每日工作 9 小时，具体班次由部门安排。薪酬、补贴、医疗与休假共同构成 XW 的员工福利体系。",stats:[["13–14 薪","依据公司效益及个人绩效"],["400 USD / 月","转试用后外宿员工住房补贴"],["1,500 USD / 年","年度医疗报销额度"]],sections:[["成长通道","专业与管理双通道，以真实项目、导师反馈和能力标准支持持续成长。"],["绩效回报","季度奖金及年度计薪月数依据公司效益与个人绩效确定。"],["生活保障","以住房、餐饮、交通、医疗和休假制度支持员工稳定工作与生活。"]]},
  contact:{code:"CONTACT / 09",title:"发送你的坐标，下一站我们一起抵达",lead:"选择感兴趣的岗位，填写基本信息并通过 Telegram 联系招聘团队。",stats:[["Telegram","@XWcompany123"],["工作地点","全球 / 海外远程"],["反馈周期","通常 3—5 个工作日"]],sections:[["投递建议","简历请重点描述你负责的问题、采取的行动、最终结果与个人贡献。"],["作品材料","产品、设计、AI 视频及游戏策划岗位建议附作品集或项目链接。"],["招聘声明","我们重视候选人隐私，不收取任何形式的招聘费用，请注意辨别非官方信息。"]]}
};
const pagesEn: Record<string, any> = {
  about:{code:"ABOUT / 05",title:"Young, fast-moving and built for global growth",lead:"Founded in 2024, XW has grown rapidly to more than 1,000 employees. Our teams are currently based across Thailand, Japan, Vietnam and Malaysia, connecting Asian markets while building for global expansion.",stats:[["2024","Founded"],["1,000+","Employees"],["4 countries","Thailand, Japan, Vietnam and Malaysia"]],sections:[["Business direction","XW currently focuses on entertainment projects across product experience, technology development and market operations. We plan to expand into financial market projects and build a more diversified portfolio."],["Flat management","We value direct, clear and efficient communication, reduce unnecessary layers and help strong professional judgment reach decisions faster."],["Career progression","A structured promotion system recognizes capability, results and team contribution, creating long-term paths for both business and technology talent."],["Fairness and grievance process","XW maintains a dedicated complaints function. Employees can report unfair treatment through a formal channel; the company will investigate the facts and respond according to policy."]]},
  business:{code:"BUSINESS / 06",title:"Starting with entertainment. Building toward broader markets.",lead:"Digital entertainment is at the center of XW today. Product, technology and operations work together to deliver dependable experiences across markets, while the company prepares for a more diverse global future.",stats:[["Digital entertainment","Connecting content, products, technology and operations"],["Global operations","Serving Asia and expanding worldwide"],["Future exploration","Preparing for opportunities in financial markets"]],sections:[["Creating engaging entertainment experiences","We follow user needs and market change, continuously improving content, interaction and operations to build products with sustainable value."],["Connecting regional and global markets","Teams across Thailand, Japan, Vietnam and Malaysia give XW strong cross-market collaboration. Local understanding supports our gradual global expansion."],["Preparing for the next chapter","While strengthening entertainment operations, XW continues to study opportunities in areas such as financial markets and build the talent, technology and compliance foundations needed for responsible growth."]]},
  life:{code:"TEAM LIFE / 07",title:"Do meaningful work. Make room for real life.",lead:"Culture lives in honest reviews, smooth collaboration, shared meals, celebrations and everyday moments worth remembering.",stats:[["Team dinners","Celebrate meaningful milestones"],["Community","Games, sports and creative interests"],["Global team","Different backgrounds, one mission"]],sections:[["Everyday connection","Team sessions, open sharing and onboarding help information and relationships move naturally."],["Milestone moments","Project launches, birthdays and team gatherings are treated with intention."],["Genuine respect","Direct communication and different viewpoints are welcome."]]},
  growth:{code:"BENEFITS / 08",title:"Clear, long-term rewards for meaningful contribution",lead:"The standard workday is 9 hours, with shifts arranged by department. XW supports employees through compensation, allowances, medical coverage and paid leave.",stats:[["13–14 months","Based on company and individual performance"],["USD 400 / month","Housing allowance after probation for eligible off-site employees"],["USD 1,500 / year","Annual medical reimbursement"]],sections:[["Growth paths","Expert and leadership tracks supported by real projects, mentorship and clear standards."],["Performance rewards","Quarterly bonuses and annual compensation depend on company results and individual performance."],["Everyday support","Housing, meals, transport, healthcare and leave policies support sustainable work and life."]]},
  contact:{code:"CONTACT / 09",title:"Send your coordinates. Let’s find the next mission.",lead:"Choose a role, complete your profile and contact the XW recruiting team on Telegram.",stats:[["Telegram","@XWcompany123"],["Locations","Global / Remote"],["Response time","Usually 3–5 business days"]],sections:[["Application advice","Show the problem, your actions, the outcome and your individual contribution."],["Portfolio","Product, design, AI video and game design roles should include relevant work samples."],["Recruitment notice","XW never charges recruitment fees. Please verify that all communication comes from our official Telegram account."]]}
};
const content = computed(() => (language.value === "zh" ? pagesZh : pagesEn)[props.page]);
const pageImage = computed(() => props.page === "life" ? "/images/xw-asia-team-life-v2.png" : props.page === "about" ? "/images/xw-asia-collaboration-v2.png" : props.page === "business" ? "/images/xw-asia-global-team-v2.png" : "");
const benefitsZh = [
  ["年度计薪月数","依据公司效益及个人绩效，全年共发放 13–14 个月薪资"],
  ["季度奖金","依据公司效益及个人绩效发放季度奖金"],
  ["住房补贴","400 USD / 月，适用于转试用之后的外宿员工"],
  ["餐补","15 USD / 天"],
  ["交通补贴 / 报销","每年享受 450 USD 年假津贴；海外招聘入职者享受一定金额交通费用报销"],
  ["医疗报销","1,500 USD / 年"],
  ["佳节礼金与礼品","元旦、春节、劳动节、端午节、中秋节、国庆节及生日礼金"],
  ["个人福利礼金","结婚礼金、生育礼金及丧假礼金"],
  ["轮班津贴","中班 3 USD / 天，夜班 5 USD / 天"],
  ["年假","入职满一年享有 11 天年假，年假从试用期起计算"],
  ["其他假期","婚假、丧假 3 天；产假 2 个月；陪产假 7 天"],
  ["工作时间","每日工作 9 小时，具体班次由部门安排"]
];
const benefitsEn = [
  ["Annual compensation","13–14 months of salary per year, based on company results and individual performance"],
  ["Quarterly bonus","Paid according to company results and individual performance"],
  ["Housing allowance","USD 400 per month for eligible off-site employees after probation"],
  ["Meal allowance","USD 15 per day"],
  ["Transport and annual-leave allowance","USD 450 annual-leave allowance per year; eligible overseas hires receive transportation reimbursement"],
  ["Medical reimbursement","USD 1,500 per year"],
  ["Holiday gifts","Gifts or cash benefits for New Year, Spring Festival, Labour Day, Dragon Boat Festival, Mid-Autumn Festival, National Day and birthdays"],
  ["Personal milestone benefits","Marriage, childbirth and bereavement benefits"],
  ["Shift allowance","USD 3 per afternoon shift and USD 5 per night shift"],
  ["Annual leave","11 days after one year of service, calculated from the start of probation"],
  ["Other leave","3 days marriage or bereavement leave, 2 months maternity leave and 7 days paternity leave"],
  ["Working hours","9 hours per day; shift schedules are arranged by department"]
];
const benefits = computed(() => language.value === "zh" ? benefitsZh : benefitsEn);
</script>

<template>
  <main class="inner-page section content-page">
    <header class="content-intro">
      <div class="content-intro-copy">
        <div class="page-kicker">{{ content.code }}</div>
        <h1>{{ content.title }}</h1>
        <p>{{ content.lead }}</p>
      </div>
      <div class="content-intro-stats">
        <div v-for="(item,index) in content.stats" :key="item[0]">
          <small>{{ String(Number(index)+1).padStart(2,'0') }}</small>
          <b>{{ item[0] }}</b>
          <span>{{ item[1] }}</span>
        </div>
      </div>
    </header>
    <img v-if="pageImage" class="content-hero-image" :src="pageImage" :alt="content.title" />
    <section v-if="page==='growth'" class="benefits-section">
      <div class="benefits-title"><span>EMPLOYEE BENEFITS / 12</span><h2>{{ language==='zh' ? '完整福利清单' : 'Complete benefits package' }}</h2></div>
      <div class="benefits-grid"><article v-for="(item,index) in benefits" :key="item[0]"><small>{{ String(index+1).padStart(2,'0') }}</small><h3>{{ item[0] }}</h3><p>{{ item[1] }}</p></article></div>
      <p class="benefits-note">{{ language==='zh' ? '具体福利的适用条件、发放时间及金额，以公司当期制度和劳动合同为准。' : 'Eligibility, timing and amounts are subject to current company policy and employment agreements.' }}</p>
    </section>
    <div class="content-sections"><article v-for="(item,index) in content.sections" :key="item[0]"><small>0{{ Number(index)+1 }}</small><h2>{{ item[0] }}</h2><p>{{ item[1] }}</p></article></div>
    <RouterLink v-if="page !== 'contact'" class="primary-btn content-cta" to="/jobs">{{ language==='zh' ? '查看开放岗位' : 'View open roles' }}　→</RouterLink>
    <a v-else class="primary-btn content-cta" href="https://t.me/XWcompany123" target="_blank" rel="noopener">{{ language==='zh' ? '打开 Telegram' : 'Open Telegram' }}　→</a>
  </main>
</template>
