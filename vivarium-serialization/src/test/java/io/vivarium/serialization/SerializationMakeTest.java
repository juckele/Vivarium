package io.vivarium.serialization;

import org.junit.Test;

import io.vivarium.core.Blueprint;
import io.vivarium.core.Creature;
import io.vivarium.core.Species;
import io.vivarium.core.World;
import io.vivarium.core.brain.Brain;
import io.vivarium.core.brain.BrainType;
import com.johnuckele.vtest.Tester;

public class SerializationMakeTest {
    @Test
    public void testWorldBlueprintMakeDefault() throws Exception {
	Blueprint blueprint = Blueprint.makeDefault();
	Tester.isNotNull("Blueprint should exist", blueprint);
    }

    @Test
    public void testWorldBlueprintMakeCopy() throws Exception {
	Blueprint blueprint = Blueprint.makeDefault();
	Blueprint copy = (Blueprint) new SerializationEngine()
		.makeCopy(blueprint);
	Tester.isNotNull("Blueprint copy should exist", copy);
    }

    @Test
    public void testSpeciesMakeDefault() throws Exception {
	Species species = Species.makeDefault();
	Tester.isNotNull("Species should exist", species);
    }

    @Test
    public void testSpeciesMakeCopy() throws Exception {
	Species species = Species.makeDefault();
	Species copy = (Species) new SerializationEngine().makeCopy(species);
	Tester.isNotNull("Species copy should exist", copy);
    }

    @Test
    public void testBrainMakeWithSpecies() throws Exception {
	for (BrainType brainType : BrainType.values()) {
	    Species species = Species.makeDefault();
	    species.setBrainType(brainType);
	    Brain brain = BrainType.makeWithSpecies(brainType, species);
	    Tester.isNotNull("Brain of type " + brainType + " should exist",
		    brain);
	}
    }

    @Test
    public void testBrainMakeCopy() throws Exception {
	for (BrainType brainType : BrainType.values()) {
	    Species species = Species.makeDefault();
	    species.setBrainType(brainType);
	    Brain brain = BrainType.makeWithSpecies(brainType, species);
	    Brain copy = (Brain) new SerializationEngine().makeCopy(brain);
	    Tester.isNotNull("Brain copy of type " + brainType + "should exist",
		    copy);
	}
    }

    @Test
    public void testCreatureMakeCopy() throws Exception {
	Species species = Species.makeDefault();
	Creature creature = new Creature(species);
	Creature copy = (Creature) new SerializationEngine().makeCopy(creature);
	Tester.isNotNull("Creature copy should exist", copy);
    }

    @Test
    public void testWorldMakeCopy() throws Exception {
	Blueprint blueprint = Blueprint.makeDefault();
	World world = new World(blueprint);
	World copy = (World) new SerializationEngine().makeCopy(world);
	Tester.isNotNull("World copy should exist", copy);
    }
}