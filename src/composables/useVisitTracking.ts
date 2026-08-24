import { onBeforeUnmount, onMounted } from "vue";
import { apiUrl } from "../utils/api";
import { collectDeviceInfo } from "../utils/deviceInfo";

const VISIT_ID_KEY = "xw-visit-id";
const VISIT_SECONDS_KEY = "xw-visit-visible-seconds";
const VISIT_ENTRY_KEY = "xw-visit-entry-path";
const QUALIFIED_SECONDS = 15;
const HEARTBEAT_SECONDS = 10;

function currentPath() {
  return `${location.pathname}${location.search}`.slice(0, 500);
}

function visitId() {
  const existing = sessionStorage.getItem(VISIT_ID_KEY);
  if (existing) return existing;
  const id = crypto.randomUUID();
  sessionStorage.setItem(VISIT_ID_KEY, id);
  return id;
}

export function useVisitTracking() {
  let timer: number | undefined;
  let seconds = Number(sessionStorage.getItem(VISIT_SECONDS_KEY) || 0);
  let qualified = seconds >= QUALIFIED_SECONDS;
  let lastSent = qualified ? seconds : 0;
  const id = visitId();
  const entryPath = sessionStorage.getItem(VISIT_ENTRY_KEY) || currentPath();
  sessionStorage.setItem(VISIT_ENTRY_KEY, entryPath);

  const post = (path: string, body: object, keepalive = false) =>
    fetch(apiUrl(path), {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify(body),
      keepalive,
    }).catch(() => undefined);

  const qualify = () => {
    qualified = true;
    lastSent = seconds;
    return post("/api/visits", {
      visitId: id,
      durationSeconds: seconds,
      entryPath,
      lastPath: currentPath(),
      ...collectDeviceInfo(),
    });
  };

  const heartbeat = (keepalive = false) => {
    lastSent = seconds;
    return post(
      `/api/visits/${encodeURIComponent(id)}/heartbeat`,
      { durationSeconds: seconds, lastPath: currentPath() },
      keepalive,
    );
  };

  const tick = () => {
    if (document.visibilityState !== "visible") return;
    seconds += 1;
    sessionStorage.setItem(VISIT_SECONDS_KEY, String(seconds));
    if (!qualified && seconds >= QUALIFIED_SECONDS) void qualify();
    else if (qualified && seconds - lastSent >= HEARTBEAT_SECONDS) void heartbeat();
  };

  const flush = () => {
    if (qualified && seconds > lastSent) void heartbeat(true);
  };

  onMounted(() => {
    if (location.pathname.startsWith("/admin")) return;
    timer = window.setInterval(tick, 1000);
    window.addEventListener("pagehide", flush);
  });

  onBeforeUnmount(() => {
    if (timer !== undefined) window.clearInterval(timer);
    window.removeEventListener("pagehide", flush);
    flush();
  });
}
