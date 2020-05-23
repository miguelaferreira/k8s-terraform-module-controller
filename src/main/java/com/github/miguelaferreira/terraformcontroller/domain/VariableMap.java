package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.Map;
import java.util.stream.Stream;

public interface VariableMap {

    Stream<Variable> getVariables();

    static VariableMap defaultMap(final Map<String, Object> map) {
        return VariablesMapModel.of(map);
    }
}
