package com.github.miguelaferreira.terraformcontroller;

import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.miguelaferreira.terraformcontroller.domain.InputSource;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class SecretInputSource implements InputSource {

    private static final String KEY_NAME = "name";
    private static final String KEY_IMAGE = "image";
    private static final String KEY_TAG = "tag";
    private static final String KEY_PATH = "path";
    private static final String KEY_VARIABLES = "variables";

    String name;
    String image;
    String tag;
    String path;
    Map<String, Object> variables;

    static SecretInputSource of(final Secret secret) throws JsonProcessingException {
        final ObjectMapper mapper = new ObjectMapper();

        final Map<String, String> data = Objects.requireNonNull(secret.getData(), "input secret data is null").entrySet().stream()
                                                .map(e -> Map.entry(e.getKey(), new String(e.getValue())))
                                                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        return SecretInputSource.builder()
                                .name(data.get(KEY_NAME))
                                .image(data.get(KEY_IMAGE))
                                .tag(data.get(KEY_TAG))
                                .path(data.get(KEY_PATH))
                                // for// @formatter:off
                                .variables(mapper.readValue(data.get(KEY_VARIABLES), new TypeReference<>() {}))
                                // @formatter:on
                                .build();
    }
}
