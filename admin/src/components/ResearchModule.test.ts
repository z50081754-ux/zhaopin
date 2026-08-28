import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ResearchModule from "./ResearchModule.vue";
import {
  deleteResearchBatch,
  deleteResearchSubmission,
  downloadResearchCsv,
  loadResearchCampaign,
  loadResearchDetail,
  loadResearchSubmissions,
  loadResearchSummary,
  updateResearchCampaign
} from "../research/api";

vi.mock("../research/api", () => ({
  loadResearchCampaign: vi.fn(),
  updateResearchCampaign: vi.fn(),
  loadResearchSummary: vi.fn(),
  loadResearchSubmissions: vi.fn(),
  loadResearchDetail: vi.fn(),
  lookupResearchWallet: vi.fn(),
  deleteResearchSubmission: vi.fn(),
  deleteResearchBatch: vi.fn(),
  downloadResearchCsv: vi.fn()
}));

const fixture = {
  id: 1,
  submissionNumber: "SP-20260828-ABC12345",
  source: "OPEN_CARD",
  rating: 5,
  scenes: ["TRAVEL"],
  concern: "SECURITY",
  feedback: "费用透明",
  maskedWalletAddress: "TJRabP••••••pRTv8",
  createdAt: "2026-08-28T00:00:00Z"
};

const detail = {
  ...fixture,
  walletNetwork: "TRC20",
  walletAddress: "TJRabP1oZkX5wX6u5h5R6xSzz9gYpRTv8",
  termsVersion: "2026-08-28",
  consentedAt: "2026-08-28T00:00:00Z"
};

const summaryFixture = {
  total: 3,
  averageRating: 4.3,
  ratingDistribution: { 5: 2 },
  sceneDistribution: { TRAVEL: 2 },
  concernDistribution: { SECURITY: 2 },
  sourceDistribution: { OPEN_CARD: 3 }
};

function mockInitialLoad() {
  vi.mocked(loadResearchCampaign).mockResolvedValue({
    status: "ACTIVE",
    termsVersion: "2026-08-28",
    updatedAt: "2026-08-28T00:00:00Z"
  });
  vi.mocked(loadResearchSummary).mockResolvedValue(summaryFixture);
  vi.mocked(loadResearchSubmissions).mockResolvedValue({
    submissions: [fixture],
    total: 1,
    pages: 1
  });
}

describe("ResearchModule", () => {
  beforeEach(() => {
    vi.clearAllMocks();
    mockInitialLoad();
    vi.mocked(updateResearchCampaign).mockResolvedValue({
      status: "PAUSED",
      termsVersion: "2026-08-28",
      updatedAt: "2026-08-28T00:01:00Z"
    });
    vi.mocked(loadResearchDetail).mockResolvedValue(detail);
    vi.mocked(downloadResearchCsv).mockResolvedValue(undefined);
    vi.mocked(deleteResearchSubmission).mockResolvedValue({ ok: true });
    vi.mocked(deleteResearchBatch).mockResolvedValue({ ok: true, deleted: 1 });
  });

  afterEach(() => {
    vi.restoreAllMocks();
    document.body.replaceChildren();
  });

  it("loads summary and submissions, then pauses the campaign", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    expect(wrapper.text()).toContain("web3钱包产品调研");
    expect(wrapper.text()).toContain("SP-20260828-ABC12345");
    expect(wrapper.text()).toContain("3");

    await wrapper.get("[data-testid='campaign-toggle']").trigger("click");
    await flushPromises();

    expect(updateResearchCampaign).toHaveBeenCalledWith("", "PAUSED");
    expect(wrapper.text()).toContain("已暂停");
  });

  it("applies a submission-number and source filter before reloading the list", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "/api" } });
    await flushPromises();

    await wrapper.get("[data-testid='number-filter']").setValue("SP-20260828");
    await wrapper.get("[data-testid='source-filter']").setValue("OPEN_CARD");
    await wrapper.get("[data-testid='filters-submit']").trigger("click");
    await flushPromises();

    expect(loadResearchSubmissions).toHaveBeenLastCalledWith("/api", {
      number: "SP-20260828",
      rating: 0,
      concern: "",
      source: "OPEN_CARD",
      scene: "",
      from: "",
      to: "",
      page: 0,
      size: 20
    });
    expect(wrapper.text()).toContain("SP-20260828-ABC12345");
  });

  it("keeps wallets masked in the table and reveals the full address only in the opened detail drawer", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    expect(wrapper.text()).toContain("TJRabP••••••pRTv8");
    expect(wrapper.text()).not.toContain("TJRabP1oZkX5wX6u5h5R6xSzz9gYpRTv8");

    await wrapper.get("[data-testid='research-detail-button-1']").trigger("click");
    await flushPromises();

    expect(loadResearchDetail).toHaveBeenCalledWith("", 1);
    expect(wrapper.get("[data-testid='research-detail-drawer']").text())
      .toContain("TJRabP1oZkX5wX6u5h5R6xSzz9gYpRTv8");
  });

  it("opens details through the dedicated keyboard-reachable control", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    const detailButton = wrapper.get("[data-testid='research-detail-button-1']");
    await detailButton.trigger("keydown", { key: " " });
    await detailButton.trigger("click");
    await flushPromises();

    expect(wrapper.get("[data-testid='research-detail-drawer']").text())
      .toContain("SP-20260828-ABC12345");
  });

  it("does not open details when a checkbox is activated with Space", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    const checkbox = wrapper.get("[data-testid='select-submission-1']");
    await checkbox.trigger("keydown", { key: " " });
    await checkbox.setValue(true);
    await flushPromises();

    expect((checkbox.element as HTMLInputElement).checked).toBe(true);
    expect(wrapper.find("[data-testid='research-detail-drawer']").exists()).toBe(false);
  });

  it("does not open details when the delete control is activated with Enter", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(false);
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    const deleteButton = wrapper.get(".research-delete-one");
    await deleteButton.trigger("keydown", { key: "Enter" });
    await deleteButton.trigger("click");
    await flushPromises();

    expect(wrapper.find("[data-testid='research-detail-drawer']").exists()).toBe(false);
  });

  it("moves focus into the detail drawer and contains Tab navigation", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" }, attachTo: document.body });
    await flushPromises();

    const invoker = wrapper.get("[data-testid='research-detail-button-1']");
    await invoker.trigger("click");
    await flushPromises();

    const closeButton = wrapper.get("[data-testid='research-detail-close']");
    expect(document.activeElement).toBe(closeButton.element);
    await closeButton.trigger("keydown", { key: "Tab" });
    expect(document.activeElement).toBe(closeButton.element);
    await closeButton.trigger("keydown", { key: "Tab", shiftKey: true });
    expect(document.activeElement).toBe(closeButton.element);
  });

  it("closes the detail drawer with Escape and restores the exact invoker", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" }, attachTo: document.body });
    await flushPromises();

    const invoker = wrapper.get("[data-testid='research-detail-button-1']");
    await invoker.trigger("click");
    await flushPromises();
    await wrapper.get("[data-testid='research-detail-close']").trigger("keydown", { key: "Escape" });
    await flushPromises();

    expect(wrapper.find("[data-testid='research-detail-drawer']").exists()).toBe(false);
    expect(document.activeElement).toBe(invoker.element);
  });

  it("starts the filtered CSV export from the export control", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "/admin-api" } });
    await flushPromises();

    await wrapper.get("[data-testid='export-csv']").trigger("click");
    await flushPromises();

    expect(downloadResearchCsv).toHaveBeenCalledWith("/admin-api", {
      number: "",
      rating: 0,
      concern: "",
      source: "",
      scene: "",
      from: "",
      to: ""
    });
    expect(wrapper.text()).toContain("导出完成");
  });

  it("deletes only the checked submission after the confirmation names that record", async () => {
    const confirm = vi.spyOn(window, "confirm").mockReturnValue(true);
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='select-submission-1']").setValue(true);
    await wrapper.get("[data-testid='batch-delete']").trigger("click");
    await flushPromises();

    expect(confirm).toHaveBeenCalledWith(expect.stringContaining("SP-20260828-ABC12345"));
    expect(deleteResearchBatch).toHaveBeenCalledWith("", [1]);
    expect(wrapper.get("[data-testid='batch-delete']").attributes("disabled")).toBeDefined();
  });

  it("reports a successful single deletion even when the follow-up refresh fails", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(loadResearchSummary).mockReset();
    vi.mocked(loadResearchSummary)
      .mockResolvedValueOnce(summaryFixture)
      .mockRejectedValueOnce(new Error("刷新连接中断"));
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get(".research-delete-one").trigger("click");
    await flushPromises();

    expect(deleteResearchSubmission).toHaveBeenCalledWith("", 1);
    expect(wrapper.text()).toContain("已删除调研记录 SP-20260828-ABC12345，但刷新失败");
    expect(wrapper.text()).not.toContain("调研记录删除失败");
  });

  it("reports a successful batch deletion even when the follow-up refresh fails", async () => {
    vi.spyOn(window, "confirm").mockReturnValue(true);
    vi.mocked(loadResearchSummary).mockReset();
    vi.mocked(loadResearchSummary)
      .mockResolvedValueOnce(summaryFixture)
      .mockRejectedValueOnce(new Error("刷新连接中断"));
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='select-submission-1']").setValue(true);
    await wrapper.get("[data-testid='batch-delete']").trigger("click");
    await flushPromises();

    expect(deleteResearchBatch).toHaveBeenCalledWith("", [1]);
    expect(wrapper.text()).toContain("已删除 1 条已选调研记录，但刷新失败");
    expect(wrapper.text()).not.toContain("批量删除失败");
  });
});
