package io.github.intisy.kubernetes.wait;

import java.io.IOException;
import java.util.function.LongConsumer;
import java.util.function.LongSupplier;
import java.util.function.Predicate;

/**
 * Polls a resource's status document on a fixed interval until a predicate holds or a deadline
 * passes, the replacement for {@code kubectl wait} and {@code kubectl rollout status}.
 *
 * @implNote This is the thin, IO-bound half of the wait: fetching over the network on a timer
 * cannot be exercised by a unit test without either a live cluster or actually sleeping, so all
 * of the real predicate logic lives in {@link Conditions} instead, where it is a pure function
 * and fully testable. The retry/fail-fast decision below is small enough, and important enough
 * to get right, that it is tested directly by injecting the clock and sleeper through the
 * three-argument constructor, never by sleeping for real.
 */
public class Waiter {
    private static final long POLL_INTERVAL_MILLIS = 2000L;

    private final Fetcher fetcher;
    private final LongConsumer sleeper;
    private final LongSupplier clock;

    public Waiter(Fetcher fetcher) {
        this(fetcher, Waiter::sleep, System::currentTimeMillis);
    }

    /**
     * @implNote Exposed publicly, rather than kept package-private, so a test in a different
     * package can inject a fake clock and a non-sleeping sleeper and prove the retry and timeout
     * behavior instantly, without a live poll interval or a real deadline.
     */
    public Waiter(Fetcher fetcher, LongConsumer sleeper, LongSupplier clock) {
        this.fetcher = fetcher;
        this.sleeper = sleeper;
        this.clock = clock;
    }

    /**
     * @implNote A fetch failure does not necessarily end the wait: the consuming CLI runs these
     * waits through events such as a node power cycle, where the API server being briefly
     * unreachable is expected, not exceptional. An {@link IOException} with no HTTP status (a
     * connection failure) or a {@link FetchFailedException} carrying a status other than 401 or
     * 403 is treated as transient and retried until the deadline. A 401 or 403 is a
     * misconfiguration that will never resolve itself, so it is rethrown immediately rather than
     * burning the whole timeout on a wait that cannot succeed. On timeout the exception names the
     * kind, namespace and name being waited on, the last status document observed (if any ever
     * arrived), and the most recent fetch failure (if the last attempt failed), so a caller does
     * not have to repeat the whole wait just to learn what happened.
     */
    public void waitFor(String kind, String namespace, String name, Predicate<String> predicate, long timeoutMillis)
            throws IOException {
        long deadline = clock.getAsLong() + timeoutMillis;
        String lastStatusJson = null;
        IOException lastFailure = null;
        while (true) {
            try {
                lastStatusJson = fetcher.fetch();
                lastFailure = null;
                if (predicate.test(lastStatusJson)) {
                    return;
                }
            } catch (IOException e) {
                if (!isRetryable(e)) {
                    throw e;
                }
                lastFailure = e;
            }
            if (clock.getAsLong() >= deadline) {
                throw timeout(kind, namespace, name, timeoutMillis, lastStatusJson, lastFailure);
            }
            sleeper.accept(POLL_INTERVAL_MILLIS);
        }
    }

    private static boolean isRetryable(IOException e) {
        if (e instanceof FetchFailedException) {
            int statusCode = ((FetchFailedException) e).getStatusCode();
            return statusCode != 401 && statusCode != 403;
        }
        return true;
    }

    private static IllegalStateException timeout(String kind, String namespace, String name, long timeoutMillis,
                                                   String lastStatusJson, IOException lastFailure) {
        String qualifiedName = namespace != null ? namespace + "/" + name : name;
        String observedState = lastStatusJson != null ? Conditions.summarize(lastStatusJson) : "no successful fetch yet";
        String message = "timed out after " + timeoutMillis + "ms waiting for " + kind + " " + qualifiedName
                + " to become ready; last observed state: " + observedState;
        if (lastFailure != null) {
            message += "; last fetch failure: " + lastFailure.getMessage();
        }
        return new IllegalStateException(message);
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

    /**
     * @implNote Carries the HTTP status code alongside the message so {@link Waiter} can tell a
     * transient server error (retry) apart from an unauthorized or forbidden request (fail fast)
     * without re-parsing the message text.
     */
    public static final class FetchFailedException extends IOException {
        private final int statusCode;

        public FetchFailedException(String message, int statusCode) {
            super(message);
            this.statusCode = statusCode;
        }

        public int getStatusCode() {
            return statusCode;
        }
    }
}
