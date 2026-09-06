package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.wait.Conditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ConditionsTest {
    @Test
    void readyWhenTheReadyConditionIsTrue() {
        assertTrue(Conditions.isReady("{\"status\":{\"conditions\":"
                + "[{\"type\":\"Initialized\",\"status\":\"True\"},"
                + "{\"type\":\"Ready\",\"status\":\"True\"}]}}"));
    }

    @Test
    void notReadyWhenTheReadyConditionIsFalse() {
        assertFalse(Conditions.isReady("{\"status\":{\"conditions\":"
                + "[{\"type\":\"Ready\",\"status\":\"False\"}]}}"));
    }

    @Test
    void notReadyWhenThereIsNoStatusYet() {
        assertFalse(Conditions.isReady("{\"metadata\":{\"name\":\"probe-writer\"}}"));
    }

    @Test
    void cnpgClusterIsReadyThroughTheSameConditionPath() {
        assertFalse(Conditions.isReady("{\"status\":{\"conditions\":"
                + "[{\"type\":\"Ready\",\"status\":\"False\",\"reason\":\"Creating\"}]}}"));
        assertTrue(Conditions.isReady("{\"status\":{\"conditions\":"
                + "[{\"type\":\"Ready\",\"status\":\"True\"}]}}"));
    }

    @Test
    void rolloutIncompleteWhileTheObservedGenerationLagsTheSpec() {
        assertFalse(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":3},"
                + "\"status\":{\"observedGeneration\":2,\"replicas\":1,\"readyReplicas\":1}}"));
    }

    @Test
    void rolloutCompleteWhenGenerationMatchesAndEveryReplicaIsReady() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":3},"
                + "\"status\":{\"observedGeneration\":3,\"replicas\":2,\"readyReplicas\":2}}"));
    }

    @Test
    void rolloutIncompleteWhenReplicasAreStillCatchingUp() {
        assertFalse(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1,\"replicas\":3,\"readyReplicas\":2}}"));
    }

    @Test
    void daemonSetRolloutUsesTheScheduledAndReadyCounts() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1,"
                + "\"desiredNumberScheduled\":1,\"numberReady\":1}}"));
        assertFalse(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1,"
                + "\"desiredNumberScheduled\":1,\"numberReady\":0}}"));
    }
}
