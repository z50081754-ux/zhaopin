import { ref } from "vue";
import { apiUrl } from "../utils/api";

export type SiteTemplate = "technology" | "apple";
const activeTemplate = ref<SiteTemplate>("technology");
const templateReady = ref(false);
let loading: Promise<void> | null = null;

export function useSiteTemplate() {
  async function loadSiteTemplate() {
    if (loading) return loading;
    loading = fetch(apiUrl("/api/site-settings"), { headers: { Accept: "application/json" } })
      .then(async response => {
        if (!response.ok) throw new Error("SITE_SETTINGS_FAILED");
        const result = await response.json() as { activeTemplate?: SiteTemplate };
        if (result.activeTemplate === "apple" || result.activeTemplate === "technology") {
          activeTemplate.value = result.activeTemplate;
        }
      })
      .catch(() => {})
      .finally(() => { templateReady.value = true; });
    return loading;
  }
  return { activeTemplate, templateReady, loadSiteTemplate };
}
