import { createRouter, createWebHistory } from "vue-router";
import ConfiguredHomeView from "./views/ConfiguredHomeView.vue";
import JobsView from "./views/JobsView.vue";
import JobDetailView from "./views/JobDetailView.vue";
import ContentView from "./views/ContentView.vue";
import LandingView from "./views/LandingView.vue";
import CampaignLandingView from "./views/CampaignLandingView.vue";

const router = createRouter({
  history: createWebHistory(),
  scrollBehavior: () => ({ top: 0 }),
  routes: [
    { path: "/", component: ConfiguredHomeView },
    { path: "/jobs", component: JobsView, props: { category: "全部职位" } },
    { path: "/jobs/functions", component: JobsView, props: { category: "职能岗位" } },
    { path: "/jobs/tech", component: JobsView, props: { category: "技术岗位" } },
    { path: "/jobs/:slug", component: JobDetailView },
    { path: "/about", component: ContentView, props: { page: "about" } },
    { path: "/business", component: ContentView, props: { page: "business" } },
    { path: "/life", component: ContentView, props: { page: "life" } },
    { path: "/growth", component: ContentView, props: { page: "growth" } },
    { path: "/contact", component: ContentView, props: { page: "contact" } },
    { path: "/landing", component: CampaignLandingView, meta: { landing: true } },
    { path: "/landing/tech", component: LandingView, props: { variant: "tech" }, meta: { landing: true } },
    { path: "/landing/performance", component: LandingView, props: { variant: "performance" }, meta: { landing: true } },
    { path: "/landing/global", component: LandingView, props: { variant: "global" }, meta: { landing: true } },
    { path: "/landing/apple", component: LandingView, props: { variant: "apple" }, meta: { landing: true } }
  ]
});

export default router;
