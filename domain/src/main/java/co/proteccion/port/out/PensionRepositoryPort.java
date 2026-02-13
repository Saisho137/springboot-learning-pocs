package co.proteccion.port.out;

import co.proteccion.model.PensionEnrollment;

public interface PensionRepositoryPort {
    void save(PensionEnrollment pensionApplication);
}
