<script setup lang="ts">
import { onBeforeUnmount, onMounted, reactive, ref } from "vue";

type VisitSummary = {
  todayEffective: number;
  averageDurationSeconds: number;
  maxDurationSeconds: number;
  submittedCount: number;
  conversionRate: number;
};

type ResearchVisit = {
  id: number;
  started_at: string;
  qualified_at: string;
  duration_seconds: number;
  last_seen_at: string;
  entry_path: string;
  device_type: string;
  operating_system: string;
  operating_system_version: string;
  browser_name: string;
  device_language: string;
  ip_address: string;
  visitor_country: string;
  submitted_research: boolean;
};

type VisitFilters = {
  from: string;
  to: string;
  minDurationSeconds: number;
  maxDurationSeconds: number | "";
  submittedResearch: string;
};

type AppliedVisitFilters = Omit<VisitFilters, "maxDurationSeconds"> & { maxDurationSeconds: number };

const props = defineProps<{ apiBase: string }>();
const emit = defineEmits<{ unauthorized: [] }>();

const emptySummary = (): VisitSummary => ({
  todayEffective: 0,
  averageDurationSeconds: 0,
  maxDurationSeconds: 0,
  submittedCount: 0,
  conversionRate: 0
});
const draftFilters = reactive<VisitFilters>({
  from: "", to: "", minDurationSeconds: 0, maxDurationSeconds: 86400, submittedResearch: "all"
});
const appliedFilters = ref<AppliedVisitFilters>({
  from: "", to: "", minDurationSeconds: 0, maxDurationSeconds: 86400, submittedResearch: "all"
});
const summary = ref<VisitSummary>(emptySummary());
const visits = ref<ResearchVisit[]>([]);
const total = ref(0);
const pages = ref(0);
const loading = ref(false);
const error = ref("");
const page = ref(0);
let controller: AbortController | null = null;
let generation = 0;
const chineseRegionNames = typeof Intl.DisplayNames === "function"
  ? new Intl.DisplayNames(["zh-CN"], { type: "region" })
  : null;

function apiUrl(path: string) {
  return `${props.apiBase.replace(/\/$/, "")}${path}`;
}

function clearResults() {
  summary.value = emptySummary();
  visits.value = [];
  total.value = 0;
  pages.value = 0;
  page.value = 0;
}

function duration(value: number) {
  const seconds = Math.max(0, Math.floor(Number(value) || 0));
  return seconds < 60 ? `${seconds} 秒` : `${Math.floor(seconds / 60)} 分 ${seconds % 60} 秒`;
}

function display(value: string | null | undefined) {
  return value || "—";
}

function country(value: string | null | undefined) {
  const code = value?.trim().toUpperCase();
  if (!code || code === "UNKNOWN" || !/^[A-Z]{2}$/.test(code)) return "未知";
  return chineseRegionNames?.of(code) || "未知";
}

async function request<T>(url: string, signal: AbortSignal): Promise<T> {
  const response = await fetch(apiUrl(url), { credentials: "include", signal });
  if (response.status === 401) throw new Error("UNAUTHORIZED");
  if (!response.ok) throw new Error("REQUEST_FAILED");
  return response.json() as Promise<T>;
}

function normalizeFilters(filters: VisitFilters): AppliedVisitFilters {
  return {
    from: filters.from,
    to: filters.to,
    minDurationSeconds: Math.max(0, Number(filters.minDurationSeconds) || 0),
    maxDurationSeconds: filters.maxDurationSeconds === "" ? 86400 : Math.max(0, Number(filters.maxDurationSeconds) || 0),
    submittedResearch: filters.submittedResearch
  };
}

async function load(): Promise<void> {
  controller?.abort();
  const requestController = new AbortController();
  controller = requestController;
  const requestGeneration = ++generation;
  const requestedPage = page.value;
  const requestFilters = appliedFilters.value;
  loading.value = true;
  error.value = "";
  const listParams = new URLSearchParams({
    systemCode: "research",
    page: String(requestedPage),
    size: "20",
    minDurationSeconds: String(requestFilters.minDurationSeconds),
    maxDurationSeconds: String(requestFilters.maxDurationSeconds),
    submittedResearch: requestFilters.submittedResearch
  });
  if (requestFilters.from) listParams.set("from", requestFilters.from);
  if (requestFilters.to) listParams.set("to", requestFilters.to);
  try {
    const [nextSummary, nextList] = await Promise.all([
      request<VisitSummary>("/api/admin/visits/summary?systemCode=research", requestController.signal),
      request<{ visits: ResearchVisit[]; total: number; pages: number }>(`/api/admin/visits?${listParams}`, requestController.signal)
    ]);
    if (requestGeneration !== generation) return;
    const nextPages = Math.max(0, nextList.pages || 0);
    if (nextPages === 0) {
      page.value = 0;
      summary.value = nextSummary;
      visits.value = [];
      total.value = nextList.total || 0;
      pages.value = 0;
      return;
    }
    if (requestedPage >= nextPages) {
      page.value = nextPages - 1;
      await load();
      return;
    }
    page.value = Math.min(requestedPage, nextPages - 1);
    summary.value = nextSummary;
    visits.value = nextList.visits || [];
    total.value = nextList.total || 0;
    pages.value = nextPages;
  } catch (cause) {
    if (requestGeneration !== generation || requestController.signal.aborted) return;
    clearResults();
    if ((cause as Error).message === "UNAUTHORIZED") emit("unauthorized");
    else error.value = "有效浏览数据加载失败，请稍后重试。";
  } finally {
    if (requestGeneration === generation) loading.value = false;
  }
}

function search() {
  page.value = 0;
  appliedFilters.value = normalizeFilters(draftFilters);
  void load();
}

function goToPage(nextPage: number) {
  if (nextPage < 0 || nextPage >= pages.value || nextPage === page.value) return;
  page.value = nextPage;
  void load();
}

onMounted(() => void load());
onBeforeUnmount(() => {
  generation++;
  controller?.abort();
});
</script>

<template>
  <section class="research-visits-module" aria-labelledby="research-visits-title">
    <div class="research-visits-title-row">
      <div>
        <small>SAKURAPAY RESEARCH VISITS</small>
        <h1 id="research-visits-title">有效浏览</h1>
      </div>
      <b>{{ total }}</b>
    </div>
    <p class="research-visits-description">查看 SakuraPay 调研访客的有效停留、来源设备及问卷提交转化。</p>
    <p v-if="error" class="admin-error" role="alert">{{ error }}</p>

    <section class="research-visits-summary" aria-label="有效浏览汇总">
      <article><small>今日有效浏览</small><b>{{ summary.todayEffective }}</b></article>
      <article><small>平均停留</small><b>{{ duration(summary.averageDurationSeconds) }}</b></article>
      <article><small>最长停留</small><b>{{ duration(summary.maxDurationSeconds) }}</b></article>
      <article><small>已提交问卷</small><b>{{ summary.submittedCount }}</b></article>
      <article><small>提交转化率</small><b>{{ summary.conversionRate }}%</b></article>
    </section>

    <div class="research-visits-filters" aria-label="有效浏览筛选">
      <label><span>开始</span><input v-model="draftFilters.from" data-testid="visits-from" type="date"></label>
      <label><span>结束</span><input v-model="draftFilters.to" data-testid="visits-to" type="date"></label>
      <label><span>时长 ≥</span><input v-model.number="draftFilters.minDurationSeconds" data-testid="visits-min-duration" type="number" min="0"></label>
      <label><span>时长 ≤</span><input v-model.number="draftFilters.maxDurationSeconds" data-testid="visits-max-duration" type="number" min="0"></label>
      <label><span>问卷</span><select v-model="draftFilters.submittedResearch" data-testid="visits-submitted"><option value="all">全部</option><option value="true">已提交</option><option value="false">未提交</option></select></label>
      <button type="button" data-testid="visits-search" @click="search">查询</button>
    </div>

    <p class="research-visits-scroll-tip">← 左右滑动查看访问详情 →</p>
    <div class="research-visits-table-wrap">
      <table class="research-visits-table">
        <thead><tr><th>进入时间</th><th>有效停留</th><th>最后上报</th><th>进入页面</th><th>设备类型 / 浏览器 / 语言</th><th>操作系统</th><th>系统版本</th><th>国家 / IP</th><th>问卷状态</th></tr></thead>
        <tbody>
          <tr v-for="visit in visits" :key="visit.id" :data-testid="`research-visit-row-${visit.id}`">
            <td>{{ new Date(visit.started_at).toLocaleString("zh-CN") }}</td>
            <td class="research-visit-duration">{{ duration(visit.duration_seconds) }}</td>
            <td>{{ new Date(visit.last_seen_at).toLocaleString("zh-CN") }}</td>
            <td :title="visit.entry_path">{{ display(visit.entry_path) }}</td>
            <td>{{ display(visit.device_type) }} · {{ display(visit.browser_name) }}<small>{{ display(visit.device_language) }}</small></td>
            <td>{{ display(visit.operating_system) }}</td>
            <td>{{ display(visit.operating_system_version) }}</td>
            <td>{{ country(visit.visitor_country) }} · {{ display(visit.ip_address) }}</td>
            <td><em class="research-visit-submitted" :class="{ submitted: visit.submitted_research }">{{ visit.submitted_research ? "已提交" : "未提交" }}</em></td>
          </tr>
          <tr v-if="!loading && !visits.length"><td class="research-visits-empty" colspan="9">暂无符合条件的有效浏览</td></tr>
        </tbody>
      </table>
    </div>
    <nav class="research-pagination" aria-label="有效浏览分页">
      <span data-testid="visits-page-status" aria-live="polite" aria-atomic="true">第 {{ pages ? page + 1 : 0 }} / {{ pages }} 页 · 共 <b>{{ total }}</b> 条</span>
      <div>
        <button type="button" data-testid="visits-previous" :disabled="loading || page === 0" @click="goToPage(page - 1)">← 上一页</button>
        <button type="button" data-testid="visits-next" :disabled="loading || pages === 0 || page >= pages - 1" @click="goToPage(page + 1)">下一页 →</button>
      </div>
    </nav>
  </section>
</template>
