import { createClient } from '@supabase/supabase-js';

const url = import.meta.env.VITE_SUPABASE_URL;
const key = import.meta.env.VITE_SUPABASE_SERVICE_ROLE_KEY;

if (!url || !key) {
  throw new Error('Set VITE_SUPABASE_URL and VITE_SUPABASE_SERVICE_ROLE_KEY in .env.local');
}

export const supabase = createClient(url, key);
export const STORAGE_BUCKET = 'soundscape-audio';
export const publicAudioUrl = (filePath: string) =>
  `${url}/storage/v1/object/public/${STORAGE_BUCKET}/${filePath}`;
