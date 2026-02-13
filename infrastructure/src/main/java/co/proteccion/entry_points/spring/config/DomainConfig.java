package co.proteccion.entry_points.spring.config;

import co.proteccion.port.in.CreateApplicationPort;
import co.proteccion.port.out.NotificationPort;
import co.proteccion.port.out.PensionRepositoryPort;
import co.proteccion.usecase.PensionApplicationUseCase;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.FilterType;

@Configuration
@ComponentScan(basePackages = "co.proteccion.usecase",
        includeFilters = {
                @ComponentScan.Filter(type = FilterType.REGEX, pattern = "^.+UseCase$")
        },
        useDefaultFilters = false)
public class DomainConfig {
}