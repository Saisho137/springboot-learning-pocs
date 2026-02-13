package co.proteccion.port.in;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;

import java.util.List;

public interface CreateEnrollmentPort {
    RiskDecision execute(PensionEnrollment pensionEnrollment);
    List<RiskDecision> evaluateBatch(List<PensionEnrollment> enrollments);
}
