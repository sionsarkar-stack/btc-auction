import { defineConfig } from "@playwright/test";

export default defineConfig({
    testDir: "./e2e",
    fullyParallel: false,
    workers: 1,
    timeout: 30_000,
    expect: {
        timeout: 10_000,
    },
    use: {
        baseURL: "http://127.0.0.1:5174",
        trace: "retain-on-failure",
        screenshot: "only-on-failure",
    },
    webServer: [
        {
            command: "cd ../../backend/btc-auction && SPRING_PROFILES_ACTIVE=e2e ./mvnw spring-boot:run",
            url: "http://127.0.0.1:8081/api/health",
            timeout: 120_000,
            reuseExistingServer: false,
        },
        {
            command: "VITE_BACKEND_URL=http://127.0.0.1:8081 npm run dev -- --host 127.0.0.1 --port 5174",
            url: "http://127.0.0.1:5174",
            timeout: 30_000,
            reuseExistingServer: false,
        },
    ],
});