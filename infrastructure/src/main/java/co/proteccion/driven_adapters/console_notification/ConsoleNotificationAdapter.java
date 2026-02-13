package co.proteccion.driven_adapters.console_notification;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;
import co.proteccion.port.out.NotificationPort;
import org.springframework.stereotype.Component;

@Component
public class ConsoleNotificationAdapter implements NotificationPort {

    @Override
    public void notifyDecision(PensionEnrollment pensionEnrollment, RiskDecision riskDecision) {
        String eventType = switch (riskDecision) {
            case RiskDecision.Approved a -> "APPLICATION_APPROVED";
            case RiskDecision.ReviewRequired r -> "APPLICATION_REVIEW_REQUIRED";
            case RiskDecision.Rejected e -> "APPLICATION_REJECTED";
        };

        System.out.println("[EVENT-BUS] Publicando evento: " + eventType +
                " para cliente " + pensionEnrollment.customer().email());
    }
}
