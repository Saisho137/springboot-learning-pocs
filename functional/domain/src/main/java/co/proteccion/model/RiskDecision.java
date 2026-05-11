package co.proteccion.model;

import java.util.List;

// Revisar sealed interface
public sealed interface RiskDecision permits
        RiskDecision.Approved,
        RiskDecision.ReviewRequired,
        RiskDecision.Rejected {

    record Approved(String reason) implements RiskDecision {
    }

    record ReviewRequired(String reason, List<String> warnings) implements RiskDecision {
    }

    record Rejected(String reason, List<String> errors) implements RiskDecision {
    }
}

