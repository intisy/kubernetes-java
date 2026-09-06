package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.wait.Conditions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

    @Test
    void rolloutCompleteWhenScaledToZero() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1,\"replicas\":0}}"));
    }

    @Test
    void daemonSetRolloutCompleteWhenNoNodesMatchTheSelector() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1,\"desiredNumberScheduled\":0}}"));
    }

    /**
     * @implNote the case that made this compare against the spec: a workload the controller has
     * only just acknowledged publishes observedGeneration with replicas 0 and no readyReplicas at
     * all, which read against status alone is indistinguishable from a rollout that finished.
     */
    @Test
    void rolloutIncompleteWhenTheSpecWantsReplicasThatDoNotExistYet() {
        assertFalse(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"spec\":{\"replicas\":1},"
                + "\"status\":{\"observedGeneration\":1,\"replicas\":0}}"));
    }

    @Test
    void rolloutCompleteWhenTheSpecIsSatisfiedEvenWhileStatusReplicasLagBehind() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"spec\":{\"replicas\":1},"
                + "\"status\":{\"observedGeneration\":1,\"replicas\":2,\"readyReplicas\":1}}"));
    }

    @Test
    void rolloutCompleteWhenTheSpecScalesToZero() {
        assertTrue(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"spec\":{\"replicas\":0},"
                + "\"status\":{\"observedGeneration\":1}}"));
    }

    @Test
    void rolloutIncompleteWhenNoReplicaCountsAreReportedAtAll() {
        assertFalse(Conditions.isRolloutComplete("{\"metadata\":{\"generation\":1},"
                + "\"status\":{\"observedGeneration\":1}}"));
    }

    @Test
    void summarizeReportsNoStatusYet() {
        assertEquals("no status reported yet",
                Conditions.summarize("{\"metadata\":{\"name\":\"probe-writer\"}}"));
    }

    @Test
    void summarizeListsConditionsWhenPresent() {
        assertEquals("Ready=True", Conditions.summarize("{\"status\":{\"conditions\":"
                + "[{\"type\":\"Ready\",\"status\":\"True\"}]}}"));
    }

    @Test
    void summarizeReportsDaemonSetCountsWhenThereAreNoConditions() {
        assertEquals("numberReady=1, desiredNumberScheduled=2", Conditions.summarize("{\"status\":{"
                + "\"desiredNumberScheduled\":2,\"numberReady\":1}}"));
    }

    @Test
    void summarizeReportsDeploymentCountsWhenThereAreNoConditions() {
        assertEquals("readyReplicas=1, replicas=2, observedGeneration=3", Conditions.summarize("{\"status\":{"
                + "\"observedGeneration\":3,\"replicas\":2,\"readyReplicas\":1}}"));
    }
}
