import { flushPromises, mount } from "@vue/test-utils";
import { afterEach, beforeEach, describe, expect, it, vi } from "vitest";
import AdminView from "./AdminView.vue";

const emptyApplications = { ok: true, applications: [], total: 0, pages: 0 };
const emptyVisits = { visits: [], total: 0, pages: 0 };

function mountAdmin(pathname: string) {
  window.history.replaceState({}, "", pathname);
  const fetch = vi.fn(async (url: string) => ({
    ok: true,
    status: 200,
    json: async () => url.includes("/api/admin/visits") ? emptyVisits : emptyApplications
  }));
  vi.stubGlobal("fetch", fetch);
  return { fetch, wrapper: mount(AdminView) };
}

describe("AdminView subsystem shell", () => {
  beforeEach(() => vi.restoreAllMocks());

  afterEach(() => {
    vi.unstubAllGlobals();
    window.history.replaceState({}, "", "/");
  });

  it("opens WalletCheck from the system home with its own visit data", async () => {
    const { fetch, wrapper } = mountAdmin("/admin/");
    await flushPromises();

    expect(wrapper.text()).toContain("招聘系统");
    expect(wrapper.text()).toContain("WalletCheck");

    await wrapper.get("[data-testid='walletcheck-entry']").trigger("click");
    await flushPromises();

    expect(fetch).toHaveBeenCalledWith(expect.stringContaining("systemCode=walletcheck"), expect.anything());
    expect(wrapper.text()).toContain("查询过地址");
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
});
