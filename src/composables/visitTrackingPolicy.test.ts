import assert from "node:assert/strict";
import { readFileSync } from "node:fs";
import test from "node:test";
// @ts-expect-error Node's strip-types test runner requires explicit .ts extensions.
import { isQualifiedVisit } from "./visitTrackingPolicy.ts";

const adminViewSource = readFileSync(new URL("../../admin/src/views/AdminView.vue", import.meta.url), "utf8");

test("qualifies only after fifteen visible seconds", () => {
  assert.equal(isQualifiedVisit(14), false);
  assert.equal(isQualifiedVisit(15), true);
});

test("describes the fifteen-second qualification threshold in admin copy", () => {
  assert.match(adminViewSource, /页面可见状态下停留满 15 秒后/);
  assert.doesNotMatch(adminViewSource, /页面可见状态下停留满 10 秒后/);
});
