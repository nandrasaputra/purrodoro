import { defineConfig, loadEnv } from 'vite';
import { adminPlugin } from './server/admin.mjs';

export default defineConfig(({ command, mode }) => {
  if (command !== 'serve' || mode !== 'development') {
    throw new Error('Soundscape admin is a local development tool; production builds and previews are disabled.');
  }
  const env = loadEnv(mode, process.cwd(), '');
  for (const key of ['VITE_SUPABASE_SERVICE_ROLE_KEY', 'VITE_R2_ACCESS_KEY_ID', 'VITE_R2_SECRET_ACCESS_KEY']) {
    if (env[key]) throw new Error(`Remove ${key} from your environment; see README.md for migration.`);
  }
  return {
    envDir: process.cwd(),
    server: { host: '127.0.0.1', port: 5173, strictPort: true },
    plugins: [adminPlugin(env)],
  };
});
