package co.proteccion.value_objects;

import java.math.BigDecimal;
import java.time.LocalDate;

public record Customer(
        DocumentType documentType,
        String documentNumber,
        String fullName,
        LocalDate birthDate,
        String email,
        String phone,
        Country residenceCountry,
        BigDecimal monthlyIncomeCOP,
        RiskProfile declaredRiskProfile,
        boolean politicallyExposed
) {
}
