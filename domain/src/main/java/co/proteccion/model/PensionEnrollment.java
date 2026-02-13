package co.proteccion.model;

import co.proteccion.value_objects.Compliance;
import co.proteccion.value_objects.Customer;
import co.proteccion.value_objects.Product;

import java.time.LocalDateTime;

public record PensionEnrollment(
        String requestId,
        LocalDateTime createdAt,
        Customer customer,
        Product product,
        Compliance compliance
) {
    public PensionEnrollment {
        if (requestId == null) throw new IllegalArgumentException("RequestId cannot be null");
    }
}
