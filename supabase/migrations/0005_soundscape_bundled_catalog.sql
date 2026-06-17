-- Local (APK) soundscape catalog metadata. Audio bytes live in res/raw;
-- this table controls visibility, premium, and order without an app release.

create table public.soundscape_bundled_catalog (
  kind text not null check (kind in ('music', 'ambience')),
  local_id int not null check (local_id >= 1 and local_id < 100),
  name text not null default '',
  artist text not null default '',
  symbol text not null default '',
  raw_resource text not null,
  is_premium boolean not null default false,
  sort_order int not null default 0,
  is_active boolean not null default true,
  primary key (kind, local_id)
);

alter table public.soundscape_bundled_catalog enable row level security;

-- Public read of ALL rows (active and inactive). The app needs inactive rows
-- to distinguish "hidden by admin" from "not yet seeded" — see the plan's
-- Design decision 3. This table holds only flags/metadata, never audio bytes,
-- so exposing inactive rows is harmless. Writes go through the service role
-- (admin tool), which bypasses RLS.
create policy "public_read_bundled" on public.soundscape_bundled_catalog
  for select using (true);

-- Seed: matches PurrodoroApp.kt defaults + res/raw filenames (2026-06-17).
insert into public.soundscape_bundled_catalog
  (kind, local_id, raw_resource, is_premium, sort_order, symbol)
values
  ('ambience', 1, 'ambience_rain',         false, 10, 'Rain'),
  ('ambience', 2, 'ambience_cafe',         false, 20, 'Cafe'),
  ('ambience', 3, 'ambience_fireplace',    true,  30, 'Fire'),
  ('ambience', 4, 'ambience_forest',       true,  40, 'Forest'),
  ('ambience', 5, 'ambience_sea_wave',     true,  50, 'Wave'),
  ('ambience', 6, 'ambience_winter_wind',  true,  60, 'Wind'),
  ('music',    1, 'lofi_bloom_yasumu',              false, 10, ''),
  ('music',    2, 'lofi_chill_fm_dope_cat',         false, 20, ''),
  ('music',    3, 'lofi_midnight_whisper_matrika',  true,  30, '');
