package com.github.miguelaferreira.terraformcontroller;

import java.io.File;
import java.util.List;
import java.util.Map;

import io.micronaut.context.annotation.ConfigurationProperties;
import lombok.Data;

@Data
@ConfigurationProperties("controller")
public class ControllerConfig {

    private File kubeconfig;
    private boolean enableLeaderElection = true;
    private String namespace;
    private String secretLabelSelector = "tf.module/input";
    private List<String> containerRegistryCredSecrets = List.of();

    public boolean hasKubeconfig() {
        return kubeconfig != null;
    }

    public boolean leaderElectionEnabled() {
        return enableLeaderElection;
    }

    private Aws aws;

    @Data
    @ConfigurationProperties("aws")
    public static class Aws {
        private String accessKeyId;
        private String secretAccessKey;

        public Map<String, String> credentialsMap() {
            return Map.of(
                    "AWS_ACCESS_KEY_ID", accessKeyId,
                    "AWS_SECRET_ACCESS_KEY", secretAccessKey
            );
        }
    }
}
