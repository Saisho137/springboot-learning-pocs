package com.parameter_store.demo;

import com.parameter_store.demo.service.ParameterStoreService;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.EnumerablePropertySource;
import org.springframework.core.env.PropertySource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.TreeMap;

@RestController
@RequestMapping("/api/v1/")
public class WebController {

    private final ParameterStoreService parameterStoreService;
    private final ConfigurableEnvironment environment;

    public WebController(ParameterStoreService parameterStoreService, ConfigurableEnvironment environment) {
        this.parameterStoreService = parameterStoreService;
        this.environment = environment;
    }

    @GetMapping("/test")
    public ResponseEntity<Boolean> test() {
        return ResponseEntity.ok().body(true);
    }

    @GetMapping("/parameter/{name}")
    public ResponseEntity<String> getParameter(@PathVariable String name) {
        String value = parameterStoreService.getParameter(name);
        return ResponseEntity.ok(value);
    }

    @GetMapping("/parameters")
    public ResponseEntity<Map<String, String>> getParameters() {
        Map<String, String> parameters = parameterStoreService.getParametersByPath();
        return ResponseEntity.ok(parameters);
    }

    /**
     * Muestra todas las propiedades aplanadas por el {@code JsonPropertyFlattener}.
     * Estas son las propiedades que se extrajeron de JSONs en Parameter Store
     * y quedaron disponibles globalmente en el Environment.
     */
    @GetMapping("/flattened-properties")
    public ResponseEntity<Map<String, String>> getFlattenedProperties() {
        Map<String, String> flattened = new TreeMap<>();
        for (PropertySource<?> ps : environment.getPropertySources()) {
            if (ps.getName().startsWith("json-flattened:") && ps instanceof EnumerablePropertySource<?> enumerable) {
                for (String name : enumerable.getPropertyNames()) {
                    Object value = enumerable.getProperty(name);
                    flattened.put(name, value != null ? value.toString() : "null");
                }
            }
        }
        return ResponseEntity.ok(flattened);
    }
}
