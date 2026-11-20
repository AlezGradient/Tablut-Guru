package model;

import java.util.ArrayList;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import utils.ReworkGameAshtonTablut;
import utils.TensorHandler;

// Agent with MinMax algorithm

public class BlackAgent implements IAgent {
	
	private ReworkGameAshtonTablut rules;
	private TensorHandler th;
	private boolean[][][] tensor;
	private int searchDepth;
	
	public BlackAgent(int searchDepth) {
		rules = (ReworkGameAshtonTablut) rules;
		this.searchDepth = searchDepth;
		rules = new ReworkGameAshtonTablut(99, 0, "fake", "fake");
		th = new TensorHandler();
	}
	
	// MinMax move
	
	@Override
	public Action getMove(State state) {
		
        tensor = th.stateToTensor(state);
        ArrayList<Action> actions = rules.getActions(state, tensor);
        if (actions.isEmpty()) {
            return null;
        }
        
        boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") || state.getTurn().equals(Turn.WHITE);
        Action bestAction = actions.get(0);
        
        float bestScore = isWhiteTurn ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;

        for (Action action : actions) {
            State clonedState = state.clone();
            State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
            
            float score = alphaBeta(newState, searchDepth - 1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, !isWhiteTurn);

            if (isWhiteTurn) {
                if (score > bestScore) {
                    bestScore = score;
                    bestAction = action;
                }
            } else {
                if (score < bestScore) {
                    bestScore = score;
                    bestAction = action;
                }
            }
        }
        return bestAction;
    }

	// Apply alpha-beta cut
	
    private float alphaBeta(State state, int depth, float alpha, float beta, boolean maximizingPlayer) {
        if (depth == 0 || isTerminalState(state)) {
            return evaluateState(state); 
        }

        ArrayList<Action> actions = rules.getActions(state, tensor);
        if (actions.isEmpty()) {
            return evaluateState(state);
        }

        if (maximizingPlayer) {
            float maxEval = Float.NEGATIVE_INFINITY;
            for (Action action : actions) {
                State clonedState = state.clone();
                State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
                

                float eval = alphaBeta(newState, depth - 1, alpha, beta, !maximizingPlayer); 
                
                maxEval = Math.max(maxEval, eval);
                alpha = Math.max(alpha, eval);
                if (beta <= alpha) {
                    break; // Beta cut-off
                }
            }
            return maxEval;
        } else {
            float minEval = Float.POSITIVE_INFINITY;
            for (Action action : actions) {
                State clonedState = state.clone();
                State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
                
                float eval = alphaBeta(newState, depth - 1, alpha, beta, !maximizingPlayer); 
                
                minEval = Math.min(minEval, eval);
                beta = Math.min(beta, eval);
                if (beta <= alpha) {
                    break; // Alpha cut-off
                }
            }
            return minEval;
        }
    }

    // Heuristic evaluation
    
    public float evaluateState(State state) {
    	// Controllo stato vincita/perdita tramite Enum di Turn o metodi nativi senza toLowerCase/toUpperCase inefficaci
        Turn turn = state.getTurn();
        if (turn == Turn.WHITEWIN) return 10000.0f;  
        if (turn == Turn.BLACKWIN) return -10000.0f; 
        if (turn == Turn.DRAW) return 0.0f;

        float score = 0.0f;

        int whitePawns = state.getNumberOf(Pawn.WHITE);
        int blackPawns = state.getNumberOf(Pawn.BLACK);
        
        score += (whitePawns * 10.0f);
        score -= (blackPawns * 10.0f);

        int newKingR = 4, newKingC = 4;
        boolean kingFound = false;
        
        // Ottimizzazione: se la libreria ha un metodo state.getKing() usalo, altrimenti mantieni la scansione ma usa confronti via Enum/Costanti
        Pawn kingPawn = Pawn.KING; 
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                Pawn p = state.getPawn(r, c);
                if (p == kingPawn || p == Pawn.KING) { // Confronto diretto di Enum, molto più veloce di .equalsPawn()
                    newKingR = r;
                    newKingC = c;
                    kingFound = true;
                    break;
                }
            }
            if (kingFound) break;
        }

        int kingDistanceFromCenter = Math.abs(newKingR - 4) + Math.abs(newKingC - 4);
        score += kingDistanceFromCenter * 3.0f; 

        int surrounders = 0;
        Pawn blackPawn = Pawn.BLACK;
        if (newKingR > 0 && state.getPawn(newKingR - 1, newKingC) == blackPawn) surrounders++;
        if (newKingR < 8 && state.getPawn(newKingR + 1, newKingC) == blackPawn) surrounders++;
        if (newKingC > 0 && state.getPawn(newKingR, newKingC - 1) == blackPawn) surrounders++;
        if (newKingC < 8 && state.getPawn(newKingR, newKingC + 1) == blackPawn) surrounders++;

        score -= (surrounders * 5.0f);

        int[][] corners = { {0, 0}, {0, 8}, {8, 0}, {8, 8} };
        int openEscapeRoutes = 0;

        for (int[] corner : corners) {
            int cR = corner[0];
            int cC = corner[1];
            
            if (newKingR == cR || newKingC == cC) {
                boolean pathBlocked = false;
                int rStep = Integer.compare(cR, newKingR);
                int cStep = Integer.compare(cC, newKingC);
                int currR = newKingR + rStep;
                int currC = newKingC + cStep;
                
                while (currR != cR || currC != cC) {
                    // Assicurati che il metodo per controllare casella vuota sia coerente con il tuo codice
                    if (!state.getPawn(currR, currC).equalsPawn("O") && !state.getPawn(currR, currC).equalsPawn("E")) {
                        pathBlocked = true;
                        break;
                    }
                    currR += rStep;
                    currC += cStep;
                }
                
                if (!pathBlocked) {
                    openEscapeRoutes++;
                }
            }
        }

        score += (openEscapeRoutes * 20.0f);

        int[][] adjDirs = { {-1, 0}, {1, 0}, {0, -1}, {0, 1} };
        int kingMobility = 0;
        for (int[] dir : adjDirs) {
            int nextR = newKingR + dir[0];
            int nextC = newKingC + dir[1];
            if (nextR >= 0 && nextR <= 8 && nextC >= 0 && nextC <= 8) {
                if (state.getPawn(nextR, nextC).equalsPawn("O") || state.getPawn(nextR, nextC).equalsPawn("E")) {
                    kingMobility++;
                }
            }
        }
        score += (kingMobility * 2.0f);

        return score;
    }

    private boolean isTerminalState(State state) {
        String turnStr = state.getTurn().toString().toUpperCase();
        return turnStr.contains("WIN") || turnStr.contains("DRAW");
    }
    
    public void setDepth(int depth) {
    	searchDepth = depth;
    }
}
