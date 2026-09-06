package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.wait.Conditions;
import io.github.intisy.kubernetes.wait.Waiter;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WaiterTest {
    private static Waiter waiterWithFakeTime(Waiter.Fetcher fetcher, AtomicLong now) {
        return new Waiter(fetcher, millis -> now.addAndGet(millis), now::get);
    }

    @Test
    void retriesTransientFetchFailuresUntilReady() throws IOException {
        AtomicLong now = new AtomicLong(0L);
        AtomicInteger attempts = new AtomicInteger(0);
        Waiter waiter = waiterWithFakeTime(() -> {
            if (attempts.incrementAndGet() <= 2) {
                throw new IOException("connection reset");
            }
            return "{\"status\":{\"conditions\":[{\"type\":\"Ready\",\"status\":\"True\"}]}}";
        }, now);

        waiter.waitFor("Pod", "default", "probe-writer", Conditions::isReady, 60000L);

        assertEquals(3, attempts.get());
    }

    @Test
    void retriesHttpServerErrorsUntilReady() throws IOException {
        AtomicLong now = new AtomicLong(0L);
        AtomicInteger attempts = new AtomicInteger(0);
        Waiter waiter = waiterWithFakeTime(() -> {
            if (attempts.incrementAndGet() == 1) {
                throw new Waiter.FetchFailedException("service unavailable", 503);
            }
            return "{\"status\":{\"conditions\":[{\"type\":\"Ready\",\"status\":\"True\"}]}}";
        }, now);

        waiter.waitFor("Pod", "default", "probe-writer", Conditions::isReady, 60000L);

        assertEquals(2, attempts.get());
    }

    @Test
    void failsFastOnUnauthorizedWithoutRetrying() {
        AtomicLong now = new AtomicLong(0L);
        AtomicInteger attempts = new AtomicInteger(0);
        Waiter waiter = waiterWithFakeTime(() -> {
            attempts.incrementAndGet();
            throw new Waiter.FetchFailedException("unauthorized", 401);
        }, now);

        assertThrows(Waiter.FetchFailedException.class, () ->
                waiter.waitFor("Pod", "default", "probe-writer", statusJson -> true, 60000L));

        assertEquals(1, attempts.get());
    }

    @Test
    void failsFastOnForbiddenWithoutRetrying() {
        AtomicLong now = new AtomicLong(0L);
        AtomicInteger attempts = new AtomicInteger(0);
        Waiter waiter = waiterWithFakeTime(() -> {
            attempts.incrementAndGet();
            throw new Waiter.FetchFailedException("forbidden", 403);
        }, now);

        assertThrows(Waiter.FetchFailedException.class, () ->
                waiter.waitFor("Pod", "default", "probe-writer", statusJson -> true, 60000L));

        assertEquals(1, attempts.get());
    }

    @Test
    void timesOutNamingTheLastFetchFailureWhenFetchNeverSucceeds() {
        AtomicLong now = new AtomicLong(0L);
        Waiter waiter = waiterWithFakeTime(() -> {
            throw new IOException("connection refused");
        }, now);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                waiter.waitFor("Deployment", "default", "web", statusJson -> false, 5000L));

        assertTrue(thrown.getMessage().contains("connection refused"));
        assertTrue(thrown.getMessage().contains("no successful fetch yet"));
    }

    @Test
    void timesOutNamingTheLastObservedStateWhenThePredicateNeverHolds() {
        AtomicLong now = new AtomicLong(0L);
        Waiter waiter = waiterWithFakeTime(() ->
                "{\"status\":{\"conditions\":[{\"type\":\"Ready\",\"status\":\"False\"}]}}", now);

        IllegalStateException thrown = assertThrows(IllegalStateException.class, () ->
                waiter.waitFor("Pod", "default", "probe-writer", Conditions::isReady, 5000L));

        assertTrue(thrown.getMessage().contains("Ready=False"));
    }
}
