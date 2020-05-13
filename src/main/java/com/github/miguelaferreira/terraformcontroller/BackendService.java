package com.github.miguelaferreira.terraformcontroller;

import static com.github.miguelaferreira.terraformcontroller.domain.Execution.ENV_VAR_TF_BACKEND_AWS_ACCESS_KEY_ID;
import static com.github.miguelaferreira.terraformcontroller.domain.Execution.ENV_VAR_TF_BACKEND_AWS_SECRET_ACCESS_KEY;
import static com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService.ensureConfigMapExists;

import javax.inject.Named;
import javax.inject.Singleton;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ExecutorService;

import com.github.miguelaferreira.terraformcontroller.domain.ModelConstants;
import com.github.miguelaferreira.terraformcontroller.utils.FileUtils;
import com.github.miguelaferreira.terraformcontroller.utils.RxUtils;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

/**
 * Makes sure that the configmap with the backend config is created/updated,
 * and warns if the secret with backend credentials does exist or does not have the expected keys
 */
@Slf4j
@Singleton
public class BackendService {

    public static final String CODE_FILE_BACKEND_TF = "backend.tf";
    public static final String TERRAFORM_BACKEND_CONFIG_MAP_NAME = "backend-tf-config";

    private final TerraformBackendConfig config;
    private final String namespace;
    private final KubernetesClient k8sClient;
    private final ExecutorService executorService;

    public BackendService(final KubernetesConfiguration k8sConfig, final TerraformBackendConfig config, final KubernetesClient k8sClient,
                          @Named("io") final ExecutorService executorService) {
        this.config = config;
        this.namespace = k8sConfig.getNamespace();
        this.k8sClient = k8sClient;
        this.executorService = executorService;
    }

    public void init() throws IOException {
        setupBackendFileConfigMap(namespace, k8sClient);
        validateBackendSecret(namespace, config, k8sClient);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void setupBackendFileConfigMap(final String namespace, final KubernetesClient k8sClient) throws IOException {
        final Single<ConfigMap> maybeConfigMap =
                ensureConfigMapExists(k8sClient, namespace, TERRAFORM_BACKEND_CONFIG_MAP_NAME, Map.of(CODE_FILE_BACKEND_TF, getTerraformBackendCode()));

        maybeConfigMap.subscribeOn(Schedulers.from(executorService))
                      .subscribe(
                              configMap -> log.info("Backend config map exists: {}", configMap.getMetadata().getName()),
                              throwable -> log.error("Failed to ensure backend configmap exists: {}", throwable.getMessage())
                      );
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void validateBackendSecret(final String namespace, final TerraformBackendConfig config, final KubernetesClient k8sClient) {
        final String credentialsSecretName = config.getS3().getCredentialsSecretName();

        RxUtils.oneShot(k8sClient.getSecret(namespace, credentialsSecretName))
               .subscribeOn(Schedulers.from(executorService))
               .subscribe(secret -> {
                   log.info("Found configured credentials secret for terraform backend: {}", credentialsSecretName);
                   validateCredentialEnvVars(secret);
               }, throwable -> {
                   final String msg = String.format("Could not find configured credentials secret for terraform backend: %s", credentialsSecretName);
                   log.warn(msg);
                   log.debug(msg, throwable);
               });
    }

    public String getConfigMapName() {
        return TERRAFORM_BACKEND_CONFIG_MAP_NAME;
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
        final TerraformBackendConfig.S3 s3Config = config.getS3();
        log.info("Configuring terraform backend with: {}", s3Config);
        return FileUtils.readResourceContent(ModelConstants.TERRAFORM_BACKEND_FILE, ModelConstants.getConfigReplacement(s3Config));
    }
}
