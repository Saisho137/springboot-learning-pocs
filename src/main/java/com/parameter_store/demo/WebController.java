package com.parameter_store.demo;

import com.parameter_store.demo.service.ParameterStoreService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/")
public class WebController {

    private final ParameterStoreService parameterStoreService;

    public WebController(ParameterStoreService parameterStoreService) {
        this.parameterStoreService = parameterStoreService;
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
}
