-- ============================================================
-- Migration: Add reverse simulation, spendable margins, and note sharing
-- ============================================================

-- Add reverse simulation target to scenario_inputs
ALTER TABLE scenario_inputs
  ADD COLUMN IF NOT EXISTS reverse_target_age integer,
  ADD COLUMN IF NOT EXISTS reverse_target_balance numeric;

-- Add spendable margin and milestone data to scenario_results
ALTER TABLE scenario_results
  ADD COLUMN IF NOT EXISTS extra_annual_spend numeric,
  ADD COLUMN IF NOT EXISTS extra_monthly_spend numeric,
  ADD COLUMN IF NOT EXISTS spendable_margins_json jsonb DEFAULT '[]'::jsonb,
  ADD COLUMN IF NOT EXISTS milestone_80 numeric,
  ADD COLUMN IF NOT EXISTS milestone_85 numeric;

-- Add note selection for summary sharing
ALTER TABLE family_notes
  ADD COLUMN IF NOT EXISTS include_in_summary boolean DEFAULT true,
  ADD COLUMN IF NOT EXISTS scenario_id uuid REFERENCES scenario_results(id);

-- Add note snapshot to shared summaries (already uses payload_json but add explicit field)
ALTER TABLE shared_summaries
  ADD COLUMN IF NOT EXISTS included_note_ids uuid[] DEFAULT '{}',
  ADD COLUMN IF NOT EXISTS notes_snapshot_json jsonb DEFAULT '[]'::jsonb;

-- Index for efficient note lookup by include flag
CREATE INDEX IF NOT EXISTS idx_family_notes_include_summary
  ON family_notes(user_id, include_in_summary)
  WHERE include_in_summary = true;
