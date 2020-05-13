package com.github.miguelaferreira.terraformcontroller.utils;

import java.util.concurrent.TimeUnit;

import io.micronaut.http.client.exceptions.HttpClientResponseException;
import io.reactivex.Single;
import io.reactivex.functions.Consumer;
import io.reactivex.functions.Predicate;
import io.reactivex.internal.functions.Functions;
import lombok.extern.slf4j.Slf4j;
import org.reactivestreams.Publisher;

@Slf4j
public class RxUtils {

    public static final Predicate<Throwable> RETRY_UNLESS_EXISTS = throwable -> !"Conflict".equals(throwable.getMessage());

    public static final int TIMEOUT = 1500;
    public static final int RETRIES = 3;

    public static <T> Single<T> oneShot(final Publisher<T> source) {
        return oneShot(source, Functions.alwaysTrue());
    }

    public static <T> Single<T> oneShot(final Publisher<T> source, final Predicate<Throwable> retryConditions) {
        return Single.fromPublisher(source)
                     .doOnError(logError())
                     .timeout(TIMEOUT, TimeUnit.MILLISECONDS)
                     .retry(RETRIES, retryConditions);
    }

    public static Consumer<Throwable> logError() {
        return t -> {
            if (t instanceof HttpClientResponseException) {
                final HttpClientResponseException e = (HttpClientResponseException) t;
                log.trace("One shot (http) error: {} :: reason {}", e.getStatus(), e.getMessage());
            } else {
                log.trace("One shot error: {}", t.getMessage());
            }
        };
    }
}
