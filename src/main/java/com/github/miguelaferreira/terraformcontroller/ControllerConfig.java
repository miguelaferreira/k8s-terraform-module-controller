package com.github.miguelaferreira.terraformcontroller;

import java.util.List;
import java.util.Objects;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("controller")
public class ControllerConfig {

    private String inputLabelSelector;
    private List<String> containerRegistryCredSecrets;

    public String getInputLabelSelector() {
        return Objects.requireNonNullElse(inputLabelSelector, "tf.module/input");
    }

    public List<String> getContainerRegistryCredSecrets() {
        return Objects.requireNonNullElse(containerRegistryCredSecrets, List.of());
    }
}
