-- Replace bundled local lo-fi catalog (6 tracks, l_lofi_* raw resources).
delete from public.soundscape_bundled_catalog where kind = 'music';
insert into public.soundscape_bundled_catalog (kind, local_id, raw_resource, is_premium, sort_order, symbol) values
  ('music', 1, 'l_lofi_desk_lamp_rain', false, 10, ''),
  ('music', 2, 'l_lofi_mosswood_morning', false, 20, ''),
  ('music', 3, 'l_lofi_paper_skies', false, 30, ''),
  ('music', 4, 'l_lofi_quiet_window_code', false, 40, ''),
  ('music', 5, 'l_lofi_sore_di_pendopo', true, 50, ''),
  ('music', 6, 'l_lofi_sudirman_rain_loop', true, 60, '');
