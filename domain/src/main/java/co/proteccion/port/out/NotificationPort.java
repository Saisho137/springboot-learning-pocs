package co.proteccion.port.out;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;

public interface NotificationPort {
    void notifyDecision(PensionEnrollment pensionEnrollment, RiskDecision riskDecision);
}
