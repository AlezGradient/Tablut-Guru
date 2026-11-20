package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.Game;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Pawn;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import utils.AgentTranslator;
import utils.ReworkGameAshtonTablut;
import utils.Transition;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.nn.core.Linear;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import ai.djl.ndarray.NDList;
import ai.djl.training.GradientCollector;

public class Agent {

    public boolean[][][] tensor;
    private Game rules;
    public Predictor<boolean[][][], Float> predictor;
    private NDManager manager;
    private float epsilon;     
    private Model model;
    private final java.util.LinkedList<String> moveHistory = new java.util.LinkedList<>();
    private final java.util.Set<String> visitedStatesInEpisode = new java.util.HashSet<>();
    private static final int HISTORY_LIMIT = 6;
    private final String modelName;

    public Agent(String modelName) {
        this.modelName = modelName;
        tensor = new boolean[9][9][6];
        specialSquares(tensor);
        rules = new ReworkGameAshtonTablut(99, 0, "fake", "fake");
        manager = NDManager.newBaseManager().newSubManager();
        model = Model.newInstance(modelName);

        SequentialBlock block = new SequentialBlock()
        		.add(Conv2d.builder()
        				.setFilters(32)
        				.setKernelShape(new Shape(3, 3))
        				.optPadding(new Shape(1, 1))
        				.build())
        		.add(Activation::relu)
        		.add(Blocks.batchFlattenBlock())
        		.add(Linear.builder().setUnits(1).build());

        block.initialize(manager, ai.djl.ndarray.types.DataType.FLOAT32, new Shape(1, 6, 9, 9));
        model.setBlock(block); 

        Path path = Paths.get("Model");

        try {
            Files.createDirectories(path);
            Path paramsFile = path.resolve(modelName + "-0000.params");

            if (Files.exists(paramsFile)) {
                model.load(path, modelName);
            } else {
                model.save(path, modelName);
            }
        } catch (MalformedModelException | IOException e) {
            e.printStackTrace();
        }

        AgentTranslator translator = new AgentTranslator();
        this.predictor = model.newPredictor(translator);
        epsilon = 0f;    
    }

    public Model getModel() {
    	return model;
    }
    
    
    public ArrayList<Action> getActions(State state) {
        ArrayList<Action> actions = new ArrayList<>();
        boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") || state.getTurn().equals(Turn.WHITE);
        int boardSize = state.getBoard().length;

        for (int r = 0; r < boardSize; r++) {
            for (int c = 0; c < boardSize; c++) {
                State.Pawn currentPawn = state.getPawn(r, c);
                
                boolean isOurPawn = isWhiteTurn ? 
                    (currentPawn.equalsPawn("W") || currentPawn.equalsPawn("K")) : 
                    currentPawn.equalsPawn("B");

                if (isOurPawn) {
                    String pawnBox = state.getBox(r, c);
                    int[][] directions = {{-1, 0}, {1, 0}, {0, -1}, {0, 1}};
                    
                    for (int[] dir : directions) {
                        int nextR = r + dir[0];
                        int nextC = c + dir[1];
                        
                        while (nextR >= 0 && nextR < boardSize && nextC >= 0 && nextC < boardSize) {
                            if (tensor[nextR][nextC][2]) {
                                try {
                                    Action a = new Action(pawnBox, state.getBox(nextR, nextC), state.getTurn());
                                    rules.checkMove(state, a);
                                    actions.add(a);
                                } catch (Exception e) {}
                            } else {
                                break;
                            }
                            nextR += dir[0];
                            nextC += dir[1];
                        }
                    }
                }
            }
        }
        return actions;
    }

    public void specialSquares(boolean[][][] t) {
        t[3][0][4] = true; t[4][0][4] = true; t[5][0][4] = true; t[4][1][4] = true;
        t[3][8][4] = true; t[4][8][4] = true; t[5][8][4] = true; t[4][7][4] = true;
        t[0][3][4] = true; t[0][4][4] = true; t[0][5][4] = true; t[1][4][4] = true;
        t[8][3][4] = true; t[8][4][4] = true; t[8][5][4] = true; t[7][4][4] = true;
        t[4][4][4] = true;
    }

    public boolean[][][] stateToTensor(State state) {
    	boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") 
    	                   || state.getTurn().equals(Turn.WHITE) 
    	                   || (state.getTurn().toString().equalsIgnoreCase("WHITE"));
        boolean[][][] new_tensor = new boolean[9][9][6];
        specialSquares(new_tensor);

        for (int i = 0; i < state.getBoard().length; i++) {
            for (int j = 0; j < state.getBoard().length; j++) {
                State.Pawn pawn = state.getPawn(i, j);
                if (pawn.equalsPawn(State.Pawn.WHITE.toString()) || pawn.equalsPawn("W")) {
                    new_tensor[i][j][0] = true;
                } else if (pawn.equalsPawn(State.Pawn.BLACK.toString()) || pawn.equalsPawn("B")) {
                    new_tensor[i][j][1] = true;
                } else if (pawn.equalsPawn(State.Pawn.EMPTY.toString()) || pawn.equalsPawn("O") || pawn.equalsPawn("E")) {
                    new_tensor[i][j][2] = true;
                } else if (pawn.equalsPawn(State.Pawn.KING.toString()) || pawn.equalsPawn("K")) {
                    new_tensor[i][j][3] = true;
                }
                new_tensor[i][j][5] = isWhiteTurn;
            }
        }
        return new_tensor;
    }

    public Action getMove(State state) throws Exception {
        this.tensor = stateToTensor(state);
        ArrayList<Action> actions = getActions(state);
        if (actions.isEmpty()) {
            throw new Exception("No valid actions found for " + state.getTurn());
        }

        float random = new Random().nextFloat(0.0f, 1.0f);
        if (random < epsilon) {
            Action chosen = actions.get(new Random().nextInt(actions.size()));
            recordMoveAndState(chosen, state);
            return chosen;
        }

        Action bestAction = actions.get(0);
        float bestScore = Float.NEGATIVE_INFINITY;

        for (Action action : actions) {
            State clonedState = state.clone();
            State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
            if(newState.getTurn().equals(Turn.WHITEWIN)) return action;
            
            boolean[][][] newTensor = stateToTensor(newState);

            try {
                float score = predictor.predict(newTensor);
                String moveSig = action.getFrom() + "->" + action.getTo();
                if (moveHistory.contains(moveSig)) {
                    score -= 1.5f; 
                }

                if (score > bestScore) {
                    bestScore = score;
                    bestAction = action;
                }
            } catch (Exception e) {}
        }
        
        recordMoveAndState(bestAction, state);
        return bestAction;
    }

    private void recordMoveAndState(Action a, State currentState) {
    	String sig = a.getFrom() + "->" + a.getTo();
        moveHistory.addLast(sig);
        if (moveHistory.size() > HISTORY_LIMIT) {
            moveHistory.removeFirst();
        }
        visitedStatesInEpisode.add(currentState.toString());
    }
    
    public void setEpsilon(float epsilon) {
        this.epsilon = epsilon;
    }

    public void trainOnTransitions(List<Transition> transitions, boolean saveToDisk) {
        TrainingConfig config = new DefaultTrainingConfig(Loss.l2Loss())
            .optOptimizer(Optimizer.adam()
                .optLearningRateTracker(Tracker.fixed(0.001f))
                .build());

        int batchSize = 32;

        try (Trainer trainer = model.newTrainer(config)) {
            trainer.initialize(new Shape(1, 6, 9, 9));

            for (int i = 0; i < transitions.size(); i += batchSize) {
                int end = Math.min(i + batchSize, transitions.size());
                List<Transition> batch = transitions.subList(i, end);
                int currentBatchSize = batch.size();

                float[] batchInputData = new float[currentBatchSize * 6 * 9 * 9];
                float[] batchTargetData = new float[currentBatchSize];

                int inputIdx = 0;
                int targetIdx = 0;

                for (Transition t : batch) {
                    boolean[][][] tData = t.getTensor();
                    for (int c = 0; c < 6; c++) {
                        for (int r = 0; r < 9; r++) {
                            for (int col = 0; col < 9; col++) {
                                batchInputData[inputIdx++] = tData[r][col][c] ? 1.0f : 0.0f;
                            }
                        }
                    }
                    batchTargetData[targetIdx++] = (float) t.getReward();
                }

                try (NDManager subManager = manager.newSubManager()) {
                    NDArray inputData = subManager.create(batchInputData, new Shape(currentBatchSize, 6, 9, 9));
                    NDArray targetReward = subManager.create(batchTargetData, new Shape(currentBatchSize, 1));

                    NDList inputs = new NDList(inputData);
                    NDList labels = new NDList(targetReward);

                    try (GradientCollector collector = trainer.newGradientCollector()) {
                        NDList predictions = trainer.forward(inputs);
                        NDArray loss = trainer.getLoss().evaluate(labels, predictions);
                        collector.backward(loss);
                    }
                    trainer.step();
                }
            }
        }

        if (saveToDisk) {
            try {
                model.setProperty("Epoch", "0");
                model.save(Paths.get("Model"), modelName);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
    
    public void resetEpisode() {
        moveHistory.clear();
        visitedStatesInEpisode.clear();
    }
    
    public Action getMoveMinMax(State state, int searchDepth) throws Exception {
        this.tensor = stateToTensor(state);
        ArrayList<Action> actions = getActions(state);
        if (actions.isEmpty()) {
            throw new Exception("No valid actions found for " + state.getTurn());
        }

        // Gestione epsilon-greedy per eventuale esplorazione durante il training
        float random = new Random().nextFloat(0.0f, 1.0f);
        if (random < epsilon) {
            Action chosen = actions.get(new Random().nextInt(actions.size()));
            recordMoveAndState(chosen, state);
            return chosen;
        }

        boolean isWhiteTurn = state.getTurn().equalsTurn("WHITE") || state.getTurn().equals(Turn.WHITE);
        Action bestAction = actions.get(0);
        
        // Bianco massimizza, Nero minimizza
        float bestScore = isWhiteTurn ? Float.NEGATIVE_INFINITY : Float.POSITIVE_INFINITY;

        for (Action action : actions) {
            State clonedState = state.clone();
            State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
            
            // Esegue l'Alpha-Beta indipendente dalla rete neurale
            float score = alphaBeta(newState, searchDepth - 1, Float.NEGATIVE_INFINITY, Float.POSITIVE_INFINITY, !isWhiteTurn);

            // Penalità opzionale per cicli di mosse ripetute
            String moveSig = action.getFrom() + "->" + action.getTo();
            if (moveHistory.contains(moveSig)) {
                score += isWhiteTurn ? -1.5f : 1.5f; 
            }

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
        
        recordMoveAndState(bestAction, state);
        return bestAction;
    }

    private float alphaBeta(State state, int depth, float alpha, float beta, boolean maximizingPlayer) {
        if (depth == 0 || isTerminalState(state)) {
            return evaluateState(state); // <--- Qui usiamo la funzione euristica invece della NN
        }

        ArrayList<Action> actions = getActions(state);
        if (actions.isEmpty()) {
            return evaluateState(state);
        }

        if (maximizingPlayer) {
            float maxEval = Float.NEGATIVE_INFINITY;
            for (Action action : actions) {
                State clonedState = state.clone();
                State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
                
                // CORRETTO: Inverte il turno passando !maximizingPlayer (quindi false)
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
                
                // CORRETTO: Inverte il turno passando !maximizingPlayer (quindi true)
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

    /**
     * FUNZIONE DI VALUTAZIONE EURISTICA INDIPENDENTE
     * Qui puoi definire facilmente quanto vale una posizione per il Bianco (valori alti) 
     * o per il Nero (valori bassi).
     */
    public float evaluateState(State state) {
        // 1. Controllo stati terminali immediati
        String turnStr = state.getTurn().toString().toUpperCase();
        if (turnStr.contains("WHITEWIN")) return 10000.0f;  // Ottimo per Bianco (Max)
        if (turnStr.contains("BLACKWIN")) return -10000.0f; // Ottimo per Nero (Min)
        if (turnStr.contains("DRAW")) return 0.0f;

        float score = 0.0f;

        // 2. Controllo materiale (conteggio pezzi attuali)
        int whitePawns = state.getNumberOf(Pawn.WHITE);
        int blackPawns = state.getNumberOf(Pawn.BLACK);
        
        // Più pedoni bianchi ha il bianco (+), meno pedoni neri ci sono (- per favorire il Nero)
        score += (whitePawns * 10.0f);
        score -= (blackPawns * 10.0f);

        // 3. Posizione del Re
        int newKingR = 4, newKingC = 4;
        boolean kingFound = false;
        for (int r = 0; r < 9; r++) {
            for (int c = 0; c < 9; c++) {
                if (state.getPawn(r, c).equalsPawn("K") || state.getPawn(r, c).equalsPawn(State.Pawn.KING.toString())) {
                    newKingR = r;
                    newKingC = c;
                    kingFound = true;
                    break;
                }
            }
            if (kingFound) break;
        }

        // Distanza del Re dal centro (4,4)
        // Il Bianco vuole che il Re vada verso i bordi/angoli -> punteggio ALTO
        // Il Nero vuole che il Re resti al centro -> punteggio BASSO
        int kingDistanceFromCenter = Math.abs(newKingR - 4) + Math.abs(newKingC - 4);
        score += kingDistanceFromCenter * 3.0f; 

        // 4. Pressione e accerchiamento del Re (Favorevole al Nero -> abbassa lo score)
        int surrounders = 0;
        if (newKingR > 0 && state.getPawn(newKingR - 1, newKingC).equalsPawn("B")) surrounders++;
        if (newKingR < 8 && state.getPawn(newKingR + 1, newKingC).equalsPawn("B")) surrounders++;
        if (newKingC > 0 && state.getPawn(newKingR, newKingC - 1).equalsPawn("B")) surrounders++;
        if (newKingC < 8 && state.getPawn(newKingR, newKingC + 1).equalsPawn("B")) surrounders++;

        // Più il re è accerchiato, più lo score scende (favorevole al Nero)
        score -= (surrounders * 5.0f);

        // 5. Vie di fuga verso gli angoli (Pericoloso per il Nero -> alza lo score)
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

        // Se ci sono vie di fuga aperte, il punteggio sale (vantaggio per il Bianco)
        score += (openEscapeRoutes * 20.0f);

        // Mobilità immediata del Re
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
        // Più caselle libere ha intorno il re, più sale lo score (vantaggio Bianco)
        score += (kingMobility * 2.0f);

        return score;
    }

    private boolean isTerminalState(State state) {
        String turnStr = state.getTurn().toString().toUpperCase();
        return turnStr.contains("WIN") || turnStr.contains("DRAW");
    }
    
    public String getModelName() {
    	return modelName;
    }
}