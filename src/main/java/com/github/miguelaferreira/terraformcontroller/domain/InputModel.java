package com.github.miguelaferreira.terraformcontroller.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class InputModel implements Input {

    Module module;
    VariableMap variables;

    public static InputModel of(final InputSource input) {
        return InputModel.builder()
                         .module(ModuleModel.builder()
                                            .image(input.getImage())
                                            .tag(input.getTag())
                                            .path(input.getPath())
                                            .build())
                         .variables(VariableMap.defaultMap(input.getVariables()))
                         .build();
    }
}
