import type { Job } from "./jobs";

const titles: Record<string,string> = {
  "高级招聘专员":"Senior Talent Acquisition Specialist","运营专员":"Operations Specialist","项目经理":"Project Manager — TY Multi-platform",
  "品牌主管":"Brand Lead","商务主管":"Business Development Lead","HRBP 专员 / 组长":"HR Business Partner / Team Lead",
  "游戏策划":"Game Designer","数值策划":"Game Economy Designer","高级商务招商专员":"Senior Partnerships Specialist",
  "技术客服专员 / 技术支持专员":"Technical Support Specialist","技术支持组长 / 主管 / 经理":"Technical Support Lead / Manager",
  "测试工程师":"QA Engineer","Java 主管":"Java Engineering Lead","AI 视频编辑":"AI Video Editor","测试主管":"QA Lead",
  "前端主管":"Frontend Engineering Lead","自动化测试工程师":"Automation QA Engineer","前端架构师":"Frontend Architect",
  "Java 架构师":"Java Architect","商务拓展总监":"Business Development Director","AI 应用开发岗":"AI Application Engineer",
  "后台产品经理":"Backend Product Manager","Golang 开发工程师":"Golang Engineer","AIGC 视频设计师":"AIGC Video Designer",
  "软件研发项目经理":"Software R&D Project Manager","产品经理 / 项目经理":"Product / Project Manager",
  "高级 Web 前端工程师":"Senior Web Frontend Engineer","前端工程师":"Frontend Engineer","Java 技术主管 / 研发主管":"Java Engineering Manager",
  "高级架构师（Java / 分布式系统）":"Principal Architect — Java / Distributed Systems",
  "Java 架构师 / 资深 Java 开发工程师":"Java Architect / Staff Java Engineer",
  "游戏产品副总监 / B端运营副总监":"Associate Director — Game Product / B2B Operations",
  "游戏设计副总监":"Associate Game Design Director"
};

export const jobTitle = (job: Job, language: "zh"|"en") => language === "zh" ? job.title : titles[job.title] || job.title;
export const jobCategory = (job: Job, language: "zh"|"en") => language === "zh" ? job.category : job.category === "技术岗位" ? "Engineering & Product" : "Business Functions";
export const jobLocation = (job: Job, language: "zh"|"en") => language === "zh" ? job.location : job.location.includes("远程") ? "Global Remote" : "Thailand · Work from home";
export const jobSummary = (job: Job, language: "zh"|"en") => language === "zh" ? job.summary : `Own a critical ${job.category === "技术岗位" ? "technology and product" : "business"} domain, connect strategy with high-quality execution, and build systems that scale with XW's global team.`;
export const englishDuties = (job: Job) => [
  `Own the goals, planning, execution, review and continuous improvement of the ${jobTitle(job,"en")} domain.`,
  "Develop a deep understanding of users and business context, turning complex problems into clear and measurable plans.",
  "Partner across product, engineering, operations and business teams to manage risks and deliver key milestones.",
  "Communicate progress, decisions, dependencies and risks clearly so stakeholders can act with confidence.",
  job.category === "技术岗位" ? "Balance delivery speed with performance, security, reliability and long-term maintainability." : "Use business data, user feedback and market signals to evaluate outcomes and refine priorities.",
  "Identify delivery, quality and collaboration risks early, create mitigation plans and follow issues through closure.",
  "Build reusable processes, documentation, standards and metrics that improve team effectiveness.",
  /Lead|Manager|Director|Architect/.test(jobTitle(job,"en")) ? "Set team direction, coach colleagues and build a culture of ownership, candid feedback and continuous improvement." : "Take ownership beyond narrow task boundaries when the wider outcome requires it."
];
export const englishRequirements = (job: Job) => [
  "Bachelor’s degree or equivalent practical experience, with a strong record in a relevant role.",
  job.category === "技术岗位" ? "Strong engineering fundamentals and hands-on experience delivering complex production systems." : "Strong structured thinking, stakeholder communication and cross-functional execution skills.",
  "Ability to independently analyze problems, propose practical solutions, drive execution and remain accountable for outcomes.",
  "Clear written communication and documentation skills, including the ability to present goals, trade-offs, progress and conclusions.",
  "Strong ownership, judgment and self-direction when working through ambiguity or rapidly changing priorities.",
  "Comfortable working in a fast-changing, international technology environment.",
  "Professional working proficiency in English; Chinese, Vietnamese, Hindi or other additional languages are welcome.",
  /Lead|Manager|Director|Architect/.test(jobTitle(job,"en")) ? "Demonstrated experience leading projects or teams, setting goals, giving feedback and developing talent." : "A habit of continuous learning and the ability to turn new tools or methods into practical improvements."
];
export const englishBonus = [
  "Experience in gaming, digital platforms, AI, global products or high-growth technology companies.",
  "A successful zero-to-one launch or meaningful cross-team leadership experience.",
  "Experience collaborating across countries, departments or distributed teams.",
  "A practical habit of using AI tools to improve quality and productivity.",
  "A portfolio, open-source contribution, professional community work or measurable organizational impact."
];
