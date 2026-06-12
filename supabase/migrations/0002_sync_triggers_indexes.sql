-- Server-set updated_at on every UPDATE (INSERT is covered by the default now()).
create trigger set_updated_at before update on public.profiles
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.categories
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.tasks
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.habits
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.habit_logs
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.session_records
  for each row execute procedure extensions.moddatetime(updated_at);
create trigger set_updated_at before update on public.daily_stamps
  for each row execute procedure extensions.moddatetime(updated_at);

-- Delta pull: where user_id = auth.uid() and updated_at > checkpoint
create index categories_user_updated_idx      on public.categories      (user_id, updated_at);
create index tasks_user_updated_idx           on public.tasks           (user_id, updated_at);
create index habits_user_updated_idx          on public.habits          (user_id, updated_at);
create index habit_logs_user_updated_idx      on public.habit_logs      (user_id, updated_at);
create index session_records_user_updated_idx on public.session_records (user_id, updated_at);
create index daily_stamps_user_updated_idx    on public.daily_stamps    (user_id, updated_at);
