-- Own-rows RLS on every synced table. No delete policy: clients tombstone via
-- deleted = true. session_records keeps the update policy so History's delete
-- action can tombstone; the append-only contract is enforced client-side.
do $$
declare t text;
begin
  foreach t in array array['profiles','categories','tasks','habits','habit_logs','session_records','daily_stamps']
  loop
    execute format('alter table public.%I enable row level security', t);
    execute format($f$create policy "own_select" on public.%I for select using (auth.uid() = user_id)$f$, t);
    execute format($f$create policy "own_insert" on public.%I for insert with check (auth.uid() = user_id)$f$, t);
    execute format($f$create policy "own_update" on public.%I for update using (auth.uid() = user_id) with check (auth.uid() = user_id)$f$, t);
  end loop;
end $$;
