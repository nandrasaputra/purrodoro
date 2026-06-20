-- Bundled raw_resource stems: l_amb_* (local ambience), l_lofi_* (local music).
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_rain' where kind = 'ambience' and local_id = 1;
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_cafe' where kind = 'ambience' and local_id = 2;
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_fireplace' where kind = 'ambience' and local_id = 3;
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_forest' where kind = 'ambience' and local_id = 4;
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_sea_wave' where kind = 'ambience' and local_id = 5;
update public.soundscape_bundled_catalog set raw_resource = 'l_amb_winter_wind' where kind = 'ambience' and local_id = 6;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_desk_lamp_rain' where kind = 'music' and local_id = 1;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_mosswood_morning' where kind = 'music' and local_id = 2;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_paper_skies' where kind = 'music' and local_id = 3;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_quiet_window_code' where kind = 'music' and local_id = 4;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_sore_di_pendopo' where kind = 'music' and local_id = 5;
update public.soundscape_bundled_catalog set raw_resource = 'l_lofi_sudirman_rain_loop' where kind = 'music' and local_id = 6;
