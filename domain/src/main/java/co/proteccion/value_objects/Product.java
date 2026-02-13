package co.proteccion.value_objects;

import java.math.BigDecimal;

public record Product(
        BigDecimal initialContributionCOP,
        BigDecimal recurringContributionCOP,
        boolean autoDebit,
        ContributionFrequency frequency
) {
}
