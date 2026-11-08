#!/usr/bin/env node
/**
 * Upload staged soundscape thumbnails to Cloudflare R2.
 * Run from repo root:
 *   node tools/soundscape-admin/scripts/upload-thumbnails.mjs
 *
 * Reads R2 credentials from tools/soundscape-admin/.env.local (R2_*).
 */
import { readFileSync, readdirSync, statSync } from 'node:fs';
import { join, relative } from 'node:path';
import { fileURLToPath } from 'node:url';
import { PutObjectCommand, S3Client } from '@aws-sdk/client-s3';

const __dirname = fileURLToPath(new URL('.', import.meta.url));
const repoRoot = join(__dirname, '../../..');
const adminDir = join(repoRoot, 'tools/soundscape-admin');
const thumbRoot = join(repoRoot, 'assets/soundscape/thumbnails');
const bucket = 'purrodoro-soundscapes';

function loadEnv(path) {
  const env = {};
  for (const line of readFileSync(path, 'utf8').split('\n')) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const eq = trimmed.indexOf('=');
    if (eq === -1) continue;
    env[trimmed.slice(0, eq)] = trimmed.slice(eq + 1);
  }
  return env;
}

function walkWebp(dir, base = dir) {
  const out = [];
  for (const name of readdirSync(dir)) {
    const full = join(dir, name);
    if (statSync(full).isDirectory()) {
      out.push(...walkWebp(full, base));
    } else if (name.endsWith('.webp')) {
      out.push(relative(base, full).replace(/\\/g, '/'));
    }
  }
  return out;
}

const env = loadEnv(join(adminDir, '.env.local'));
const accountId = env.R2_ACCOUNT_ID;
const accessKeyId = env.R2_ACCESS_KEY_ID;
const secretAccessKey = env.R2_SECRET_ACCESS_KEY;
if (!accountId || !accessKeyId || !secretAccessKey) {
  console.error('Missing R2_* in tools/soundscape-admin/.env.local');
  process.exit(1);
}

const s3 = new S3Client({
  region: 'auto',
  endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
  credentials: { accessKeyId, secretAccessKey },
});

const keys = walkWebp(thumbRoot);
if (keys.length === 0) {
  console.error('No .webp files under', thumbRoot);
  process.exit(1);
}

for (const key of keys.sort()) {
  const body = readFileSync(join(thumbRoot, key));
  await s3.send(
    new PutObjectCommand({
      Bucket: bucket,
      Key: key,
      Body: body,
      ContentType: 'image/webp',
      ContentLength: body.byteLength,
    }),
  );
  console.log(`uploaded ${key} (${body.byteLength} bytes)`);
}

console.log(`Done — ${keys.length} thumbnails uploaded to ${bucket}`);
