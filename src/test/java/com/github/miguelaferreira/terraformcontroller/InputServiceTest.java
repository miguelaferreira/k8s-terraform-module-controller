package com.github.miguelaferreira.terraformcontroller;

import static org.hamcrest.MatcherAssert.assertThat;

import javax.inject.Inject;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.miguelaferreira.terraformcontroller.domain.Input;
import com.github.miguelaferreira.terraformcontroller.utils.RxUtils;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Metadata;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.test.annotation.MicronautTest;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = {"k8s", "local"})
class InputServiceTest {

    public static final String NAMESPACE = "default";

    @Inject
    InputService inputService;

    @Inject
    KubernetesClient k8sClient;

    private final String name = "test-input";

    @BeforeEach
    void setUp() {
        try {
            RxUtils.oneShot(k8sClient.deleteConfigMap(NAMESPACE, inputService.getModuleConfigMapName(name)));
        } catch (final Throwable e) {
            // ignoring
        }

        try {
            RxUtils.oneShot(k8sClient.deleteSecret(NAMESPACE, inputService.getVariablesSecretName(name)));
        } catch (final Throwable e) {
            // ignoring
        }
    }

    @Test
    void testConfigureInput() throws JsonProcessingException {
        final Secret inputSecret = new Secret();

        final Metadata metadata = new Metadata();
        metadata.setName(name);
        inputSecret.setMetadata(metadata);
        final Map<String, byte[]> data = Map.of(
                "name", this.name.getBytes(),
                "image", "image".getBytes(),
                "tag", "tag".getBytes(),
                "path", "path".getBytes(),
                "variables", "{\"key\":\"value\"}".getBytes()
        );
        inputSecret.setData(data);

        inputService.configureInput(this.name, Input.fromDefaultSource(SecretInputSource.of(inputSecret)));

        final ConfigMap moduleConfigmap = RxUtils.oneShot(k8sClient.getConfigMap(NAMESPACE, inputService.getModuleConfigMapName(this.name))).blockingGet();
        assertThat(moduleConfigmap.getMetadata().getName(), Matchers.is(inputService.getModuleConfigMapName(this.name)));
        assertThat(moduleConfigmap.getData().containsKey("object"), Matchers.is(true));
        assertThat(moduleConfigmap.getData().get("object"), Matchers.is("{\"image\":\"image\",\"tag\":\"tag\",\"path\":\"path\"}"));

        final Secret variablesSecret = RxUtils.oneShot(k8sClient.getSecret(NAMESPACE, inputService.getVariablesSecretName(this.name))).blockingGet();
        assertThat(variablesSecret.getMetadata().getName(), Matchers.is(inputService.getVariablesSecretName(this.name)));
        assertThat(variablesSecret.getData().containsKey("TF_VAR_key"), Matchers.is(true));
        assertThat(new String(variablesSecret.getData().get("TF_VAR_key")), Matchers.is("value"));
    }
}
