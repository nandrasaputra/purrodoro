-- Seed cloud ambience catalog (audio on Cloudflare R2, metadata here).
insert into public.soundscape_tracks
  (id, kind, name, artist, symbol, file_path, file_size_bytes, is_premium, sort_order, is_active)
values
  (120, 'ambience', 'Storm', 'Rolling thunder', 'Storm', 'ambience/amb_storm.mp3', 484039, false, 10, true),
  (121, 'ambience', 'Night Crickets', 'Gentle chirping', 'Cricket', 'ambience/amb_night_cricket.mp3', 491981, false, 20, true)
on conflict (id) do update set
  kind = excluded.kind,
  name = excluded.name,
  artist = excluded.artist,
  symbol = excluded.symbol,
  file_path = excluded.file_path,
  file_size_bytes = excluded.file_size_bytes,
  is_premium = excluded.is_premium,
  sort_order = excluded.sort_order,
  is_active = excluded.is_active,
  updated_at = now();
