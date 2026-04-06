import type { ScenarioInput, SimulationResults, MonteCarloOutput, FamilyNote, SharedSummary } from '@/lib/simulation/types';
import { getSupabaseClient } from './client';
import { generateShareId } from '@/lib/utils/shareId';

/** Save scenario input + results to Supabase. Returns the scenario_input id. */
export async function saveSimulation(
  userId: string,
  input: ScenarioInput,
  results: SimulationResults,
  mc: MonteCarloOutput,
): Promise<string | null> {
  const sb = getSupabaseClient();
  if (!sb) return null;

  // Upsert scenario_inputs
  const { data: inputRow, error: inputErr } = await sb
    .from('scenario_inputs')
    .insert({
      user_id:                   userId,
      age:                       input.age,
      family_type:               input.familyType,
      financial_assets:          input.financialAssets,
      annual_income:             input.annualIncome,
      annual_essential_expenses: input.annualEssentialExpenses,
      annual_comfort_expenses:   input.annualComfortExpenses,
      retirement_age:            input.retirementAge,
      expected_lifespan:         input.expectedLifespan,
      return_mode:               input.returnMode,
      detail_input_json:         {
        children: input.children,
        housing:  input.housing,
        care:     input.care,
        pensionMonthly: input.pensionMonthly,
      },
    })
    .select('id')
    .single();

  if (inputErr || !inputRow) return null;

  const inputId = inputRow.id;

  // Insert scenario results
  const resultRows = (['worst', 'main', 'upside'] as const).map(s => ({
    scenario_input_id: inputId,
    scenario_type:     s,
    required_assets:   results[s].requiredAssets,
    projected_assets:  results[s].projectedAssets,
    gap_value:         results[s].gapValue,
    age_at_depletion:  results[s].ageAtDepletion,
    milestone_90:      results[s].milestoneBalances.age90,
    milestone_95:      results[s].milestoneBalances.age95,
    cash_flow_json:    results[s].cashFlowTimeline,
  }));

  await sb.from('scenario_results').insert(resultRows);

  // Insert Monte Carlo result
  await sb.from('montecarlo_results').insert({
    scenario_input_id:  inputId,
    trials:             mc.trials,
    success_rate:       mc.successRate,
    percentiles_json:   mc.percentiles,
    depletion_dist_json: mc.ageAtDepletionDistribution,
  });

  return inputId;
}

/** Save family notes to Supabase (replaces all notes for user). */
export async function saveNotes(userId: string, notes: FamilyNote[]): Promise<void> {
  const sb = getSupabaseClient();
  if (!sb) return;

  // Delete existing and re-insert
  await sb.from('family_notes').delete().eq('user_id', userId);
  if (notes.length === 0) return;

  await sb.from('family_notes').insert(
    notes.map(n => ({
      id:         n.id,
      user_id:    userId,
      category:   n.category,
      content:    n.content,
      created_at: n.createdAt,
      updated_at: n.updatedAt,
    })),
  );
}

/** Create a shareable summary and return the share URL path. */
export async function createSharedSummary(
  payload: Omit<SharedSummary, 'shareId' | 'createdAt' | 'expiresAt'>,
  userId?: string,
): Promise<string | null> {
  const sb = getSupabaseClient();
  if (!sb) return null;

  const shareId = generateShareId();
  const now     = new Date();
  const expires = new Date(now.getTime() + 30 * 24 * 60 * 60 * 1000);

  const { error } = await sb.from('shared_summaries').insert({
    share_id:     shareId,
    user_id:      userId ?? null,
    payload_json: { ...payload, shareId, createdAt: now.toISOString(), expiresAt: expires.toISOString() },
  });

  if (error) return null;
  return shareId;
}

/** Fetch a shared summary by shareId. */
export async function getSharedSummary(shareId: string): Promise<SharedSummary | null> {
  const sb = getSupabaseClient();
  if (!sb) return null;

  const { data } = await sb
    .from('shared_summaries')
    .select('payload_json, expires_at')
    .eq('share_id', shareId)
    .single();

  if (!data) return null;
  if (new Date(data.expires_at) < new Date()) return null;

  return data.payload_json as SharedSummary;
}
