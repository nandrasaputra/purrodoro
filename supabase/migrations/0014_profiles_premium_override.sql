-- Admin-granted premium per user (reviewers, support). Clients may read;
-- only service_role (Supabase dashboard / backend) may write these columns.

alter table public.profiles
  add column premium_override_enabled boolean not null default false,
  add column premium_override_active boolean not null default false;

comment on column public.profiles.premium_override_enabled is
  'When true, the app uses premium_override_active instead of Play/App Store billing.';
comment on column public.profiles.premium_override_active is
  'Premium flag applied when premium_override_enabled is true.';

create or replace function public.protect_profiles_premium_override()
returns trigger
language plpgsql
as $$
begin
  if coalesce(auth.jwt() ->> 'role', '') = 'service_role' then
    return new;
  end if;

  if tg_op = 'INSERT' then
    new.premium_override_enabled := false;
    new.premium_override_active := false;
    return new;
  end if;

  new.premium_override_enabled := old.premium_override_enabled;
  new.premium_override_active := old.premium_override_active;
  return new;
end;
$$;

create trigger protect_profiles_premium_override
  before insert or update on public.profiles
  for each row
  execute function public.protect_profiles_premium_override();
