import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, describe, expect, it, vi } from "vitest";
import ResearchVisitsModule from "./ResearchVisitsModule.vue";

const summary = {
  todayEffective: 3,
  averageDurationSeconds: 125,
  maxDurationSeconds: 240,
  submittedCount: 2,
  conversionRate: 66.67
};

const visit = {
  id: 7,
  visit_id: "research-7",
  started_at: "2026-08-29T01:00:00Z",
  qualified_at: "2026-08-29T01:02:05Z",
  last_seen_at: "2026-08-29T01:04:10Z",
  duration_seconds: 125,
  ip_address: "203.0.113.7",
  entry_path: "/research",
  last_path: "/research/complete",
  device_type: "mobile",
  device_model: "iPhone",
  operating_system: "iOS 18",
  browser_name: "Safari",
  screen_resolution: "1170x2532",
  device_language: "zh-CN",
  device_timezone: "Asia/Bangkok",
  user_agent: "test-agent",
  detected_wallets: "",
  system_code: "research",
  queried_address: false,
  submitted_research: true,
  visitor_country: "TH"
};

const list = { visits: [visit], total: 1, pages: 1 };
const initialListUrl = "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all";

function response(body: object, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => { resolve = done; });
  return { promise, resolve };
}

afterEach(() => vi.unstubAllGlobals());

describe("ResearchVisitsModule", () => {
  it("requests the exact research summary and default list, then renders all visit fields", async () => {
    const fetch = vi.fn<(url: string, options?: RequestInit) => Promise<ReturnType<typeof response>>>(
      async (url: string) => response(url.includes("/summary") ? summary : list)
    );
    vi.stubGlobal("fetch", fetch);

    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    expect(fetch).toHaveBeenCalledWith("/api/admin/visits/summary?systemCode=research", expect.objectContaining({ credentials: "include" }));
    expect(fetch).toHaveBeenCalledWith(initialListUrl, expect.objectContaining({ credentials: "include" }));
    expect(fetch.mock.calls[0][1]?.signal).toBe(fetch.mock.calls[1][1]?.signal);
    expect(wrapper.text()).toContain("今日有效浏览");
    expect(wrapper.text()).toContain("平均停留");
    expect(wrapper.text()).toContain("最长停留");
    expect(wrapper.text()).toContain("已提交问卷");
    expect(wrapper.text()).toContain("提交转化率");
    expect(wrapper.get("[data-testid='research-visit-row-7']").text()).toContain("2 分 5 秒");
    expect(wrapper.text()).toContain("mobile · Safari");
    expect(wrapper.text()).toContain("zh-CN");
    expect(wrapper.text()).not.toContain("iPhone");
    expect(wrapper.text()).toContain("Safari");
    expect(wrapper.text()).toContain("TH · 203.0.113.7");
    expect(wrapper.text()).toContain("已提交");
  });

  it("uses changed optional filters in the next exact list request", async () => {
    const fetch = vi.fn(async (url: string) => response(url.includes("/summary") ? summary : list));
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "/admin-api/" } });
    await flushPromises();

    await wrapper.get("[data-testid='visits-from']").setValue("2026-08-01");
    await wrapper.get("[data-testid='visits-to']").setValue("2026-08-29");
    await wrapper.get("[data-testid='visits-min-duration']").setValue("120");
    await wrapper.get("[data-testid='visits-max-duration']").setValue("360");
    await wrapper.get("[data-testid='visits-submitted']").setValue("false");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenLastCalledWith(
      "/admin-api/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=120&maxDurationSeconds=360&submittedResearch=false&from=2026-08-01&to=2026-08-29",
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("restores the maximum duration default when the optional input is blank", async () => {
    const fetch = vi.fn(async (url: string) => response(url.includes("/summary") ? summary : list));
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='visits-max-duration']").setValue("");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenLastCalledWith(
      initialListUrl,
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("renders the actual metrics and complete research visit fields", async () => {
    const incompleteVisit = { ...visit, submitted_research: false, visitor_country: "UNKNOWN" };
    const fetch = vi.fn(async (url: string) => response(url.includes("/summary") ? summary : {
      visits: [incompleteVisit], total: 1, pages: 1
    }));
    vi.stubGlobal("fetch", fetch);

    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    const cards = wrapper.findAll(".research-visits-summary article").map(card => card.text());
    expect(cards).toEqual(["今日有效浏览3", "平均停留2 分 5 秒", "最长停留4 分 0 秒", "已提交问卷2", "提交转化率66.67%"]);
    const row = wrapper.get("[data-testid='research-visit-row-7']").text();
    expect(row).toContain("2026/8/29 08:00:00");
    expect(row).toContain("2026/8/29 08:04:10");
    expect(row).toContain("/research");
    expect(row).toContain("iPhone · mobile");
    expect(row).toContain("Safari");
    expect(row).toContain("未知 · 203.0.113.7");
    expect(row).toContain("未提交");
  });

  it("emits unauthorized and clears stale metrics and rows after a 401", async () => {
    let request = 0;
    const fetch = vi.fn(async (url: string) => {
      request++;
      return request <= 2 ? response(url.includes("/summary") ? summary : list) : response({}, 401);
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    expect(wrapper.text()).toContain("2 分 5 秒");

    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();

    expect(wrapper.emitted("unauthorized")).toHaveLength(1);
    expect(wrapper.text()).not.toContain("2 分 5 秒");
    expect(wrapper.text()).not.toContain("66.67%");
  });

  it("keeps only the latest generation when an older search resolves late", async () => {
    const stale = deferred<ReturnType<typeof response>>();
    const latest = deferred<ReturnType<typeof response>>();
    let listCalls = 0;
    const fetch = vi.fn((url: string) => {
      if (url.includes("/summary")) return Promise.resolve(response(summary));
      listCalls++;
      if (listCalls === 1) return Promise.resolve(response(list));
      return listCalls === 2 ? stale.promise : latest.promise;
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='visits-min-duration']").setValue("30");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await wrapper.get("[data-testid='visits-min-duration']").setValue("60");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    latest.resolve(response({ visits: [{ ...visit, id: 9, duration_seconds: 60 }], total: 1, pages: 1 }));
    await flushPromises();
    stale.resolve(response({ visits: [{ ...visit, id: 8, duration_seconds: 30 }], total: 1, pages: 1 }));
    await flushPromises();

    expect(wrapper.text()).toContain("1 分 0 秒");
    expect(wrapper.text()).not.toContain("30 秒");
  });

  it("aborts its shared request controller on a new search and unmount", async () => {
    const fetch = vi.fn<(url: string, options?: RequestInit) => Promise<never>>(
      () => new Promise<never>(() => {})
    );
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    const initialSignal = fetch.mock.calls[0][1]?.signal as AbortSignal;

    await wrapper.get("[data-testid='visits-search']").trigger("click");
    expect(initialSignal.aborted).toBe(true);
    const currentSignal = fetch.mock.calls.at(-1)?.[1]?.signal as AbortSignal;
    wrapper.unmount();
    expect(currentSignal.aborted).toBe(true);
  });
});
