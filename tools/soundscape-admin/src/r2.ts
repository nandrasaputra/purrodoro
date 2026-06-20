import { PutObjectCommand, S3Client } from '@aws-sdk/client-s3';

export const R2_BUCKET = 'purrodoro-soundscapes';

function requireEnv(name: string): string {
  const value = import.meta.env[name] as string | undefined;
  if (!value) throw new Error(`Set ${name} in .env.local`);
  return value;
}

function s3(): S3Client {
  const accountId = requireEnv('VITE_R2_ACCOUNT_ID');
  return new S3Client({
    region: 'auto',
    endpoint: `https://${accountId}.r2.cloudflarestorage.com`,
    credentials: {
      accessKeyId: requireEnv('VITE_R2_ACCESS_KEY_ID'),
      secretAccessKey: requireEnv('VITE_R2_SECRET_ACCESS_KEY'),
    },
  });
}

/** Public HTTPS URL for an R2 object key. */
export function publicObjectUrl(filePath: string): string {
  const base = requireEnv('VITE_R2_PUBLIC_URL').replace(/\/$/, '');
  const path = filePath.replace(/^\//, '');
  return `${base}/${path}`;
}

/** Public HTTPS URL for an object key such as `lofi/lofi_cedar_rain_study.mp3`. */
export function publicAudioUrl(filePath: string): string {
  return publicObjectUrl(filePath);
}

/** Public HTTPS URL for a catalog thumbnail key such as `lofi/lofi_cedar_rain_study.webp`. */
export function publicThumbnailUrl(thumbnailPath: string): string {
  return publicObjectUrl(thumbnailPath);
}

export async function uploadAudio(filePath: string, file: File): Promise<void> {
  const key = filePath.replace(/^\//, '');
  const body = new Uint8Array(await file.arrayBuffer());
  await s3().send(
    new PutObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
      Body: body,
      ContentType: 'audio/mpeg',
      ContentLength: body.byteLength,
    }),
  );
}

export async function uploadImage(filePath: string, file: File): Promise<void> {
  const key = filePath.replace(/^\//, '');
  const body = new Uint8Array(await file.arrayBuffer());
  const contentType = file.name.endsWith('.png')
    ? 'image/png'
    : file.name.endsWith('.webp')
      ? 'image/webp'
      : 'application/octet-stream';
  await s3().send(
    new PutObjectCommand({
      Bucket: R2_BUCKET,
      Key: key,
      Body: body,
      ContentType: contentType,
      ContentLength: body.byteLength,
    }),
  );
}

/** Derive thumbnail object key from an MP3 path: lofi/foo.mp3 -> lofi/foo.webp */
export function thumbnailPathForAudio(filePath: string): string {
  return filePath.replace(/\.mp3$/i, '.webp');
}
