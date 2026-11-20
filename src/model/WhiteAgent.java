package model;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.Set;

import ai.djl.MalformedModelException;
import ai.djl.Model;
import ai.djl.inference.Predictor;
import ai.djl.ndarray.NDArray;
import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.nn.Activation;
import ai.djl.nn.Blocks;
import ai.djl.nn.SequentialBlock;
import ai.djl.nn.convolutional.Conv2d;
import ai.djl.nn.core.Linear;
import ai.djl.training.DefaultTrainingConfig;
import ai.djl.training.GradientCollector;
import ai.djl.training.Trainer;
import ai.djl.training.TrainingConfig;
import ai.djl.training.loss.Loss;
import ai.djl.training.optimizer.Optimizer;
import ai.djl.training.tracker.Tracker;
import it.unibo.ai.didattica.competition.tablut.domain.Action;
import it.unibo.ai.didattica.competition.tablut.domain.State;
import it.unibo.ai.didattica.competition.tablut.domain.State.Turn;
import utils.AgentTranslator;
import utils.ReworkGameAshtonTablut;
import utils.TensorHandler;
import utils.Transition;

public class WhiteAgent implements IAgent {
	public boolean[][][] tensor;
    private ReworkGameAshtonTablut rules;
    public Predictor<boolean[][][], Float> predictor;
    private NDManager manager;
    private float epsilon;     
    private Model model;
    private final LinkedList<String> moveHistory = new LinkedList<>();
    private final Set<String> visitedStatesInEpisode = new HashSet<>();
    private static final int HISTORY_LIMIT = 6;
    private final String MODELNAME = "WhiteAgent";
    private TensorHandler th;
    
    
	public WhiteAgent() {
        th = new TensorHandler();
        rules = new ReworkGameAshtonTablut(99, 0, "fake", "fake");

        manager = ai.djl.engine.Engine.getEngine("PyTorch").newBaseManager().newSubManager();
        model = Model.newInstance(MODELNAME);
        
        // Net properties: 
        // - 1 convolutional layer: kernel 3x3, padding 1x1 (keep the same dimensions)
        // - ReLU as activation function 
        // - Linear output: 1 float value (score)
        // - Transform tensor in a 1D vector for each channel (flatten batches)
        
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
            Path paramsFile = path.resolve(MODELNAME + "-0000.params");

            if (Files.exists(paramsFile)) {
                model.load(path, MODELNAME);
            } else {
                model.save(path, MODELNAME);
            }
        } catch (MalformedModelException | IOException e) {
            e.printStackTrace();
        }
        
        
        AgentTranslator translator = new AgentTranslator();
        this.predictor = model.newPredictor(translator);
        
        // The model is ready to play
        epsilon = 0f;
	}
	
	
	@Override
	public Action getMove(State state) {
		tensor = th.stateToTensor(state);
		
        ArrayList<Action> actions = rules.getActions(state, tensor);
        if (actions.isEmpty()) {
            return null;
        }

        float random = new Random().nextFloat(0.0f, 1.0f);
        
        // Exploration
        if (random < epsilon) {
            Action chosen = actions.get(new Random().nextInt(actions.size()));
            recordMoveAndState(chosen, state);
            return chosen;
        }
        
        // Exploitation
        Action bestAction = actions.get(0);
        float bestScore = Float.NEGATIVE_INFINITY;

        for (Action action : actions) {
            State clonedState = state.clone();
            State newState = ((ReworkGameAshtonTablut) rules).movePawn(clonedState, action);
            
            // If the nextState is a win for white don't calculate anything else
            if(newState.getTurn().equals(Turn.WHITEWIN)) return action;
            
            boolean[][][] newTensor = th.stateToTensor(newState);

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
	
	public void resetEpisode() {
        moveHistory.clear();
        visitedStatesInEpisode.clear();
    }
	
    public void trainOnTransitions(List<Transition> transitions, boolean saveToDisk) {
    	
    	// Train with Adam optimizer + L2 penalty
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
                
                // Calculate the loss and backpropagate
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
        
        // Update the same model file
        if (saveToDisk) {
            try {
                model.setProperty("Epoch", "0");
                model.save(Paths.get("Model"), MODELNAME);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
