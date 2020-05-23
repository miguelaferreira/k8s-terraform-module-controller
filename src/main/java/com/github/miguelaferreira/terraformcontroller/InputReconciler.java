package com.github.miguelaferreira.terraformcontroller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.validation.constraints.NotNull;
import java.io.IOException;
import java.util.concurrent.ExecutorService;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.miguelaferreira.terraformcontroller.domain.Execution;
import com.github.miguelaferreira.terraformcontroller.domain.Execution.TF_COMMAND;
import com.github.miguelaferreira.terraformcontroller.domain.Input;
import com.github.miguelaferreira.terraformcontroller.domain.InputSource;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;
import io.reactivex.Single;
import io.reactivex.schedulers.Schedulers;
import io.vavr.control.Either;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InputReconciler implements Reconciler {

    private final BackendService backendService;
    private final InputService inputService;
    private final ExecutionService executionService;
    private final RegCredService regCredService;
    private final ExecutorService executorService;

    @Inject
    public InputReconciler(final BackendService backendService, final InputService inputService, final ExecutionService executionService,
                           final RegCredService regCredService, @Named("io") final ExecutorService executorService) {
        this.backendService = backendService;
        this.inputService = inputService;
        this.executionService = executionService;
        this.regCredService = regCredService;
        this.executorService = executorService;
    }

    @PostConstruct
    public void init() throws IOException {
        log.info("Initializing input reconciler");
        backendService.init();
    }

    @Override
    public void handleSecretWatch(final SecretWatchEvent event) {
        log.info("Reconciling module event: {}", event);

        switch (event.getType()) {
            case ADDED:
            case MODIFIED:
                provisionModule(event);
                break;
            case DELETED:
                destroyModule(event);
                break;
            case ERROR:
                log.warn("Will not handle error event: {}", event);
                break;
            default:
                log.error("Unexpected event: {}", event);
        }

        log.info("Module event reconciled: {}", event);
    }

    private void destroyModule(final SecretWatchEvent event) {
        final Secret secret = event.getObject();
        final String inputName = secret.getMetadata().getName();

        log.info("Destroying module: {}", inputName);

        if (!inputService.hasInputBeenSeen(inputName)) {
            log.debug("Module {} has NOT been seen", inputName);
            return;
        }

        log.debug("Module {} has been seen", inputName);

        createExecutionResources(secret, inputName, TF_COMMAND.DESTROY);
    }

    @NotNull
    private void provisionModule(final SecretWatchEvent event) {
        final Secret secret = event.getObject();
        final String inputName = secret.getMetadata().getName();
        log.info("Provisioning module: {}", inputName);

        if (inputService.hasInputBeenSeen(inputName)) {
            log.debug("Module {} has been seen", inputName);
            return;
        }

        log.debug("Module {} has NOT been seen", inputName);

        createExecutionResources(secret, inputName, TF_COMMAND.APPLY);
    }

    private void createExecutionResources(final Secret secret, final String inputName, final TF_COMMAND destroy) {
        processInput(destroy, secret, inputName);
    }

    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void processInput(final TF_COMMAND tfCommand, final Secret secret, final String inputName) {
        final Either<Throwable, InputSource> inputSourceOrError = parseInputSecret(secret);
        if (inputSourceOrError.isLeft()) {
            log.error("Failed to process input with an error parsing the input secret: " + inputName, inputSourceOrError.getLeft());
            return;
        }

        final Input inputResource = Input.fromDefaultSource(inputSourceOrError.get());
        registerInput(inputName, inputResource);

        final Execution executionPodModel = Execution.defaultExecution(
                tfCommand,
                inputName,
                inputResource,
                backendService.getCredentialsSecretName(),
                backendService.getConfigMapName(),
                inputService.getVariablesSecretName(inputName),
                regCredService.getSecrets());

        final Single<Pod> podEmitter = executionService.createExecutionPod(tfCommand, executionPodModel);
        podEmitter.subscribeOn(Schedulers.from(executorService))
                  .subscribe(
                          pod -> {
                              log.info("Created pod to {} module {}: {}", tfCommand == TF_COMMAND.APPLY ? "provision" : "destroy", inputName, pod.getMetadata().getName());
                              inputService.setInputStatus(inputName, tfCommand);
                          },
                          throwable -> log.error("Failed to create pod to execute module " + inputName + ": {}", throwable.getMessage())
                  );
    }

    public void registerInput(final String inputName, final Input inputResource) {
        log.debug("Registering input: {}", inputName);
        inputService.configureInput(inputName, inputResource);
    }

    public Either<Throwable, InputSource> parseInputSecret(final Secret secret) {
        try {
            return Either.right(SecretInputSource.of(secret));
        } catch (final JsonProcessingException e) {
            return Either.left(e);
        }
    }
}
