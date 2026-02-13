package co.proteccion.model;

import java.time.LocalDateTime;

public record PensionEnrollment(
        String requestId,
        LocalDateTime createdAt,
        Customer customer,
        Product product,
        Compliance compliance
) {
}
