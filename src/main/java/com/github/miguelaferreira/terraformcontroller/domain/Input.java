package com.github.miguelaferreira.terraformcontroller.domain;

public interface Input {

    Module getModule();

    static Input fromDefaultSource(final InputSource inputSource) {
        return InputModel.of(inputSource);
    }

    VariableMap getVariables();
}
