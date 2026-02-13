package co.proteccion.model;

import co.proteccion.value_objects.Country;
import co.proteccion.value_objects.DocumentType;
import co.proteccion.value_objects.RiskProfile;

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
        Boolean politicallyExposed
) {
}
