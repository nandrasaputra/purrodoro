import type { BundledTrack, RemoteTrack, TrackKind } from './types';

let token = '';
let publicUrl = '';

export async function initApi() {
  const response = await fetch('/api/session');
  if (!response.ok) throw new Error('Could not connect to local admin server');
  ({ token, publicUrl } = await response.json());
}

async function request<T>(path: string, method = 'GET', body?: BodyInit): Promise<T> {
  const response = await fetch(`/api/${path}`, { method, body, headers: { 'x-admin-token': token } });
  if (!response.ok) {
    const result = await response.json().catch(() => ({}));
    throw new Error(result.error || 'Local admin request failed');
  }
  return response.json();
}

export const loadRemoteTracks = () => request<RemoteTrack[]>('remote');
export const loadBundledTracks = () => request<BundledTrack[]>('bundled');
export const updateRemote = (id: number, patch: Partial<RemoteTrack>) => request(`remote/${id}`, 'PATCH', JSON.stringify(patch));
export const updateBundled = (kind: TrackKind, id: number, patch: Partial<BundledTrack>) => request(`bundled/${kind}/${id}`, 'PATCH', JSON.stringify(patch));
export const insertRemote = (row: RemoteTrack) => request('remote', 'POST', JSON.stringify(row));
export const uploadAudio = (path: string, file: File) => request(`upload?path=${encodeURIComponent(path)}`, 'POST', file);
export const uploadImage = uploadAudio;
export const thumbnailPathForAudio = (path: string) => path.replace(/\.mp3$/i, '.webp');
export const publicAudioUrl = (path: string) => `${publicUrl.replace(/\/$/, '')}/${path.replace(/^\//, '')}`;
export const publicThumbnailUrl = publicAudioUrl;
