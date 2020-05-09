package com.github.miguelaferreira.terraformcontroller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.time.LocalDateTime;
import java.util.List;

import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;
import io.reactivex.Flowable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class Controller {

    private final String namespace;
    private final KubernetesClient k8sClient;
    private final ControllerConfig controllerConfig;
    private final List<Reconciler> inputReconcilers;

    @Inject
    public Controller(final KubernetesConfiguration kubernetesConfiguration, final KubernetesClient k8sClient, final ControllerConfig controllerConfig,
                      final List<Reconciler> inputReconcilers) {
        this.namespace = kubernetesConfiguration.getNamespace();
        this.k8sClient = k8sClient;
        this.controllerConfig = controllerConfig;
        this.inputReconcilers = inputReconcilers;
    }

    @PostConstruct
    public void start() {
        final String secretLabelSelector = controllerConfig.getSecretLabelSelector();

        log.info("Creating secret watcher in namespace for controller inputs (using labelSelector {})", secretLabelSelector);
        final Flowable<SecretWatchEvent> inputSecretWatcher = Flowable.fromPublisher(k8sClient.watchSecrets(namespace, 0L, secretLabelSelector))
                                                                      .doOnError(t -> log.error("Input secret watcher error", t))
                                                                      .retry(5)
                                                                      .subscribeOn(Schedulers.io());
        log.info("Registering input reconcilers");
        //noinspection ResultOfMethodCallIgnored
        inputReconcilers.forEach(reconciler -> inputSecretWatcher.subscribe(
                reconciler::handleSecretWatch,
                e -> log.error("Input handling error", e),
                () -> log.info("Input complete!")
        ));

        //noinspection InfiniteLoopStatement
        while (true) {
            try {
                //noinspection BusyWait
                Thread.sleep(3600000L);
                log.trace("Main loop ping {}", LocalDateTime.now());
            } catch (final InterruptedException e) {
                log.info("Exit: main loop interrupted: {}", e.getMessage());
                log.debug("Main loop interrupted.", e);
            }
        }
    }
}
