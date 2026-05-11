package co.proteccion.entry_points.api.dto;

public record CustomerDTO(
        String documentType,
        String documentNumber,
        String email,
        String monthlyIncomeCOP,
        String declaredRiskProfile,
        boolean politicallyExposed
) {
}
