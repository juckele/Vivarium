package com.johnuckele.vivarium.scripts;

import com.johnuckele.vivarium.core.World;
import com.johnuckele.vivarium.core.WorldObject;
import com.johnuckele.vivarium.util.Rand;

public class RunSimulation extends Script
{
    public RunSimulation(String[] args)
    {
        super(args);
    }

    @Override
    protected boolean argumentCountIsValid(int argCount)
    {
        if (argCount >= 2 && argCount <= 4)
        {
            return true;
        }
        return false;
    }

    @Override
    protected String getUsage()
    {
        return "Usage: java scriptPath inputFilePath ticks [outputFilePath] [randomSeed]";
    }

    @Override
    protected void run(String[] args)
    {
        // Load
        World w = ScriptIO.loadWorld(args[0], Format.JAVA_SERIALIZABLE);
        int creatureCount = w.getCount(WorldObject.CREATURE);
        System.out.println("Creature count in loaded world: " + creatureCount);

        // If the user passed in a random seed use that for the run
        if (args.length == 4)
        {
            Rand.setRandomSeed(Integer.parseInt(args[3]));
        }

        // Run simulation
        int simulationTicks = Integer.parseInt(args[1]);
        for (int i = 0; i < simulationTicks; i++)
        {
            w.tick();
        }
        creatureCount = w.getCount(WorldObject.CREATURE);
        System.out.println("Creature count after simulations: " + creatureCount);

        // Save
        if (args.length == 3)
        {
            System.out.println("Saving file: " + args[2]);
            ScriptIO.saveWorld(w, args[2], Format.JAVA_SERIALIZABLE);
        }
        else
        {
            System.out.println("Results not saved");
        }
    }

    public static void main(String[] args)
    {
        new RunSimulation(args);
    }
}