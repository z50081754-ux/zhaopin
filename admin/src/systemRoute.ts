export type AdminSystem = "home" | "recruitment" | "walletcheck";

export function parseAdminPath(pathname: string): AdminSystem {
  if (pathname.startsWith("/admin/walletcheck")) return "walletcheck";
  if (pathname.startsWith("/admin/recruitment")) return "recruitment";
  return "home";
}

export const pathForAdminSystem = (system: AdminSystem) =>
  system === "walletcheck" ? "/admin/walletcheck/visits"
    : system === "recruitment" ? "/admin/recruitment" : "/admin/";
