package co.proteccion.entry_points.api.dto;

public record ComplianceDTO(
        boolean isOnRestrictedList,
        boolean sourceOfFundsDeclared
) {
}
