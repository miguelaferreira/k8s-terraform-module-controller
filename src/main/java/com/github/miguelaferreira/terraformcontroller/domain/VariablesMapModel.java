package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.Arrays;
import java.util.Collection;
import java.util.Map;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;

@Slf4j
class VariablesMapModel implements VariableMap {

    private final Map<String, Object> variablesMap;

    private VariablesMapModel(final Map<String, Object> variables) {
        this.variablesMap = Map.copyOf(variables);
    }

    static VariablesMapModel of(final Map<String, Object> map) {
        return new VariablesMapModel(map);
    }

    @Override
    public Stream<Variable> getVariables() {
        return variablesMap.entrySet()
                           .stream()
                           .map(this::buildVariable);
    }

    private NameValuePair buildVariable(final Map.Entry<String, Object> mapEntry) {
        return NameValuePair.builder()
                            .name(String.format("TF_VAR_%s", mapEntry.getKey()))
                            .value(serializeValue(mapEntry.getValue()))
                            .build();
    }

    private String serializeValue(final Object value) {
        if (value instanceof Number || value instanceof Boolean || value instanceof String) {
            return String.valueOf(value);
        } else if (value instanceof Collection) {
            final Collection<?> collection = (Collection<?>) value;
            return collection.stream()
                             .map(this::serializeValue)
                             .map(this::wrapValue)
                             .collect(listCollector());
        } else if (value instanceof Object[]) {
            final Object[] array = (Object[]) value;
            return Arrays.stream(array)
                         .map(this::serializeValue)
                         .map(this::wrapValue)
                         .collect(listCollector());
        } else if (value instanceof Map) {
            @SuppressWarnings("unchecked") final Map<String, Object> map = (Map<String, Object>) value;
            return map.entrySet().stream()
                      .map(e -> Map.entry(e.getKey(), serializeValue(e.getValue())))
                      .map(e -> String.format("%s: %s", wrapValue(e.getKey()), wrapValue(e.getValue())))
                      .collect(mapCollector());
        } else {
            final String msg = String.format("Cannot serialize value of type %s", value.getClass().getName());
            log.error(msg);
            throw new RuntimeException(msg);
        }
    }

    private String wrapValue(final String s) {
        return String.format("\"%s\"", s);
    }

    private Collector<CharSequence, ?, String> mapCollector() {
        return Collectors.joining(", ", "{", "}");
    }

    private Collector<CharSequence, ?, String> listCollector() {
        return Collectors.joining(", ", "[", "]");
    }
}
