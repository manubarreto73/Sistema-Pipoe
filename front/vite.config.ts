import { fileURLToPath } from "node:url";

import tailwindcss from "@tailwindcss/vite";
import react from "@vitejs/plugin-react";
import { defineConfig } from "vite";

// https://vite.dev/config/
export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // La API solo permite este origen por CORS. Sin strictPort, si el 5173 está
    // ocupado Vite se movería al 5174 y todas las requests fallarían por CORS con
    // un error que no menciona el puerto. Mejor que no arranque.
    port: 5173,
    strictPort: true,
  },
  resolve: {
    alias: {
      "@": fileURLToPath(new URL("./src", import.meta.url)),
    },
  },
});
