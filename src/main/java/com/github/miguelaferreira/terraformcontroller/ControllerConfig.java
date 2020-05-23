package com.github.miguelaferreira.terraformcontroller;

import java.util.List;
import java.util.Objects;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("controller")
public class ControllerConfig {

    public static final String TF_MODULE_INPUT_LABEL = "tf.module/input";
    public static final String DEFAULT_SERVICE_ACCOUNT = "default";

    private String inputLabelSelector;
    private List<String> containerRegistryCredSecrets;

    public String getInputLabelSelector() {
        return Objects.requireNonNullElse(inputLabelSelector, TF_MODULE_INPUT_LABEL);
    }

    public List<String> getContainerRegistryCredSecrets() {
        return Objects.requireNonNullElse(containerRegistryCredSecrets, List.of());
    }

}
