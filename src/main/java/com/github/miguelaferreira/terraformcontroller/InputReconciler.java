package com.github.miguelaferreira.terraformcontroller;

import javax.annotation.PostConstruct;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.io.IOException;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.github.miguelaferreira.terraformcontroller.domain.Execution;
import com.github.miguelaferreira.terraformcontroller.domain.Execution.TF_COMMAND;
import com.github.miguelaferreira.terraformcontroller.domain.Input;
import com.github.miguelaferreira.terraformcontroller.domain.InputSource;
import io.micronaut.kubernetes.client.v1.pods.Pod;
import io.micronaut.kubernetes.client.v1.secrets.Secret;
import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;
import io.vavr.control.Either;
import io.vavr.control.Try;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class InputReconciler implements Reconciler {

    private final BackendService backendService;
    private final InputService inputService;
    private final ExecutionService executionService;
    private final RegCredService regCredService;

    @Inject
    public InputReconciler(final BackendService backendService, final InputService inputService, final ExecutionService executionService, final RegCredService
            regCredService) {
        this.backendService = backendService;
        this.inputService = inputService;
        this.executionService = executionService;
        this.regCredService = regCredService;
    }

    @PostConstruct
    public void init() throws IOException {
        backendService.init();
    }

    @Override
    public void handleSecretWatch(final SecretWatchEvent event) {
        log.info("Reconciling input event: {}", event);

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
    }

    private void destroyModule(final SecretWatchEvent event) {
        final Secret secret = event.getObject();
        final String inputName = secret.getMetadata().getName();

        log.info("Destroying module for: {}", inputName);

        if (!inputService.hasInputBeenSeen(inputName)) {
            log.debug("Input {} has NOT been seen", inputName);
            return;
        }

        log.debug("Input {} has been seen", inputName);

        processInput(TF_COMMAND.DESTROY, secret, inputName);

        inputService.setInputDestroyed(inputName);
    }

    private void provisionModule(final SecretWatchEvent event) {
        final Secret secret = event.getObject();
        final String inputName = secret.getMetadata().getName();
        log.info("Provisioning module for: {}", inputName);

        if (inputService.hasInputBeenSeen(inputName)) {
            log.debug("Input {} has been seen", inputName);
            return;
        }

        log.debug("Input {} has NOT been seen", inputName);

        processInput(TF_COMMAND.APPLY, secret, inputName);
    }

    private void processInput(final TF_COMMAND tfCommand, final Secret secret, final String inputName) {
        final InputSource inputSource = parseInputSecret(inputName, secret);
        final Input inputResource = Input.fromDefaultSource(inputSource);
        registerInput(inputName, inputResource);
        try {
            final Execution executionPodModel = Execution.defaultExecution(
                    tfCommand,
                    inputName,
                    inputResource,
                    backendService.getCredentialsSecretName(),
                    backendService.getConfigMapName(),
                    inputService.getVariablesSecretName(inputName),
                    regCredService.getSecrets());

            final Either<Throwable, Pod> maybePod = executePodForInput(inputName, executionPodModel);
            if (maybePod.isLeft()) {
                log.error("Reconcile request failed: {}", maybePod.getLeft().getMessage());
            } else {
                log.info("Reconcile request served: {}", maybePod.get().getMetadata().getName());
            }
        } catch (final Throwable t) {
            log.error("Reconciler exception: " + t.getMessage(), t);
        }
    }

    public Either<Throwable, Pod> executePodForInput(final String inputName, final Execution executionPodModel) {
        return Try.of(() -> executionService.executeInput(executionPodModel))
                  .onFailure(throwable -> {
                      final String message = String.format("Failed to execute pod for input %s", inputName);
                      log.error("{}: {}", message, throwable.getMessage());
                      log.debug(message, throwable);
                  }).toEither();
    }

    public void registerInput(final String inputName, final Input inputResource) {
        try {
            inputService.configureInput(inputName, inputResource);
        } catch (final JsonProcessingException e) {
            final String message = String.format("Failed to register input %s", inputName);
            log.error("{}: {}", message, e.getMessage());
            log.debug(message, e);
            throw new RuntimeException(message, e); // controller typically swallows these so we log them first
        }
    }

    public InputSource parseInputSecret(final String inputName, final Secret secret) {
        final InputSource inputSource;
        try {
            inputSource = SecretInputSource.of(secret);
        } catch (final JsonProcessingException e) {
            final String message = String.format("Failed to parse input secret %s", inputName);
            log.error("{}: {}", message, e.getMessage());
            log.debug(message, e);
            throw new RuntimeException(message, e); // controller typically swallows these so we log them first
        }
        return inputSource;
    }
}
