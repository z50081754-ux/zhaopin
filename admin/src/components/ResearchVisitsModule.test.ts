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
  operating_system: "iOS",
  operating_system_version: "18.6.2",
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
    expect(wrapper.findAll("thead th").map(cell => cell.text())).toContain("操作系统");
    expect(wrapper.findAll("thead th").map(cell => cell.text())).toContain("系统版本");
    expect(wrapper.get("[data-testid='research-visit-row-7']").text()).toContain("iOS");
    expect(wrapper.get("[data-testid='research-visit-row-7']").text()).toContain("18.6.2");
    expect(wrapper.text()).not.toContain("iPhone");
    expect(wrapper.text()).toContain("Safari");
    expect(wrapper.text()).toContain("泰国 · 203.0.113.7");
    expect(wrapper.text()).not.toContain("TH · 203.0.113.7");
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

  it("loads page one so the twenty-first research visit is reachable, then returns to page zero", async () => {
    const pageOne = { visits: [{ ...visit, id: 21, entry_path: "/research/21" }], total: 21, pages: 2 };
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(summary);
      return response(new URL(url, "https://admin.test").searchParams.get("page") === "1" ? pageOne : { ...list, total: 21, pages: 2 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 1 / 2 页 · 共 21 条");
    expect(wrapper.find("nav[aria-label='有效浏览分页']").exists()).toBe(true);
    expect(wrapper.get("[data-testid='visits-page-status']").attributes("aria-live")).toBe("polite");
    expect(wrapper.get("[data-testid='visits-previous']").attributes("disabled")).toBeDefined();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenLastCalledWith(
      "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
      expect.objectContaining({ credentials: "include" })
    );
    expect(wrapper.get("[data-testid='research-visit-row-21']").text()).toContain("/research/21");
    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 2 / 2 页 · 共 21 条");
    expect(wrapper.get("[data-testid='visits-next']").attributes("disabled")).toBeDefined();

    await wrapper.get("[data-testid='visits-previous']").trigger("click");
    await flushPromises();
    expect(fetch).toHaveBeenLastCalledWith(initialListUrl, expect.objectContaining({ credentials: "include" }));
    expect(wrapper.find("[data-testid='research-visit-row-7']").exists()).toBe(true);
  });

  it("resets a later page to zero when applying filters while preserving the filter values", async () => {
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(summary);
      return response({ ...list, total: 21, pages: 2 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    await wrapper.get("[data-testid='visits-min-duration']").setValue("60");
    await wrapper.get("[data-testid='visits-submitted']").setValue("false");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenLastCalledWith(
      "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=60&maxDurationSeconds=86400&submittedResearch=false",
      expect.objectContaining({ credentials: "include" })
    );
    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 1 / 2 页");
  });

  it("keeps edited draft filters out of pagination until search applies their normalized snapshot", async () => {
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(summary);
      return response({ ...list, total: 41, pages: 3 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    await wrapper.get("[data-testid='visits-from']").setValue("2026-08-01");
    await wrapper.get("[data-testid='visits-to']").setValue("2026-08-29");
    await wrapper.get("[data-testid='visits-min-duration']").setValue("60");
    await wrapper.get("[data-testid='visits-max-duration']").setValue("");
    await wrapper.get("[data-testid='visits-submitted']").setValue("false");
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenLastCalledWith(
      "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
      expect.objectContaining({ credentials: "include" })
    );
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();
    expect(fetch).toHaveBeenLastCalledWith(
      "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=60&maxDurationSeconds=86400&submittedResearch=false&from=2026-08-01&to=2026-08-29",
      expect.objectContaining({ credentials: "include" })
    );
  });

  it("corrects a delayed out-of-range page exactly once and ignores its late result after a new search", async () => {
    const shrunkenPage = deferred<ReturnType<typeof response>>();
    const correction = deferred<ReturnType<typeof response>>();
    let pageOneRequests = 0;
    const fetch = vi.fn((url: string) => {
      if (url.includes("/summary")) return Promise.resolve(response(summary));
      const requestUrl = new URL(url, "https://admin.test");
      const requestedPage = requestUrl.searchParams.get("page");
      const minDuration = requestUrl.searchParams.get("minDurationSeconds");
      if (minDuration === "60") return Promise.resolve(response({ visits: [{ ...visit, id: 60, duration_seconds: 60 }], total: 1, pages: 1 }));
      if (requestedPage === "2") return shrunkenPage.promise;
      if (requestedPage === "1") {
        pageOneRequests += 1;
        return pageOneRequests === 1
          ? Promise.resolve(response({ visits: [{ ...visit, id: 21 }], total: 41, pages: 3 }))
          : correction.promise;
      }
      return Promise.resolve(response({ ...list, total: 41, pages: 3 }));
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    shrunkenPage.resolve(response({ visits: [], total: 21, pages: 2 }));
    await flushPromises();

    const listUrlsAfterCorrection = fetch.mock.calls.map(([url]) => String(url)).filter(url => url.includes("/api/admin/visits?"));
    expect(listUrlsAfterCorrection).toEqual([
      "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
      "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
      "/api/admin/visits?systemCode=research&page=2&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
      "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all"
    ]);

    await wrapper.get("[data-testid='visits-min-duration']").setValue("60");
    await wrapper.get("[data-testid='visits-search']").trigger("click");
    await flushPromises();
    correction.resolve(response({ visits: [{ ...visit, id: 99, entry_path: "/stale-correction" }], total: 21, pages: 2 }));
    await flushPromises();

    expect(wrapper.find("[data-testid='research-visit-row-60']").exists()).toBe(true);
    expect(wrapper.text()).not.toContain("/stale-correction");
  });

  it("resets to page zero with disabled controls when a page request fails", async () => {
    let pageOne = false;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(summary);
      if (pageOne) return response({}, 500);
      return response({ ...list, total: 21, pages: 2 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    pageOne = true;
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 0 / 0 页 · 共 0 条");
    expect(wrapper.get("[data-testid='visits-previous']").attributes("disabled")).toBeDefined();
    expect(wrapper.get("[data-testid='visits-next']").attributes("disabled")).toBeDefined();
  });

  it("commits a successful empty list with its nonzero summary and no corrective request", async () => {
    const emptySummary = { todayEffective: 9, averageDurationSeconds: 45, maxDurationSeconds: 90, submittedCount: 4, conversionRate: 44.44 };
    const fetch = vi.fn(async (url: string) => response(url.includes("/summary") ? emptySummary : { visits: [], total: 0, pages: 0 }));
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();

    expect(wrapper.findAll(".research-visits-summary article").map(card => card.text()))
      .toEqual(["今日有效浏览9", "平均停留45 秒", "最长停留1 分 30 秒", "已提交问卷4", "提交转化率44.44%"]);
    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 0 / 0 页 · 共 0 条");
    expect(wrapper.get("[data-testid='visits-previous']").attributes("disabled")).toBeDefined();
    expect(wrapper.get("[data-testid='visits-next']").attributes("disabled")).toBeDefined();
    expect(fetch.mock.calls.filter(([url]) => String(url).includes("/api/admin/visits?")).length).toBe(1);
  });

  it("keeps the empty-page summary and stops correcting when a later page shrinks to zero", async () => {
    const emptySummary = { todayEffective: 8, averageDurationSeconds: 30, maxDurationSeconds: 60, submittedCount: 3, conversionRate: 37.5 };
    let listCalls = 0;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(listCalls === 0 ? summary : emptySummary);
      listCalls += 1;
      return listCalls === 1 ? response({ ...list, total: 21, pages: 2 }) : response({ visits: [], total: 0, pages: 0 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    expect(wrapper.findAll(".research-visits-summary article").map(card => card.text()))
      .toEqual(["今日有效浏览8", "平均停留30 秒", "最长停留1 分 0 秒", "已提交问卷3", "提交转化率37.5%"]);
    expect(fetch.mock.calls.map(([url]) => String(url)).filter(url => url.includes("/api/admin/visits?")))
      .toEqual([
        "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
        "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all"
      ]);
    expect(wrapper.get("[data-testid='visits-page-status']").text()).toContain("第 0 / 0 页 · 共 0 条");
  });

  it("follows repeated page shrinkage with real corrective requests until page zero", async () => {
    let pageZeroRequests = 0;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/summary")) return response(summary);
      const requestedPage = new URL(url, "https://admin.test").searchParams.get("page");
      if (requestedPage === "2") return response({ visits: [{ ...visit, id: 2, entry_path: "/invalid-page-2" }], total: 21, pages: 2 });
      if (requestedPage === "1") {
        const pageOneCount = fetch.mock.calls.map(([callUrl]) => String(callUrl)).filter(callUrl => callUrl.includes("/api/admin/visits?") && callUrl.includes("page=1")).length;
        return pageOneCount === 1
          ? response({ visits: [{ ...visit, id: 1, entry_path: "/valid-page-1" }], total: 41, pages: 3 })
          : response({ visits: [{ ...visit, id: 10, entry_path: "/invalid-page-1" }], total: 1, pages: 1 });
      }
      pageZeroRequests += 1;
      return pageZeroRequests === 1
        ? response({ visits: [{ ...visit, id: 0, entry_path: "/initial-page-0" }], total: 41, pages: 3 })
        : response({ visits: [{ ...visit, id: 0, entry_path: "/final-page-0" }], total: 1, pages: 1 });
    });
    vi.stubGlobal("fetch", fetch);
    const wrapper = mount(ResearchVisitsModule, { props: { apiBase: "" } });
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();
    await wrapper.get("[data-testid='visits-next']").trigger("click");
    await flushPromises();

    expect(fetch.mock.calls.map(([url]) => String(url)).filter(url => url.includes("/api/admin/visits?")))
      .toEqual([
        "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
        "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
        "/api/admin/visits?systemCode=research&page=2&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
        "/api/admin/visits?systemCode=research&page=1&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all",
        "/api/admin/visits?systemCode=research&page=0&size=20&minDurationSeconds=0&maxDurationSeconds=86400&submittedResearch=all"
      ]);
    expect(wrapper.find("[data-testid='research-visit-row-0']").exists()).toBe(true);
    expect(wrapper.text()).toContain("/final-page-0");
    expect(wrapper.text()).not.toContain("/invalid-page-1");
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
    expect(row).toContain("mobile · Safari");
    expect(row).toContain("zh-CN");
    expect(row).toContain("iOS");
    expect(row).toContain("18.6.2");
    expect(row).not.toContain("iPhone");
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
