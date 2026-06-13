-- Cloud catalog for downloadable soundscape tracks (music + ambience).
-- Publishing a track = upload the mp3 to the soundscape-audio bucket, then
-- insert a row here. Unpublishing = set is_active = false (or delete the
-- row). Clients replace their whole cached catalog with each fetch, so
-- removal propagates automatically on the next refresh.
--
-- ids < 100 are reserved for tracks bundled in the APK (res/raw); the check
-- constraint plus a client-side filter guarantees no collision.

create table public.soundscape_tracks (
  id int primary key check (id >= 100),
  kind text not null check (kind in ('music', 'ambience')),
  name text not null,
  artist text not null default '',      -- music: artist name; ambience: short description shown under the name
  symbol text not null default '',      -- ambience only: symbol label the UI renders on the tile
  file_path text not null,              -- object path inside the soundscape-audio bucket, e.g. 'lofi/rainy_window.mp3'
  file_size_bytes bigint not null,
  is_premium boolean not null default false,
  sort_order int not null default 0,
  is_active boolean not null default true,
  updated_at timestamptz not null default now()
);

-- Reuses the moddatetime extension installed by 0001_sync_tables.sql.
create trigger soundscape_tracks_updated_at
  before update on public.soundscape_tracks
  for each row execute function extensions.moddatetime(updated_at);

alter table public.soundscape_tracks enable row level security;

-- Anyone (anon included — catalog must be readable before login) can read
-- active tracks. No insert/update/delete policies: writes go through the
-- dashboard / service role only.
create policy "public_read_active" on public.soundscape_tracks
  for select using (is_active = true);

-- Public bucket: audio is served from the public object URL
-- {SUPABASE_URL}/storage/v1/object/public/soundscape-audio/{file_path}
insert into storage.buckets (id, name, public)
values ('soundscape-audio', 'soundscape-audio', true)
on conflict (id) do nothing;
