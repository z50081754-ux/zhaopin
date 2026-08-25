<script setup lang="ts">
import { onMounted, reactive, ref } from "vue";
import { useJobs } from "../composables/useJobs";
import { useLanguage } from "../composables/useLanguage";
import XwLogo from "./XwLogo.vue";
import { collectDeviceInfo } from "../utils/deviceInfo";
import { apiUrl } from "../utils/api";
import { useSiteTemplate } from "../composables/useSiteTemplate";
import { waitForVisibleVisitSeconds } from "../composables/useVisitTracking";

defineProps<{ open: boolean }>();
const emit = defineEmits<{ close: [] }>();
const { language } = useLanguage();
const { activeTemplate } = useSiteTemplate();
const { jobs, loadJobs } = useJobs();
const submitting = ref(false);
const finalizingSubmission = ref(false);
const submitted = ref(false);
const submitError = ref("");
const applicationNo = ref("");
const uploadProgress = ref(0);
const resume = ref<File | null>(null);
const referrerInput = ref<HTMLInputElement | null>(null);
const form = reactive({
  resumeName:"", telegram:"", gender:"", age:"", birthDate:"", nationality:"",
  job:"", currentSalary:"", expectedSalary:"", bcExperience:"", employmentStatus:"",
  educationType:"", school:"", educationPeriod:"", passport:"", visa:"",
  interviewTime:"", startTime:"", currentCountry:"", preferredCountry:"", referrer:"", remarks:"", consent:false
});
onMounted(loadJobs);
const label = (zh:string,en:string) => language.value === "zh" ? zh : en;
const onFile = (event: Event) => {
  const input = event.currentTarget as HTMLInputElement;
  const file = input.files?.[0] || null;
  submitError.value = "";
  if (file && file.size > 10 * 1024 * 1024) {
    resume.value = null;
    input.value = "";
    submitError.value = label("简历不能超过 10MB。","Resume must be no larger than 10MB.");
  } else {
    resume.value = file;
  }
  requestAnimationFrame(() => input.blur());
};
const upload = (payload: FormData) => new Promise<{ok?:boolean;applicationNo?:string;code?:string}>((resolve,reject)=>{
  const request = new XMLHttpRequest();
  const apiEndpoint = apiUrl("/api/applications");
  request.open("POST",apiEndpoint);
  request.timeout = 60000;
  request.upload.onprogress = event => {
    if (event.lengthComputable) uploadProgress.value = Math.round(event.loaded / event.total * 100);
  };
  request.onload = () => {
    try {
      const result = JSON.parse(request.responseText || "{}");
      request.status >= 200 && request.status < 300 ? resolve(result) : reject(new Error(result.code || "SUBMIT_FAILED"));
    } catch { reject(new Error("INVALID_RESPONSE")); }
  };
  request.onerror = () => reject(new Error("NETWORK_ERROR"));
  request.ontimeout = () => reject(new Error("TIMEOUT"));
  request.onabort = () => reject(new Error("ABORTED"));
  request.send(payload);
});
const submit = async () => {
  if (!form.referrer.trim()) {
    window.alert(label("请填写推荐人后再提交。","Please enter the referrer before submitting."));
    referrerInput.value?.focus();
    return;
  }
  submitting.value = true;
  uploadProgress.value = 0;
  submitError.value = "";
  try {
    const payload = new FormData();
    Object.entries(form).forEach(([key, value]) => payload.append(key, String(value)));
    Object.entries(collectDeviceInfo()).forEach(([key, value]) => payload.append(key, value));
    if (resume.value) payload.append("resume", resume.value);
    const result = await upload(payload);
    if (!result.ok) throw new Error(result.code || "SUBMIT_FAILED");
    finalizingSubmission.value = true;
    await waitForVisibleVisitSeconds(15);
    applicationNo.value = result.applicationNo || "";
    submitted.value = true;
  } catch (error) {
    const timedOut = error instanceof Error && error.message === "TIMEOUT";
    submitError.value = label(
      timedOut
        ? "提交超时，请确认简历已下载到手机本地且网络稳定，然后重试。"
        : "提交暂时失败，请稍后重试；如持续失败，请通过 Telegram 联系招聘团队。",
      timedOut
        ? "Submission timed out. Download the resume to your phone and try again on a stable connection."
        : "Submission failed. Please try again or contact our recruiting team on Telegram."
    );
  } finally {
    finalizingSubmission.value = false;
    submitting.value = false;
  }
};
</script>

<template>
  <Teleport to="body">
    <Transition name="modal">
      <div v-if="open" class="modal-backdrop" :class="{ 'apple-site': activeTemplate === 'apple' }" @click.self="emit('close')">
        <section class="apply-modal long-form-modal" role="dialog" aria-modal="true" :aria-label="label('填写基本信息','Application form')">
          <button class="modal-close" type="button" :aria-label="label('关闭','Close')" @click="emit('close')">×</button>
          <header class="long-form-head">
            <XwLogo />
            <div><span>APPLICATION / ONLINE</span><h2>{{ label('填写基本信息','Application profile') }}</h2><p>{{ label('按实际情况填写，可直接提交。','Complete any fields that apply, then submit.') }}</p></div>
            <a href="https://t.me/XWcompany123" target="_blank" rel="noopener">@XWcompany123</a>
          </header>

          <form v-if="!submitted" class="long-form-body" novalidate @submit.prevent="submit">
            <label><span>{{ label('简历名','Resume name') }}</span><input v-model="form.resumeName" :placeholder="label('英文名或姓名首字母','English name or initials')" /></label>
            <label><span>Telegram</span><input v-model="form.telegram" placeholder="@username" /></label>
            <label><span>{{ label('性别','Gender') }}</span><input v-model="form.gender" /></label>
            <label><span>{{ label('年龄（岁）','Age') }}</span><input v-model="form.age" /></label>
            <label><span>{{ label('出生年月日','Date of birth') }}</span><input v-model="form.birthDate" :placeholder="label('年 / 月 / 日','Year / Month / Day')" /></label>
            <label><span>{{ label('国籍（国家）','Nationality (country)') }}</span><input v-model="form.nationality" /></label>
            <label><span>{{ label('求职岗位','Position') }}</span><input v-model="form.job" list="xw-job-list" /><datalist id="xw-job-list"><option v-for="job in jobs" :key="job.slug" :value="job.title" /></datalist></label>
            <label><span class="required-label"><i>*</i>{{ label('推荐人','Referrer') }}</span><input ref="referrerInput" v-model="form.referrer" required :placeholder="label('推荐人姓名或联系方式','Name or contact details')" /></label>
            <label><span>{{ label('目前薪资（月）','Current monthly salary') }}</span><input v-model="form.currentSalary" :placeholder="label('金额及币种，如 50,000 THB','Amount and currency, e.g. 2,000 USD')" /></label>
            <label><span>{{ label('期望薪资（月）','Expected monthly salary') }}</span><input v-model="form.expectedSalary" :placeholder="label('金额及币种','Amount and currency')" /></label>
            <label><span>{{ label('有无 BC 经验','BC experience') }}</span><input v-model="form.bcExperience" /></label>
            <label><span>{{ label('在职中 / 待业中','Employment status') }}</span><input v-model="form.employmentStatus" /></label>
            <label><span>{{ label('第一学历（统招 / 自考）','First degree type') }}</span><input v-model="form.educationType" /></label>
            <label><span>{{ label('第一学历学校全名','School full name') }}</span><input v-model="form.school" /></label>
            <label><span>{{ label('第一学历入学及毕业时间','Education start and graduation time') }}</span><input v-model="form.educationPeriod" :placeholder="label('入学时间 - 毕业时间','Start time - Graduation time')" /></label>
            <label><span>{{ label('是否持有护照','Passport status') }}</span><input v-model="form.passport" /></label>
            <label><span>{{ label('签证情况','Visa status') }}</span><input v-model="form.visa" /></label>
            <label><span>{{ label('可面试时间','Interview availability') }}</span><input v-model="form.interviewTime" /></label>
            <label><span>{{ label('可到职时间','Available start time') }}</span><input v-model="form.startTime" /></label>
            <label><span>{{ label('目前所在地（国家）','Current country') }}</span><input v-model="form.currentCountry" /></label>
            <label><span>{{ label('期望工作地（国家）','Preferred work country') }}</span><input v-model="form.preferredCountry" /></label>
            <label><span>{{ label('备注','Remarks') }}</span><textarea v-model="form.remarks" maxlength="2000" :placeholder="label('可补充需要招聘团队了解的信息','Add anything else the recruiting team should know')"></textarea></label>
            <label class="upload-label">
              <span>{{ label('上传简历','Upload resume') }}</span>
              <input type="file" accept=".pdf,.doc,.docx,application/pdf,application/msword,application/vnd.openxmlformats-officedocument.wordprocessingml.document" @change="onFile" />
              <b>{{ resume?.name || label('选择 PDF / DOC / DOCX 文件','Choose a PDF / DOC / DOCX file') }}</b>
              <small>{{ resume ? label('已选择，点击可重新选择','Selected · Click to replace') : label('最大 10MB','Up to 10MB') }}</small>
            </label>
            <label class="consent"><input v-model="form.consent" type="checkbox" /><span>{{ label('我同意 XW 为招聘联系及评估处理以上资料，并记录浏览器提供的设备类型、系统、浏览器、语言和时区信息用于投递安全与技术支持。','I consent to XW processing this information and recording browser-provided device, operating system, browser, language and time-zone details for application security and technical support.') }}</span></label>
            <p v-if="submitError" class="submit-error" role="alert">{{ submitError }}</p>
            <button class="primary-btn long-submit" type="submit" :disabled="submitting">{{ submitting ? (finalizingSubmission ? label('提交中…','Submitting…') : uploadProgress ? label(`正在上传 ${uploadProgress}%`,`Uploading ${uploadProgress}%`) : label('正在连接…','Connecting…')) : label('确认提交','Submit application') }}</button>
          </form>

          <div v-else class="success-state">
            <span>APPLICATION RECEIVED</span>
            <h3>{{ label('投递成功','Application received') }}</h3>
            <p>{{ label(`申请编号：${applicationNo}。招聘团队会通过 Telegram 与你联系，请留意消息。`,`Application ID: ${applicationNo}. Our recruiting team will contact you on Telegram.`) }}</p>
            <a class="primary-btn" href="https://t.me/XWcompany123" target="_blank" rel="noopener">{{ label('打开 Telegram','Open Telegram') }} →</a>
          </div>
        </section>
      </div>
    </Transition>
  </Teleport>
</template>
