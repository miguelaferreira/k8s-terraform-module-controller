package com.github.miguelaferreira.terraformcontroller.domain;

import static com.github.miguelaferreira.terraformcontroller.utils.RxUtils.RETRY_UNLESS_EXISTS;

import java.util.Map;

import com.github.miguelaferreira.terraformcontroller.utils.RxUtils;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.Metadata;
import io.micronaut.kubernetes.client.v1.configmaps.ConfigMap;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.reactivex.Single;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class KubernetesOperationsService {

    public static Single<ConfigMap> ensureConfigMapExists(final KubernetesClient k8sClient,
                                                          final String namespace, final String name, final Map<String, String> data) {

        log.debug("Trying to ensure config map exists: {}/{}", namespace, name);

        final ConfigMap configMap = new ConfigMap();
        final Metadata metadata = new Metadata();
        metadata.setName(name);
        configMap.setMetadata(metadata);
        configMap.setData(data);

        return RxUtils.oneShot(k8sClient.createConfigMap(namespace, configMap), RETRY_UNLESS_EXISTS)
                      .onErrorResumeNext(throwable -> {
                          log.debug("Could not create configmap {}, will try update", name);
                          return RxUtils.oneShot(k8sClient.replaceConfigMap(namespace, name, configMap));
                      })
                      .doOnError(throwable -> {
                          log.debug("Attempt to update configmap {}/{} failed with {}", namespace, name, throwable.getMessage());
                          log.warn("Cannot ensure configmap exists: {}/{}", namespace, name);
                      });
    }

    public static Single<Secret> ensureSecretExists(final KubernetesClient k8sClient,
                                                    final String namespace, final String name, final Map<String, byte[]> data) {

        log.debug("Trying to ensure secret exists: {}/{}", namespace, name);

        final Secret secret = new Secret();
        final Metadata metadata = new Metadata();
        metadata.setName(name);
        secret.setMetadata(metadata);
        secret.setData(data);

        return RxUtils.oneShot(k8sClient.createSecret(namespace, secret), RETRY_UNLESS_EXISTS)
                      .onErrorResumeNext(throwable -> {
                          log.debug("Could not create secret {}, will try update", name);
                          return RxUtils.oneShot(k8sClient.replaceSecret(namespace, name, secret));
                      })
                      .doOnError(throwable -> {
                          log.debug("Attempt to update secret {}/{} failed with {}", namespace, name, throwable.getMessage());
                          log.warn("Cannot ensure secret exists: {}/{}", namespace, name);
                      });
    }
}
