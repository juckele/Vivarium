package io.vivarium.scripts;

import java.io.File;
import java.io.IOException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class RunSimulationTest
{
    @Rule
    public TemporaryFolder folder = new TemporaryFolder();
    public String path;

    @Before
    public void setupPath() throws IOException
    {
        path = folder.getRoot().getCanonicalPath() + File.separator;
    }

    @Test
    public void testDefaultWithWorld()
    {
        {
            String[] commandArgs = { "-o", path + "w.viv" };
            CreateWorld.main(commandArgs);
        }
        {
            String[] commandArgs = { "-i", path + "w.viv", "-o", path + "w2.viv", "-t", "5000" };
            RunSimulation.main(commandArgs);
        }
    }

    @Test
    public void testDefaultWithSimulation()
    {
        {
            String[] commandArgs = { "-o", path + "s.viv", "-t", "5000" };
            CreateSimulation.main(commandArgs);
        }
        {
            String[] commandArgs = { "-i", path + "s.viv", "-o", path + "s2.viv" };
            RunSimulation.main(commandArgs);
        }
    }
}
