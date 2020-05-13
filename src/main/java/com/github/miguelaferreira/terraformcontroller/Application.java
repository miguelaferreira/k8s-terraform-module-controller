package com.github.miguelaferreira.terraformcontroller;

import java.io.IOException;

import io.micronaut.runtime.Micronaut;
import io.reactivex.exceptions.UndeliverableException;
import io.reactivex.plugins.RxJavaPlugins;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Application {

    public static void main(final String[] args) {
        log.info("K8s Terraform Controller Started cmd");
        configureRxJavaErrorHandling();
        Micronaut.run(Application.class, args);
    }

    private static void configureRxJavaErrorHandling() {
        // See https://github.com/ReactiveX/RxJava/wiki/What's-different-in-2.0#error-handling
        RxJavaPlugins.setErrorHandler(e -> {
            if (e instanceof UndeliverableException) {
                e = e.getCause();
            }
            if (e instanceof IOException) {
                // fine, irrelevant network problem or API that throws on cancellation
                return;
            }
            if (e instanceof InterruptedException) {
                // fine, some blocking code was interrupted by a dispose call
                return;
            }
            if ((e instanceof NullPointerException) || (e instanceof IllegalArgumentException)) {
                // that's likely a bug in the application
                Thread.currentThread().getUncaughtExceptionHandler()
                      .uncaughtException(Thread.currentThread(), e);
                return;
            }
            if (e instanceof IllegalStateException) {
                // that's a bug in RxJava or in a custom operator
                Thread.currentThread().getUncaughtExceptionHandler()
                      .uncaughtException(Thread.currentThread(), e);
                return;
            }

            log.debug("Undeliverable exception received, not sure what to do", e);
        });
    }
}
