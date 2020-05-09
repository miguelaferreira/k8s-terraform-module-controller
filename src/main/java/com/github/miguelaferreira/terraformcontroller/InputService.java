package com.github.miguelaferreira.terraformcontroller;

import javax.inject.Singleton;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.miguelaferreira.terraformcontroller.domain.Input;
import com.github.miguelaferreira.terraformcontroller.domain.KubernetesOperationsService;
import com.github.miguelaferreira.terraformcontroller.domain.Variable;
import com.github.miguelaferreira.terraformcontroller.domain.VariableMap;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.vavr.control.Either;
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
    public static final String TAG_CONFIG_FAILED = "config-failed";
    private final String namespace;
    private final KubernetesClient k8sClient;
    private final ObjectMapper mapper;
    private final ConcurrentHashMap<String, String> inputLedger = new ConcurrentHashMap<>();

    public InputService(final String namespace, final KubernetesClient k8sClient, final ObjectMapper mapper) {
        this.namespace = namespace;
        this.k8sClient = k8sClient;
        this.mapper = mapper;
    }

    // Called on the reconciliation of each input
    public void configureInput(final String name, final Input model) throws JsonProcessingException {
        String tag = TAG_SEEN;
        final Either<Throwable, ConfigMap> maybeConfigMap =
                KubernetesOperationsService.ensureConfigMapExists(k8sClient, namespace, getModuleConfigMapName(name), serializeObject(model.getModule()));
        if (maybeConfigMap.isLeft()) {
            final Throwable cause = maybeConfigMap.getLeft();
            final String msg = String.format("Failed to ensure %s input module configmap exists", name);
            log.error(msg + ": {}", cause.getMessage());
            log.error(msg, cause);
            tag = TAG_CONFIG_FAILED;
        }
        final Either<Throwable, Secret> maybeSecret =
                KubernetesOperationsService.ensureSecretExists(k8sClient, namespace, getVariablesSecretName(name), buildVariablesData(model.getVariables()));
        if (maybeSecret.isLeft()) {
            final Throwable cause = maybeSecret.getLeft();
            final String msg = String.format("Failed to ensure %s input variables secret exists", name);
            log.error(msg + ": {}", cause.getMessage());
            log.error(msg, cause);
            tag = TAG_CONFIG_FAILED;
        }
        inputLedger.put(name, tag);
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

    public void setInputDestroyed(final String name) {
        inputLedger.put(name, TAG_DESTROYED);
    }
}
