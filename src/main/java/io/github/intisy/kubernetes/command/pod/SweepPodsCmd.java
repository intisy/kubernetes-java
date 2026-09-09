package io.github.intisy.kubernetes.command.pod;

import io.github.intisy.kubernetes.exception.NotFoundException;
import io.github.intisy.kubernetes.model.Pod;
import io.github.intisy.kubernetes.transport.KubernetesHttpClient;

import java.util.ArrayList;
import java.util.List;

/**
 * Command to delete every pod in a namespace that matches a label selector, returning the names it
 * swept.
 *
 * @author Finn Birich
 */
public class SweepPodsCmd {
    private final KubernetesHttpClient client;
    private String namespace = "default";
    private String labelSelector;
    private Integer gracePeriodSeconds;

    public SweepPodsCmd(KubernetesHttpClient client) {
        this.client = client;
    }

    public SweepPodsCmd withNamespace(String namespace) {
        this.namespace = namespace;
        return this;
    }

    public SweepPodsCmd withLabelSelector(String labelSelector) {
        this.labelSelector = labelSelector;
        return this;
    }

    public SweepPodsCmd withGracePeriod(int seconds) {
        this.gracePeriodSeconds = seconds;
        return this;
    }

    public SweepPodsCmd withForce(boolean force) {
        if (force) {
            this.gracePeriodSeconds = 0;
        }
        return this;
    }

    /**
     * @implNote the selector is required rather than defaulted, because an absent selector matches
     * every pod in the namespace: the convenient default is also the destructive one. Deletion
     * tolerates a missing pod the way {@code kubectl delete --ignore-not-found} does, since a pod
     * listed a moment ago can be gone by the time it is deleted, and that race is the normal case
     * for the ephemeral pods this exists to clear rather than an error.
     */
    public List<String> exec() {
        if (labelSelector == null || labelSelector.isEmpty()) {
            throw new IllegalStateException("sweepPods requires a label selector; without one it would"
                    + " delete every pod in namespace " + namespace);
        }
        List<String> swept = new ArrayList<>();
        List<Pod> matched = new ListPodsCmd(client)
                .withNamespace(namespace)
                .withLabelSelector(labelSelector)
                .exec();
        for (Pod pod : matched) {
            String name = pod.getMetadata().getName();
            DeletePodCmd delete = new DeletePodCmd(client, name).withNamespace(namespace);
            if (gracePeriodSeconds != null) {
                delete.withGracePeriod(gracePeriodSeconds);
            }
            try {
                delete.exec();
            } catch (NotFoundException alreadyGone) {
                // listed a moment ago, gone before the delete; the sweep's goal is already met.
            }
            swept.add(name);
        }
        return swept;
    }
}
