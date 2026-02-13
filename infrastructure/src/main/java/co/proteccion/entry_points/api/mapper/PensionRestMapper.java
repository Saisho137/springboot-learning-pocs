package co.proteccion.entry_points.api.mapper;

import co.proteccion.entry_points.api.dto.PensionRequestDTO;
import co.proteccion.model.PensionEnrollment;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface PensionRestMapper {
    @Mapping(target = "createdAt", source = "createdAtISO")
    @Mapping(target = "customer.monthlyIncomeCOP", source = "customer.monthlyIncomeCOP")
    @Mapping(target = "customer.declaredRiskProfile", source = "customer.declaredRiskProfile")
    @Mapping(target = "product.initialContributionCOP", source = "product.initialContributionCOP")
    @Mapping(target = "product.recurringContributionCOP", source = "product.recurringContributionCOP")
    PensionEnrollment toDomain(PensionRequestDTO pensionRequestDTO);
}
