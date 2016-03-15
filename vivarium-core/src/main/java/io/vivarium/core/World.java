package io.vivarium.core;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;

import io.vivarium.audit.AuditBlueprint;
import io.vivarium.audit.AuditRecord;
import io.vivarium.core.processor.Processor;
import io.vivarium.core.processor.ProcessorType;
import io.vivarium.serialization.SerializedParameter;
import io.vivarium.serialization.VivariumObject;
import io.vivarium.util.Rand;
import io.vivarium.visualization.RenderCode;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("serial") // Default serialization is never used for a durable store
public class World extends VivariumObject
{
    @SerializedParameter
    private int _maximumCreatureID;
    @SerializedParameter
    private int _tick;
    @SerializedParameter
    private int _width;
    @SerializedParameter
    private int _height;

    @SerializedParameter
    protected EntityType[][] _entityGrid;
    @SerializedParameter
    protected Creature[][] _creatureGrid;
    @SerializedParameter
    protected AuditRecord[] _auditRecords;

    @SerializedParameter
    private WorldBlueprint _blueprint;

    protected World()
    {
    }

    public World(WorldBlueprint blueprint)
    {
        // Store the blueprint
        this._blueprint = blueprint;

        // Set up base variables
        this._maximumCreatureID = 0;

        // Size the world
        this.setWorldDimensions(blueprint.getWidth(), blueprint.getHeight());

        // Fill the world with creatures and food
        this.populatateWorld();

        // Build audit records
        this.constructAuditRecords();
        this.performAudits();
    }

    private void constructAuditRecords()
    {
        int auditRecordCount = _blueprint.getCreatureBlueprints().size() * _blueprint.getAuditBlueprints().size();
        _auditRecords = new AuditRecord[auditRecordCount];
        int i = 0;
        for (CreatureBlueprint creatureBlueprint : _blueprint.getCreatureBlueprints())
        {
            for (AuditBlueprint auditBlueprint : _blueprint.getAuditBlueprints())
            {
                _auditRecords[i] = auditBlueprint.makeRecordWithCreatureBlueprint(creatureBlueprint);
                i++;
            }
        }
    }

    private void populatateWorld()
    {
        WorldPopulator populator = new WorldPopulator();
        populator.setCreatureBlueprints(_blueprint.getCreatureBlueprints());
        populator.setWallProbability(_blueprint.getInitialWallGenerationProbability());
        populator.setFoodProbability(_blueprint.getInitialFoodGenerationProbability());
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                setObject(EntityType.EMPTY, r, c);
                _creatureGrid[r][c] = null;
                if (r < 1 || c < 1 || r > _height - 2 || c > _width - 2)
                {
                    setObject(EntityType.WALL, r, c);
                }
                else
                {
                    EntityType object = populator.getNextEntityType();
                    if (object == EntityType.CREATURE)
                    {
                        CreatureBlueprint creatureBlueprint = populator.getNextCreatureBlueprint();
                        Creature creature = new Creature(creatureBlueprint);
                        addCreature(creature, r, c);
                    }
                    else
                    {
                        setObject(object, r, c);
                    }
                }
            }
        }
    }

    public int getNewCreatureID()
    {
        return (++_maximumCreatureID);
    }

    /**
     * Top level simulation step of the entire world and all denizens within it. Simulations are divided into four
     * phases: 1, each creature will age and compute other time based values. 2, each creatur will decide on an action
     * to attempt. 3, each creature will attempt to execute the planned action (the order of execution on the actions is
     * currently left to right, top to bottom, so some creature will get priority if actions conflict). 4, finally, food
     * is spawned at a constant chance in empty spaces in the world.
     */
    public void tick()
    {
        // Increment tick counter
        _tick++;

        // Each creature calculates time based
        // changes in condition such as age,
        // gestation, and energy levels.
        tickCreatures();

        // Creatures transmit sound
        transmitSounds();

        // Each creature plans which actions to
        // attempt to do during the next phase
        letCreaturesPlan();

        // Each creature will physically try to carry
        // out the planned action
        executeCreaturePlans();

        // New food resources will be spawned in the world
        spawnFood();

        // Record with audit records
        performAudits();
    }

    private void tickCreatures()
    {
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (_entityGrid[r][c] == EntityType.CREATURE)
                {
                    _creatureGrid[r][c].tick();
                }
            }
        }
    }

    private void transmitSounds()
    {
        if (this._blueprint.getSoundEnabled())
        {
            for (int r = 0; r < this._height; r++)
            {
                for (int c = 0; c < this._width; c++)
                {
                    if (_entityGrid[r][c] == EntityType.CREATURE)
                    {
                        transmitSoundsFrom(r, c);
                    }
                }
            }
        }
    }

    private void transmitSoundsFrom(int r1, int c1)
    {
        for (int c2 = c1 + 1; c2 < this._width; c2++)
        {
            int r2 = r1;
            if (_entityGrid[r2][c2] == EntityType.CREATURE)
            {
                transmitSoundsFromTo(r1, c1, r2, c2);
            }
        }
        for (int r2 = r1 + 1; r2 < this._height; r2++)
        {
            for (int c2 = c1; c2 < this._width; c2++)
            {
                if (_entityGrid[r2][c2] == EntityType.CREATURE)
                {
                    transmitSoundsFromTo(r1, c1, r2, c2);
                }
            }
        }
    }

    private void transmitSoundsFromTo(int r1, int c1, int r2, int c2)
    {
        int distanceSquared = (r1 - r2) * (r1 - r2) + (c1 - c2) * (c1 - c2);
        _creatureGrid[r1][c1].listenToCreature(_creatureGrid[r2][c2], distanceSquared);
        _creatureGrid[r2][c2].listenToCreature(_creatureGrid[r1][c1], distanceSquared);
    }

    private void letCreaturesPlan()
    {
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (_entityGrid[r][c] == EntityType.CREATURE)
                {
                    _creatureGrid[r][c].planAction(this, r, c);
                }
            }
        }
    }

    private void executeCreaturePlans()
    {
        // Creatures act
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (_entityGrid[r][c] == EntityType.CREATURE)
                {
                    if (!_creatureGrid[r][c].hasActed())
                    {
                        executeCreaturePlan(r, c);
                    }
                }
            }
        }
    }

    private void executeCreaturePlan(int r, int c)
    {
        Creature creature = _creatureGrid[r][c];
        Action action = creature.getAction();
        Direction facing = creature.getFacing();
        int facingR = r + Direction.getVerticalComponent(facing);
        int facingC = c + Direction.getHorizontalComponent(facing);
        // Death
        if (action == Action.DIE)
        {
            creature.executeAction(action);
            killCreature(r, c);
        }
        // Various actions that always succeed and are simple
        else if (action == Action.TURN_LEFT || action == Action.TURN_RIGHT || action == Action.REST)
        {
            creature.executeAction(action);
        }
        // Movement
        else if (action == Action.MOVE && _entityGrid[facingR][facingC] == EntityType.EMPTY)
        {
            creature.executeAction(action);
            moveObject(r, c, facing);
        }
        // Eating
        else if (action == Action.EAT && _entityGrid[facingR][facingC] == EntityType.FOOD)
        {
            creature.executeAction(action);
            removeObject(r, c, facing);
        }
        // Attempt to breed
        else if (action == Action.BREED
                // Make sure we're facing another creature
                && _entityGrid[facingR][facingC] == EntityType.CREATURE
                // And that creature is shares the same blueprint as us
                && _creatureGrid[facingR][facingC].getBlueprint() == creature.getBlueprint()
                // And that creature also is trying to breed
                && _creatureGrid[facingR][facingC].getAction() == Action.BREED
                // And that creature is the opposite gender
                && _creatureGrid[facingR][facingC].getIsFemale() != creature.getIsFemale()
                // Make sure the creatures are facing each other
                && creature.getFacing() == Direction.flipDirection(_creatureGrid[facingR][facingC].getFacing()))
        {
            creature.executeAction(action, _creatureGrid[facingR][facingC]);
        }
        // Giving Birth
        else if (action == Action.BIRTH && _entityGrid[facingR][facingC] == EntityType.EMPTY)
        {
            Creature spawningCreature = creature.getFetus();
            creature.executeAction(action);
            addCreature(spawningCreature, facingR, facingC);
        }
        // Action failed
        else
        {
            creature.failAction(action);
        }
    }

    private void spawnFood()
    {
        // Generate food at a given rate
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (_entityGrid[r][c] == EntityType.EMPTY)
                {
                    double randomNumber = Rand.getInstance().getRandomPositiveDouble();
                    if (randomNumber < this._blueprint.getFoodGenerationProbability())
                    {
                        setObject(EntityType.FOOD, r, c);
                        _entityGrid[r][c] = EntityType.FOOD;
                    }
                }
            }
        }
    }

    private void performAudits()
    {
        for (int i = 0; i < _auditRecords.length; i++)
        {
            _auditRecords[i].record(this, _tick);
        }
    }

    public int getTickCounter()
    {
        return _tick;
    }

    private void moveObject(int r, int c, Direction direction)
    {
        int r1 = r;
        int c1 = c;
        int r2 = r;
        int c2 = c;
        switch (direction)
        {
            case NORTH:
                r2--;
                break;
            case EAST:
                c2++;
                break;
            case SOUTH:
                r2++;
                break;
            case WEST:
                c2--;
                break;
            default:
                System.err.println("Non-Fatal Error, unhandled action");
                new Error().printStackTrace();
        }

        // Default object move
        _entityGrid[r2][c2] = _entityGrid[r1][c1];
        _entityGrid[r1][c1] = EntityType.EMPTY;
        // Special creatures move extras
        if (_entityGrid[r2][c2] == EntityType.CREATURE)
        {
            _creatureGrid[r2][c2] = _creatureGrid[r1][c1];
            _creatureGrid[r1][c1] = null;
        }
    }

    private void removeObject(int r, int c)
    {
        this.removeObject(r, c, Direction.NORTH, 0);
    }

    private void removeObject(int r, int c, Direction direction)
    {
        this.removeObject(r, c, direction, 1);
    }

    private void removeObject(int r, int c, Direction direction, int distance)
    {
        switch (direction)
        {
            case NORTH:
                r -= distance;
                break;
            case EAST:
                c += distance;
                break;
            case SOUTH:
                r += distance;
                break;
            case WEST:
                c -= distance;
                break;
            default:
                System.err.println("Non-Fatal Error, unhandled action");
                new Error().printStackTrace();
        }

        _entityGrid[r][c] = EntityType.EMPTY;
        _creatureGrid[r][c] = null;
    }

    public LinkedList<AuditRecord> getAuditRecords()
    {
        LinkedList<AuditRecord> auditRecords = new LinkedList<>();
        for (int i = 0; i < this._auditRecords.length; i++)
        {
            auditRecords.add(_auditRecords[i]);
        }
        return auditRecords;
    }

    public LinkedList<Creature> getCreatures()
    {
        LinkedList<Creature> allCreatures = new LinkedList<>();
        for (int r = 0; r < this._height; r++)
        {
            for (int c = 0; c < this._width; c++)
            {
                if (_entityGrid[r][c] == EntityType.CREATURE)
                {
                    allCreatures.add(_creatureGrid[r][c]);
                }
            }
        }
        Collections.sort(allCreatures, new Comparator<Creature>()
        {
            @Override
            public int compare(Creature c1, Creature c2)
            {
                int generationComparison = Double.compare(c1.getGeneration(), c2.getGeneration());
                if (generationComparison != 0)
                {
                    return generationComparison;
                }
                else
                {
                    return Integer.compare(c1.getID(), c2.getID());
                }
            }
        });
        return allCreatures;
    }

    public int getCount(EntityType obj)
    {
        int count = 0;
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (this._entityGrid[r][c] == obj)
                {
                    count++;
                }
            }
        }
        return (count);
    }

    public int getCount(CreatureBlueprint s)
    {
        int count = 0;
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (this._creatureGrid[r][c] != null && this._creatureGrid[r][c].getBlueprint().equals(s))
                {
                    count++;
                }
            }
        }
        return (count);
    }

    public int getMaximimCreatureID()
    {
        return this._maximumCreatureID;
    }

    public void setMaximumCreatureID(int maximumCreatureID)
    {
        this._maximumCreatureID = maximumCreatureID;
    }

    public Creature getCreature(int r, int c)
    {
        return this._creatureGrid[r][c];
    }

    private void addCreature(Creature creature, int r, int c)
    {
        creature.setID(this.getNewCreatureID());
        setObject(EntityType.CREATURE, creature, r, c);
    }

    private void killCreature(int r, int c)
    {
        removeObject(r, c);
    }

    public void addImmigrant(Creature creature)
    {
        boolean immigrantPlaced = false;
        while (!immigrantPlaced)
        {
            int r = Rand.getInstance().getRandomInt(this._height);
            int c = Rand.getInstance().getRandomInt(this._width);
            if (_entityGrid[r][c] == EntityType.EMPTY)
            {
                addCreature(creature, r, c);
                immigrantPlaced = true;
            }
        }
    }

    public int getWorldWidth()
    {
        return this._width;
    }

    public int getWorldHeight()
    {
        return this._height;
    }

    public void setWorldDimensions(int width, int height)
    {
        this._width = width;
        this._height = height;

        this._entityGrid = new EntityType[height][width];
        this._creatureGrid = new Creature[height][width];
    }

    public EntityType getEntityType(int r, int c)
    {
        return (this._entityGrid[r][c]);
    }

    public void setObject(EntityType obj, int r, int c)
    {
        if (obj == EntityType.CREATURE)
        {
            throw new Error("Creature EntityTypes should not be assinged directly, use setCreature");
        }
        setObject(obj, null, r, c);
    }

    private void setObject(EntityType obj, Creature creature, int r, int c)
    {
        _entityGrid[r][c] = obj;
        _creatureGrid[r][c] = creature;
    }

    public String render(RenderCode code)
    {
        if (code == RenderCode.WORLD_MAP)
        {
            return (renderMap());
        }
        else if (code == RenderCode.PROCESSOR_WEIGHTS)
        {
            return (renderProcessorWeights());
        }
        else if (code == RenderCode.LIVE_CREATURE_LIST)
        {
            StringBuilder creatureOutput = new StringBuilder();
            for (int r = 0; r < this._height; r++)
            {
                for (int c = 0; c < this._width; c++)
                {
                    if (_entityGrid[r][c] == EntityType.CREATURE)
                    {
                        creatureOutput.append(_creatureGrid[r][c].render(RenderCode.SIMPLE_CREATURE, r, c));
                        creatureOutput.append('\n');
                    }
                }
            }
            return (creatureOutput.toString());
        }
        else
        {
            throw new IllegalArgumentException("RenderCode " + code + " not supported for type " + this.getClass());
        }
    }

    private String renderMap()
    {
        String[] glyphs = { "中", "马", "心" };
        // Draw world map
        StringBuilder worldOutput = new StringBuilder();
        worldOutput.append("Walls: ");
        worldOutput.append(this.getCount(EntityType.WALL));
        HashMap<CreatureBlueprint, String> creatureBlueprintToGlyph = new HashMap<>();
        for (CreatureBlueprint s : this._blueprint.getCreatureBlueprints())
        {
            creatureBlueprintToGlyph.put(s, glyphs[creatureBlueprintToGlyph.size()]);
            worldOutput.append(", ").append(creatureBlueprintToGlyph.get(s)).append("-creatures: ");
            worldOutput.append(this.getCount(s));
        }
        worldOutput.append(", Food: ");
        worldOutput.append(this.getCount(EntityType.FOOD));
        worldOutput.append('\n');
        for (int r = 0; r < _height; r++)
        {
            for (int c = 0; c < _width; c++)
            {
                if (_entityGrid[r][c] == EntityType.EMPTY)
                {
                    worldOutput.append('\u3000');
                }
                else if (_entityGrid[r][c] == EntityType.WALL)
                {
                    worldOutput.append('口');
                }
                else if (_entityGrid[r][c] == EntityType.FOOD)
                {
                    worldOutput.append('一');
                }
                else if (_entityGrid[r][c] == EntityType.CREATURE)
                {
                    worldOutput.append(creatureBlueprintToGlyph.get(_creatureGrid[r][c].getBlueprint()));
                }
            }
            worldOutput.append('\n');
        }
        return (worldOutput.toString());
    }

    private String renderProcessorWeights()
    {
        StringBuilder multiCreatureBlueprintOutput = new StringBuilder();
        for (CreatureBlueprint blueprint : this._blueprint.getCreatureBlueprints())
        {
            multiCreatureBlueprintOutput.append(this.renderProcessorWeights(blueprint));
        }
        return multiCreatureBlueprintOutput.toString();
    }

    private String renderProcessorWeights(CreatureBlueprint s)
    {
        // Draw average processor
        // Draw creature readouts
        LinkedList<Processor> processors = new LinkedList<>();
        for (int r = 0; r < this._height; r++)
        {
            for (int c = 0; c < this._width; c++)
            {
                if (_entityGrid[r][c] == EntityType.CREATURE && _creatureGrid[r][c].getBlueprint().equals(s))
                {
                    processors.add(_creatureGrid[r][c].getProcessor());
                }
            }
        }
        if (processors.size() > 0)
        {
            return ProcessorType.render(processors.getFirst().getProcessorType(), processors);
        }
        return "";
    }

    @Override
    public void finalizeSerialization()
    {
        // Do nothing
    }
}
