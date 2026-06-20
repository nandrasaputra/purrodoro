-- Fireplace, ocean shore ambience, and Paper Skies lo-fi are free.
update public.soundscape_bundled_catalog
set is_premium = false
where (kind = 'ambience' and local_id in (3, 5))
   or (kind = 'music' and local_id = 3);
