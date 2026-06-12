create extension if not exists moddatetime schema extensions;

create table public.profiles (
  user_id uuid primary key references auth.users(id) on delete cascade,
  display_name text not null default '',
  timer_settings jsonb not null default '{}'::jsonb,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false
);

create table public.categories (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  color_argb bigint,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false
);

create table public.tasks (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  title text not null,
  completed boolean not null default false,
  minutes int not null default 25,
  planned_sessions int not null default 1,
  completed_sessions int not null default 0,
  category_id uuid references public.categories(id),
  priority text not null default 'N',          -- 'L' | 'N' | 'H', reuses TaskPriority.storageTag
  due_date date,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false
);

create table public.habits (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  name text not null,
  emoji text not null default '',
  color_argb bigint not null,
  created_at_millis bigint not null,
  occurrence_tag text not null default 'D',     -- 'D' daily | 'W' weekdays (existing encoder tags)
  occurrence_days text not null default '',     -- 'MONDAY,WEDNESDAY' when tag = 'W'
  time_block text not null default 'X',         -- 'M' | 'A' | 'E' | 'X'
  updated_at timestamptz not null default now(),
  deleted boolean not null default false
);

create table public.habit_logs (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  habit_id uuid not null references public.habits(id),
  log_date date not null,
  state text not null,                          -- 'C' completed | 'S' skipped | 'P' partial (legacy)
  progress int not null default 0,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false,
  unique (user_id, habit_id, log_date)
);

create table public.session_records (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  local_id bigint not null,
  started_at bigint not null,                   -- epoch millis, matches SessionRecord
  ended_at bigint not null,
  focus_minutes int not null,
  short_break_minutes int not null,
  long_break_minutes int not null,
  completed_focus_count int not null,
  total_focus_count int not null,
  completed_short_break_count int not null default 0,
  completed_long_break_count int not null default 0,
  status text not null,                         -- 'Completed' | 'Partial' (SessionStatus.name)
  task_name text,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false,
  unique (user_id, local_id)
);

create table public.daily_stamps (
  id uuid primary key,
  user_id uuid not null references auth.users(id) on delete cascade,
  stamp_date date not null,
  count int not null default 0,
  updated_at timestamptz not null default now(),
  deleted boolean not null default false,
  unique (user_id, stamp_date)
);
