package com.parameter_store.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.ssm.SsmClient;
import software.amazon.awssdk.services.ssm.model.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ParameterStoreService {

    private static final Logger logger = LoggerFactory.getLogger(ParameterStoreService.class);
    private final SsmClient ssmClient;
    private final String prefix;

    public ParameterStoreService(SsmClient ssmClient, @Value("${aws.parameter-store.prefix}") String prefix) {
        this.ssmClient = ssmClient;
        this.prefix = prefix;
    }

    public String getParameter(String parameterName) {
        try {
            String fullPath = prefix + parameterName;
            GetParameterRequest request = GetParameterRequest.builder()
                    .name(fullPath)
                    .withDecryption(true)
                    .build();
            GetParameterResponse response = ssmClient.getParameter(request);
            String value = response.parameter().value();
            logger.info("Parámetro obtenido: {}", fullPath);
            return value;
        } catch (ParameterNotFoundException e) {
            logger.error("Parámetro no encontrado: {}{}", prefix, parameterName);
            throw new RuntimeException("Parámetro no encontrado: " + parameterName, e);
        } catch (Exception e) {
            logger.error("Error al obtener el parámetro: {}", parameterName, e);
            throw new RuntimeException("Error al obtener el parámetro: " + parameterName, e);
        }
    }

    public Map<String, String> getParametersByPath() {
        Map<String, String> parameters = new HashMap<>();
        String nextToken = null;
        try {
            do {
                GetParametersByPathRequest request = GetParametersByPathRequest.builder()
                        .path(prefix)
                        .recursive(true)
                        .withDecryption(true)
                        .nextToken(nextToken)
                        .build();
                GetParametersByPathResponse response = ssmClient.getParametersByPath(request);
                List<Parameter> params = response.parameters();
                for (Parameter param : params) {
                    String name = param.name().replace(prefix, "");
                    parameters.put(name, param.value());
                }
                nextToken = response.nextToken();
            } while (nextToken != null);
            logger.info("Se obtuvieron {} parámetros del path: {}", parameters.size(), prefix);
            return parameters;
        } catch (Exception e) {
            logger.error("Error al obtener parámetros por path: {}", prefix, e);
            throw new RuntimeException("Error al obtener parámetros por path", e);
        }
    }
}
