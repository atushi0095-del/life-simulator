-- ============================================================
-- 人生資金シミュレーター — Initial Schema
-- Run this in the Supabase SQL Editor or via supabase db push
-- ============================================================

-- Users (mirrors auth.users, populated via trigger)
create table if not exists public.users (
  id         uuid primary key references auth.users(id) on delete cascade,
  email      text,
  created_at timestamptz default now() not null
);

-- Scenario inputs
create table if not exists public.scenario_inputs (
  id                        uuid primary key default gen_random_uuid(),
  user_id                   uuid references public.users(id) on delete cascade,
  age                       integer not null,
  family_type               text    not null,
  financial_assets          numeric not null,
  annual_income             numeric not null,
  annual_essential_expenses numeric not null,
  annual_comfort_expenses   numeric not null,
  retirement_age            integer not null,
  expected_lifespan         integer not null,
  return_mode               text    not null,
  detail_input_json         jsonb,
  created_at                timestamptz default now() not null,
  updated_at                timestamptz default now() not null
);

-- Scenario results (one row per scenario type per input)
create table if not exists public.scenario_results (
  id                   uuid primary key default gen_random_uuid(),
  scenario_input_id    uuid references public.scenario_inputs(id) on delete cascade,
  scenario_type        text    not null check (scenario_type in ('worst','main','upside')),
  required_assets      numeric not null,
  projected_assets     numeric not null,
  gap_value            numeric not null,
  age_at_depletion     integer,
  milestone_90         numeric,
  milestone_95         numeric,
  cash_flow_json       jsonb   not null,
  created_at           timestamptz default now() not null
);

-- Monte Carlo results (one row per run)
create table if not exists public.montecarlo_results (
  id                   uuid primary key default gen_random_uuid(),
  scenario_input_id    uuid references public.scenario_inputs(id) on delete cascade,
  trials               integer not null,
  success_rate         numeric not null,
  percentiles_json     jsonb   not null,
  depletion_dist_json  jsonb,
  created_at           timestamptz default now() not null
);

-- Family notes
create table if not exists public.family_notes (
  id         uuid primary key default gen_random_uuid(),
  user_id    uuid references public.users(id) on delete cascade,
  category   text not null,
  content    text not null,
  created_at timestamptz default now() not null,
  updated_at timestamptz default now() not null
);

-- Shared summaries (shareable, expires after 30 days)
create table if not exists public.shared_summaries (
  id           uuid primary key default gen_random_uuid(),
  share_id     text unique not null,
  user_id      uuid references public.users(id) on delete set null,
  payload_json jsonb not null,
  created_at   timestamptz default now() not null,
  expires_at   timestamptz default (now() + interval '30 days') not null
);

-- ---- Indexes ----
create index if not exists idx_scenario_inputs_user  on public.scenario_inputs(user_id);
create index if not exists idx_family_notes_user     on public.family_notes(user_id);
create index if not exists idx_shared_summaries_sid  on public.shared_summaries(share_id);

-- ---- updated_at trigger ----
create or replace function public.handle_updated_at()
returns trigger language plpgsql as $$
begin
  new.updated_at = now();
  return new;
end;
$$;

create trigger set_scenario_inputs_updated_at
  before update on public.scenario_inputs
  for each row execute procedure public.handle_updated_at();

create trigger set_family_notes_updated_at
  before update on public.family_notes
  for each row execute procedure public.handle_updated_at();

-- ---- Row Level Security ----
alter table public.users             enable row level security;
alter table public.scenario_inputs   enable row level security;
alter table public.scenario_results  enable row level security;
alter table public.montecarlo_results enable row level security;
alter table public.family_notes      enable row level security;
alter table public.shared_summaries  enable row level security;

-- Users
create policy "Users can read own profile"
  on public.users for select using (auth.uid() = id);

-- Scenario inputs
create policy "Users own their scenario inputs"
  on public.scenario_inputs for all using (auth.uid() = user_id);

-- Scenario results (through input ownership)
create policy "Users read own scenario results"
  on public.scenario_results for select
  using (
    scenario_input_id in (
      select id from public.scenario_inputs where user_id = auth.uid()
    )
  );

create policy "Users insert own scenario results"
  on public.scenario_results for insert
  with check (
    scenario_input_id in (
      select id from public.scenario_inputs where user_id = auth.uid()
    )
  );

-- Monte Carlo results
create policy "Users read own mc results"
  on public.montecarlo_results for select
  using (
    scenario_input_id in (
      select id from public.scenario_inputs where user_id = auth.uid()
    )
  );

create policy "Users insert own mc results"
  on public.montecarlo_results for insert
  with check (
    scenario_input_id in (
      select id from public.scenario_inputs where user_id = auth.uid()
    )
  );

-- Family notes
create policy "Users own their notes"
  on public.family_notes for all using (auth.uid() = user_id);

-- Shared summaries: anyone can read, auth or anon can insert
create policy "Anyone can read shared summaries"
  on public.shared_summaries for select using (true);

create policy "Users can create shared summaries"
  on public.shared_summaries for insert
  with check (user_id is null or auth.uid() = user_id);
