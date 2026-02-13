package co.proteccion.service;

import co.proteccion.model.PensionEnrollment;

import java.util.Optional;

@FunctionalInterface
public interface RiskRule {
    Optional<RiskDecision> apply(PensionEnrollment pensionEnrollment);
}
