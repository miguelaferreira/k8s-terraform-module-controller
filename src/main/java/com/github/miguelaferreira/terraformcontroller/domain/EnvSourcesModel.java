package com.github.miguelaferreira.terraformcontroller.domain;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class EnvSourcesModel {

    SrcType type;
    String name;

    public enum SrcType {
        SECRET_REF
    }
}
