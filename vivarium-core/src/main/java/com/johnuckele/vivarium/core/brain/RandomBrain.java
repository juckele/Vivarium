package com.johnuckele.vivarium.core.brain;

import com.johnuckele.vivarium.core.Species;
import com.johnuckele.vivarium.serialization.SerializedParameter;
import com.johnuckele.vivarium.util.Rand;
import com.johnuckele.vivarium.visualization.RenderCode;

@SuppressWarnings("serial")
public class RandomBrain extends Brain
{
    @SerializedParameter
    private double[] _outputs;

    private RandomBrain(Species species, RandomBrain parentBrain1, RandomBrain parentBrain2)
    {
        // Random brain has no state, it's literally random output. This brain
        // does not evolve.
        this._outputs = new double[species.getTotalBrainOutputCount()];
    }

    private RandomBrain(int totalBrainOutputCount)
    {
        this._outputs = new double[totalBrainOutputCount];
    }

    private RandomBrain()
    {
    }

    @Override
    public BrainType getBrainType()
    {
        return BrainType.RANDOM;
    }

    @Override
    public double[] outputs(double[] inputs)
    {
        for (int i = 0; i < _outputs.length; i++)
        {
            _outputs[i] = Rand.getRandomDouble();
        }
        return _outputs;
    }

    @Override
    public String render(RenderCode code)
    {
        return "Hand coded brain: no render available";
    }

    public static Brain makeUninitialized()
    {
        return new RandomBrain();
    }

    public static RandomBrain makeWithSpecies(Species species)
    {
        RandomBrain brain = new RandomBrain(species.getTotalBrainOutputCount());
        return brain;
    }

    public static Brain makeWithParents(Species species, RandomBrain untypedParentBrain1,
            RandomBrain untypedParentBrain2)
    {
        RandomBrain parentBrain1 = untypedParentBrain1;
        RandomBrain parentBrain2 = untypedParentBrain2;
        RandomBrain brain = new RandomBrain(species, parentBrain1, parentBrain2);
        return brain;
    }

}
