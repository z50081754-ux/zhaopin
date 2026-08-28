import assert from "node:assert/strict";
import test from "node:test";
// @ts-expect-error Node's strip-types test runner requires explicit .ts extensions.
import { isQualifiedVisit } from "./visitTrackingPolicy.ts";

test("qualifies only after fifteen visible seconds", () => {
  assert.equal(isQualifiedVisit(14), false);
  assert.equal(isQualifiedVisit(15), true);
});
