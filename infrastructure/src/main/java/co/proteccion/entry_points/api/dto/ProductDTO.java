package co.proteccion.entry_points.api.dto;

import java.math.BigDecimal;

public record ProductDTO(
        BigDecimal initialContributionCOP,
        BigDecimal recurringContributionCOP,
        boolean autoDebit,
        String frequency
) {
}
