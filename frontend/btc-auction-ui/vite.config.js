import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";

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
        target: "http://localhost:8080",
        changeOrigin: true,
      },
      "/ws": {
        target: "http://localhost:8080",
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
