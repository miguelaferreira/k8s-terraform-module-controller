package com.github.miguelaferreira.terraformcontroller;

import static com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService.ensureConfigMapExists;
import static com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService.ensureSecretExists;

import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.miguelaferreira.terraformcontroller.domain.Execution;
import com.github.miguelaferreira.terraformcontroller.domain.Input;
import com.github.miguelaferreira.terraformcontroller.domain.Variable;
import com.github.miguelaferreira.terraformcontroller.domain.VariableMap;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

/*
 * Manages the data behind the provisioning of input resources.
 * Creates and manages a ledger of input resources, and per resources,
 * a copy of the input data to allow destroying the resources when the input
 * is deleted.
 *
 *
 * Input resource specifies:
 *   - a module
 *   - input variables
 *
 * Per input resource, Manager creates and maintains:
 *   - variables secret
 *   - module configmap
 */
@Slf4j
@Singleton
public class InputService {

    public static final String TAG_SEEN = "seen";
    public static final String TAG_DESTROYED = "destroyed";
    private final String namespace;
    private final KubernetesClient k8sClient;
    private final ExecutorService executorService;
    private final ObjectMapper mapper;

    private final ConcurrentHashMap<String, String> inputLedger = new ConcurrentHashMap<>();

    public InputService(final KubernetesConfiguration k8sConfig, final KubernetesClient k8sClient, @Named("io") final ExecutorService executorService, final ObjectMapper mapper) {
        this.namespace = k8sConfig.getNamespace();
        this.k8sClient = k8sClient;
        this.executorService = executorService;
        this.mapper = mapper;
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    public void configureInput(final String name, final Input model) {
        log.debug("Configuring input {}: {}", name, model);

        final Map<String, String> serialisedModuleObject;
        try {
            serialisedModuleObject = serializeObject(model.getModule());
        } catch (final JsonProcessingException e) {
            log.error("Failed to serialize module object to save in config map: {}", e.getMessage());
            log.debug("Failed to serialize module object to save in config map", e);
            return;
        }

        final Single<ConfigMap> configMapEmitter = ensureConfigMapExists(k8sClient, namespace, getModuleConfigMapName(name), serialisedModuleObject);
        configMapEmitter.subscribeOn(Schedulers.from(executorService))
                        .subscribe(
                                configMap -> log.info("Config map {} for module created for input: {}", configMap.getMetadata().getName(), name),
                                throwable -> log.error("Failed to create config map for module for input {}: {}", name, throwable.getMessage())
                        );

        final Single<Secret> secretEmitter = ensureSecretExists(k8sClient, namespace, getVariablesSecretName(name), buildVariablesData(model.getVariables()));
        secretEmitter.subscribeOn(Schedulers.from(executorService))
                     .subscribe(
                             secret -> log.info("Secret {} for variables created for input: {}", secret.getMetadata().getName(), name),
                             throwable -> log.error("Failed to create secret for variables for input {}: {}", name, throwable.getMessage())
                     );
    }

    public void setInputStatus(final String name, final Execution.TF_COMMAND tfCommand) {
        inputLedger.put(name, tfCommand == Execution.TF_COMMAND.APPLY ? TAG_SEEN : TAG_DESTROYED);
    }

    public String getVariablesSecretName(final String name) {
        return String.format("vars-%s", name);
    }

    public String getModuleConfigMapName(final String name) {
        return String.format("module-%s", name);
    }

    public boolean hasInputBeenSeen(final String name) {
        return inputLedger.containsKey(name) && inputLedger.get(name).equals(TAG_SEEN);
    }

    private Map<String, byte[]> buildVariablesData(final VariableMap variables) {
        return variables.getVariables()
                        .collect(Collectors.toMap(Variable::getName, var -> var.getValue().getBytes()));
    }

    private <T> Map<String, String> serializeObject(final T model) throws JsonProcessingException {
        return Map.of("object", mapper.writeValueAsString(model));
    }
}
