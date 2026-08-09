/// <reference types="vitest/config" />
import { defineConfig } from "vite";
import react from "@vitejs/plugin-react";
import tailwindcss from "@tailwindcss/vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  test: {
    // Components are asserted against a real DOM rather than a renderer's
    // output, so the queries match what a user could actually find.
    environment: "jsdom",
    globals: true,
    setupFiles: ["./src/test/setup.ts"],
    css: false,
    coverage: {
      provider: "v8",
      include: ["src/**/*.{ts,tsx}"],
      exclude: [
        "src/**/*.d.ts",
        "src/test/**",
        "src/main.tsx",
        // Generated coastline data — thousands of coordinate literals.
        "src/components/europeLand.ts",
        "src/components/worldLand.ts",
      ],
    },
  },
});
