package io.vivarium.core.brain;

import io.vivarium.serialization.VivariumObject;
import io.vivarium.visualization.RenderCode;

@SuppressWarnings("serial") // Default serialization is never used for a durable store
public abstract class Brain extends VivariumObject
{
    public abstract BrainType getBrainType();

    /**
     * Given a double of inputs to a brain, computes a set out outputs. This call returns a probabilistic solution, and
     * can vary between multiple calls, but the brain object stores no state that changes due to this method being
     * evoked.
     *
     * The input and output mapping is generated based on the species, and each species or creature object will have
     * code to map the arrays from world state and into actions.
     *
     * @param inputs
     * @return outputsS
     */
    public abstract double[] outputs(double[] inputs);

    /**
     * Applies a brain defined normalization procedure to a brain.
     */
    public abstract void normalizeWeights();

    public abstract String render(RenderCode code);

    @Override
    public void finalizeSerialization()
    {
        // TODO Auto-generated method stub

    }
}
