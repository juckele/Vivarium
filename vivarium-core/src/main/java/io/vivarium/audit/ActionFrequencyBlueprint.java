package io.vivarium.audit;

import io.vivarium.core.Species;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("serial") // Default serialization is never used for a durable store
public class ActionFrequencyBlueprint extends AuditBlueprint
{
    public ActionFrequencyBlueprint()
    {
        super(AuditType.ACTION_FREQUENCY);
    }

    @Override
    public ActionFrequencyRecord makeRecordWithSpecies(Species species)
    {
        return new ActionFrequencyRecord(species);
    }
}