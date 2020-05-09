package com.github.miguelaferreira.terraformcontroller;

import io.micronaut.kubernetes.client.v1.secrets.SecretWatchEvent;

public interface Reconciler {

    void handleSecretWatch(final SecretWatchEvent event);
}
