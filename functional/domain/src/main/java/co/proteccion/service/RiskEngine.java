package co.proteccion.service;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class RiskEngine {

    private final RiskRule checkComplianceRule = pensionEnrollment -> {
        if (pensionEnrollment.compliance().isOnRestrictedList()) {
            return Optional.of(new RiskDecision.Rejected("Customer is in restricted list", List.of("Non-compliant customer")));
        }
        return Optional.empty();
    };

    private final RiskRule checkProductEligibilityRule = pensionEnrollment -> {
        var income = pensionEnrollment.customer().monthlyIncomeCOP();
        var recurring = pensionEnrollment.product().recurringContributionCOP();

        if (income.compareTo(BigDecimal.ZERO) == 0) return Optional.empty();

        double ratio = recurring.doubleValue() / income.doubleValue();
        if (ratio > 0.4) {
            return Optional.of(new RiskDecision.Rejected("Indebtedness capacity exceeded", List.of("DEBT_RATIO_FAIL")));
        }
        return Optional.empty();
    };

    private final RiskRule checkLargeAmountRule = pensionEnrollment -> {
        if (pensionEnrollment.product().initialContributionCOP().compareTo(new BigDecimal("20000000")) >= 0) {
            return Optional.of(new RiskDecision.ReviewRequired("High amount", List.of("SOURCE_OF_FUNDS_CHECK")));
        }
        return Optional.empty();
    };

    private final List<RiskRule> rules = List.of(
            checkComplianceRule,
            checkProductEligibilityRule,
            checkLargeAmountRule
    );

    public RiskDecision evaluate(PensionEnrollment pensionEnrollment) {
        return rules.stream()
                .map(rule -> rule.apply(pensionEnrollment))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .findFirst()
                .orElse(new RiskDecision.Approved(""));
    }
}
