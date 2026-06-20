-- Seed cloud lo-fi catalog (audio on Cloudflare R2, metadata here).
insert into public.soundscape_tracks
  (id, kind, name, artist, symbol, file_path, file_size_bytes, is_premium, sort_order, is_active)
values
  (100, 'music', 'Cedar Rain Study', '', '', 'lofi/lofi_cedar_rain_study.mp3', 1993395, false, 10, true),
  (101, 'music', 'Cloud Garden Drift', '', '', 'lofi/lofi_cloud_garden_drift.mp3', 1809911, false, 20, true),
  (102, 'music', 'Kacapi Rain Drift', '', '', 'lofi/lofi_kacapi_rain_drift.mp3', 2277607, false, 30, true),
  (103, 'music', 'Last Page Of Summer', '', '', 'lofi/lofi_last_page_of_summer.mp3', 1812837, false, 40, true),
  (104, 'music', 'Last Train Glow', '', '', 'lofi/lofi_last_train_glow.mp3', 2130486, false, 50, true),
  (105, 'music', 'Midnight Platform', '', '', 'lofi/lofi_midnight_platform.mp3', 2785846, false, 60, true),
  (106, 'music', 'Midnight Rain Grid', '', '', 'lofi/lofi_midnight_rain_grid.mp3', 2436432, false, 70, true),
  (107, 'music', 'Moonlit Glass', '', '', 'lofi/lofi_moonlit_glass.mp3', 2507485, false, 80, true),
  (108, 'music', 'Paper Crane Study', '', '', 'lofi/lofi_paper_crane_study.mp3', 2477810, false, 90, true),
  (109, 'music', 'Paper Cup Evening', '', '', 'lofi/lofi_paper_cup_evening.mp3', 1672402, false, 100, true),
  (110, 'music', 'Paper Lantern Drift', '', '', 'lofi/lofi_paper_lantern_drift.mp3', 1888069, false, 110, true),
  (111, 'music', 'Rain On Glass', '', '', 'lofi/lofi_rain_on_glass.mp3', 1983364, false, 120, true),
  (112, 'music', 'Rain On Washi', '', '', 'lofi/lofi_rain_on_washi.mp3', 2365797, false, 130, true),
  (113, 'music', 'Rain Window Atlas', '', '', 'lofi/lofi_rain_window_atlas.mp3', 2849793, false, 140, true),
  (114, 'music', 'Rainy Compiler', '', '', 'lofi/lofi_rainy_compiler.mp3', 2257963, false, 150, true),
  (115, 'music', 'Rainy Window Code', '', '', 'lofi/lofi_rainy_window_code.mp3', 1812419, false, 160, true),
  (116, 'music', 'Sky Between Clouds', '', '', 'lofi/lofi_sky_between_clouds.mp3', 2074897, false, 170, true),
  (117, 'music', 'Soft Loop Atlas', '', '', 'lofi/lofi_soft_loop_atlas.mp3', 2157235, false, 180, true),
  (118, 'music', 'Sunlit Window Postcards', '', '', 'lofi/lofi_sunlit_window_postcards.mp3', 2420967, false, 190, true),
  (119, 'music', 'Velvet Study Room', '', '', 'lofi/lofi_velvet_study_room.mp3', 2041460, false, 200, true)
on conflict (id) do update set
  name = excluded.name,
  file_path = excluded.file_path,
  file_size_bytes = excluded.file_size_bytes,
  sort_order = excluded.sort_order,
  is_active = excluded.is_active,
  updated_at = now();
