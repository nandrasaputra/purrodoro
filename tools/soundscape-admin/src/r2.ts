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

/** Public HTTPS URL for an object key such as `lofi/lofi_cedar_rain_study.mp3`. */
export function publicAudioUrl(filePath: string): string {
  const base = requireEnv('VITE_R2_PUBLIC_URL').replace(/\/$/, '');
  const path = filePath.replace(/^\//, '');
  return `${base}/${path}`;
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
