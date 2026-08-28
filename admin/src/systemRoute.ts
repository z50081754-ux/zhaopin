export type AdminSystem = "home" | "recruitment" | "walletcheck" | "research";
export type ResearchAdminModule = "visits" | "submissions";

export function parseAdminPath(pathname: string): AdminSystem {
  if (pathname.startsWith("/admin/walletcheck")) return "walletcheck";
  if (pathname.startsWith("/admin/research")) return "research";
  if (pathname.startsWith("/admin/recruitment")) return "recruitment";
  return "home";
}

export const pathForAdminSystem = (system: AdminSystem) =>
  system === "walletcheck" ? "/admin/walletcheck/visits"
    : system === "research" ? "/admin/research/visits"
      : system === "recruitment" ? "/admin/recruitment" : "/admin/";

export function parseResearchModule(pathname: string): ResearchAdminModule {
  return pathname.startsWith("/admin/research/submissions") ? "submissions" : "visits";
}

export const pathForResearchModule = (module: ResearchAdminModule) =>
  module === "submissions" ? "/admin/research/submissions" : "/admin/research/visits";
