'use client';

import {
  createContext,
  useContext,
  useReducer,
  useEffect,
  useMemo,
  useCallback,
  type ReactNode,
  type Dispatch,
} from 'react';
import type {
  ScenarioInput,
  ScenarioType,
  SimulationResults,
  MonteCarloOutput,
  FamilyNote,
  NoteCategory,
  ReverseSimulationResult,
  SpendableMargin,
  ReverseSimulationInput,
} from '@/lib/simulation/types';
import { runAllScenarios } from '@/lib/simulation/engine';
import { runMonteCarlo }   from '@/lib/simulation/montecarlo';
import { computeSpendableMargins, runReverseSimulation, computeSustainableMonthlyBudget, type SustainableMonthlyBudget } from '@/lib/simulation/reverseEngine';
import { localStore }      from '@/lib/storage/localStore';
import { v4 as uuidv4 }   from 'uuid';

// ---- Default input values ----

export const DEFAULT_INPUT: ScenarioInput = {
  age:                      40,
  familyType:               'couple',
  financialAssets:          500,
  annualIncome:             600,
  annualEssentialExpenses:  240,
  annualComfortExpenses:    60,
  retirementAge:            65,
  expectedLifespan:         90,
  returnMode:               'moderate',
  children:                 [],
  retirementBonus:          0,
};

// ---- State ----

interface SimState {
  input:    ScenarioInput;
  results:  SimulationResults | null;
  monteCarlo: MonteCarloOutput | null;
  notes:    FamilyNote[];
  isDirty:  boolean; // input changed since last calculation
  /** Spendable margins for each scenario — computed eagerly */
  spendableMargins: Record<ScenarioType, SpendableMargin[]> | null;
  /** Sustainable monthly budget per scenario */
  sustainableBudgets: Record<ScenarioType, SustainableMonthlyBudget> | null;
  /** Reverse simulation results — computed on-demand */
  reverseResults: Record<ScenarioType, ReverseSimulationResult> | null;
}

// ---- Actions ----

type Action =
  | { type: 'SET_INPUT';   payload: Partial<ScenarioInput> }
  | { type: 'SET_RESULTS'; payload: {
      results: SimulationResults;
      mc: MonteCarloOutput;
      margins: Record<ScenarioType, SpendableMargin[]>;
      sustainableBudgets: Record<ScenarioType, SustainableMonthlyBudget>;
    } }
  | { type: 'SET_REVERSE_RESULTS'; payload: Record<ScenarioType, ReverseSimulationResult> }
  | { type: 'ADD_NOTE';    payload: { category: NoteCategory; content: string } }
  | { type: 'UPDATE_NOTE'; payload: { id: string; patch: Partial<Pick<FamilyNote, 'content' | 'category' | 'includeInSummary'>> } }
  | { type: 'DELETE_NOTE'; payload: { id: string } }
  | { type: 'LOAD_STORED'; payload: Partial<SimState> };

function reducer(state: SimState, action: Action): SimState {
  switch (action.type) {
    case 'SET_INPUT':
      return {
        ...state,
        input: { ...state.input, ...action.payload },
        isDirty: true,
      };

    case 'SET_RESULTS':
      return {
        ...state,
        results:           action.payload.results,
        monteCarlo:        action.payload.mc,
        spendableMargins:  action.payload.margins,
        sustainableBudgets: action.payload.sustainableBudgets,
        isDirty:           false,
      };

    case 'SET_REVERSE_RESULTS':
      return {
        ...state,
        reverseResults: action.payload,
      };

    case 'ADD_NOTE': {
      const now = new Date().toISOString();
      const note: FamilyNote = {
        id:              uuidv4(),
        category:        action.payload.category,
        content:         action.payload.content,
        createdAt:       now,
        updatedAt:       now,
        includeInSummary: true,
      };
      return { ...state, notes: [note, ...state.notes] };
    }

    case 'UPDATE_NOTE': {
      return {
        ...state,
        notes: state.notes.map(n =>
          n.id === action.payload.id
            ? { ...n, ...action.payload.patch, updatedAt: new Date().toISOString() }
            : n,
        ),
      };
    }

    case 'DELETE_NOTE':
      return { ...state, notes: state.notes.filter(n => n.id !== action.payload.id) };

    case 'LOAD_STORED':
      return { ...state, ...action.payload, isDirty: false };

    default:
      return state;
  }
}

// ---- Context value ----

interface SimContextValue {
  state:         SimState;
  dispatch:      Dispatch<Action>;
  runSimulation: () => void;
  runReverse:    (target: ReverseSimulationInput) => void;
  setInput:      (patch: Partial<ScenarioInput>) => void;
}

const SimulationContext = createContext<SimContextValue | null>(null);

// ---- Provider ----

export function SimulationProvider({ children }: { children: ReactNode }) {
  const [state, dispatch] = useReducer(reducer, {
    input:              DEFAULT_INPUT,
    results:            null,
    monteCarlo:         null,
    notes:              [],
    isDirty:            true,
    spendableMargins:   null,
    sustainableBudgets: null,
    reverseResults:     null,
  });

  // Load from localStorage on first mount
  useEffect(() => {
    const storedInput   = localStore.getInput();
    const storedResults = localStore.getResults();
    const storedMC      = localStore.getMC();
    const storedNotes   = localStore.getNotes();

    dispatch({
      type: 'LOAD_STORED',
      payload: {
        input:      storedInput   ?? DEFAULT_INPUT,
        results:    storedResults ?? null,
        monteCarlo: storedMC      ?? null,
        notes:      storedNotes,
      },
    });
  }, []);

  // Persist input + notes whenever they change
  useEffect(() => {
    localStore.setInput(state.input);
  }, [state.input]);

  useEffect(() => {
    localStore.setNotes(state.notes);
  }, [state.notes]);

  const runSimulation = useCallback(() => {
    const results    = runAllScenarios(state.input);
    const monteCarlo = runMonteCarlo(state.input);

    // Compute spendable margins at milestone ages
    const milestoneAges = [65, 70, 75, 80, 85, 90, 95];
    const margins: Record<ScenarioType, SpendableMargin[]> = {
      worst:  computeSpendableMargins(state.input, 'worst', milestoneAges),
      main:   computeSpendableMargins(state.input, 'main', milestoneAges),
      upside: computeSpendableMargins(state.input, 'upside', milestoneAges),
    };

    // Compute sustainable monthly budget (total spending to reach 0 at expectedLifespan)
    const sustainableBudgets: Record<ScenarioType, SustainableMonthlyBudget> = {
      worst:  computeSustainableMonthlyBudget(state.input, 'worst'),
      main:   computeSustainableMonthlyBudget(state.input, 'main'),
      upside: computeSustainableMonthlyBudget(state.input, 'upside'),
    };

    dispatch({ type: 'SET_RESULTS', payload: { results, mc: monteCarlo, margins, sustainableBudgets } });
    localStore.setResults(results);
    localStore.setMC(monteCarlo);
  }, [state.input]);

  const runReverse = useCallback((target: ReverseSimulationInput) => {
    const scenarios: ScenarioType[] = ['worst', 'main', 'upside'];
    const reverseResults = {} as Record<ScenarioType, ReverseSimulationResult>;
    for (const s of scenarios) {
      reverseResults[s] = runReverseSimulation(
        state.input, s, target.targetAge, target.targetBalance,
      );
    }
    dispatch({ type: 'SET_REVERSE_RESULTS', payload: reverseResults });
  }, [state.input]);

  const setInput = useCallback((patch: Partial<ScenarioInput>) => {
    dispatch({ type: 'SET_INPUT', payload: patch });
  }, []);

  const value = useMemo(
    () => ({ state, dispatch, runSimulation, runReverse, setInput }),
    [state, dispatch, runSimulation, runReverse, setInput],
  );

  return (
    <SimulationContext.Provider value={value}>
      {children}
    </SimulationContext.Provider>
  );
}

// ---- Hook ----

export function useSimulation() {
  const ctx = useContext(SimulationContext);
  if (!ctx) throw new Error('useSimulation must be used inside SimulationProvider');
  return ctx;
}
