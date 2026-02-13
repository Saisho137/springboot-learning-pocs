package co.proteccion.entry_points.api.controller;

import co.proteccion.entry_points.api.dto.PensionRequestDTO;
import co.proteccion.entry_points.api.mapper.PensionRestMapper;
import co.proteccion.model.RiskDecision;
import co.proteccion.port.in.CreateEnrollmentPort;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/pension/enrollment")
@RequiredArgsConstructor
public class PensionController {
    private final CreateEnrollmentPort useCase;
    private final PensionRestMapper mapper;

    @PostMapping
    public ResponseEntity<?> create(@RequestBody PensionRequestDTO request) {
        // 1. Convertir DTO -> Dominio (MapStruct)
        var domainCommand = mapper.toDomain(request);

        // 2. Ejecutar Caso de Uso
        RiskDecision result = useCase.execute(domainCommand);

        // 3. Convertir Dominio -> Respuesta HTTP (Pattern Matching Java 17)
        return switch (result) {
            case RiskDecision.Approved a -> ResponseEntity.ok(
                    new ResponseWrapper("APPROVED", a.reason(), null)
            );

            case RiskDecision.ReviewRequired r -> ResponseEntity.status(202).body(
                    new ResponseWrapper("PENDING_REVIEW", r.reason(), r.warnings())
            );

            case RiskDecision.Rejected e -> ResponseEntity.badRequest().body(
                    new ResponseWrapper("REJECTED", e.reason(), e.errors())
            );
        };
    }

    // DTO de respuesta interno (o record)
    record ResponseWrapper(String status, String details, Object reasons) {
    }
}
