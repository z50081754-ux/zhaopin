import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

export default defineConfig({
  base: "/admin/",
  plugins: [vue()],
  server: {
    host: "0.0.0.0",
    port: 3001,
    allowedHosts: [".trycloudflare.com"],
    proxy: {
      "/api": {
        target: "http://localhost:8080",
        changeOrigin: true,
        configure(proxy) {
          proxy.on("proxyReq", proxyRequest => proxyRequest.removeHeader("origin"));
        }
      }
    }
  },
  test: {
    environment: "jsdom"
  },
  build: { outDir: "dist", emptyOutDir: true }
});
