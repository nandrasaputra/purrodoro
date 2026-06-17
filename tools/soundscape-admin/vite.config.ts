import { defineConfig } from 'vite';

// Local-only dev server. Do NOT deploy a production build — see README:
// Vite inlines VITE_* env vars (incl. the service role key) into client JS.
export default defineConfig({
  server: {
    port: 5173,
  },
});
