package Test;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import model.Agent;
import model.BlackAgent;
import model.IAgent;
import model.WhiteAgent;
import it.unibo.ai.didattica.competition.tablut.domain.StateTablut;
import utils.ReworkGameAshtonTablut;
import utils.TensorHandler;
import utils.Transition;

public class TrainingEnvironment {
    // Epsilon decay for exploration (STOPPED)
    // private static float EPSILON_DECAY = 30000.0f;
    private int actualDepth;
    private TensorHandler th;
    
    public TrainingEnvironment() {
    	actualDepth = 1; // Starting depth for the master
    	th = new TensorHandler();
    }

    public void runTraining(int totalEpisodes) {
        WhiteAgent whiteAgent = new WhiteAgent();	// Neural Network (apprentice)
        BlackAgent blackAgent = new BlackAgent(actualDepth); // MinMax (master)
        
        int whiteWins = 0, blackWins = 0, draws = 0;
        
        float whiteEpsilon = 0.05f;					// Sharping policies
        
        System.out.println("Training started");
        
        Queue<Integer> recentWhiteWins = new LinkedList<>();
        
        for (int episode = 1; episode <= totalEpisodes; episode++) {
            whiteAgent.resetEpisode();
            
            blackAgent.setDepth(calculateDynamicDepth(episode, recentWhiteWins));
            whiteAgent.setEpsilon(whiteEpsilon);

            StateTablut state = new StateTablut();
            state.setTurn(Turn.WHITE);
            
            ReworkGameAshtonTablut game = new ReworkGameAshtonTablut(99, 0, "W", "B");
            
            List<Transition> whiteTransitions = new ArrayList<>();

            int turnCount = 0;
            int maxTurns = 100; 
            int kingR = 4, kingC = 4;

            while (turnCount < maxTurns) {
                if (state.getTurn().equalsTurn("WW") || state.getTurn().equalsTurn("BW") || 
                    state.getTurn().equals(Turn.WHITEWIN) || state.getTurn().equals(Turn.BLACKWIN)) {
                    break;
                }

                boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") || state.getTurn().equals(Turn.WHITE);
                IAgent currentAgent = isWhiteTurn ? whiteAgent : blackAgent;

                // Check the number of pawns before the move
                int prevWhitePawns = state.getNumberOf(Pawn.WHITE);
                int prevBlackPawns = state.getNumberOf(Pawn.BLACK);

                Action move;
                try {
                    move = currentAgent.getMove(state);
                } catch (Exception e) {
                    if (isWhiteTurn) {
                        state.setTurn(Turn.BLACKWIN);
                    } else {
                        state.setTurn(Turn.WHITEWIN);
                    }
                    break;
                }
                
                try {
                    state = (StateTablut) game.movePawn(state, move);

                    // Check the amount of pawns after the move
                    int afterWhitePawns = state.getNumberOf(Pawn.WHITE);
                    int afterBlackPawns = state.getNumberOf(Pawn.BLACK);

                    boolean[][][] resultingTensor = th.stateToTensor(state);

                    // Find the king position
                    int newKingR = kingR, newKingC = kingC;
                    boolean kingFound = false;
                    for (int r = 0; r < 9; r++) {
                        for (int c = 0; c < 9; c++) {
                            if (state.getPawn(r, c).equalsPawn("K")) {
                                newKingR = r;
                                newKingC = c;
                                kingFound = true;
                                break;
                            }
                        }
                        if (kingFound) break;
                    }
                    
                    // Little penalty for longer games
                    double stepReward = -0.05;            
                
                    if (isWhiteTurn) {
                    	
                    	// Check the distance of the king from the border
                        int oldDistToEdge = Math.min(Math.min(kingR, 8 - kingR), Math.min(kingC, 8 - kingC));
                        int newDistToEdge = Math.min(Math.min(newKingR, 8 - newKingR), Math.min(newKingC, 8 - newKingC));
                        if (newDistToEdge < oldDistToEdge) stepReward += 2.0;
                        
                        // Check escape routes for white
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
                        
                        // Check king's surrounders
                        int surrounders = 0;
                        if (newKingR > 0 && state.getPawn(newKingR - 1, newKingC).equalsPawn("B")) surrounders++;
                        if (newKingR < 8 && state.getPawn(newKingR + 1, newKingC).equalsPawn("B")) surrounders++;
                        if (newKingC > 0 && state.getPawn(newKingR, newKingC - 1).equalsPawn("B")) surrounders++;
                        if (newKingC < 8 && state.getPawn(newKingR, newKingC + 1).equalsPawn("B")) surrounders++;

                        // The more the king is surrounded the more the white is in danger
                        stepReward -= (surrounders * 3.0f);
                        
                        // If white has open escape routes it's a good sign for white
                        stepReward += (openEscapeRoutes * 3.0f);
                        
                        stepReward += (prevBlackPawns - afterBlackPawns) - ((prevWhitePawns - afterWhitePawns) * 4);
                    }

                    kingR = newKingR;
                    kingC = newKingC;

                    // Store white moves
                    if (isWhiteTurn) {
                        Transition t = new Transition(resultingTensor, move);
                        t.addReward(stepReward);
                        whiteTransitions.add(t);
                    }

                } catch (Exception e) {
                	e.printStackTrace();
                    break;
                }
                turnCount++;
            }

            boolean whiteWon = state.getTurn().equalsTurn("WW") || state.getTurn().equals(Turn.WHITEWIN);
            boolean blackWon = state.getTurn().equalsTurn("BW") || state.getTurn().equals(Turn.BLACKWIN);

            if (whiteWon) {
                whiteWins++;
            } else if (blackWon) {
                blackWins++;
            } else {
                draws++;
            }

            // Register white wins to increase training difficulty if necessary
            recentWhiteWins.add(whiteWon ? 1 : 0);
            if (recentWhiteWins.size() > 1000) {
                recentWhiteWins.poll(); 
            }


            // Establishing final white rewards for the game
            double finalWhiteReward = whiteWon ? 15.0 : (blackWon ? -15.0 : -5.0);
            
            // Extra penalty if white loses fast
            if (blackWon && turnCount < 30) {
                finalWhiteReward -= (30 - turnCount);
            }
            
            for (Transition t : whiteTransitions) {
                t.addReward(finalWhiteReward);
            }

            boolean saveToDisk = (episode % 20 == 0 || episode == totalEpisodes);

            // Train white on transitions
            if (!whiteTransitions.isEmpty()) {
                whiteAgent.trainOnTransitions(whiteTransitions, saveToDisk);
            }

            if (episode % 10 == 0 || episode == 1) {
                System.out.println(String.format("Episodio %3d/%d | Vincitore: %-6s | Turni: %3d | WhiteWins: %2d | Actual Depth: %d", 
                    episode, totalEpisodes, (whiteWon ? "WHITE" : (blackWon ? "BLACK" : "DRAW")), turnCount, whiteWins, actualDepth));
            }
        }

        System.out.println("\n=== Training Completato ===");
        System.out.println(String.format("Stats finali -> White Wins: %d | Black Wins: %d | Draws: %d | WhiteEpsilon: %.2f | Actual Depth: %d", 
            whiteWins, blackWins, draws, whiteEpsilon, actualDepth));
    }
    
    // Increase master's strength if white becomes stronger
    private int calculateDynamicDepth(int episode, Queue<Integer> recentWhiteWins) {
//        double whiteWinRate = 0.0;
//        if (recentWhiteWins.size() >= 1000) { 
//            int wins = recentWhiteWins.stream().mapToInt(Integer::intValue).sum();
//            whiteWinRate = (double) wins / recentWhiteWins.size();
//        }
//        
//        if (whiteWinRate >= 0.75) {
//        	recentWhiteWins.clear();
//        	actualDepth += 1;
//        }
//        
    	actualDepth = 4;
        return actualDepth;

    }
}