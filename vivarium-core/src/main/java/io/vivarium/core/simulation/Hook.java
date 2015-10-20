/*
 * Copyright © 2015 John H Uckele. All rights reserved.
 */

package io.vivarium.core.simulation;

import io.vivarium.core.World;
import io.vivarium.serialization.VivariumObject;

@SuppressWarnings("serial") // Default serialization is never used for a durable store
public abstract class Hook extends VivariumObject
{
    @Override
    public void finalizeSerialization()
    {
    }

    public abstract void apply(Simulation simulation, World world);
}
