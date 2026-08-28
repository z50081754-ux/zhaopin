import { flushPromises, mount, type VueWrapper } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AdminView from "./AdminView.vue";

const emptyApplications = { ok: true, applications: [], total: 0, pages: 0 };
const emptyVisits = { visits: [], total: 0, pages: 0 };
const emptyVisitSummary = {
  todayEffective: 0,
  averageDurationSeconds: 0,
  maxDurationSeconds: 0,
  submittedCount: 0,
  conversionRate: 0
};
const emptyResearchCampaign = {
  status: "ACTIVE",
  effectiveStatus: "ACTIVE",
  intakeEnabled: true,
  dataAvailable: true,
  termsVersion: "2026-08-29",
  updatedAt: "2026-08-29T00:00:00Z"
};
const emptyResearchSummary = {
  total: 0,
  averageRating: 0,
  ratingDistribution: {},
  sceneDistribution: {},
  concernDistribution: {},
  sourceDistribution: {}
};
const emptyResearchSubmissions = { submissions: [], total: 0, pages: 0 };
let wrappers: VueWrapper[] = [];

function trackWrapper(wrapper: VueWrapper) {
  wrappers.push(wrapper);
  return wrapper;
}

function response(body: object, status = 200) {
  return { ok: status >= 200 && status < 300, status, json: async () => body };
}

function visitResponse(id: number, entryPath: string, systemCode: "recruitment" | "walletcheck") {
  return {
    visits: [{
      id,
      visit_id: `visit-${id}`,
      started_at: "2026-08-28T00:00:00Z",
      qualified_at: "2026-08-28T00:00:15Z",
      last_seen_at: "2026-08-28T00:00:15Z",
      duration_seconds: 15,
      ip_address: "127.0.0.1",
      entry_path: entryPath,
      last_path: entryPath,
      device_type: "desktop",
      device_model: "Test PC",
      operating_system: "Test OS",
      browser_name: "Test Browser",
      screen_resolution: "1920x1080",
      device_language: "zh-CN",
      device_timezone: "Asia/Bangkok",
      user_agent: "test-agent",
      detected_wallets: systemCode === "walletcheck" ? "Phantom" : "",
      system_code: systemCode,
      queried_address: systemCode === "walletcheck"
    }],
    total: 1,
    pages: 1
  };
}

function applicationResponse(id: number) {
  return {
    ok: true,
    applications: [{
      id,
      application_no: `APP-${id}`,
      resume_name: "stale candidate",
      telegram: "@stale",
      gender: "female",
      age: "28",
      birth_date: "1998-01-01",
      nationality_country: "TH",
      job_title: "Engineer",
      current_salary: "2000",
      referrer: "",
      remarks: "",
      expected_salary: "3000",
      bc_experience: "yes",
      employment_status: "employed",
      education_type: "bachelor",
      school: "Test University",
      education_period: "2016-2020",
      passport_status: "valid",
      visa_status: "valid",
      interview_time: "any",
      start_time: "2026-09-01",
      current_country: "TH",
      preferred_country: "TH",
      stage: "new",
      is_possible_duplicate: 0,
      original_filename: "resume.pdf",
      resume_size: 1024,
      ip_address: "127.0.0.1",
      device_type: "desktop",
      device_model: "Test PC",
      operating_system: "Test OS",
      browser_name: "Test Browser",
      screen_resolution: "1920x1080",
      device_language: "zh-CN",
      device_timezone: "Asia/Bangkok",
      user_agent: "test-agent",
      created_at: "2026-08-28T00:00:00Z"
    }],
    total: 1,
    pages: 1
  };
}

function jobResponse(id: number) {
  return [{
    id,
    slug: `stale-job-${id}`,
    title: "stale job",
    category: "技术岗位",
    businessUnit: "XW",
    requiredLocation: "泰国",
    workMode: "远程",
    salaryRange: "2000-3000",
    internationalSalaryRange: "3000-4500",
    summary: "stale",
    responsibilities: ["stale"],
    requirements: ["stale"],
    bonus: [],
    status: "open",
    recruitmentCount: 1,
    updatedAt: "2026-08-28T00:00:00Z"
  }];
}

function deferred<T>() {
  let resolve!: (value: T) => void;
  let reject!: (reason?: unknown) => void;
  const promise = new Promise<T>((done, fail) => { resolve = done; reject = fail; });
  return { promise, reject, resolve };
}

function setupValue<T>(wrapper: VueWrapper, name: string): T {
  return (wrapper.vm as unknown as { $: { setupState: Record<string, T> } }).$.setupState[name];
}

function mountAdmin(pathname: string, visitBody: object = emptyVisits) {
  window.history.replaceState({}, "", pathname);
  const fetch = vi.fn(async (url: string) => ({
    ok: true,
    status: 200,
    json: async () => url.includes("/api/admin/session")
      ? { account: "admin" }
      : url.includes("/api/admin/research/campaign") ? emptyResearchCampaign
        : url.includes("/api/admin/research/summary") ? emptyResearchSummary
          : url.includes("/api/admin/research/submissions") ? emptyResearchSubmissions
            : url.includes("/api/admin/visits/summary") ? emptyVisitSummary
      : url.includes("/api/admin/visits") ? visitBody : emptyApplications
  }));
  vi.stubGlobal("fetch", fetch);
  return { fetch, wrapper: trackWrapper(mount(AdminView)) };
}

describe("AdminView subsystem shell", () => {
  beforeEach(() => vi.restoreAllMocks());

  afterEach(() => {
    for (const wrapper of wrappers) wrapper.unmount();
    wrappers = [];
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/");
  });

  it("opens the research subsystem from the system home without loading recruitment data", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/");
    await flushPromises();

    expect(wrapper.find("[data-testid='research-entry']").exists()).toBe(true);
    await wrapper.get("[data-testid='research-entry']").trigger("click");
    await flushPromises();

    expect(window.location.pathname).toBe("/admin/research/visits");
    expect(wrapper.text()).toContain("Web3 钱包产品调研");
    expect(wrapper.text()).toContain("有效浏览");
    expect(wrapper.text()).not.toContain("候选人管理");
    expect(wrapper.text()).not.toContain("招聘岗位");
    expect(wrapper.text()).not.toContain("官网模板");
    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings)/.test(url))).toBe(false);
    expect(requestedUrls.some(url => url === "/api/admin/visits/summary?systemCode=research")).toBe(true);
    expect(requestedUrls.some(url => url.includes("/api/admin/visits?systemCode=research"))).toBe(true);
  });

  it("renders research submissions without loading recruitment or WalletCheck data", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/research/submissions");
    await flushPromises();

    expect(wrapper.find(".research-module").exists()).toBe(true);
    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings|visits)/.test(url))).toBe(false);
  });

  it("keeps research visit and submission history isolated from other admin APIs", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/research/visits");
    await flushPromises();

    const researchNavigation = wrapper.get(".admin-modules");
    expect(researchNavigation.text()).toContain("有效浏览");
    expect(researchNavigation.text()).toContain("调研记录");
    expect(wrapper.find(".research-visits-module").exists()).toBe(true);
    expect(wrapper.text()).toContain("查看 SakuraPay 调研访客的有效停留");
    expect(wrapper.get(".admin-header-actions a").attributes("href")).toBe("https://sakurapay.xw-company.com/");

    await researchNavigation.get("button:nth-child(2)").trigger("click");
    await flushPromises();
    expect(window.location.pathname).toBe("/admin/research/submissions");
    expect(wrapper.find(".research-module").exists()).toBe(true);

    window.history.replaceState({}, "", "/admin/research/visits");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();
    expect(wrapper.find(".research-visits-module").exists()).toBe(true);
    expect(wrapper.find(".research-module").exists()).toBe(false);

    window.history.replaceState({}, "", "/admin/research/submissions");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();
    expect(wrapper.find(".research-module").exists()).toBe(true);
    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings)/.test(url))).toBe(false);
    expect(requestedUrls.some(url => url === "/api/admin/visits/summary?systemCode=research")).toBe(true);
    expect(requestedUrls.some(url => url.includes("/api/admin/visits?systemCode=research"))).toBe(true);
  });

  it("keeps research out of the recruitment navigation", async () => {
    const { wrapper } = mountAdmin("/admin/recruitment");
    await flushPromises();

    expect(wrapper.text()).not.toContain("web3钱包产品调研");
  });

  it("opens WalletCheck from the system home with its own visit data", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/", visitResponse(9, "/wallet/:address", "walletcheck"));
    await flushPromises();

    expect(wrapper.text()).toContain("招聘系统");
    expect(wrapper.text()).toContain("WalletCheck");

    await wrapper.get("[data-testid='walletcheck-entry']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenCalledWith(expect.stringContaining("systemCode=walletcheck"), expect.anything());
    expect(wrapper.text()).toContain("查询过地址");
    expect(wrapper.text()).toContain("地址查询转化");
    expect(wrapper.text()).not.toContain("检测到的钱包");
    expect(wrapper.text()).not.toContain("Phantom");
    expect(wrapper.text()).not.toContain("候选人管理");

    await wrapper.get("[data-testid='system-home']").trigger("click");
    expect(wrapper.text()).toContain("系统管理");
  });

  it("keeps recruitment visits separate from WalletCheck address data", async () => {
    const { wrapper } = mountAdmin("/admin/recruitment");
    await flushPromises();

    expect(wrapper.text()).toContain("候选人管理");
    await wrapper.get(".admin-modules button:nth-child(2)").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("有效浏览");
    expect(wrapper.text()).not.toContain("查询过地址");
  });

  it("loads only WalletCheck visits for a direct WalletCheck session", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/walletcheck/visits");
    await flushPromises();

    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(wrapper.text()).toContain("WalletCheck 有效浏览");
    expect(requestedUrls.some(url => url.includes("/api/admin/visits") && url.includes("systemCode=walletcheck"))).toBe(true);
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings)/.test(url))).toBe(false);
  });

  it("loads only WalletCheck visits after WalletCheck login", async () => {
    let sessionAuthenticated = false;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/login")) {
        sessionAuthenticated = true;
        return { ok: true, status: 200, json: async () => ({ ok: true }) };
      }
      if (url.includes("/api/admin/session") && sessionAuthenticated) {
        return response({ account: "admin" });
      }
      if (!sessionAuthenticated) return { ok: false, status: 401, json: async () => ({}) };
      return { ok: true, status: 200, json: async () => url.includes("/api/admin/visits") ? emptyVisits : emptyApplications };
    });
    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get("input[autocomplete='username']").setValue("admin");
    await wrapper.get("input[autocomplete='current-password']").setValue("secret");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(wrapper.text()).toContain("WalletCheck 有效浏览");
    expect(requestedUrls.filter(url => url.includes("/api/admin/visits") && url.includes("systemCode=walletcheck"))).toHaveLength(1);
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings)/.test(url))).toBe(false);
  });

  it("shows the authenticated account, returns home from both systems, and logs out", async () => {
    let sessionAuthenticated = false;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/login")) {
        sessionAuthenticated = true;
        return response({ ok: true });
      }
      if (url.includes("/api/admin/logout")) {
        sessionAuthenticated = false;
        return response({}, 204);
      }
      if (url.includes("/api/admin/session") && sessionAuthenticated) {
        return response({ account: "admin" });
      }
      if (!sessionAuthenticated) return response({}, 401);
      return response(url.includes("/api/admin/visits") ? emptyVisits : emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get("input[autocomplete='username']").setValue("admin");
    await wrapper.get("input[autocomplete='current-password']").setValue("secret");
    await wrapper.get("form").trigger("submit");
    await flushPromises();

    expect(wrapper.get("[data-testid='current-account']").text()).toContain("admin");
    await wrapper.get("[data-testid='system-home']").trigger("click");
    await wrapper.get("[data-testid='recruitment-entry']").trigger("click");
    await flushPromises();
    expect(wrapper.find("[data-testid='system-home']").exists()).toBe(true);

    await wrapper.get("[data-testid='logout']").trigger("click");
    await flushPromises();
    expect(fetch).toHaveBeenCalledWith(expect.stringContaining("/api/admin/logout"), expect.objectContaining({ method: "POST" }));
    expect(wrapper.text()).toContain("安全登录");
  });

  it("reloads recruitment visits and rejects a stale WalletCheck response", async () => {
    const staleWallet = deferred<ReturnType<typeof response>>();
    const latestRecruitment = deferred<ReturnType<typeof response>>();
    let recruitmentVisitCalls = 0;
    const fetch = vi.fn(async (url: string) => {
      if (!url.includes("/api/admin/visits")) return response(emptyApplications);
      if (url.includes("systemCode=walletcheck")) return staleWallet.promise;
      recruitmentVisitCalls += 1;
      if (recruitmentVisitCalls === 1) return response(visitResponse(1, "/initial-recruitment", "recruitment"));
      return latestRecruitment.promise;
    });
    window.history.replaceState({}, "", "/admin/recruitment");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();
    await wrapper.get(".admin-modules button:nth-child(2)").trigger("click");
    await flushPromises();

    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();
    window.history.replaceState({}, "", "/admin/");
    window.dispatchEvent(new PopStateEvent("popstate"));
    window.history.replaceState({}, "", "/admin/recruitment");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();

    expect(recruitmentVisitCalls).toBe(2);
    expect(wrapper.text()).not.toContain("/initial-recruitment");
    latestRecruitment.resolve(response(visitResponse(2, "/latest-recruitment", "recruitment")));
    await flushPromises();
    expect(wrapper.text()).toContain("/latest-recruitment");

    staleWallet.resolve(response(visitResponse(3, "/stale-wallet", "walletcheck")));
    await flushPromises();
    expect(wrapper.text()).toContain("/latest-recruitment");
    expect(wrapper.text()).not.toContain("/stale-wallet");
  });

  it("releases a pending visit request when returning home and ignores its late response", async () => {
    const pendingVisit = deferred<ReturnType<typeof response>>();
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/visits")) return pendingVisit.promise;
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get("[data-testid='walletcheck-entry']").trigger("click");
    expect(setupValue<boolean>(wrapper, "loading")).toBe(true);
    await wrapper.get("[data-testid='system-home']").trigger("click");

    expect(wrapper.text()).toContain("系统管理");
    expect(setupValue<boolean>(wrapper, "loading")).toBe(false);

    await wrapper.get("[data-testid='recruitment-entry']").trigger("click");
    await flushPromises();
    expect(wrapper.find(".admin-empty").exists()).toBe(true);

    pendingVisit.resolve(response(visitResponse(41, "/stale-home-visit", "walletcheck")));
    await flushPromises();
    expect(wrapper.text()).toContain("候选人管理");
    expect(wrapper.text()).not.toContain("/stale-home-visit");
    expect(wrapper.text()).not.toContain("有效浏览数据加载失败。");
  });

  it("keeps jobs loading and error ownership when a previous visit request rejects", async () => {
    const pendingVisit = deferred<ReturnType<typeof response>>();
    const pendingJobs = deferred<ReturnType<typeof response>>();
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/visits")) return pendingVisit.promise;
      if (url.includes("/api/admin/jobs")) return pendingJobs.promise;
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/recruitment");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get(".admin-modules button:nth-child(2)").trigger("click");
    await wrapper.get(".admin-modules button:nth-child(3)").trigger("click");
    pendingVisit.reject(new Error("late visit failure"));
    await flushPromises();

    expect(wrapper.text()).toContain("招聘岗位");
    expect(wrapper.text()).not.toContain("有效浏览数据加载失败。");
    expect(wrapper.find(".admin-job-empty").exists()).toBe(false);
    expect(setupValue<boolean>(wrapper, "loading")).toBe(true);

    pendingJobs.resolve(response([]));
    await flushPromises();
    expect(wrapper.find(".admin-job-empty").exists()).toBe(true);
  });

  it("restores the exact authenticated principal and loads only the visible subsystem", async () => {
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored.operator" });
      if (url.includes("/api/admin/visits")) return response(emptyVisits);
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    expect(wrapper.get("[data-testid='current-account']").text()).toContain("restored.operator");
    const requestedUrls = fetch.mock.calls.map(([url]) => String(url));
    expect(requestedUrls.some(url => url.includes("/api/admin/session"))).toBe(true);
    expect(requestedUrls.some(url => url.includes("/api/admin/visits") && url.includes("systemCode=walletcheck"))).toBe(true);
    expect(requestedUrls.some(url => /\/api\/admin\/(applications|jobs|site-settings)/.test(url))).toBe(false);
  });

  it("keeps a failed logout authoritative after the current jobs request resolves", async () => {
    const pendingJobs = deferred<ReturnType<typeof response>>();
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/logout")) return response({}, 500);
      if (url.includes("/api/admin/jobs")) return pendingJobs.promise;
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/recruitment");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get(".admin-modules button:nth-child(3)").trigger("click");
    await wrapper.get("[data-testid='logout']").trigger("click");
    await flushPromises();

    expect(wrapper.text()).toContain("退出登录失败，请稍后重试。");
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(true);
    expect(setupValue<boolean>(wrapper, "loading")).toBe(false);

    pendingJobs.resolve(response(jobResponse(77)));
    await flushPromises();

    expect(wrapper.text()).toContain("退出登录失败，请稍后重试。");
    expect(setupValue<unknown[]>(wrapper, "jobs")).toEqual([]);
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(true);
    expect(setupValue<boolean>(wrapper, "loading")).toBe(false);
  });

  it("keeps a failed logout authoritative after the current visit request rejects", async () => {
    const pendingVisits = deferred<ReturnType<typeof response>>();
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/logout")) return response({}, 500);
      if (url.includes("/api/admin/visits")) return pendingVisits.promise;
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    await wrapper.get("[data-testid='logout']").trigger("click");
    await flushPromises();
    pendingVisits.reject(new Error("late visit failure"));
    await flushPromises();

    expect(wrapper.text()).toContain("退出登录失败，请稍后重试。");
    expect(wrapper.text()).not.toContain("有效浏览数据加载失败。");
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(true);
    expect(setupValue<boolean>(wrapper, "loading")).toBe(false);
  });

  it("makes logout single-flight and disables its button until success", async () => {
    const pendingLogout = deferred<ReturnType<typeof response>>();
    let logoutRequests = 0;
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/logout")) {
        logoutRequests += 1;
        return pendingLogout.promise;
      }
      return response(emptyApplications);
    });
    window.history.replaceState({}, "", "/admin/");
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();

    const logoutButton = wrapper.get("[data-testid='logout']");
    await logoutButton.trigger("click");
    expect.soft(logoutButton.attributes("disabled")).toBeDefined();
    await logoutButton.trigger("click");
    expect(logoutRequests).toBe(1);

    pendingLogout.resolve(response({}, 204));
    await flushPromises();
    expect(wrapper.text()).toContain("安全登录");
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(false);
  });

  it.each([
    {
      name: "applications",
      path: "/admin/recruitment",
      endpoint: "/api/admin/applications",
      activate: "",
      staleBody: applicationResponse(81),
      stateName: "applications",
      clearedValue: []
    },
    {
      name: "visits",
      path: "/admin/walletcheck/visits",
      endpoint: "/api/admin/visits",
      activate: "",
      staleBody: visitResponse(82, "/stale-direct-visit", "walletcheck"),
      stateName: "visits",
      clearedValue: []
    },
    {
      name: "jobs",
      path: "/admin/recruitment",
      endpoint: "/api/admin/jobs",
      activate: ".admin-modules button:nth-child(3)",
      staleBody: jobResponse(83),
      stateName: "jobs",
      clearedValue: []
    },
    {
      name: "templates",
      path: "/admin/recruitment",
      endpoint: "/api/admin/site-settings",
      activate: ".admin-modules button:nth-child(4)",
      staleBody: { activeTemplate: "apple", defaultLanguage: "en" },
      stateName: "activeTemplate",
      clearedValue: "technology"
    }
  ])("prevents the current $name loader from writing while successful logout is in flight", async testCase => {
    const pendingLoader = deferred<ReturnType<typeof response>>();
    const pendingLogout = deferred<ReturnType<typeof response>>();
    const fetch = vi.fn(async (url: string) => {
      if (url.includes("/api/admin/session")) return response({ account: "restored-admin" });
      if (url.includes("/api/admin/logout")) return pendingLogout.promise;
      if (url.includes(testCase.endpoint)) return pendingLoader.promise;
      if (url.includes("/api/admin/applications")) return response(emptyApplications);
      if (url.includes("/api/admin/visits")) return response(emptyVisits);
      if (url.includes("/api/admin/jobs")) return response([]);
      if (url.includes("/api/admin/site-settings")) return response({ activeTemplate: "technology", defaultLanguage: "auto" });
      return response({ ok: true });
    });
    window.history.replaceState({}, "", testCase.path);
    vi.stubGlobal("fetch", fetch);
    const wrapper = trackWrapper(mount(AdminView));
    await flushPromises();
    if (testCase.activate) await wrapper.get(testCase.activate).trigger("click");

    await wrapper.get("[data-testid='logout']").trigger("click");
    pendingLoader.resolve(response(testCase.staleBody));
    await flushPromises();

    expect(setupValue<unknown>(wrapper, testCase.stateName)).toEqual(testCase.clearedValue);
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(true);

    pendingLogout.resolve(response({}, 204));
    await flushPromises();
    expect(wrapper.text()).toContain("安全登录");
    expect(setupValue<unknown>(wrapper, testCase.stateName)).toEqual(testCase.clearedValue);
    expect(setupValue<boolean>(wrapper, "authenticated")).toBe(false);
  });

  it("synchronizes the displayed subsystem when browser history changes", async () => {
    const { wrapper } = mountAdmin("/admin/");
    await flushPromises();

    window.history.replaceState({}, "", "/admin/walletcheck/visits");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();
    expect(wrapper.text()).toContain("WalletCheck 有效浏览");

    window.history.replaceState({}, "", "/admin/recruitment");
    window.dispatchEvent(new PopStateEvent("popstate"));
    await flushPromises();
    expect(wrapper.text()).toContain("候选人管理");
    expect(wrapper.text()).not.toContain("查询过地址");
  });
});
