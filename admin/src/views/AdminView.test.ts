import { flushPromises, mount, type VueWrapper } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AdminView from "./AdminView.vue";

const emptyApplications = { ok: true, applications: [], total: 0, pages: 0 };
const emptyVisits = { visits: [], total: 0, pages: 0 };
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

function deferred<T>() {
  let resolve!: (value: T) => void;
  const promise = new Promise<T>(done => { resolve = done; });
  return { promise, resolve };
}

function mountAdmin(pathname: string, visitBody: object = emptyVisits) {
  window.history.replaceState({}, "", pathname);
  const fetch = vi.fn(async (url: string) => ({
    ok: true,
    status: 200,
    json: async () => url.includes("/api/admin/visits") ? visitBody : emptyApplications
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
    expect(requestedUrls.filter(url => url.includes("/api/admin/visits") && url.includes("systemCode=walletcheck"))).toHaveLength(2);
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
