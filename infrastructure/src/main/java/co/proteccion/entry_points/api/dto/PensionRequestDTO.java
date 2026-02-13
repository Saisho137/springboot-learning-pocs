package co.proteccion.entry_points.api.dto;

import java.time.LocalDate;

public record PensionRequestDTO(
        String requestId,
        CustomerDTO customer,
        ComplianceDTO compliance,
        ProductDTO product,
        LocalDate createdAtISO
) {
}
