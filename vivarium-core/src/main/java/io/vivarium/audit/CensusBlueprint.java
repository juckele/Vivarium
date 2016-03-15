package io.vivarium.audit;

import io.vivarium.core.CreatureBlueprint;
import lombok.EqualsAndHashCode;
import lombok.ToString;

@EqualsAndHashCode(callSuper = true)
@ToString
@SuppressWarnings("serial") // Default serialization is never used for a durable store
public class CensusBlueprint extends AuditBlueprint
{
    public CensusBlueprint()
    {
        super(AuditType.CENSUS);
    }

    @Override
    public CensusRecord makeRecordWithSpecies(CreatureBlueprint species)
    {
        return new CensusRecord(this, species);
    }
}
