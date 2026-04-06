import type { ScenarioInput, SimulationResults, MonteCarloOutput, FamilyNote } from '@/lib/simulation/types';

const KEYS = {
  input:   'life_sim_input',
  results: 'life_sim_results',
  mc:      'life_sim_mc',
  notes:   'life_sim_notes',
} as const;

// ---- Generic safe get/set ----

function safeGet<T>(key: string): T | null {
  if (typeof window === 'undefined') return null;
  try {
    const raw = localStorage.getItem(key);
    return raw ? (JSON.parse(raw) as T) : null;
  } catch {
    return null;
  }
}

function safeSet(key: string, value: unknown): void {
  if (typeof window === 'undefined') return;
  try {
    localStorage.setItem(key, JSON.stringify(value));
  } catch {
    // quota exceeded or private mode — silently ignore
  }
}

// ---- Public API ----

export const localStore = {
  getInput:   ()            => safeGet<ScenarioInput>(KEYS.input),
  setInput:   (v: ScenarioInput) => safeSet(KEYS.input, v),

  getResults: ()            => safeGet<SimulationResults>(KEYS.results),
  setResults: (v: SimulationResults) => safeSet(KEYS.results, v),

  getMC:      ()            => safeGet<MonteCarloOutput>(KEYS.mc),
  setMC:      (v: MonteCarloOutput) => safeSet(KEYS.mc, v),

  getNotes:   ()            => safeGet<FamilyNote[]>(KEYS.notes) ?? [],
  setNotes:   (v: FamilyNote[]) => safeSet(KEYS.notes, v),

  clear:      () => {
    if (typeof window === 'undefined') return;
    Object.values(KEYS).forEach(k => localStorage.removeItem(k));
  },
};
