package co.proteccion.usecase;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.port.out.PensionRepositoryPort;
import co.proteccion.service.RiskDecision;
import co.proteccion.service.RiskRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class PensionEnrollmentUseCase {
    private final PensionRepositoryPort pensionRepositoryPort;

    private final RiskRule checkComplianceRule = pensionEnrollment -> {
        if (pensionEnrollment.compliance().isOnRestrictedList()) {
            return Optional.of(new RiskDecision.Rejected("Customer does not meet compliance requirements", List.of("Non-compliant customer")));
        } else {
            return Optional.empty();
        }
    };

    private final RiskRule checkProductEligibilityRule = pensionEnrollment -> {
        var income = pensionEnrollment.customer().monthlyIncomeCOP();
        var recurring = pensionEnrollment.product().recurringContributionCOP();

        if (income.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        double ratio = recurring.doubleValue() / income.doubleValue();
        if (ratio > 0.4) {
            return Optional.of(new RiskDecision.Rejected("Capacidad de endeudamiento excedida", List.of("DEBT_RATIO_FAIL")));
        }
        return Optional.empty();
    };

    private final List<RiskRule> rules = List.of(
            checkComplianceRule,
            checkProductEligibilityRule
    );

    public PensionEnrollmentUseCase(PensionRepositoryPort pensionRepositoryPort) {
        this.pensionRepositoryPort = pensionRepositoryPort;
    }

    public void save(PensionEnrollment pensionEnrollment) {
        rules.stream()
                .map( rule -> rule.apply(pensionEnrollment))
                .filter(Optional::isPresent)
                .findFirst();
    }

}
