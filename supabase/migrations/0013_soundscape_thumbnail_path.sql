-- Add R2 thumbnail object key for cloud soundscape tracks.
-- Thumbnails mirror audio paths: lofi/foo.mp3 -> lofi/foo.webp

alter table public.soundscape_tracks
  add column if not exists thumbnail_path text not null default '';

-- Backfill from existing audio keys (same stem, .webp extension).
update public.soundscape_tracks
set thumbnail_path = regexp_replace(file_path, '\.mp3$', '.webp')
where thumbnail_path = '';
