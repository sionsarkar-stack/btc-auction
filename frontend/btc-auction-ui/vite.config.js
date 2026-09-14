import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import { env } from "node:process";

const backendUrl = env.VITE_BACKEND_URL || "http://localhost:8080";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react()],

  define: {
    global: "globalThis",
  },

  // Keep relative API URLs in the UI while forwarding local development
  // requests to the Spring Boot server.
  server: {
    proxy: {
      "/api": {
        target: backendUrl,
        changeOrigin: true,
      },
      "/ws": {
        target: backendUrl,
        changeOrigin: true,
        ws: true,
      },
    },
  },

  optimizeDeps: {
    include: [
      "sockjs-client",
      "@stomp/stompjs",
    ],
  },
});
