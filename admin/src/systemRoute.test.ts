import { describe, expect, it } from "vitest";
import {
  parseAdminPath,
  parseResearchModule,
  pathForAdminSystem,
  pathForResearchModule
} from "./systemRoute";

describe("admin subsystem routes", () => {
  it("maps each supported admin path to its subsystem", () => {
    expect(parseAdminPath("/admin/")).toBe("home");
    expect(parseAdminPath("/admin/recruitment")).toBe("recruitment");
    expect(parseAdminPath("/admin/walletcheck/visits")).toBe("walletcheck");
    expect(parseAdminPath("/admin/research/visits")).toBe("research");
    expect(parseAdminPath("/admin/research/submissions")).toBe("research");
  });

  it("builds the canonical path for each subsystem", () => {
    expect(pathForAdminSystem("home")).toBe("/admin/");
    expect(pathForAdminSystem("recruitment")).toBe("/admin/recruitment");
    expect(pathForAdminSystem("walletcheck")).toBe("/admin/walletcheck/visits");
    expect(pathForAdminSystem("research")).toBe("/admin/research/visits");
  });

  it("maps research modules to their canonical paths", () => {
    expect(parseResearchModule("/admin/research/visits")).toBe("visits");
    expect(parseResearchModule("/admin/research/submissions")).toBe("submissions");
    expect(pathForResearchModule("submissions")).toBe("/admin/research/submissions");
  });
});
