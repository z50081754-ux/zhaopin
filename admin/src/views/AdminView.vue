<script setup lang="ts">
import { computed, onBeforeUnmount, onMounted, reactive, ref } from "vue";
import XwLogo from "../components/XwLogo.vue";
import ResearchModule from "../components/ResearchModule.vue";
import { parseAdminPath, pathForAdminSystem, type AdminSystem } from "../systemRoute";

type Application = {
  id:number; application_no:string; resume_name:string; telegram:string; gender:string; age:string;
  birth_date:string; nationality_country:string; job_title:string; current_salary:string;
  referrer:string; remarks:string;
  expected_salary:string; bc_experience:string; employment_status:string; education_type:string;
  school:string; education_period:string; passport_status:string; visa_status:string;
  interview_time:string; start_time:string; current_country:string; preferred_country:string;
  stage:string; is_possible_duplicate:number; original_filename?:string; resume_size:number;
  ip_address:string; device_type:string; device_model:string; operating_system:string; browser_name:string;
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
  screen_resolution:string; device_language:string; device_timezone:string; user_agent:string; detected_wallets:string;
  system_code:string; queried_address:boolean;
};

const API_BASE=(import.meta.env.VITE_API_BASE_URL||"").replace(/\/$/,"");
const PUBLIC_SITE_URL=import.meta.env.VITE_PUBLIC_SITE_URL||"/";
const apiUrl=(path:string)=>`${API_BASE}${path}`;
const account=ref(""), password=ref(""), authenticated=ref(false), loading=ref(false), error=ref("");
const activeSystem=ref<AdminSystem>(parseAdminPath(window.location.pathname));
type SiteTemplate="technology"|"apple";
type DefaultLanguage="auto"|"zh"|"en";
const activeModule=ref<"applications"|"visits"|"jobs"|"templates"|"research">("applications"), query=ref(""), stage=ref("");
const referrerQuery=ref(""), createdFrom=ref(""), createdTo=ref(""), operatingSystemQuery=ref(""), deviceModelQuery=ref("");
const applications=ref<Application[]>([]), selectedApplication=ref<ApplicationDetail|null>(null);
const selectedApplicationIds=ref<number[]>([]);
const currentPage=ref(0), totalPages=ref(0), totalApplications=ref(0), pageSize=20;
const visits=ref<WebsiteVisit[]>([]), visitPage=ref(0), totalVisitPages=ref(0), totalVisits=ref(0), visitPageSize=20;
const visitsSystem=ref<"recruitment"|"walletcheck"|null>(null);
const selectedVisitIds=ref<number[]>([]);
const visitMinDuration=ref<number|"">("");
const visitTodayOnly=ref(false);
const jobs=ref<AdminJob[]>([]), jobEditorOpen=ref(false), editingJobId=ref<number|null>(null);
const jobStatusFilter=ref<"all"|"online"|"offline">("all");
const activeTemplate=ref<SiteTemplate>("technology"), templateSaving=ref(false), templateMessage=ref("");
const defaultLanguage=ref<DefaultLanguage>("auto");
type RequestOwner={requestId:number;sessionGeneration:number};
type ViewRequestOwner=RequestOwner&{viewGeneration:number};
let sessionGeneration=0, viewGeneration=0, requestSequence=0;
let activeSessionRequestId=0, activeViewRequestId=0, loadingRequestId=0;
const emptyJob=()=>({title:"",category:"职能岗位",requiredLocation:"泰国",workMode:"居家",salaryRange:"",responsibilities:"",requirements:"",bonus:"",status:"draft",recruitmentCount:1});
const jobForm=reactive(emptyJob());
const stageLabels:Record<string,string>={new:"新投递",screening:"筛选中",interview:"面试中",offer:"Offer",hired:"已录用",rejected:"不合适"};
const jobStatusLabels:Record<string,string>={draft:"未上线",open:"已上线",paused:"已下线",closed:"已下线"};
const filteredCount=computed(()=>totalApplications.value);
const allApplicationsOnPageSelected=computed(()=>applications.value.length>0&&applications.value.every(item=>selectedApplicationIds.value.includes(item.id)));
const allVisitsOnPageSelected=computed(()=>visits.value.length>0&&visits.value.every(item=>selectedVisitIds.value.includes(item.id)));
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

function beginLoading(requestId:number){loadingRequestId=requestId;loading.value=true;error.value=""}
function finishLoading(owner:RequestOwner){if(loadingRequestId===owner.requestId){loadingRequestId=0;loading.value=false}}
function beginSessionRequest():RequestOwner{
  const owner={requestId:++requestSequence,sessionGeneration};
  activeSessionRequestId=owner.requestId;beginLoading(owner.requestId);return owner;
}
function ownsSessionRequest(owner:RequestOwner){return owner.sessionGeneration===sessionGeneration&&owner.requestId===activeSessionRequestId}
function beginViewRequest():ViewRequestOwner{
  const owner={requestId:++requestSequence,sessionGeneration,viewGeneration};
  activeViewRequestId=owner.requestId;beginLoading(owner.requestId);return owner;
}
function ownsViewRequest(owner:ViewRequestOwner){
  return owner.sessionGeneration===sessionGeneration&&owner.viewGeneration===viewGeneration&&owner.requestId===activeViewRequestId;
}
function invalidateViewRequests(){
  const invalidatedRequestId=activeViewRequestId;
  activeViewRequestId=0;viewGeneration++;error.value="";templateMessage.value="";templateSaving.value=false;
  if(loadingRequestId===invalidatedRequestId){loadingRequestId=0;loading.value=false}
}
function clearVisitData(){
  visits.value=[];visitsSystem.value=null;totalVisits.value=0;totalVisitPages.value=0;selectedVisitIds.value=[];
}
function clearProtectedData(){
  applications.value=[];selectedApplication.value=null;selectedApplicationIds.value=[];currentPage.value=0;totalPages.value=0;totalApplications.value=0;
  jobs.value=[];jobEditorOpen.value=false;editingJobId.value=null;jobStatusFilter.value="all";Object.assign(jobForm,emptyJob());
  clearVisitData();visitPage.value=0;activeTemplate.value="technology";defaultLanguage.value="auto";templateMessage.value="";templateSaving.value=false;
}
function clearAuthenticatedSession(expectedGeneration:number){
  if(expectedGeneration!==sessionGeneration)return false;
  sessionGeneration++;activeSessionRequestId=0;invalidateViewRequests();clearProtectedData();
  authenticated.value=false;account.value="";password.value="";loadingRequestId=0;loading.value=false;error.value="";
  return true;
}
function handleViewError(owner:ViewRequestOwner,cause:unknown,message:string){
  if(!ownsViewRequest(owner))return;
  if((cause as Error).message==="UNAUTHORIZED"){clearAuthenticatedSession(owner.sessionGeneration);return}
  error.value=message;
}

async function api<T>(url:string,options:RequestInit={}):Promise<T>{
  const response=await fetch(apiUrl(url),{credentials:"include",...options});
  if(response.status===401)throw new Error("UNAUTHORIZED");
  const result=await response.json();
  if(!response.ok) throw new Error(result?.message||result?.code||"REQUEST_FAILED");
  return result as T;
}
async function loadApplications(){
  const owner=beginViewRequest();selectedApplicationIds.value=[];
  try{
    const params=new URLSearchParams({q:query.value,stage:stage.value,referrer:referrerQuery.value,
      createdFrom:createdFrom.value,createdTo:createdTo.value,operatingSystem:operatingSystemQuery.value,
      deviceModel:deviceModelQuery.value,page:String(currentPage.value),size:String(pageSize)});
    const result=await api<{ok:boolean;applications:Application[];total:number;pages:number}>(`/api/admin/applications?${params}`);
    if(!ownsViewRequest(owner))return;
    applications.value=result.applications||[];totalApplications.value=result.total||0;totalPages.value=result.pages||0;
  }catch(e){handleViewError(owner,e,"数据加载失败，请稍后重试。")}
  finally{finishLoading(owner)}
}
function searchApplications(){currentPage.value=0;void loadApplications()}
function goToPage(page:number){
  if(page<0||page>=totalPages.value||page===currentPage.value)return;
  currentPage.value=page;void loadApplications();window.scrollTo({top:0,behavior:"smooth"});
}
async function loadJobs(){
  const owner=beginViewRequest();
  try{const result=await api<AdminJob[]>("/api/admin/jobs");if(ownsViewRequest(owner))jobs.value=result}
  catch(e){handleViewError(owner,e,"岗位数据加载失败。")}
  finally{finishLoading(owner)}
}
async function loadVisits(system:AdminSystem=activeSystem.value){
  const requestedSystem=system==="walletcheck"?"walletcheck":"recruitment";
  const owner=beginViewRequest();
  if(visitsSystem.value!==requestedSystem){
    visits.value=[];totalVisits.value=0;totalVisitPages.value=0;selectedVisitIds.value=[];
    visitsSystem.value=requestedSystem;
  }
  selectedVisitIds.value=[];
  try{
    const minDurationSeconds=Math.max(0,Math.floor(Number(visitMinDuration.value)||0));
    const params=new URLSearchParams({page:String(visitPage.value),size:String(visitPageSize),minDurationSeconds:String(minDurationSeconds),today:String(visitTodayOnly.value)});
    params.set("systemCode",requestedSystem);
    const result=await api<{visits:WebsiteVisit[];total:number;pages:number}>(`/api/admin/visits?${params}`);
    if(!ownsViewRequest(owner))return;
    visits.value=result.visits||[];totalVisits.value=result.total||0;totalVisitPages.value=result.pages||0;
  }catch(e){handleViewError(owner,e,"有效浏览数据加载失败。")}
  finally{finishLoading(owner)}
}
function searchVisits(){visitPage.value=0;void loadVisits()}
function toggleTodayVisits(){visitTodayOnly.value=!visitTodayOnly.value;searchVisits()}
async function deleteVisit(visit:WebsiteVisit){
  if(!window.confirm(`确定删除这条有效浏览记录吗？\nIP：${display(visit.ip_address)}\n有效停留：${visitDuration(visit.duration_seconds)}\n删除后无法恢复。`))return;
  const owner=beginViewRequest();
  try{
    await api(`/api/admin/visits/${visit.id}`,{method:"DELETE"});
    if(!ownsViewRequest(owner))return;
    if(visits.value.length===1&&visitPage.value>0)visitPage.value--;
    await loadVisits();
  }catch(e){handleViewError(owner,e,`有效浏览记录删除失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
function toggleAllVisits(){
  const pageIds=visits.value.map(item=>item.id);
  selectedVisitIds.value=allVisitsOnPageSelected.value
    ?selectedVisitIds.value.filter(id=>!pageIds.includes(id))
    :Array.from(new Set([...selectedVisitIds.value,...pageIds]));
}
async function deleteSelectedVisits(){
  const ids=[...selectedVisitIds.value];
  if(!ids.length||!window.confirm(`确定批量删除选中的 ${ids.length} 条有效浏览记录吗？删除后无法恢复。`))return;
  const owner=beginViewRequest();
  try{
    await api("/api/admin/visits/batch",{method:"DELETE",headers:{"Content-Type":"application/json"},body:JSON.stringify({ids})});
    if(!ownsViewRequest(owner))return;
    selectedVisitIds.value=[];
    if(visits.value.every(visit=>ids.includes(visit.id))&&visitPage.value>0)visitPage.value--;
    await loadVisits();
  }catch(e){handleViewError(owner,e,`有效浏览记录批量删除失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
function goToVisitPage(page:number){
  if(page<0||page>=totalVisitPages.value||page===visitPage.value)return;
  visitPage.value=page;void loadVisits();window.scrollTo({top:0,behavior:"smooth"});
}
async function loadTemplate(){
  const owner=beginViewRequest();
  try{
    const result=await api<{activeTemplate:SiteTemplate;defaultLanguage:DefaultLanguage}>("/api/admin/site-settings");
    if(!ownsViewRequest(owner))return;
    activeTemplate.value=result.activeTemplate;defaultLanguage.value=result.defaultLanguage||"auto";
  }catch(e){handleViewError(owner,e,"官网模板读取失败。")}
  finally{finishLoading(owner)}
}
function loadVisibleSystem(system:AdminSystem){
  if(system==="walletcheck")return loadVisits(system);
  if(system==="recruitment"){
    if(activeModule.value==="visits")return loadVisits(system);
    if(activeModule.value==="jobs")return loadJobs();
    if(activeModule.value==="templates")return loadTemplate();
    if(activeModule.value==="applications")return loadApplications();
  }
  return Promise.resolve();
}
async function restoreSession(){
  const owner=beginSessionRequest();
  try{
    const session=await api<{account:string}>("/api/admin/session");
    if(!ownsSessionRequest(owner))return;
    activeSessionRequestId=0;account.value=session.account;authenticated.value=true;
    void loadVisibleSystem(activeSystem.value);
  }catch(cause){
    if(!ownsSessionRequest(owner))return;
    if((cause as Error).message==="UNAUTHORIZED")clearAuthenticatedSession(owner.sessionGeneration);
    else{authenticated.value=false;account.value="";error.value="无法验证登录状态，请稍后重试。"}
  }finally{finishLoading(owner)}
}
async function login(){
  const owner=beginSessionRequest();
  try{
    await api("/api/admin/login",{method:"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify({account:account.value,password:password.value})});
    const session=await api<{account:string}>("/api/admin/session");
    if(!ownsSessionRequest(owner))return;
    sessionGeneration++;activeSessionRequestId=0;invalidateViewRequests();
    account.value=session.account;password.value="";authenticated.value=true;
    void loadVisibleSystem(activeSystem.value);
  }catch{if(ownsSessionRequest(owner))error.value="账号或密码错误，或后端服务尚未启动。"}
  finally{finishLoading(owner)}
}
async function logout(){
  const owner=beginSessionRequest();
  try{
    const response=await fetch(apiUrl("/api/admin/logout"),{method:"POST",credentials:"include"});
    if(!response.ok&&response.status!==401)throw new Error("REQUEST_FAILED");
    if(!ownsSessionRequest(owner))return;
  }catch{if(ownsSessionRequest(owner))error.value="退出登录失败，请稍后重试。";finishLoading(owner);return}
  if(clearAuthenticatedSession(owner.sessionGeneration)){
    activeSystem.value="home";window.history.pushState({},"",pathForAdminSystem("home"));
  }
}
async function openApplication(item:Application){
  const owner=beginViewRequest();
  try{const result=await api<ApplicationDetail>(`/api/admin/applications/${item.id}`);if(ownsViewRequest(owner))selectedApplication.value=result}
  catch(e){handleViewError(owner,e,"无法读取候选人详情。")}
  finally{finishLoading(owner)}
}
async function changeStage(value:string){
  if(!selectedApplication.value)return;
  const owner=beginViewRequest();
  const applicationId=selectedApplication.value.summary.id;
  try{
    await api(`/api/admin/applications/${applicationId}/stage`,{method:"PATCH",headers:{"Content-Type":"application/json"},body:JSON.stringify({stage:value})});
    if(!ownsViewRequest(owner))return;
    selectedApplication.value.summary.stage=value;await loadApplications();
  }catch(e){handleViewError(owner,e,"阶段更新失败。")}
  finally{finishLoading(owner)}
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
  const responsibilities=lines(jobForm.responsibilities), requirements=lines(jobForm.requirements);
  if(!jobForm.title.trim()||!jobForm.requiredLocation.trim()){
    error.value="请填写岗位名称和要求工作地后再保存。";
    return;
  }
  const owner=beginViewRequest();
  const payload={...jobForm,responsibilities,requirements,bonus:lines(jobForm.bonus)};
  try{
    await api(editingJobId.value?`/api/admin/jobs/${editingJobId.value}`:"/api/admin/jobs",{method:editingJobId.value?"PUT":"POST",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
    if(!ownsViewRequest(owner))return;
    jobEditorOpen.value=false;await loadJobs();
  }catch(e){handleViewError(owner,e,`岗位保存失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
async function deleteJob(){
  if(!editingJobId.value)return;
  if(!window.confirm(`确定删除岗位“${jobForm.title}”吗？删除后前台将不再显示，且无法恢复。`))return;
  const owner=beginViewRequest();
  try{
    await api(`/api/admin/jobs/${editingJobId.value}`,{method:"DELETE"});
    if(!ownsViewRequest(owner))return;
    jobEditorOpen.value=false;editingJobId.value=null;await loadJobs();
  }catch(e){handleViewError(owner,e,`岗位删除失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
async function toggleJobStatus(job:AdminJob){
  const owner=beginViewRequest();
  const status=job.status==="open"?"paused":"open";
  const currentFilter=jobStatusFilter.value;
  const payload={...job,status,salaryRange:salaryInput(job.salaryRange),internationalSalaryRange:undefined};
  try{
    await api(`/api/admin/jobs/${job.id}`,{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify(payload)});
    if(!ownsViewRequest(owner))return;
    activeModule.value="jobs";
    jobStatusFilter.value=currentFilter;
    await loadJobs();
  }
  catch(e){handleViewError(owner,e,`岗位${status==="open"?"上线":"下线"}失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
async function deleteApplication(item:Application){
  if(!window.confirm(`确定删除候选人“${display(item.resume_name)}”的投递记录吗？简历文件也会一并删除，且无法恢复。`))return;
  const owner=beginViewRequest();
  try{
    await api(`/api/admin/applications/${item.id}`,{method:"DELETE"});
    if(!ownsViewRequest(owner))return;
    applications.value=applications.value.filter(application=>application.id!==item.id);
    totalApplications.value=Math.max(0,totalApplications.value-1);
    totalPages.value=Math.ceil(totalApplications.value/pageSize);
    if(selectedApplication.value?.summary.id===item.id)selectedApplication.value=null;
    if(!applications.value.length&&currentPage.value>0)currentPage.value--;
    await loadApplications();
  }
  catch(e){handleViewError(owner,e,`候选人删除失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
function toggleAllApplications(){
  const pageIds=applications.value.map(item=>item.id);
  selectedApplicationIds.value=allApplicationsOnPageSelected.value
    ?selectedApplicationIds.value.filter(id=>!pageIds.includes(id))
    :Array.from(new Set([...selectedApplicationIds.value,...pageIds]));
}
async function deleteSelectedApplications(){
  const ids=[...selectedApplicationIds.value];
  if(!ids.length||!window.confirm(`确定批量删除选中的 ${ids.length} 条候选人记录吗？简历文件也会一并删除，且无法恢复。`))return;
  const owner=beginViewRequest();
  try{
    await api("/api/admin/applications/batch",{method:"DELETE",headers:{"Content-Type":"application/json"},body:JSON.stringify({ids})});
    if(!ownsViewRequest(owner))return;
    if(selectedApplication.value&&ids.includes(selectedApplication.value.summary.id))selectedApplication.value=null;
    selectedApplicationIds.value=[];
    if(applications.value.every(item=>ids.includes(item.id))&&currentPage.value>0)currentPage.value--;
    await loadApplications();
  }catch(e){handleViewError(owner,e,`候选人批量删除失败：${(e as Error).message}`)}finally{finishLoading(owner)}
}
async function selectTemplate(template:SiteTemplate){
  if(template===activeTemplate.value)return;
  const owner=beginViewRequest();templateSaving.value=true;templateMessage.value="";
  try{
    const result=await api<{activeTemplate:SiteTemplate;defaultLanguage:DefaultLanguage}>("/api/admin/site-settings",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({activeTemplate:template,defaultLanguage:defaultLanguage.value})});
    if(!ownsViewRequest(owner))return;
    activeTemplate.value=result.activeTemplate;templateMessage.value="模板已启用，刷新招聘官网即可看到新风格。";
  }catch(e){handleViewError(owner,e,`模板切换失败：${(e as Error).message}`)}finally{if(ownsViewRequest(owner))templateSaving.value=false;finishLoading(owner)}
}
async function selectDefaultLanguage(value:DefaultLanguage){
  if(value===defaultLanguage.value)return;
  const owner=beginViewRequest();templateSaving.value=true;templateMessage.value="";
  try{
    const result=await api<{activeTemplate:SiteTemplate;defaultLanguage:DefaultLanguage}>("/api/admin/site-settings",{method:"PUT",headers:{"Content-Type":"application/json"},body:JSON.stringify({activeTemplate:activeTemplate.value,defaultLanguage:value})});
    if(!ownsViewRequest(owner))return;
    defaultLanguage.value=result.defaultLanguage;templateMessage.value="默认语言已保存，仅影响尚未手动选择过语言的访客。";
  }catch(e){handleViewError(owner,e,`默认语言保存失败：${(e as Error).message}`)}finally{if(ownsViewRequest(owner))templateSaving.value=false;finishLoading(owner)}
}
function switchModule(module:"applications"|"visits"|"jobs"|"templates"|"research"){
  if(activeModule.value!==module)invalidateViewRequests();
  activeModule.value=module;
  if(module==="visits")void loadVisits();else if(module==="jobs")void loadJobs();else if(module==="templates")void loadTemplate();else if(module==="applications")void loadApplications();
}
function navigateSystem(system:AdminSystem){
  if(activeSystem.value!==system){
    invalidateViewRequests();clearVisitData();
  }
  activeSystem.value=system;
  window.history.pushState({},"",pathForAdminSystem(system));
  void loadVisibleSystem(system);
}
function syncSystemFromPath(){
  const system=parseAdminPath(window.location.pathname);
  if(activeSystem.value!==system){
    invalidateViewRequests();clearVisitData();
  }
  activeSystem.value=system;
  if(authenticated.value)void loadVisibleSystem(activeSystem.value);
}
onMounted(()=>{
  void restoreSession();
  window.addEventListener("popstate",syncSystemFromPath);
});
onBeforeUnmount(()=>window.removeEventListener("popstate",syncSystemFromPath));
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
      <header class="admin-header">
        <div class="admin-brand"><XwLogo/><span>{{activeSystem==='home'?'系统管理':activeSystem==='walletcheck'?'WalletCheck':'招聘系统'}}</span></div>
        <div class="admin-header-actions">
          <button v-if="activeSystem!=='home'" type="button" data-testid="system-home" @click="navigateSystem('home')">← 返回系统首页</button>
          <span v-if="account" data-testid="current-account">账号：{{account}}</span>
          <a v-if="activeSystem!=='walletcheck'" :href="PUBLIC_SITE_URL" target="_blank" rel="noopener">查看招聘官网 ↗</a>
          <button type="button" data-testid="logout" @click="logout">退出登录</button>
        </div>
      </header>
      <section v-if="activeSystem==='home'" class="admin-main system-home">
        <div class="system-home-title"><small>SYSTEM MANAGEMENT</small><h1>系统管理</h1><p>选择要管理的业务系统。</p></div>
        <div class="system-entry-grid">
          <button type="button" class="system-entry" data-testid="recruitment-entry" @click="navigateSystem('recruitment')"><small>RECRUITMENT</small><h2>招聘系统</h2><p>管理候选人、有效浏览、招聘岗位与官网设置。</p><span>进入系统 →</span></button>
          <button type="button" class="system-entry" data-testid="walletcheck-entry" @click="navigateSystem('walletcheck')"><small>WALLETCHECK</small><h2>WalletCheck</h2><p>查看 WalletCheck 的有效访问与地址查询情况。</p><span>进入系统 →</span></button>
        </div>
      </section>
      <section v-else class="admin-main">
        <nav v-if="activeSystem==='recruitment'" class="admin-modules">
          <button :class="{active:activeModule==='applications'}" @click="switchModule('applications')">候选人管理</button>
          <button :class="{active:activeModule==='visits'}" @click="switchModule('visits')">有效浏览</button>
          <button :class="{active:activeModule==='jobs'}" @click="switchModule('jobs')">岗位管理</button>
          <button :class="{active:activeModule==='templates'}" @click="switchModule('templates')">官网模板</button>
          <button :class="{active:activeModule==='research'}" @click="switchModule('research')">web3钱包产品调研</button>
        </nav>
        <template v-if="activeSystem==='recruitment'&&activeModule==='applications'">
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
          <div class="bulk-actions"><button type="button" :disabled="!selectedApplicationIds.length||loading" @click="deleteSelectedApplications">批量删除已选（{{selectedApplicationIds.length}}）</button></div>
          <p v-if="error" class="admin-error">{{error}}</p>
          <p class="admin-scroll-tip">← 左右滑动查看全部候选人信息，点击任意一行打开详情 →</p>
          <div class="admin-table admin-wide-table">
            <div class="admin-wide-row admin-row-head">
              <span class="select-action"><input type="checkbox" :checked="allApplicationsOnPageSelected" aria-label="选择本页候选人" @change="toggleAllApplications"/>操作</span><span>提交时间</span><span>网络 IP</span><span>推荐人</span><span>备注</span><span>系统版本</span>
              <span>申请编号</span><span>简历名</span><span>Telegram</span><span>性别</span><span>年龄</span>
              <span>出生年月日</span><span>国籍</span><span>求职岗位</span><span>目前薪资</span><span>期望薪资</span>
              <span>BC 经验</span><span>就业状态</span><span>第一学历</span><span>学校全名</span><span>就读时间</span>
              <span>护照</span><span>签证</span><span>可面试时间</span><span>可到职时间</span><span>目前所在地</span>
              <span>期望工作地</span><span>招聘阶段</span><span>简历文件</span><span>文件大小</span><span>设备类型</span>
              <span>设备机型</span><span>浏览器</span><span>屏幕</span><span>语言</span>
              <span>时区</span><span>User Agent</span>
            </div>
            <article v-for="item in applications" :key="item.id" class="admin-wide-row clickable" @click="openApplication(item)">
              <span class="select-action"><input v-model="selectedApplicationIds" :value="item.id" type="checkbox" :aria-label="`选择候选人 ${display(item.resume_name)}`" @click.stop/><button class="candidate-delete" type="button" @click.stop="deleteApplication(item)">删除</button></span>
              <span>{{new Date(item.created_at).toLocaleString("zh-CN")}}</span>
              <span>{{display(item.ip_address)}}</span>
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

        <template v-else-if="activeModule==='visits'||activeSystem==='walletcheck'">
          <div class="admin-title"><div><small>{{activeSystem==='walletcheck'?'WALLETCHECK QUALIFIED VISITS':'QUALIFIED WEBSITE VISITS'}}</small><h1>{{activeSystem==='walletcheck'?'WalletCheck 有效浏览':'有效浏览'}}</h1></div><b>{{String(totalVisits).padStart(2,"0")}}</b></div>
          <p v-if="activeSystem==='walletcheck'" class="visit-description">WalletCheck 仅累计页面可见停留；满 15 秒后记录有效浏览，并以“查询过地址”展示地址查询转化。</p>
          <p v-else class="visit-description">钱包检测在页面进入时立即开始；访客在页面可见状态下停留满 15 秒后，才将有效浏览与检测到的钱包一起上报后台。</p>
          <div class="admin-toolbar">
            <label><span>有效时长 ≥</span><input v-model.number="visitMinDuration" type="number" min="0" step="1" placeholder="秒" @keyup.enter="searchVisits"/></label>
            <button type="button" class="toolbar-filter" :class="{active:visitTodayOnly}" :aria-pressed="visitTodayOnly" @click="toggleTodayVisits">今日</button>
            <button type="button" @click="searchVisits">查询</button>
          </div>
          <div class="bulk-actions"><button type="button" :disabled="!selectedVisitIds.length||loading" @click="deleteSelectedVisits">批量删除已选（{{selectedVisitIds.length}}）</button></div>
          <p v-if="error" class="admin-error">{{error}}</p>
          <p class="admin-scroll-tip">← 左右滑动查看完整访问与设备信息 →</p>
          <div class="admin-table admin-wide-table visit-table">
            <div class="admin-wide-row admin-row-head visit-row" :class="{'walletcheck-visit-row':activeSystem==='walletcheck'}">
              <span class="select-action"><input type="checkbox" :checked="allVisitsOnPageSelected" aria-label="选择本页有效浏览" @change="toggleAllVisits"/>操作</span><span>达标时间</span><span>有效停留</span><span>网络 IP</span><span>系统版本</span>
              <span v-if="activeSystem==='walletcheck'">查询过地址</span>
              <span>设备机型</span><span>设备类型</span><span>浏览器</span><span v-if="activeSystem==='recruitment'">检测到的钱包</span><span>进入页面</span>
              <span>最后页面</span><span>最后活跃</span><span>屏幕</span><span>语言</span><span>时区</span><span>User Agent</span>
            </div>
            <article v-for="visit in visits" :key="visit.id" class="admin-wide-row visit-row" :class="{'walletcheck-visit-row':activeSystem==='walletcheck'}">
              <span class="select-action"><input v-model="selectedVisitIds" :value="visit.id" type="checkbox" :aria-label="`选择访问记录 ${visit.id}`"/><button class="candidate-delete" type="button" :disabled="loading" @click="deleteVisit(visit)">删除</button></span>
              <span>{{new Date(visit.qualified_at).toLocaleString("zh-CN")}}</span><span><b>{{visitDuration(visit.duration_seconds)}}</b></span>
              <span>{{display(visit.ip_address)}}</span><span>{{display(visit.operating_system)}}</span>
              <span v-if="activeSystem==='walletcheck'"><b class="visit-boolean" :class="{yes:visit.queried_address}">{{visit.queried_address?'是':'否'}}</b></span>
              <span>{{display(visit.device_model)}}</span><span>{{display(visit.device_type)}}</span>
              <span>{{display(visit.browser_name)}}</span><span v-if="activeSystem==='recruitment'" class="wallet-list" :title="visit.detected_wallets">{{display(visit.detected_wallets)}}</span><span :title="visit.entry_path">{{display(visit.entry_path)}}</span>
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
        <ResearchModule v-else-if="activeModule==='research'" :api-base="API_BASE" />
        <template v-else>
          <div class="admin-title"><div><small>WEBSITE APPEARANCE</small><h1>官网模板</h1></div><a class="template-preview-link" :href="PUBLIC_SITE_URL" target="_blank" rel="noopener">打开官网预览 ↗</a></div>
          <p class="template-intro">选择招聘官网首页的视觉风格。启用后立即保存到数据库，所有访客刷新首页后都会看到新的模板。</p>
          <section class="language-setting">
            <div><small>DEFAULT LANGUAGE</small><h2>访客首次打开的默认语言</h2><p>中文或 English 将作为首次访问语言；自动模式下，中国 IP 使用中文，其他国家和地区使用英文。访客之后仍可手动切换。</p></div>
            <div class="language-options">
              <button type="button" :class="{active:defaultLanguage==='auto'}" :disabled="templateSaving" @click="selectDefaultLanguage('auto')">自动</button>
              <button type="button" :class="{active:defaultLanguage==='zh'}" :disabled="templateSaving" @click="selectDefaultLanguage('zh')">中文</button>
              <button type="button" :class="{active:defaultLanguage==='en'}" :disabled="templateSaving" @click="selectDefaultLanguage('en')">English</button>
            </div>
          </section>
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
          <div><dt>网络 IP</dt><dd>{{display(selectedApplication.summary.ip_address)}}</dd></div>
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
