package com.github.miguelaferreira.terraformcontroller;

import static com.github.miguelaferreira.terraformcontroller.domain.Execution.ENV_VAR_TF_BACKEND_AWS_ACCESS_KEY_ID;
import static com.github.miguelaferreira.terraformcontroller.domain.Execution.ENV_VAR_TF_BACKEND_AWS_SECRET_ACCESS_KEY;

import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

import com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService;
import com.github.miguelaferreira.terraformcontroller.domain.ModelConstants;
import com.github.miguelaferreira.terraformcontroller.utils.FileUtils;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

/**
 * Makes sure that the configmap with the backend config is created/updated,
 * and warns if the secret with backend credentials does exist or does not have the expected keys
 */
@Slf4j
@Singleton
public class BackendService {

    public static final String CODE_FILE_BACKEND_TF = "backend.tf";

    private final TerraformBackendConfig config;
    private final String namespace;
    private final KubernetesClient k8sClient;

    public BackendService(final String namespace, final TerraformBackendConfig config, final KubernetesClient k8sClient) {
        this.config = config;
        this.namespace = namespace;
        this.k8sClient = k8sClient;
    }

    public void init() throws IOException {
        setupBackendConfigMap(namespace, config, k8sClient);
        validateBackendSecret(namespace, config, k8sClient);
    }

    private void setupBackendConfigMap(final String namespace, final TerraformBackendConfig config, final KubernetesClient k8sClient) throws IOException {
        final Either<Throwable, ConfigMap> maybeConfigMap =
                KubernetesOperationsService.ensureConfigMapExists(k8sClient, namespace, config.getConfigMapName(), Map.of(CODE_FILE_BACKEND_TF, getTerraformBackendCode()));
        if (maybeConfigMap.isLeft()) {
            final Throwable cause = maybeConfigMap.getLeft();
            log.error("Failed to ensure backend configmap exists: {}", cause.getMessage());
            throw new RuntimeException("Failed to ensure backend configmap exists", cause);
        }
    }

    private void validateBackendSecret(final String namespace, final TerraformBackendConfig config, final KubernetesClient k8sClient) {
        final String credentialsSecretName = config.getS3().getCredentialsSecretName();
        Try.of(() -> Flowable.fromPublisher(k8sClient.getSecret(namespace, credentialsSecretName)).blockingFirst())
           .onSuccess(secret -> {
               log.info("Found configured credentials secret for terraform backend: {}", credentialsSecretName);
               validateCredentialEnvVars(secret);
           }).onFailure(throwable -> log.warn("Could not find configured credentials secret for terraform backend: {}", credentialsSecretName));
    }

    public String getConfigMapName() {
        return config.getConfigMapName();
    }

    public String getCredentialsSecretName() {
        return config.getS3().getCredentialsSecretName();
    }

    private void validateCredentialEnvVars(final Secret secret) {
        final Map<String, byte[]> data = Objects.requireNonNullElse(secret.getData(), Map.of());
        validateEnvVarKeyExists(data, ENV_VAR_TF_BACKEND_AWS_ACCESS_KEY_ID);
        validateEnvVarKeyExists(data, ENV_VAR_TF_BACKEND_AWS_SECRET_ACCESS_KEY);
        log.debug("Keys in terraform backend credentials: {}", data.keySet());
    }

    private void validateEnvVarKeyExists(final Map<String, byte[]> data, final String key) {
        if (!data.containsKey(key)) {
            log.warn("Terraform backend credentials secret should have key {}", key);
        }
    }

    private String getTerraformBackendCode() throws IOException {
        return FileUtils.readResourceContent(ModelConstants.TERRAFORM_BACKEND_FILE, ModelConstants.getConfigReplacement(config.getS3()));
    }
}
