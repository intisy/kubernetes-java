package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.KubernetesProvider;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Unit tests for which providers {@link KubernetesProvider#stopAll()} stops.
 *
 * @author Finn Birich
 * @implNote that this library installs NO shutdown hook is the property these tests exist for, and
 * it is the one property they cannot assert: {@code ApplicationShutdownHooks.hooks} is not
 * reachable without {@code --add-opens}, and a hook that only runs at JVM exit does nothing
 * observable during a suite. So the registration path is pinned by its effect on {@code stopAll}
 * instead, and a reintroduced hook would be caught by review rather than here.
 */
public class ProviderLifecycleTest {

    private final List<TestableProvider> registered = new ArrayList<TestableProvider>();

    @AfterEach
    void tearDown() {
        for (TestableProvider provider : registered) {
            provider.doUnregisterInstance();
        }
        registered.clear();
    }

    @Test
    @DisplayName("stopAll stops a registered provider")
    void stopsRegisteredProvider() {
        TestableProvider provider = register(new TestableProvider());

        KubernetesProvider.stopAll();

        assertEquals(1, provider.stopCount, "a registered provider should have been stopped");
    }

    @Test
    @DisplayName("stopAll leaves a provider that unregistered itself alone")
    void leavesUnregisteredProviderAlone() {
        TestableProvider provider = new TestableProvider();
        provider.doRegisterInstance();
        provider.doUnregisterInstance();

        KubernetesProvider.stopAll();

        assertEquals(0, provider.stopCount, "an unregistered provider should not have been stopped");
    }

    @Test
    @DisplayName("stopAll stops the rest when one provider's stop throws")
    void stopsTheRestWhenOneThrows() {
        TestableProvider failing = register(new TestableProvider());
        failing.failOnStop = true;
        TestableProvider healthy = register(new TestableProvider());

        KubernetesProvider.stopAll();

        assertTrue(failing.stopCount > 0, "the failing provider should have been attempted");
        assertEquals(1, healthy.stopCount, "a later provider should be stopped despite an earlier failure");
    }

    private TestableProvider register(TestableProvider provider) {
        provider.doRegisterInstance();
        registered.add(provider);
        return provider;
    }

    /**
     * Counts its own stops and can be told to fail one, so that a caller's guarding is observable.
     */
    static class TestableProvider extends KubernetesProvider {

        private final String instanceId = "lifecycle-" + System.nanoTime();

        int stopCount;
        boolean failOnStop;

        @Override
        public String getInstanceId() {
            return instanceId;
        }

        @Override
        public void start() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public KubernetesClient createClient() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public KubernetesClient getClient() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public void stop() {
            stopCount++;
            unregisterInstance();
            if (failOnStop) {
                throw new IllegalStateException("stop refused for " + instanceId);
            }
        }

        @Override
        public void ensureInstalled() throws IOException {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        void doRegisterInstance() {
            registerInstance();
        }

        void doUnregisterInstance() {
            unregisterInstance();
        }
    }
}
