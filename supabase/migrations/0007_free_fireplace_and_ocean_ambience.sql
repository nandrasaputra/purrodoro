-- Fireplace + ocean shore ambience are free for all users.
update public.soundscape_bundled_catalog
set is_premium = false
where kind = 'ambience' and local_id in (3, 5);
