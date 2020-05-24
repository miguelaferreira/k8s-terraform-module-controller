package com.github.miguelaferreira.terraformcontroller;

import javax.inject.Inject;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Base64;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.test.annotation.MicronautTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "k8s")
public class ControllerTest {

    @Inject
    KubernetesClient k8sClient;

    @Inject
    ObjectMapper objectMapper;

    private final String keybaseCredentialsPath = "/keybase/team/functorful/development/github.com/miguelaferreira/ks8-terraform-module-controller/.credentials";
    Path backendAwsCredentials = Path.of(keybaseCredentialsPath + "/terraform-backend-user-aws.json");
    Path githubDockerCredentials = Path.of(keybaseCredentialsPath + "/tf-module-controller-github-docker.json");

    @BeforeEach
    void setUp() throws IOException {
        ensureBackendConfigMapExists();
        ensureGithubDockerSecretExists();
    }

    private void ensureGithubDockerSecretExists() throws IOException {
        final String credentialsJson = Files.readString(githubDockerCredentials);
        final Map<String, String> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {
        });

        final String username = credentials.get("github_username");
        final String token = credentials.get("github_token");
        final String authString = String.format("%s:%s", username, token);
        final String auth = Base64.getEncoder().encodeToString(authString.getBytes());
        final Map<String, Map<String, String>> githubEntry = Map.of("docker.pkg.github.com", Map.of("username", username, "password", token, "auth", auth));
        final Map<String, byte[]> secretData =
                Map.of("auths", objectMapper.writeValueAsString(githubEntry).getBytes());
        KubernetesOperationsService.ensureSecretExists(k8sClient, "default", "regcred-github", secretData);
    }

    private void ensureBackendConfigMapExists() throws IOException {
        final String credentialsJson = Files.readString(backendAwsCredentials);
        final Map<String, String> credentials = objectMapper.readValue(credentialsJson, new TypeReference<>() {
        });

        final Map<String, byte[]> secretData = Map.of(
                "TF_BACKEND_AWS_ACCESS_KEY_ID", credentials.get("access_key_id").getBytes(),
                "TF_BACKEND_AWS_SECRET_ACCESS_KEY", credentials.get("secret_access_key").getBytes()
        );
        KubernetesOperationsService.ensureSecretExists(k8sClient, "default", "terraform-backend-credentials", secretData);
    }

    @BeforeAll
    static void beforeAll() {

    }

    @Test
    void testApply() {

    }
}
