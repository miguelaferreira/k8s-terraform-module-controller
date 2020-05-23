package com.github.miguelaferreira.terraformcontroller;

import javax.annotation.PreDestroy;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.function.Function;
import java.util.stream.Collectors;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.kubernetes.client.v1.KubernetesClient;
import io.micronaut.kubernetes.client.v1.KubernetesConfiguration;
import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;
import io.micronaut.runtime.event.ApplicationStartupEvent;
import io.reactivex.Flowable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class Controller implements ApplicationEventListener<ApplicationStartupEvent> {

    private final String namespace;
    private final KubernetesClient k8sClient;
    private final ControllerConfig controllerConfig;
    private final List<Reconciler> inputReconcilers;
    private final ExecutorService executorService;

    private Collection<Disposable> subscriptions;

    @Inject
    public Controller(final KubernetesConfiguration kubernetesConfiguration, final KubernetesClient k8sClient, final ControllerConfig controllerConfig,
                      final List<Reconciler> inputReconcilers, @Named("io") final ExecutorService executorService) {
        this.namespace = kubernetesConfiguration.getNamespace();
        this.k8sClient = k8sClient;
        this.controllerConfig = controllerConfig;
        this.inputReconcilers = inputReconcilers;
        this.executorService = executorService;

        log.debug("Created controller with {} reconciler(s): {}",
                inputReconcilers.size(),
                inputReconcilers.stream()
                                .map(Reconciler::getClass)
                                .map(Class::toString)
                                .collect(Collectors.joining(", ")));
    }

    @Override
    public void onApplicationEvent(final ApplicationStartupEvent event) {
        log.debug("Caught application start event: {}", event);
        start();
    }

    public void start() {
        final String secretLabelSelector = controllerConfig.getInputLabelSelector();

        log.info("Creating secret watcher in namespace [{}] for controller inputs (using labelSelector {})", namespace, secretLabelSelector);
        final Flowable<SecretWatchEvent> inputSecretWatcher = Flowable.fromPublisher(k8sClient.watchSecrets(namespace, 0L, secretLabelSelector));

        log.info("Registering {} input reconcilers", inputReconcilers.size());
        subscriptions = inputReconcilers.stream()
                                        .map(subscribeReconcilers(inputSecretWatcher))
                                        .collect(Collectors.toList());
    }

    public Function<Reconciler, Disposable> subscribeReconcilers(final Flowable<SecretWatchEvent> inputSecretWatcher) {
        return reconciler -> inputSecretWatcher.doOnNext(e -> log.debug("Received input (secret) watch event: {}", e))
                                               .doOnError(t -> log.error("Input (secret) watcher error", t))
                                               .onErrorReturnItem(new SecretWatchEvent(SecretWatchEvent.EventType.ERROR))
                                               .retry(5)
                                               .subscribeOn(Schedulers.from(executorService))
                                               .subscribe(reconciler::handleSecretWatch);
    }

    @PreDestroy
    public void stop() {
        subscriptions.forEach(Disposable::dispose);
    }
}
