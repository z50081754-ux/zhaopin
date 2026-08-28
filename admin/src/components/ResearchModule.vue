<script setup lang="ts">
import { computed, nextTick, onMounted, reactive, ref } from "vue";
import {
  deleteResearchBatch,
  deleteResearchSubmission,
  downloadResearchCsv,
  loadResearchCampaign,
  loadResearchDetail,
  loadResearchSubmissions,
  loadResearchSummary,
  lookupResearchWallet,
  updateResearchCampaign
} from "../research/api";
import type {
  ResearchCampaign,
  ResearchDetail,
  ResearchFilters,
  ResearchSubmission,
  ResearchSummary
} from "../research/types";

const props = defineProps<{ apiBase: string }>();
const PAGE_SIZE = 20;
const emptySummary = (): ResearchSummary => ({
  total: 0,
  averageRating: 0,
  ratingDistribution: {},
  sceneDistribution: {},
  concernDistribution: {},
  sourceDistribution: {}
});

const campaign = ref<ResearchCampaign | null>(null);
const campaignLoadState = ref<"loading" | "ready" | "error">("loading");
const summary = ref<ResearchSummary>(emptySummary());
const submissions = ref<ResearchSubmission[]>([]);
const selectedSubmissionIds = ref<number[]>([]);
const selectedDetail = ref<ResearchDetail | null>(null);
const detailDrawer = ref<HTMLElement | null>(null);
const detailCloseButton = ref<HTMLButtonElement | null>(null);
const detailInvoker = ref<HTMLElement | null>(null);
const total = ref(0);
const pages = ref(0);
const loading = ref(false);
const listLoading = ref(false);
const exporting = ref(false);
const error = ref("");
const notice = ref("");
const walletLookup = ref("");
const filters = reactive<Required<ResearchFilters>>({
  number: "",
  rating: 0,
  concern: "",
  source: "",
  scene: "",
  from: "",
  to: "",
  page: 0,
  size: PAGE_SIZE
});

const selectedSubmissions = computed(() => submissions.value
  .filter(submission => selectedSubmissionIds.value.includes(submission.id)));
const allOnPageSelected = computed(() => submissions.value.length > 0
  && submissions.value.every(submission => selectedSubmissionIds.value.includes(submission.id)));
const campaignLabel = computed(() => {
  if (campaignLoadState.value === "loading") return "状态读取中";
  if (campaignLoadState.value === "error" || !campaign.value) return "状态读取失败";
  return {
    ACTIVE: "进行中",
    PAUSED: "已暂停",
    DISABLED: "报名入口已禁用",
    UNAVAILABLE: "状态不可用"
  }[campaign.value.effectiveStatus];
});
const campaignToggleLabel = computed(() => {
  if (campaignLoadState.value !== "ready" || !campaign.value) return "状态不可操作";
  return campaign.value.status === "ACTIVE" ? "暂停调研" : "恢复调研";
});
const campaignCanToggle = computed(() => campaignLoadState.value === "ready"
  && campaign.value !== null
  && (campaign.value.status === "ACTIVE"
    || (campaign.value.intakeEnabled && campaign.value.dataAvailable)));
const topDistribution = (distribution: Record<string, number>) => Object.entries(distribution)
  .sort(([, countA], [, countB]) => countB - countA)[0]?.[0] || "—";
const currentFilters = (): Required<ResearchFilters> => ({ ...filters });
const exportFilters = (): ResearchFilters => {
  const { page: _page, size: _size, ...activeFilters } = currentFilters();
  return activeFilters;
};

function formatDate(value: string) {
  return new Date(value).toLocaleString("zh-CN");
}

function display(value: string | number | undefined | null) {
  return value === undefined || value === null || value === "" ? "—" : String(value);
}

function requestError(action: string, reason: unknown) {
  error.value = `${action}失败：${reason instanceof Error ? reason.message : "请稍后重试"}`;
}

async function loadSubmissions(resetSelection = true, propagateError = false) {
  listLoading.value = true;
  if (resetSelection) selectedSubmissionIds.value = [];
  try {
    const result = await loadResearchSubmissions(props.apiBase, currentFilters());
    submissions.value = result.submissions || [];
    total.value = result.total || 0;
    pages.value = result.pages || 0;
  } catch (reason) {
    if (propagateError) throw reason;
    requestError("调研记录加载", reason);
  } finally {
    listLoading.value = false;
  }
}

async function loadDashboard() {
  loading.value = true;
  campaignLoadState.value = "loading";
  campaign.value = null;
  error.value = "";
  try {
    campaign.value = await loadResearchCampaign(props.apiBase);
    campaignLoadState.value = "ready";
    summary.value = await loadResearchSummary(props.apiBase);
    await loadSubmissions();
  } catch (reason) {
    if (!campaign.value) campaignLoadState.value = "error";
    requestError("调研模块加载", reason);
  } finally {
    loading.value = false;
  }
}

async function refreshAfterMutation() {
  const [summaryResult] = await Promise.all([
    loadResearchSummary(props.apiBase),
    loadSubmissions(true, true)
  ]);
  summary.value = summaryResult;
}

async function toggleCampaign() {
  if (!campaign.value) return;
  loading.value = true;
  error.value = "";
  try {
    campaign.value = await updateResearchCampaign(props.apiBase,
      campaign.value.status === "ACTIVE" ? "PAUSED" : "ACTIVE");
  } catch (reason) {
    requestError("调研状态更新", reason);
  } finally {
    loading.value = false;
  }
}

function applyFilters() {
  filters.page = 0;
  error.value = "";
  void loadSubmissions();
}

function clearFilters() {
  Object.assign(filters, {
    number: "", rating: 0, concern: "", source: "", scene: "", from: "", to: "", page: 0
  });
  error.value = "";
  void loadSubmissions();
}

function goToPage(page: number) {
  if (page < 0 || page >= pages.value || page === filters.page) return;
  filters.page = page;
  void loadSubmissions();
}

function toggleAll() {
  const pageIds = submissions.value.map(submission => submission.id);
  selectedSubmissionIds.value = allOnPageSelected.value
    ? selectedSubmissionIds.value.filter(id => !pageIds.includes(id))
    : Array.from(new Set([...selectedSubmissionIds.value, ...pageIds]));
}

function invokerFrom(event?: Event) {
  const invoker = event?.currentTarget;
  return invoker instanceof HTMLElement ? invoker : null;
}

async function showDetail(detail: ResearchDetail, invoker: HTMLElement | null) {
  detailInvoker.value = invoker;
  selectedDetail.value = detail;
  await nextTick();
  detailCloseButton.value?.focus();
}

async function openDetail(submission: ResearchSubmission, event: Event) {
  const invoker = invokerFrom(event);
  error.value = "";
  try {
    await showDetail(await loadResearchDetail(props.apiBase, submission.id), invoker);
  } catch (reason) {
    requestError("详情读取", reason);
  }
}

async function findWallet(event?: Event) {
  const invoker = invokerFrom(event);
  const address = walletLookup.value.trim();
  if (!address) {
    error.value = "请输入完整 TRC20 钱包地址后再精确查找。";
    return;
  }
  error.value = "";
  try {
    await showDetail(await lookupResearchWallet(props.apiBase, address), invoker);
    walletLookup.value = "";
  } catch (reason) {
    requestError("钱包精确查找", reason);
  }
}

function focusableDetailElements() {
  return Array.from(detailDrawer.value?.querySelectorAll<HTMLElement>(
    "button:not([disabled]), [href], input:not([disabled]), select:not([disabled]), textarea:not([disabled]), [tabindex]:not([tabindex='-1'])"
  ) || []).filter(element => !element.hasAttribute("hidden"));
}

function trapDetailFocus(event: KeyboardEvent) {
  if (event.key !== "Tab") return;
  const focusable = focusableDetailElements();
  if (!focusable.length) {
    event.preventDefault();
    return;
  }
  const first = focusable[0];
  const last = focusable[focusable.length - 1];
  if (event.shiftKey && document.activeElement === first) {
    event.preventDefault();
    last.focus();
  } else if (!event.shiftKey && document.activeElement === last) {
    event.preventDefault();
    first.focus();
  }
}

async function closeDetail() {
  selectedDetail.value = null;
  await nextTick();
  detailInvoker.value?.focus();
  detailInvoker.value = null;
}

async function exportCsv() {
  exporting.value = true;
  error.value = "";
  notice.value = "";
  try {
    await downloadResearchCsv(props.apiBase, exportFilters());
    notice.value = "导出完成，文件已开始下载。";
  } catch (reason) {
    requestError("CSV 导出", reason);
  } finally {
    exporting.value = false;
  }
}

async function deleteOne(submission: ResearchSubmission) {
  if (!window.confirm(`确定删除调研记录 ${submission.submissionNumber} 吗？删除后无法恢复。`)) return;
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await deleteResearchSubmission(props.apiBase, submission.id);
    if (submissions.value.length === 1 && filters.page > 0) filters.page--;
    try {
      await refreshAfterMutation();
      notice.value = `已删除调研记录 ${submission.submissionNumber}。`;
    } catch {
      notice.value = `已删除调研记录 ${submission.submissionNumber}，但刷新失败。`;
    }
  } catch (reason) {
    requestError("调研记录删除", reason);
  } finally {
    loading.value = false;
  }
}

async function deleteSelected() {
  const selected = selectedSubmissions.value;
  const ids = selected.map(submission => submission.id);
  if (!ids.length) return;
  const targets = selected.map(submission => submission.submissionNumber).join("、");
  if (!window.confirm(`确定删除以下 ${ids.length} 条调研记录吗？\n${targets}\n删除后无法恢复。`)) return;
  loading.value = true;
  error.value = "";
  notice.value = "";
  try {
    await deleteResearchBatch(props.apiBase, ids);
    if (submissions.value.every(submission => ids.includes(submission.id)) && filters.page > 0) {
      filters.page--;
    }
    try {
      await refreshAfterMutation();
      notice.value = `已删除 ${ids.length} 条已选调研记录。`;
    } catch {
      notice.value = `已删除 ${ids.length} 条已选调研记录，但刷新失败。`;
    }
  } catch (reason) {
    requestError("批量删除", reason);
  } finally {
    loading.value = false;
  }
}

onMounted(() => void loadDashboard());
</script>

<template>
  <section class="research-module" aria-labelledby="research-title">
    <div class="research-title-row">
      <div>
        <small>WEB3 WALLET RESEARCH</small>
        <h1 id="research-title">web3钱包产品调研</h1>
      </div>
      <div class="research-campaign" :class="campaign?.effectiveStatus === 'ACTIVE' ? 'active' : 'paused'">
        <span>活动{{ campaignLabel }}</span>
        <button data-testid="campaign-toggle" type="button" :disabled="loading || !campaignCanToggle" @click="toggleCampaign">
          {{ campaignToggleLabel }}
        </button>
      </div>
    </div>

    <p class="research-description">管理问卷收集记录。列表直接显示完整收款地址，也可通过精确查找、详情和 CSV 导出进行后续处理。</p>
    <p v-if="error" class="admin-error" role="alert">{{ error }}</p>
    <p v-if="campaign && !campaign.dataAvailable" class="admin-error" role="alert">
      加密数据不可用：请配置完整且相互独立的调研密钥后再读取受保护数据。
    </p>
    <p v-if="notice" class="research-notice" role="status">{{ notice }}</p>

    <section class="research-summary" aria-label="调研汇总">
      <article><small>提交总数</small><b>{{ summary.total }}</b></article>
      <article><small>平均评分</small><b>{{ summary.averageRating.toFixed(1) }}</b></article>
      <article><small>主要使用场景</small><b>{{ topDistribution(summary.sceneDistribution) }}</b></article>
      <article><small>首要顾虑</small><b>{{ topDistribution(summary.concernDistribution) }}</b></article>
    </section>

    <section class="research-distributions" aria-label="分布统计">
      <article><h2>评分分布</h2><ul><li v-for="(count, key) in summary.ratingDistribution" :key="key"><span>{{ key }} 分</span><b>{{ count }}</b></li><li v-if="!Object.keys(summary.ratingDistribution).length">暂无数据</li></ul></article>
      <article><h2>场景分布</h2><ul><li v-for="(count, key) in summary.sceneDistribution" :key="key"><span>{{ key }}</span><b>{{ count }}</b></li><li v-if="!Object.keys(summary.sceneDistribution).length">暂无数据</li></ul></article>
      <article><h2>顾虑分布</h2><ul><li v-for="(count, key) in summary.concernDistribution" :key="key"><span>{{ key }}</span><b>{{ count }}</b></li><li v-if="!Object.keys(summary.concernDistribution).length">暂无数据</li></ul></article>
      <article><h2>来源分布</h2><ul><li v-for="(count, key) in summary.sourceDistribution" :key="key"><span>{{ key }}</span><b>{{ count }}</b></li><li v-if="!Object.keys(summary.sourceDistribution).length">暂无数据</li></ul></article>
    </section>

    <section class="research-controls" aria-label="记录筛选和操作">
      <div class="research-filters">
        <label><span>提交编号</span><input v-model="filters.number" data-testid="number-filter" placeholder="SP-..." @keyup.enter="applyFilters"></label>
        <label><span>评分</span><select v-model.number="filters.rating"><option :value="0">全部评分</option><option v-for="value in 5" :key="value" :value="value">{{ value }} 分</option></select></label>
        <label><span>顾虑</span><select v-model="filters.concern"><option value="">全部顾虑</option><option value="FEES">FEES</option><option value="SECURITY">SECURITY</option><option value="REGIONS">REGIONS</option><option value="SPEED">SPEED</option></select></label>
        <label><span>来源</span><select v-model="filters.source" data-testid="source-filter"><option value="">全部来源</option><option value="OPEN_CARD">OPEN_CARD</option><option value="APP_DOWNLOAD">APP_DOWNLOAD</option><option value="FREE_CARD">FREE_CARD</option></select></label>
        <label><span>使用场景</span><select v-model="filters.scene"><option value="">全部场景</option><option value="SUBSCRIPTIONS">SUBSCRIPTIONS</option><option value="SHOPPING">SHOPPING</option><option value="ATM">ATM</option><option value="TRAVEL">TRAVEL</option><option value="GAMING">GAMING</option><option value="ADS">ADS</option></select></label>
        <label><span>开始日期</span><input v-model="filters.from" type="date"></label>
        <label><span>结束日期</span><input v-model="filters.to" type="date"></label>
        <button data-testid="filters-submit" type="button" @click="applyFilters">筛选</button>
        <button class="research-secondary" type="button" @click="clearFilters">清除</button>
      </div>
      <div class="research-actions">
        <label class="research-wallet-lookup"><span>精确查找</span><input v-model="walletLookup" autocomplete="off" placeholder="完整 TRC20 钱包地址" @keyup.enter="findWallet"></label>
        <button type="button" @click="findWallet">查找</button>
        <button data-testid="export-csv" class="research-secondary" type="button" :disabled="exporting" @click="exportCsv">{{ exporting ? "导出中…" : "导出 CSV" }}</button>
        <button data-testid="batch-delete" class="research-delete" type="button" :disabled="!selectedSubmissionIds.length || loading" @click="deleteSelected">删除已选（{{ selectedSubmissionIds.length }}）</button>
      </div>
    </section>

    <p class="research-scroll-tip">← 左右滑动查看完整记录和收款地址；“查看详情”可读取其余受保护信息 →</p>
    <div class="research-table-wrap">
      <table class="research-table">
        <thead><tr><th><input type="checkbox" :checked="allOnPageSelected" aria-label="选择本页调研记录" @change="toggleAll"></th><th>提交编号</th><th>来源</th><th>评分</th><th>场景</th><th>顾虑</th><th>反馈</th><th>完整收款地址</th><th>提交时间</th><th>操作</th></tr></thead>
        <tbody>
          <tr v-for="submission in submissions" :key="submission.id" :data-testid="`research-row-${submission.id}`">
            <td><input v-model="selectedSubmissionIds" :data-testid="`select-submission-${submission.id}`" :value="submission.id" type="checkbox" :aria-label="`选择调研记录 ${submission.submissionNumber}`" @click.stop></td>
            <td><b>{{ submission.submissionNumber }}</b></td><td>{{ submission.source }}</td><td>{{ submission.rating }} 分</td><td>{{ submission.scenes.join("、") }}</td><td>{{ submission.concern }}</td><td>{{ display(submission.feedback) }}</td><td class="research-full-wallet">{{ submission.walletAddress }}</td><td>{{ formatDate(submission.createdAt) }}</td>
            <td class="research-row-actions"><button :data-testid="`research-detail-button-${submission.id}`" class="research-detail" type="button" @click="openDetail(submission, $event)">查看详情</button><button class="research-delete research-delete-one" type="button" :disabled="loading" @click="deleteOne(submission)">删除</button></td>
          </tr>
          <tr v-if="!listLoading && !submissions.length"><td colspan="10" class="research-empty">暂无符合条件的调研记录</td></tr>
        </tbody>
      </table>
    </div>
    <div v-if="total" class="research-pagination">
      <span>共 <b>{{ total }}</b> 条 · 每页 {{ PAGE_SIZE }} 条</span>
      <div><button type="button" :disabled="filters.page === 0 || listLoading" @click="goToPage(filters.page - 1)">← 上一页</button><button v-for="page in pages" :key="page" type="button" :class="{ active: page - 1 === filters.page }" :disabled="listLoading" @click="goToPage(page - 1)">{{ page }}</button><button type="button" :disabled="filters.page >= pages - 1 || listLoading" @click="goToPage(filters.page + 1)">下一页 →</button></div>
    </div>

    <div v-if="selectedDetail" class="admin-drawer-backdrop" @click.self="closeDetail">
      <aside ref="detailDrawer" data-testid="research-detail-drawer" class="admin-drawer research-drawer" role="dialog" aria-modal="true" aria-label="调研记录详情" tabindex="-1" @keydown="trapDetailFocus" @keydown.esc.prevent="closeDetail">
        <button ref="detailCloseButton" data-testid="research-detail-close" class="modal-close" type="button" aria-label="关闭详情" @click="closeDetail">×</button>
        <small>{{ selectedDetail.submissionNumber }}</small><h2>调研记录详情</h2>
        <section><h3>问卷回答</h3><dl><div><dt>来源</dt><dd>{{ selectedDetail.source }}</dd></div><div><dt>评分</dt><dd>{{ selectedDetail.rating }} 分</dd></div><div><dt>使用场景</dt><dd>{{ selectedDetail.scenes.join("、") }}</dd></div><div><dt>主要顾虑</dt><dd>{{ selectedDetail.concern }}</dd></div><div><dt>反馈</dt><dd>{{ display(selectedDetail.feedback) }}</dd></div></dl></section>
        <section><h3>受保护信息</h3><dl><div><dt>网络</dt><dd>{{ selectedDetail.walletNetwork }}</dd></div><div><dt>完整钱包地址</dt><dd class="research-full-wallet">{{ selectedDetail.walletAddress }}</dd></div><div><dt>条款版本</dt><dd>{{ selectedDetail.termsVersion }}</dd></div><div><dt>同意时间</dt><dd>{{ formatDate(selectedDetail.consentedAt) }}</dd></div><div><dt>提交时间</dt><dd>{{ formatDate(selectedDetail.createdAt) }}</dd></div></dl></section>
      </aside>
    </div>
  </section>
</template>
