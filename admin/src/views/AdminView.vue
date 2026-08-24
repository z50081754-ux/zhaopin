<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import XwLogo from "../components/XwLogo.vue";

type Application = {
  id:number; application_no:string; resume_name:string; telegram:string; gender:string; age:string;
  birth_date:string; nationality_country:string; job_title:string; current_salary:string;
  referrer:string; remarks:string;
  expected_salary:string; bc_experience:string; employment_status:string; education_type:string;
  school:string; education_period:string; passport_status:string; visa_status:string;
  interview_time:string; start_time:string; current_country:string; preferred_country:string;
  stage:string; is_possible_duplicate:number; original_filename?:string; resume_size:number;
  device_type:string; device_model:string; operating_system:string; browser_name:string;
  screen_resolution:string; device_language:string; device_timezone:string; user_agent:string;
  created_at:string;
};
type ApplicationDetail = {
  summary: Application; gender:string; age:string; birth_date:string; current_salary:string;
  bc_experience:string; employment_status:string; education_type:string; school:string;
  education_period:string; passport_status:string; visa_status:string; interview_time:string;
  start_time:string; device_type:string; device_model:string; operating_system:string;
  browser_name:string; screen_resolution:string; device_language:string;
  device_timezone:string; user_agent:string;
};
type AdminJob = {
  id:number; slug:string; title:string; category:string; businessUnit:string; requiredLocation:string;
  workMode:string; salaryRange:string; internationalSalaryRange:string; summary:string; responsibilities:string[];
  requirements:string[]; bonus:string[]; status:string; recruitmentCount:number; updatedAt:string;
};
type WebsiteVisit = {
  id:number; visit_id:string; started_at:string; qualified_at:string; last_seen_at:string;
  duration_seconds:number; ip_address:string; entry_path:string; last_path:string;
  device_type:string; device_model:string; operating_system:string; browser_name:string;
  screen_resolution:string; device_language:string; device_timezone:string; user_agent:string;
};

const API_BASE=(import.meta.env.VITE_API_BASE_URL||"").replace(/\/$/,"");
const PUBLIC_SITE_URL=import.meta.env.VITE_PUBLIC_SITE_URL||"/";
const apiUrl=(path:string)=>`${API_BASE}${path}`;
const account=ref(""), password=ref(""), authenticated=ref(false), loading=ref(false), error=ref("");
type SiteTemplate="technology"|"apple";
const activeModule=ref<"applications"|"visits"|"jobs"|"templates">("applications"), query=ref(""), stage=ref("");
const referrerQuery=ref(""), createdFrom=ref(""), createdTo=ref(""), operatingSystemQuery=ref(""), deviceModelQuery=ref("");
const applications=ref<Application[]>([]), selectedApplication=ref<ApplicationDetail|null>(null);
const currentPage=ref(0), totalPages=ref(0), totalApplications=ref(0), pageSize=20;
const visits=ref<WebsiteVisit[]>([]), visitPage=ref(0), totalVisitPages=ref(0), totalVisits=ref(0), visitPageSize=20;
const visitMinDuration=ref<number|"">("");
const jobs=ref<AdminJob[]>([]), jobEditorOpen=ref(false), editingJobId=ref<number|null>(null);
const jobStatusFilter=ref<"all"|"online"|"offline">("all");
const activeTemplate=ref<SiteTemplate>("technology"), templateSaving=ref(false), templateMessage=ref("");
const emptyJob=()=>({title:"",category:"职能岗位",requiredLocation:"泰国",workMode:"居家",salaryRange:"",responsibilities:"",requirements:"",bonus:"",status:"draft",recruitmentCount:1});
const jobForm=reactive(emptyJob());
const stageLabels:Record<string,string>={new:"新投递",screening:"筛选中",interview:"面试中",offer:"Offer",hired:"已录用",rejected:"不合适"};
const jobStatusLabels:Record<string,string>={draft:"未上线",open:"已上线",paused:"已下线",closed:"已下线"};
const filteredCount=computed(()=>totalApplications.value);
const totalJobCount=computed(()=>jobs.value.length);
const filteredJobs=computed(()=>jobs.value.filter(job=>jobStatusFilter.value==="all"||(jobStatusFilter.value==="online"?job.status==="open":job.status!=="open")));
const onlineJobCount=computed(()=>jobs.value.filter(job=>job.status==="open").length);
const offlineJobCount=computed(()=>jobs.value.length-onlineJobCount.value);
const display=(value:unknown)=>value===null||value===undefined||value===""?"—":String(value);
const fileSize=(bytes:number)=>bytes?`${(bytes/1024/1024).toFixed(2)} MB`:"—";
const visitDuration=(seconds:number)=>seconds<60?`${seconds} 秒`:`${Math.floor(seconds/60)} 分 ${seconds%60} 秒`;
const salaryInput=(value:string)=>value?.replace(/\s*USDT\s*\/月\s*$/i,"")||"";
const CNY_PER_USDT=7;
const parsedSalaryRange=computed(()=>jobForm.salaryRange.replace(/,/g,"").trim().match(/^([0-9]+(?:\.[0-9]+)?)([kK]?)\s*[-–—~至]\s*([0-9]+(?:\.[0-9]+)?)([kK]?)$/));
const salaryPreview=computed(()=>{
  const match=parsedSalaryRange.value;
  if(!match)return "";
  const amount=(value:string,suffix:string)=>`${Number(value)*1.5}${suffix.toUpperCase()}`;
  return `${amount(match[1],match[2])}–${amount(match[3],match[4])} USDT/月`;
});
const rmbPreview=computed(()=>{
  const match=parsedSalaryRange.value;
  if(!match)return "";
  const amount=(value:string,suffix:string)=>Number(value)*(suffix?1000:1)*CNY_PER_USDT;
  const format=(value:number)=>new Intl.NumberFormat("zh-CN",{maximumFractionDigits:0}).format(value);
  return `¥${format(amount(match[1],match[2]))}–¥${format(amount(match[3],match[4]))}/月`;
});

async function api<T>(url:string,options:RequestInit={}):Promise<T>{
  const response=await fetch(apiUrl(url),{credentials:"include",...options});
  if(response.status===401){authenticated.value=false;throw new Error("UNAUTHORIZED")}
  const result=await response.json();
  if(!response.ok) throw new Error(result?.message||result?.code||"REQUEST_FAILED");
  return result as T;
}
async function loadApplications(){
  loading.value=true; error.value="";
  try{
    const params=new URLSearchParams({q:query.value,stage:stage.value,referrer:referrerQuery.value,
      createdFrom:createdFrom.value,createdTo:createdTo.value,operatingSystem:operatingSystemQuery.value,
      deviceModel:deviceModelQuery.value,page:String(currentPage.value),size:String(pageSize)});
    const result=await api<{ok:boolean;applications:Application[];total:number;pages:number}>(`/api/admin/applications?${params}`);
    applications.value=result.applications||[];totalApplications.value=result.total||0;totalPages.value=result.pages||0;authenticated.value=true;
  }catch(e){if((e as Error).message!=="UNAUTHORIZED")error.value="数据加载失败，请稍后重试。"}
  finally{loading.value=false}
}
function searchApplications(){currentPage.value=0;void loadApplications()}
function goToPage(page:number){
  if(page<0||page>=totalPages.value||page===currentPage.value)return;
  currentPage.value=page;void loadApplications();window.scrollTo({top:0,behavior:"smooth"});
}
async function loadJobs(){
  loading.value=true; error.value="";
  try{jobs.value=await api<AdminJob[]>("/api/admin/jobs");authenticated.value=true}
  catch(e){if((e as Error).message!=="UNAUTHORIZED")error.value="岗位数据加载失败。"}
  finally{loading.value=false}
}
async function loadVisits(){
  loading.value=true;error.value="";
  try{
    const minDurationSeconds=Math.max(0,Math.floor(Number(visitMinDuration.value)||0));
    const params=new URLSearchParams({page:String(visitPage.value),size:String(visitPageSize),minDurationSeconds:String(minDurationSeconds)});
    const result=await api<{visits:WebsiteVisit[];total:number;pages:number}>(`/api/admin/visits?${params}`);
    visits.value=result.visits||[];totalVisits.value=result.total||0;totalVisitPages.value=result.pages||0;authenticated.value=true;
  }catch(e){if((e as Error).message!=="UNAUTHORIZED")error.value="有效浏览数据加载失败。"}
  finally{loading.value=false}
}
function searchVisits(){visitPage.value=0;void loadVisits()}
function goToVisitPage(page:number){
  if(page<0||page>=totalVisitPages.value||page===visitPage.value)return;
  visitPage.value=page;void loadVisits();window.scrollTo({top:0,behavior:"smooth"});
}
async function loadTemplate(){
  try{const result=await api<{activeTemplate:SiteTemplate}>("/api/admin/site-settings");activeTemplate.value=result.activeTemplate;authenticated.value=true}
  catch(e){if((e as Error).message!=="UNAUTHORIZED")error.value="官网模板读取失败。"}
}
async function login(){
  loading.value=true;error.value="";
  try{
    await api("/api/admin/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({account:account.value,password:password.value})});
    password.value="";authenticated.value=true;await Promise.all([loadApplications(),loadJobs(),loadTemplate()]);
  }catch{error.value="账号或密码错误，或后端服务尚未启动。"}finally{loading.value=false}
}
async function openApplication(item:Application){
  try{selectedApplication.value=await api<ApplicationDetail>(`/api/admin/applications/${item.id}`)}
  catch{error.value="无法读取候选人详情。"}
}
async function changeStage(value:string){
  if(!selectedApplication.value)return;
  try{
    await api(`/api/admin/applications/${selectedApplication.value.summary.id}/stage`,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({stage:value})});
    selectedApplication.value.summary.stage=value;await loadApplications();
  }catch{error.value="阶段更新失败。"}
}
function newJob(){error.value="";editingJobId.value=null;Object.assign(jobForm,emptyJob());jobEditorOpen.value=true}
function editJob(job:AdminJob){
  error.value="";
  editingJobId.value=job.id;
  Object.assign(jobForm,{title:job.title,category:job.category,requiredLocation:job.requiredLocation,workMode:job.workMode,salaryRange:salaryInput(job.salaryRange),responsibilities:job.responsibilities.join("\n"),requirements:job.requirements.join("\n"),bonus:job.bonus.join("\n"),status:job.status,recruitmentCount:job.recruitmentCount||1});
  jobEditorOpen.value=true;
}
const lines=(value:string)=>value.split("\n").map(item=>item.trim()).filter(Boolean);
async function saveJob(){
  error.value="";
  const responsibilities=lines(jobForm.responsibilities), requirements=lines(jobForm.requirements);
  if(!jobForm.title.trim()||!jobForm.requiredLocation.trim()){
    error.value="请填写岗位名称和要求工作地后再保存。";
    return;
  }
  loading.value=true;
  const payload={...jobForm,responsibilities,requirements,bonus:lines(jobForm.bonus)};
  try{
    await api(editingJobId.value?`/api/admin/jobs/${editingJobId.value}`:"/api/admin/jobs",{method:editingJobId.value?"PUT":"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
    jobEditorOpen.value=false;await loadJobs();
  }catch(e){error.value=`岗位保存失败：${(e as Error).message}`}finally{loading.value=false}
}
async function deleteJob(){
  if(!editingJobId.value)return;
  if(!window.confirm(`确定删除岗位“${jobForm.title}”吗？删除后前台将不再显示，且无法恢复。`))return;
  loading.value=true;error.value="";
  try{
    await api(`/api/admin/jobs/${editingJobId.value}`,{method:"DELETE"});
    jobEditorOpen.value=false;editingJobId.value=null;await loadJobs();
  }catch(e){error.value=`岗位删除失败：${(e as Error).message}`}finally{loading.value=false}
}
async function toggleJobStatus(job:AdminJob){
  loading.value=true;error.value="";
  const status=job.status==="open"?"paused":"open";
  const currentFilter=jobStatusFilter.value;
  const payload={...job,status,salaryRange:salaryInput(job.salaryRange),internationalSalaryRange:undefined};
  try{
    await api(`/api/admin/jobs/${job.id}`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
    activeModule.value="jobs";
    jobStatusFilter.value=currentFilter;
    await loadJobs();
  }
  catch(e){error.value=`岗位${status==="open"?"上线":"下线"}失败：${(e as Error).message}`}finally{loading.value=false}
}
async function deleteApplication(item:Application){
  if(!window.confirm(`确定删除候选人“${display(item.resume_name)}”的投递记录吗？简历文件也会一并删除，且无法恢复。`))return;
  loading.value=true;error.value="";
  try{
    await api(`/api/admin/applications/${item.id}`,{method:"DELETE"});
    applications.value=applications.value.filter(application=>application.id!==item.id);
    totalApplications.value=Math.max(0,totalApplications.value-1);
    totalPages.value=Math.ceil(totalApplications.value/pageSize);
    if(selectedApplication.value?.summary.id===item.id)selectedApplication.value=null;
    if(!applications.value.length&&currentPage.value>0)currentPage.value--;
    await loadApplications();
  }
  catch(e){error.value=`候选人删除失败：${(e as Error).message}`}finally{loading.value=false}
}
async function selectTemplate(template:SiteTemplate){
  if(template===activeTemplate.value)return;
  templateSaving.value=true;templateMessage.value="";error.value="";
  try{
    const result=await api<{activeTemplate:SiteTemplate}>("/api/admin/site-settings",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({activeTemplate:template})});
    activeTemplate.value=result.activeTemplate;templateMessage.value="模板已启用，刷新招聘官网即可看到新风格。";
  }catch(e){error.value=`模板切换失败：${(e as Error).message}`}finally{templateSaving.value=false}
}
function switchModule(module:"applications"|"visits"|"jobs"|"templates"){
  activeModule.value=module;
  if(module==="visits")void loadVisits();else if(module==="jobs")void loadJobs();else if(module==="templates")void loadTemplate();else void loadApplications();
}
onMounted(loadApplications);
</script>

<template>
  <main class="admin-shell">
    <section v-if="!authenticated" class="admin-login">
      <XwLogo/><small>RECRUITMENT CONTROL CENTER</small><h1>招聘管理后台</h1><p>仅限 XW 授权招聘人员访问。</p>
      <form @submit.prevent="login">
        <label><span>管理员账号</span><input v-model="account" required autocomplete="username"/></label>
        <label><span>密码</span><input v-model="password" required type="password" autocomplete="current-password"/></label>
        <p v-if="error" class="admin-error">{{error}}</p>
        <button class="primary-btn" :disabled="loading" type="submit">{{loading?"验证中…":"安全登录"}}</button>
      </form><a :href="PUBLIC_SITE_URL">← 返回招聘官网</a>
    </section>

    <template v-else>
      <header class="admin-header"><div><XwLogo/><span>招聘管理后台</span></div><a :href="PUBLIC_SITE_URL" target="_blank" rel="noopener">查看招聘官网 ↗</a></header>
      <section class="admin-main">
        <nav class="admin-modules">
          <button :class="{active:activeModule==='applications'}" @click="switchModule('applications')">候选人管理</button>
          <button :class="{active:activeModule==='visits'}" @click="switchModule('visits')">有效浏览</button>
          <button :class="{active:activeModule==='jobs'}" @click="switchModule('jobs')">岗位管理</button>
          <button :class="{active:activeModule==='templates'}" @click="switchModule('templates')">官网模板</button>
        </nav>
        <template v-if="activeModule==='applications'">
          <div class="admin-title"><div><small>APPLICATION PIPELINE</small><h1>候选人投递</h1></div><b>{{String(filteredCount).padStart(2,"0")}}</b></div>
          <div class="admin-toolbar">
            <label class="search-main"><span>⌕</span><input v-model="query" placeholder="姓名、岗位、Telegram 或申请编号" @keyup.enter="searchApplications"/></label>
            <select v-model="stage" @change="searchApplications"><option value="">全部阶段</option><option v-for="(label,key) in stageLabels" :key="key" :value="key">{{label}}</option></select>
            <label><span>推荐人</span><input v-model="referrerQuery" placeholder="推荐人" @keyup.enter="searchApplications"/></label>
            <label><span>开始</span><input v-model="createdFrom" type="date"/></label>
            <label><span>结束</span><input v-model="createdTo" type="date"/></label>
            <label><span>系统</span><input v-model="operatingSystemQuery" placeholder="如 iOS、Android" @keyup.enter="searchApplications"/></label>
            <label><span>机型</span><input v-model="deviceModelQuery" placeholder="如 iPhone" @keyup.enter="searchApplications"/></label>
            <button type="button" @click="searchApplications">查询</button>
          </div>
          <p v-if="error" class="admin-error">{{error}}</p>
          <p class="admin-scroll-tip">← 左右滑动查看全部候选人信息，点击任意一行打开详情 →</p>
          <div class="admin-table admin-wide-table">
            <div class="admin-wide-row admin-row-head">
              <span>操作</span><span>提交时间</span><span>推荐人</span><span>备注</span><span>系统版本</span>
              <span>申请编号</span><span>简历名</span><span>Telegram</span><span>性别</span><span>年龄</span>
              <span>出生年月日</span><span>国籍</span><span>求职岗位</span><span>目前薪资</span><span>期望薪资</span>
              <span>BC 经验</span><span>就业状态</span><span>第一学历</span><span>学校全名</span><span>就读时间</span>
              <span>护照</span><span>签证</span><span>可面试时间</span><span>可到职时间</span><span>目前所在地</span>
              <span>期望工作地</span><span>招聘阶段</span><span>简历文件</span><span>文件大小</span><span>设备类型</span>
              <span>设备机型</span><span>浏览器</span><span>屏幕</span><span>语言</span>
              <span>时区</span><span>User Agent</span>
            </div>
            <article v-for="item in applications" :key="item.id" class="admin-wide-row clickable" @click="openApplication(item)">
              <span><button class="candidate-delete" type="button" @click.stop="deleteApplication(item)">删除</button></span>
              <span>{{new Date(item.created_at).toLocaleString("zh-CN")}}</span>
              <span :title="item.referrer"><b>{{display(item.referrer)}}</b></span>
              <span :title="item.remarks">{{display(item.remarks)}}</span>
              <span>{{display(item.operating_system)}}</span>
              <span :title="item.application_no">{{display(item.application_no)}}</span>
              <span :title="item.resume_name"><b>{{display(item.resume_name)}}</b></span>
              <span :title="item.telegram">{{display(item.telegram)}}</span>
              <span>{{display(item.gender)}}</span><span>{{display(item.age)}}</span><span>{{display(item.birth_date)}}</span>
              <span>{{display(item.nationality_country)}}</span><span :title="item.job_title"><b>{{display(item.job_title)}}</b></span>
              <span>{{display(item.current_salary)}}</span><span>{{display(item.expected_salary)}}</span>
              <span>{{display(item.bc_experience)}}</span><span>{{display(item.employment_status)}}</span>
              <span>{{display(item.education_type)}}</span><span :title="item.school">{{display(item.school)}}</span>
              <span>{{display(item.education_period)}}</span><span>{{display(item.passport_status)}}</span>
              <span>{{display(item.visa_status)}}</span><span>{{display(item.interview_time)}}</span>
              <span>{{display(item.start_time)}}</span><span>{{display(item.current_country)}}</span>
              <span>{{display(item.preferred_country)}}</span>
              <span><em>{{stageLabels[item.stage]||item.stage}}</em><small v-if="item.is_possible_duplicate">可能重复</small></span>
              <span :title="item.original_filename">{{display(item.original_filename)}}</span><span>{{fileSize(item.resume_size)}}</span>
              <span>{{display(item.device_type)}}</span><span>{{display(item.device_model)}}</span>
              <span>{{display(item.browser_name)}}</span>
              <span>{{display(item.screen_resolution)}}</span><span>{{display(item.device_language)}}</span>
              <span>{{display(item.device_timezone)}}</span><span :title="item.user_agent">{{display(item.user_agent)}}</span>
            </article>
            <div v-if="!loading&&!applications.length" class="admin-empty">暂无符合条件的候选人投递</div>
          </div>
          <div v-if="totalApplications" class="admin-pagination">
            <span>共 <b>{{totalApplications}}</b> 条 · 每页 {{pageSize}} 条</span>
            <div>
              <button :disabled="currentPage===0||loading" @click="goToPage(currentPage-1)">← 上一页</button>
              <button v-for="page in totalPages" :key="page" :class="{active:page-1===currentPage}" :disabled="loading" @click="goToPage(page-1)">{{page}}</button>
              <button :disabled="currentPage>=totalPages-1||loading" @click="goToPage(currentPage+1)">下一页 →</button>
            </div>
          </div>
        </template>

        <template v-else-if="activeModule==='visits'">
          <div class="admin-title"><div><small>QUALIFIED WEBSITE VISITS</small><h1>有效浏览</h1></div><b>{{String(totalVisits).padStart(2,"0")}}</b></div>
          <p class="visit-description">访客在页面可见状态下停留满 10 秒后计入，停留期间持续更新有效时长。</p>
          <div class="admin-toolbar">
            <label><span>有效时长 ≥</span><input v-model.number="visitMinDuration" type="number" min="0" step="1" placeholder="秒" @keyup.enter="searchVisits"/></label>
            <button type="button" @click="searchVisits">查询</button>
          </div>
          <p v-if="error" class="admin-error">{{error}}</p>
          <p class="admin-scroll-tip">← 左右滑动查看完整访问与设备信息 →</p>
          <div class="admin-table admin-wide-table visit-table">
            <div class="admin-wide-row admin-row-head visit-row">
              <span>达标时间</span><span>有效停留</span><span>网络 IP</span><span>系统版本</span>
              <span>设备机型</span><span>设备类型</span><span>浏览器</span><span>进入页面</span>
              <span>最后页面</span><span>最后活跃</span><span>屏幕</span><span>语言</span><span>时区</span><span>User Agent</span>
            </div>
            <article v-for="visit in visits" :key="visit.id" class="admin-wide-row visit-row">
              <span>{{new Date(visit.qualified_at).toLocaleString("zh-CN")}}</span><span><b>{{visitDuration(visit.duration_seconds)}}</b></span>
              <span>{{display(visit.ip_address)}}</span><span>{{display(visit.operating_system)}}</span>
              <span>{{display(visit.device_model)}}</span><span>{{display(visit.device_type)}}</span>
              <span>{{display(visit.browser_name)}}</span><span :title="visit.entry_path">{{display(visit.entry_path)}}</span>
              <span :title="visit.last_path">{{display(visit.last_path)}}</span><span>{{new Date(visit.last_seen_at).toLocaleString("zh-CN")}}</span>
              <span>{{display(visit.screen_resolution)}}</span><span>{{display(visit.device_language)}}</span>
              <span>{{display(visit.device_timezone)}}</span><span :title="visit.user_agent">{{display(visit.user_agent)}}</span>
            </article>
            <div v-if="!loading&&!visits.length" class="admin-empty">暂无有效浏览记录</div>
          </div>
          <div v-if="totalVisits" class="admin-pagination">
            <span>共 <b>{{totalVisits}}</b> 条 · 每页 {{visitPageSize}} 条</span>
            <div><button :disabled="visitPage===0||loading" @click="goToVisitPage(visitPage-1)">← 上一页</button><button v-for="page in totalVisitPages" :key="page" :class="{active:page-1===visitPage}" :disabled="loading" @click="goToVisitPage(page-1)">{{page}}</button><button :disabled="visitPage>=totalVisitPages-1||loading" @click="goToVisitPage(visitPage+1)">下一页 →</button></div>
          </div>
        </template>

        <template v-else-if="activeModule==='jobs'">
          <div class="admin-title"><div><small>JOB MANAGEMENT</small><h1>招聘岗位</h1></div><div class="admin-job-summary"><span>总岗位数 <b>{{totalJobCount}}</b></span><button class="primary-btn admin-create" @click="newJob">＋ 新建岗位</button></div></div>
          <p v-if="error" class="admin-error">{{error}}</p>
          <div class="job-status-filters">
            <button :class="{active:jobStatusFilter==='all'}" @click="jobStatusFilter='all'">全部 <b>{{totalJobCount}}</b></button>
            <button :class="{active:jobStatusFilter==='online'}" @click="jobStatusFilter='online'">已上线 <b>{{onlineJobCount}}</b></button>
            <button :class="{active:jobStatusFilter==='offline'}" @click="jobStatusFilter='offline'">已下线 <b>{{offlineJobCount}}</b></button>
          </div>
          <div class="admin-job-grid">
            <article v-for="job in filteredJobs" :key="job.id" @click="editJob(job)">
              <div class="job-card-status"><button class="job-online-toggle" :class="{online:job.status==='open'}" type="button" @click.stop="toggleJobStatus(job)">{{job.status==='open'?'下线':'上线'}}</button><em>{{jobStatusLabels[job.status]}}</em><small>{{job.category}}</small></div>
              <h2>{{job.title}}</h2>
              <p>{{job.requiredLocation}} · {{job.workMode}} · 招聘 {{job.recruitmentCount||1}} 人<br>东南亚：{{job.salaryRange||"面议"}}<br>国际：{{job.internationalSalaryRange||"面议"}}</p>
              <span>编辑岗位 →</span>
            </article>
            <button v-if="!filteredJobs.length&&!loading" class="admin-job-empty" @click="jobStatusFilter==='all'&&newJob()">{{jobs.length?'当前筛选下暂无岗位':'还没有后台岗位，创建第一个岗位'}}</button>
          </div>
        </template>
        <template v-else>
          <div class="admin-title"><div><small>WEBSITE APPEARANCE</small><h1>官网模板</h1></div><a class="template-preview-link" :href="PUBLIC_SITE_URL" target="_blank" rel="noopener">打开官网预览 ↗</a></div>
          <p class="template-intro">选择招聘官网首页的视觉风格。启用后立即保存到数据库，所有访客刷新首页后都会看到新的模板。</p>
          <p v-if="error" class="admin-error">{{error}}</p><p v-if="templateMessage" class="template-success">{{templateMessage}}</p>
          <div class="template-grid">
            <article :class="{selected:activeTemplate==='technology'}">
              <div class="template-shot template-shot-tech"><i></i><span>XW CAREERS</span><b>加入 XW，让技术<br>与热爱彼此成就</b><small>GLOBAL · REMOTE · TECHNOLOGY</small></div>
              <div class="template-meta"><div><small>TECHNOLOGY / DARK</small><h2>科技深色版</h2><p>延续现有科技感视觉，适合招聘门户与岗位浏览。</p></div><button :disabled="templateSaving||activeTemplate==='technology'" @click="selectTemplate('technology')">{{activeTemplate==='technology'?'当前使用':'启用模板'}}</button></div>
            </article>
            <article :class="{selected:activeTemplate==='apple'}">
              <div class="template-shot template-shot-apple"><span>XW CAREERS</span><b>让出色的工作，<br>成为日常。</b><small>简约、留白、清晰与高级感</small></div>
              <div class="template-meta"><div><small>APPLE / MINIMAL</small><h2>Apple 简约大气版</h2><p>大面积留白、精炼导航与圆角卡片，适合品牌推广。</p></div><button :disabled="templateSaving||activeTemplate==='apple'" @click="selectTemplate('apple')">{{activeTemplate==='apple'?'当前使用':'启用模板'}}</button></div>
            </article>
          </div>
        </template>
      </section>
    </template>

    <div v-if="selectedApplication" class="admin-drawer-backdrop" @click.self="selectedApplication=null">
      <aside class="admin-drawer">
        <button class="modal-close" @click="selectedApplication=null">×</button>
        <small>{{selectedApplication.summary.application_no}}</small><h2>{{selectedApplication.summary.resume_name}}</h2>
        <div class="drawer-stage"><label>招聘阶段</label><select :value="selectedApplication.summary.stage" @change="changeStage(($event.target as HTMLSelectElement).value)"><option v-for="(label,key) in stageLabels" :key="key" :value="key">{{label}}</option></select></div>
        <section><h3>求职信息</h3><dl>
          <div><dt>岗位</dt><dd>{{display(selectedApplication.summary.job_title)}}</dd></div><div><dt>Telegram</dt><dd>{{display(selectedApplication.summary.telegram)}}</dd></div>
          <div><dt>推荐人</dt><dd>{{display(selectedApplication.summary.referrer)}}</dd></div>
          <div><dt>备注</dt><dd>{{display(selectedApplication.summary.remarks)}}</dd></div>
          <div><dt>目前 / 期望薪资</dt><dd>{{selectedApplication.current_salary}} / {{selectedApplication.summary.expected_salary}}</dd></div>
          <div><dt>所在地 / 意向地</dt><dd>{{selectedApplication.summary.current_country}} / {{selectedApplication.summary.preferred_country}}</dd></div>
          <div><dt>面试 / 到职</dt><dd>{{selectedApplication.interview_time}} / {{selectedApplication.start_time}}</dd></div>
        </dl></section>
        <section><h3>设备信息</h3><dl>
          <div><dt>设备</dt><dd>{{selectedApplication.device_type}} · {{selectedApplication.device_model}}</dd></div>
          <div><dt>系统</dt><dd>{{selectedApplication.operating_system}}</dd></div><div><dt>浏览器</dt><dd>{{selectedApplication.browser_name}}</dd></div>
          <div><dt>屏幕</dt><dd>{{selectedApplication.screen_resolution}}</dd></div><div><dt>语言 / 时区</dt><dd>{{selectedApplication.device_language}} / {{selectedApplication.device_timezone}}</dd></div>
        </dl></section>
        <a v-if="selectedApplication.summary.original_filename" class="primary-btn" :href="apiUrl(`/api/admin/applications/${selectedApplication.summary.id}/resume`)">下载简历</a>
      </aside>
    </div>

    <div v-if="jobEditorOpen" class="admin-drawer-backdrop" @click.self="jobEditorOpen=false">
      <form class="admin-job-editor" novalidate @submit.prevent="saveJob">
        <button class="modal-close" type="button" @click="jobEditorOpen=false">×</button>
        <small>JOB EDITOR</small><h2>{{editingJobId?"编辑岗位":"新建岗位"}}</h2>
        <div class="job-form-grid">
          <label class="wide"><span>岗位名称 *</span><input v-model="jobForm.title" required/></label>
          <label><span>部门 *</span><select v-model="jobForm.category"><option>职能岗位</option><option>技术岗位</option></select></label>
          <label><span>要求工作地 *</span><input v-model="jobForm.requiredLocation" required placeholder="如：泰国、越南、全球"/></label>
          <label><span>工作方式 *</span><select v-model="jobForm.workMode"><option>居家</option><option>远程</option></select></label>
          <label class="wide"><span>东南亚薪资范围 <b v-if="rmbPreview" class="salary-rmb-title">≈ 人民币 {{rmbPreview}}</b></span><div class="salary-range-input"><input v-model="jobForm.salaryRange" placeholder="如：2000-3000"/><b>USDT/月</b></div><small class="salary-auto">人民币估算固定按 1 USDT = ¥7，仅供填写时参考；国际薪资：{{salaryPreview||"输入数字范围后自动计算"}}（东南亚薪资 × 1.5）</small></label>
          <label><span>岗位状态 *</span><select v-model="jobForm.status"><option value="draft">草稿</option><option value="open">招聘中</option><option value="paused">暂停</option><option value="closed">关闭</option></select></label>
          <label><span>招聘人数（默认 1 人）</span><input v-model.number="jobForm.recruitmentCount" min="1" step="1" type="number"/></label>
          <label class="wide"><span>岗位职责（选填，每行一条）</span><textarea v-model="jobForm.responsibilities" rows="7"/></label>
          <label class="wide"><span>任职要求（选填，每行一条）</span><textarea v-model="jobForm.requirements" rows="7"/></label>
          <label class="wide"><span>加分项（每行一条）</span><textarea v-model="jobForm.bonus" rows="5"/></label>
        </div>
        <p v-if="error" class="admin-error admin-editor-error" role="alert">{{error}}</p>
        <div class="admin-editor-actions">
          <button v-if="editingJobId" class="admin-delete" :disabled="loading" type="button" @click="deleteJob">删除岗位</button>
          <button class="primary-btn admin-save" :disabled="loading" type="button" @click="saveJob">{{loading?"保存中…":"保存岗位"}}</button>
        </div>
      </form>
    </div>
  </main>
</template>
