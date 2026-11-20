package utils;

import ai.djl.ndarray.NDList;
import ai.djl.ndarray.NDManager;
import ai.djl.ndarray.types.Shape;
import ai.djl.ndarray.NDArray;
import ai.djl.translate.Translator;
import ai.djl.translate.TranslatorContext;
import ai.djl.translate.Batchifier;

public class AgentTranslator implements Translator<boolean[][][], Float> {
    
	@Override
	public NDList processInput(TranslatorContext ctx, boolean[][][] input) {
	    // Use getNDManager() instead of getManager()
	    NDManager manager = ctx.getNDManager(); 
	    
	    float[] flatData = flattenTensor(input);
	    NDArray array = manager.create(flatData, new Shape(1, 6, 9, 9));
	    return new NDList(array);
	}

    @Override
    public Float processOutput(TranslatorContext ctx, NDList list) {
        // Extract the float value *while* the manager is still active
        NDArray array = list.singletonOrThrow();
        return array.toFloatArray()[0];
    }

    @Override
    public Batchifier getBatchifier() {
        return null; // or Batchifier.STACK if batching
    }

    private float[] flattenTensor(boolean[][][] t) {
        float[] flat = new float[6 * 9 * 9];
        int idx = 0;
        for (int c = 0; c < 6; c++) {
            for (int r = 0; r < 9; r++) {
                for (int col = 0; col < 9; col++) {
                    flat[idx++] = t[r][col][c] ? 1.0f : 0.0f;
                }
            }
        }
        return flat;
    }
}