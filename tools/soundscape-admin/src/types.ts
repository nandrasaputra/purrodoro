export type TrackKind = 'music' | 'ambience';

export interface RemoteTrack {
  id: number;
  kind: TrackKind;
  name: string;
  artist: string;
  symbol: string;
  file_path: string;
  file_size_bytes: number;
  is_premium: boolean;
  sort_order: number;
  is_active: boolean;
  updated_at: string;
}

export interface BundledTrack {
  kind: TrackKind;
  local_id: number;
  name: string;
  artist: string;
  symbol: string;
  raw_resource: string;
  is_premium: boolean;
  sort_order: number;
  is_active: boolean;
}
