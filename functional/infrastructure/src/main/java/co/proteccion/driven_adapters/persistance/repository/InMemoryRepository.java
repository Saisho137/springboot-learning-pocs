package co.proteccion.driven_adapters.persistance.repository;

import co.proteccion.model.PensionEnrollment;
import co.proteccion.model.RiskDecision;
import co.proteccion.port.out.PensionRepositoryPort;
import org.springframework.stereotype.Repository;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class InMemoryRepository implements PensionRepositoryPort {
    private final Map<String, PensionEnrollment> pensionEnrollments = new ConcurrentHashMap<>();
    private final Map<String, RiskDecision> riskDecisions = new ConcurrentHashMap<>();


    @Override
    public void save(PensionEnrollment pensionApplication) {
        System.out.println("[PERSISTENCE] saving request: " + pensionApplication.requestId());
        pensionEnrollments.put(pensionApplication.requestId(), pensionApplication);
    }

    @Override
    public void saveDecision(String requestId, RiskDecision decision) {
        System.out.println("[PERSISTENCE] Updating status for ID " + requestId +
                " -> Decision: " + decision.getClass().getSimpleName());
        riskDecisions.put(requestId, decision);
    }
}
