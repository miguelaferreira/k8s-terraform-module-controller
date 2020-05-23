package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.List;
import java.util.Objects;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
class ContainerModel implements Container {
    String name;
    String image;
    List<String> command;
    String volumePath;
    List<EnvSourcesModel> envFrom;

    @Override
    public List<EnvSourcesModel> getEnvFrom() {
        return Objects.requireNonNullElse(envFrom, List.of());
    }
}
