-- Bundled ambience raw_resource stems now use l_* (local APK audio).
update public.soundscape_bundled_catalog set raw_resource = 'l_rain' where kind = 'ambience' and local_id = 1;
update public.soundscape_bundled_catalog set raw_resource = 'l_cafe' where kind = 'ambience' and local_id = 2;
update public.soundscape_bundled_catalog set raw_resource = 'l_fireplace' where kind = 'ambience' and local_id = 3;
update public.soundscape_bundled_catalog set raw_resource = 'l_forest' where kind = 'ambience' and local_id = 4;
update public.soundscape_bundled_catalog set raw_resource = 'l_sea_wave' where kind = 'ambience' and local_id = 5;
update public.soundscape_bundled_catalog set raw_resource = 'l_winter_wind' where kind = 'ambience' and local_id = 6;
