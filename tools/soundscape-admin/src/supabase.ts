import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_SERVICE_ROLE_KEY;

if (!url || !key) {
  throw new Error('Set VITE_SUPABASE_URL and VITE_SUPABASE_SERVICE_ROLE_KEY in .env.local');
}

/** Postgres catalog only — audio bytes live on Cloudflare R2 (see ./r2.ts). */
export const supabase = createClient(url, key);
