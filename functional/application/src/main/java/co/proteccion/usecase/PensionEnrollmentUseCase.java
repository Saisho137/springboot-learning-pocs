package co.proteccion.usecase;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.port.in.CreateEnrollmentPort;
import co.proteccion.port.out.NotificationPort;
import co.proteccion.port.out.PensionRepositoryPort;
import co.proteccion.model.RiskDecision;
import co.proteccion.service.RiskEngine;
import co.proteccion.service.RiskRule;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

public class PensionEnrollmentUseCase implements CreateEnrollmentPort {
    private final PensionRepositoryPort repository;
    private final NotificationPort notificationPort;
    private final RiskEngine riskEngine;

    public PensionEnrollmentUseCase(PensionRepositoryPort repository, NotificationPort notificationPort) {
        this.repository = repository;
        this.notificationPort = notificationPort;
        this.riskEngine = new RiskEngine();
    }

    @Override
    public RiskDecision execute(PensionEnrollment pensionEnrollment) {
        repository.save(pensionEnrollment);

        RiskDecision decision = riskEngine.evaluate(pensionEnrollment);

        handleSideEffects(pensionEnrollment, decision);

        return decision;
    }

    @Override
    public List<RiskDecision> evaluateBatch(List<PensionEnrollment> enrollments) {
        return enrollments.stream()
                .map(riskEngine::evaluate)
                .filter(riskDecision -> riskDecision instanceof RiskDecision.Approved)
                .toList();
    }

    private void handleSideEffects(PensionEnrollment enrollment, RiskDecision decision) {
        repository.saveDecision(enrollment.requestId(), decision);

        switch (decision) {
            case RiskDecision.Approved approved -> {
                System.out.println("✅ APROBADO: " + approved.reason());
                notificationPort.notifyDecision(enrollment, approved);
            }
            case RiskDecision.ReviewRequired review -> {
                System.out.println("⚠️ REQUIERE REVISIÓN: " + review.reason());
                notificationPort.notifyDecision(enrollment, review);
            }
            case RiskDecision.Rejected rejected -> {
                System.out.println("❌ RECHAZADO: " + rejected.reason());
                notificationPort.notifyDecision(enrollment, rejected);
            }
        }
    }
}
