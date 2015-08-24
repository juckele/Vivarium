package com.johnuckele.vivarium.scripts;

import com.johnuckele.vivarium.core.World;

public class ConvertToJSON extends Script
{

    public ConvertToJSON(String[] args)
    {
        super(args);
    }

    @Override
    protected boolean argumentCountIsValid(int argCount)
    {
        if (argCount == 2)
        {
            return true;
        }
        else
        {
            return false;
        }
    }

    @Override
    protected String getUsage()
    {
        return "Usage: java scriptPath inputFilePath outputFilePath";
    }

    @Override
    protected void run(String[] args)
    {
        // Load
        World w = (World) ScriptIO.loadObject(args[0], Format.JSON);

        // Save
        ScriptIO.saveSerializer(w, args[1], Format.JSON);
    }

    public static void main(String[] args)
    {
        new ConvertToJSON(args);
    }

}