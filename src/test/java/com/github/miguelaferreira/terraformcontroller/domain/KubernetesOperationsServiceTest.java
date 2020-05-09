package com.github.miguelaferreira.terraformcontroller.domain;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertTrue;

import javax.inject.Inject;
import java.util.Map;

import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.test.annotation.MicronautTest;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

@MicronautTest(environments = "k8s")
class KubernetesOperationsServiceTest {

    @Inject
    KubernetesClient k8sClient;

    private final String name = "create-or-update-test";

    @AfterEach
    void tearDown() {
        Try.of(() -> Flowable.fromPublisher(k8sClient.deleteConfigMap("default", "create-or-update-test")).blockingFirst());
    }

    @Test
    void testEnsureConfigMapExists() {
        final Either<Throwable, ConfigMap> created = KubernetesOperationsService.ensureConfigMapExists(k8sClient,
                "default", name, Map.of("key1", "value1"));

        assertTrue(created.isRight());
        assertThat(created.get(), Matchers.notNullValue());
        assertThat(created.get().getData().entrySet(), Matchers.hasSize(1));
        assertThat(created.get().getData().get("key1"), Matchers.is("value1"));

        final Either<Throwable, ConfigMap> updated = KubernetesOperationsService.ensureConfigMapExists(k8sClient,
                "default", name, Map.of("key1", "value1", "key2", "value2"));

        assertTrue(updated.isRight());
        assertThat(updated.get(), Matchers.notNullValue());
        assertThat(created.get().getData().entrySet(), Matchers.hasSize(1));
        assertThat(updated.get().getData().get("key1"), Matchers.is("value1"));
    }

    @Test
    void testEnsureSecretExists() {
        final Either<Throwable, Secret> created = KubernetesOperationsService.ensureSecretExists(k8sClient,
                "default", name, Map.of("key1", "value1".getBytes()));

        assertTrue(created.isRight());
        assertThat(created.get(), Matchers.notNullValue());
        assertThat(created.get().getData().entrySet(), Matchers.hasSize(1));
        assertThat(created.get().getData().get("key1"), Matchers.is("value1".getBytes()));

        final Either<Throwable, Secret> updated = KubernetesOperationsService.ensureSecretExists(k8sClient,
                "default", name, Map.of("key1", "value1".getBytes(), "key2", "value2".getBytes()));

        assertTrue(updated.isRight());
        assertThat(updated.get(), Matchers.notNullValue());
        assertThat(created.get().getData().entrySet(), Matchers.hasSize(1));
        assertThat(updated.get().getData().get("key1"), Matchers.is("value1".getBytes()));
    }
}
