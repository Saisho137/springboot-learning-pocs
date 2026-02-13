package co.proteccion.port.out;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;

public interface PensionRepositoryPort {
    void save(PensionEnrollment pensionApplication);

    void saveDecision(String requestId, RiskDecision decision);
}
