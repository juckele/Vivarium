package io.vivarium.ga;

import io.vivarium.core.Creature;
import io.vivarium.core.processor.NeuralNetwork;

public class GenomeWeightFF extends FitnessFunction
{

    @Override
    public double evaluate(Creature c)
    {
        NeuralNetwork b = (NeuralNetwork) c.getProcessor();
        return b.getWeights()[0][0];
    }

}
