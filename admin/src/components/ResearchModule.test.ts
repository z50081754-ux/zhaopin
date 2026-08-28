import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import ResearchModule from "./ResearchModule.vue";
import {
  deleteResearchBatch,
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

function mockInitialLoad() {
  vi.mocked(loadResearchCampaign).mockResolvedValue({
    status: "ACTIVE",
    termsVersion: "2026-08-28",
    updatedAt: "2026-08-28T00:00:00Z"
  });
  vi.mocked(loadResearchSummary).mockResolvedValue({
    total: 3,
    averageRating: 4.3,
    ratingDistribution: { 5: 2 },
    sceneDistribution: { TRAVEL: 2 },
    concernDistribution: { SECURITY: 2 },
    sourceDistribution: { OPEN_CARD: 3 }
  });
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
    vi.mocked(deleteResearchBatch).mockResolvedValue({ ok: true, deleted: 1 });
  });

  afterEach(() => {
    vi.restoreAllMocks();
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

    await wrapper.get("[data-testid='research-row-1']").trigger("click");
    await flushPromises();

    expect(loadResearchDetail).toHaveBeenCalledWith("", 1);
    expect(wrapper.get("[data-testid='research-detail-drawer']").text())
      .toContain("TJRabP1oZkX5wX6u5h5R6xSzz9gYpRTv8");
  });

  it("opens a focused record with the Space key", async () => {
    const wrapper = mount(ResearchModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='research-row-1']").trigger("keydown", { key: " " });
    await flushPromises();

    expect(wrapper.get("[data-testid='research-detail-drawer']").text())
      .toContain("SP-20260828-ABC12345");
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
});
