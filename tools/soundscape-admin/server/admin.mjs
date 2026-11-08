import { randomBytes } from 'node:crypto';
import { createClient } from '@supabase/supabase-js';
import { PutObjectCommand, S3Client } from '@aws-sdk/client-s3';

const MAX_UPLOAD = 100 * 1024 * 1024;
const MAX_JSON = 32 * 1024;
const tables = { remote: 'soundscape_tracks', bundled: 'soundscape_bundled_catalog' };
const patchFields = new Set(['is_active', 'is_premium', 'sort_order', 'thumbnail_path', 'updated_at']);
const remoteFields = new Set(['id', 'kind', 'name', 'artist', 'symbol', 'file_path', 'thumbnail_path', 'file_size_bytes', 'is_premium', 'sort_order', 'is_active', 'updated_at']);

function validPath(value, extensions) {
  return typeof value === 'string' && /^(lofi|ambience)\/[a-zA-Z0-9][a-zA-Z0-9._-]*$/.test(value)
    && !value.includes('..') && extensions.some(ext => value.toLowerCase().endsWith(ext));
}

function validPatch(patch) {
  return patch && typeof patch === 'object' && !Array.isArray(patch) && Object.keys(patch).length > 0
    && Object.entries(patch).every(([key, value]) => patchFields.has(key) && (
      (['is_active', 'is_premium'].includes(key) && typeof value === 'boolean') ||
      (key === 'sort_order' && Number.isSafeInteger(value)) ||
      (key === 'thumbnail_path' && validPath(value, ['.webp', '.png'])) ||
      (key === 'updated_at' && typeof value === 'string' && !Number.isNaN(Date.parse(value)))
    ));
}

async function body(req, limit) {
  const chunks = []; let size = 0;
  for await (const chunk of req) {
    size += chunk.length;
    if (size > limit) throw new Error('Request too large');
    chunks.push(chunk);
  }
  return Buffer.concat(chunks);
}

export function createAdminHandler({ token, db, upload, publicUrl }) {
  return async (req, res, next) => {
    const host = req.headers.host;
    if (host !== '127.0.0.1:5173') { res.writeHead(403).end(); return; }
    if (!req.url?.startsWith('/api/')) return next();
    res.setHeader('Cache-Control', 'no-store');
    res.setHeader('Content-Type', 'application/json');
    const origin = req.headers.origin;
    if ((origin && origin !== 'http://127.0.0.1:5173') ||
        (req.headers['sec-fetch-site'] && !['same-origin', 'none'].includes(req.headers['sec-fetch-site']))) {
      res.writeHead(403).end(); return;
    }
    const url = new URL(req.url, 'http://127.0.0.1:5173');
    if (url.pathname === '/api/session' && req.method === 'GET') {
      res.end(JSON.stringify({ token, publicUrl })); return;
    }
    if (req.headers['x-admin-token'] !== token || (req.method !== 'GET' && origin !== 'http://127.0.0.1:5173')) {
      res.writeHead(403).end(); return;
    }
    try {
      const parts = url.pathname.split('/').filter(Boolean);
      let result;
      if (parts.length === 2 && tables[parts[1]] && req.method === 'GET') {
        result = await db.list(tables[parts[1]]);
      } else if (parts.length === 2 && parts[1] === 'remote' && req.method === 'POST') {
        const row = JSON.parse((await body(req, MAX_JSON)).toString());
        if (!row || Object.keys(row).some(k => !remoteFields.has(k)) || !Number.isSafeInteger(row.id) || row.id < 100 ||
            !['music', 'ambience'].includes(row.kind) || !validPath(row.file_path, ['.mp3']) ||
            !validPath(row.thumbnail_path, ['.webp', '.png']) || !Number.isSafeInteger(row.file_size_bytes) ||
            row.file_size_bytes < 1 || row.file_size_bytes > MAX_UPLOAD ||
            !['name', 'artist', 'symbol'].every(k => typeof row[k] === 'string' && row[k].length <= 200) ||
            !validPatch({ is_active: row.is_active, is_premium: row.is_premium, sort_order: row.sort_order, updated_at: row.updated_at })) throw new Error('Invalid track');
        result = await db.insert(row);
      } else if (req.method === 'PATCH' && ((parts.length === 3 && parts[1] === 'remote') || (parts.length === 4 && parts[1] === 'bundled'))) {
        const id = Number(parts.at(-1));
        const kind = parts[1] === 'bundled' ? parts[2] : null;
        const patch = JSON.parse((await body(req, MAX_JSON)).toString());
        if (!Number.isSafeInteger(id) || id < (kind ? 0 : 100) || (kind && !['music', 'ambience'].includes(kind)) || !validPatch(patch) ||
            (kind && Object.keys(patch).some(k => !['is_active', 'is_premium', 'sort_order'].includes(k)))) throw new Error('Invalid update');
        result = await db.update(tables[parts[1]], id, kind, patch);
      } else if (parts.length === 2 && parts[1] === 'upload' && req.method === 'POST') {
        const path = url.searchParams.get('path');
        if (!validPath(path, ['.mp3', '.webp', '.png'])) throw new Error('Invalid object path');
        const bytes = await body(req, MAX_UPLOAD);
        if (!bytes.length) throw new Error('Empty upload');
        const type = path.toLowerCase().endsWith('.mp3') ? 'audio/mpeg' : path.toLowerCase().endsWith('.png') ? 'image/png' : 'image/webp';
        await upload(path, bytes, type); result = { ok: true };
      } else { res.writeHead(404).end(); return; }
      res.end(JSON.stringify(result ?? { ok: true }));
    } catch (error) {
      // Upstream errors may contain credentials or sensitive request details.
      const message = ['Request too large', 'Invalid track', 'Invalid update', 'Invalid object path', 'Empty upload'].includes(error.message) ? error.message : 'Request failed; check server configuration and upstream services';
      res.writeHead(400).end(JSON.stringify({ error: message }));
    }
  };
}

export function adminPlugin(env) {
  const required = ['SUPABASE_URL', 'SUPABASE_SERVICE_ROLE_KEY', 'R2_ACCOUNT_ID', 'R2_ACCESS_KEY_ID', 'R2_SECRET_ACCESS_KEY', 'R2_PUBLIC_URL'];
  for (const name of required) if (!env[name]) throw new Error(`Missing ${name} in .env.local`);
  const token = randomBytes(32).toString('hex');
  const supabase = createClient(env.SUPABASE_URL, env.SUPABASE_SERVICE_ROLE_KEY);
  const s3 = new S3Client({ region: 'auto', endpoint: `https://${env.R2_ACCOUNT_ID}.r2.cloudflarestorage.com`, credentials: { accessKeyId: env.R2_ACCESS_KEY_ID, secretAccessKey: env.R2_SECRET_ACCESS_KEY } });
  const checked = async query => { const { data, error } = await query; if (error) throw error; return data; };
  const db = {
    list: table => checked(supabase.from(table).select('*').order('sort_order')),
    insert: row => checked(supabase.from(tables.remote).insert(row)),
    update: (table, id, kind, patch) => checked(kind ? supabase.from(table).update(patch).eq('kind', kind).eq('local_id', id) : supabase.from(table).update(patch).eq('id', id)),
  };
  return { name: 'local-admin-api', configureServer(server) {
    if (server.config.server.host !== '127.0.0.1' || server.config.server.port !== 5173 || !server.config.server.strictPort) {
      throw new Error('Admin server must listen on 127.0.0.1:5173 with strictPort enabled');
    }
    server.middlewares.use(createAdminHandler({ token, db, publicUrl: env.R2_PUBLIC_URL, upload: (key, bytes, contentType) => s3.send(new PutObjectCommand({ Bucket: 'purrodoro-soundscapes', Key: key, Body: bytes, ContentType: contentType })) }));
  } };
}
