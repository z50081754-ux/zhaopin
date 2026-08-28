import { describe, expect, it } from "vitest";
import { parseAdminPath, pathForAdminSystem } from "./systemRoute";

describe("admin subsystem routes", () => {
  it("maps each supported admin path to its subsystem", () => {
    expect(parseAdminPath("/admin/")).toBe("home");
    expect(parseAdminPath("/admin/recruitment")).toBe("recruitment");
    expect(parseAdminPath("/admin/walletcheck/visits")).toBe("walletcheck");
  });

  it("builds the canonical path for each subsystem", () => {
    expect(pathForAdminSystem("home")).toBe("/admin/");
    expect(pathForAdminSystem("recruitment")).toBe("/admin/recruitment");
    expect(pathForAdminSystem("walletcheck")).toBe("/admin/walletcheck/visits");
  });
});
