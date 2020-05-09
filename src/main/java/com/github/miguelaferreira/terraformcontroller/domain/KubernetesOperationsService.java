package com.github.miguelaferreira.terraformcontroller.domain;

import java.util.Map;

import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Metadata;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.reactivex.Flowable;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesOperationsService {

    public static Either<Throwable, ConfigMap> ensureConfigMapExists(final KubernetesClient k8sClient,
                                                                     final String namespace, final String name, final Map<String, String> data) {
        final ConfigMap configMap = new ConfigMap();
        final Metadata metadata = new Metadata();
        metadata.setName(name);
        configMap.setMetadata(metadata);
        configMap.setData(data);

        return Try.of(() -> Flowable.fromPublisher(k8sClient.createConfigMap(namespace, configMap)).blockingFirst())
                  .recover(throwable -> {
                      log.debug("Could not create configmap {}, will try update", name);
                      return Flowable.fromPublisher(k8sClient.replaceConfigMap(namespace, name, configMap)).blockingFirst();
                  }).toEither();
    }

    public static Either<Throwable, Secret> ensureSecretExists(final KubernetesClient k8sClient,
                                                               final String namespace, final String name, final Map<String, byte[]> data) {
        final Secret secret = new Secret();
        final Metadata metadata = new Metadata();
        metadata.setName(name);
        secret.setMetadata(metadata);
        secret.setData(data);

        return Try.of(() -> Flowable.fromPublisher(k8sClient.createSecret(namespace, secret)).blockingFirst())
                  .recover(throwable -> {
                      log.debug("Could not create configmap {}, will try update", name);
                      return Flowable.fromPublisher(k8sClient.replaceSecret(namespace, name, secret)).blockingFirst();
                  }).toEither();
    }
}
