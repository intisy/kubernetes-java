package io.github.intisy.kubernetes.wait;

import java.io.IOException;
import java.util.function.Predicate;

/**
 * Polls a resource's status document on a fixed interval until a predicate holds or a deadline
 * passes, the replacement for {@code kubectl wait} and {@code kubectl rollout status}.
 *
 * @implNote This is the thin, IO-bound half of the wait: fetching over the network on a timer
 * cannot be exercised by a unit test without either a live cluster or actually sleeping, so all
 * of the real logic lives in {@link Conditions} instead, where it is a pure function and fully
 * testable.
 */
public class Waiter {
    private static final long POLL_INTERVAL_MILLIS = 2000L;

    private final Fetcher fetcher;

    public Waiter(Fetcher fetcher) {
        this.fetcher = fetcher;
    }

    /**
     * @implNote On timeout the exception names the kind, namespace and name being waited on,
     * plus a summary of the last status document observed, so a caller does not have to repeat
     * the whole wait just to learn what the object actually looked like when it gave up.
     */
    public void waitFor(String kind, String namespace, String name, Predicate<String> predicate, long timeoutMillis)
            throws IOException {
        long deadline = System.currentTimeMillis() + timeoutMillis;
        String lastStatusJson = fetcher.fetch();
        while (!predicate.test(lastStatusJson)) {
            if (System.currentTimeMillis() >= deadline) {
                throw timeout(kind, namespace, name, timeoutMillis, lastStatusJson);
            }
            sleep(POLL_INTERVAL_MILLIS);
            lastStatusJson = fetcher.fetch();
        }
    }

    private static IllegalStateException timeout(String kind, String namespace, String name,
                                                   long timeoutMillis, String lastStatusJson) {
        String qualifiedName = namespace != null ? namespace + "/" + name : name;
        return new IllegalStateException("timed out after " + timeoutMillis + "ms waiting for "
                + kind + " " + qualifiedName + " to become ready; last observed state: "
                + Conditions.summarize(lastStatusJson));
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("interrupted while waiting", e);
        }
    }

    public interface Fetcher {
        String fetch() throws IOException;
    }
}
