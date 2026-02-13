package co.proteccion.model;

import co.proteccion.value_objects.ContributionFrequency;
import co.proteccion.value_objects.RiskProfile;

public record Product(
        Long initialContributionCOP,
        Boolean autoDebit,
        Long recurringContributionCOP,
        ContributionFrequency frequency,
        RiskProfile strategy
) {
}
