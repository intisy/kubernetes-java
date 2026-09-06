package io.github.intisy.kubernetes.wait;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Pure predicates over a resource's status document, the replacement for
 * {@code kubectl wait --for=condition=Ready} and {@code kubectl rollout status}.
 */
public final class Conditions {
    private Conditions() {
    }

    /**
     * @implNote Returns false rather than throwing when any part of the path is missing. A
     * newly created object has no {@code status} at all until a controller reconciles it, and
     * that must read as "not ready yet" so a caller polling this can keep waiting instead of
     * treating a normal startup race as a hard failure.
     */
    public static boolean isReady(String statusJson) {
        JsonObject status = objectAt(parse(statusJson), "status");
        JsonArray conditions = status != null ? arrayAt(status, "conditions") : null;
        if (conditions == null) {
            return false;
        }
        for (JsonElement element : conditions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject condition = element.getAsJsonObject();
            if ("Ready".equals(stringAt(condition, "type"))) {
                return "True".equals(stringAt(condition, "status"));
            }
        }
        return false;
    }

    /**
     * @implNote {@code status.observedGeneration} must equal {@code metadata.generation} before
     * any replica count is trusted: while it lags, the counts still describe the previous spec,
     * so reporting complete from them would be reporting success for a rollout that has not
     * started. Once generation matches, the replica pair compared depends on which fields the
     * status carries: {@code numberReady} against {@code desiredNumberScheduled} for a DaemonSet,
     * {@code readyReplicas} against {@code replicas} for everything else (Deployment, ReplicaSet,
     * StatefulSet). Which pair applies, and whether it counts as reported at all, is decided by
     * key presence rather than by value: a workload scaled to zero (or a DaemonSet whose node
     * selector matches no nodes) legitimately reports its count as {@code 0} with the ready
     * counterpart absent, and that is a completed rollout, not an incomplete one. Only a pair
     * whose primary key ({@code replicas} or {@code desiredNumberScheduled}) is missing entirely
     * reads as incomplete, since that is what an object with no status yet looks like.
     * <p>
     * The replica target is {@code spec.replicas} when the object carries one. Reading
     * {@code status.replicas} as the target instead is what a freshly created workload publishes
     * before any pod exists, so a caller polling a cold object would be told a rollout that has not
     * started is complete. Two further conditions keep an update in progress from reading as
     * finished, both of which {@code kubectl rollout status} also requires: every replica must be
     * on the new revision ({@code updatedReplicas}), and no pod from the old revision may remain
     * ({@code status.replicas} above the target means a surge pod is still being replaced). Without
     * them a rolling update reads complete the moment the OLD pods happen to satisfy the ready
     * count.
     */
    public static boolean isRolloutComplete(String statusJson) {
        JsonObject root = parse(statusJson);
        JsonObject metadata = objectAt(root, "metadata");
        JsonObject status = objectAt(root, "status");
        if (metadata == null || status == null
                || !metadata.has("generation") || !status.has("observedGeneration")) {
            return false;
        }
        if (longAt(metadata, "generation") != longAt(status, "observedGeneration")) {
            return false;
        }
        if (status.has("desiredNumberScheduled") || status.has("numberReady")) {
            return status.has("desiredNumberScheduled")
                    && longAt(status, "numberReady") >= longAt(status, "desiredNumberScheduled");
        }
        JsonObject spec = objectAt(root, "spec");
        if (spec != null && spec.has("replicas")) {
            long target = longAt(spec, "replicas");
            if (status.has("updatedReplicas") && longAt(status, "updatedReplicas") < target) {
                return false;
            }
            if (status.has("replicas") && longAt(status, "replicas") > target) {
                return false;
            }
            return longAt(status, "readyReplicas") >= target;
        }
        return status.has("replicas")
                && longAt(status, "readyReplicas") >= longAt(status, "replicas");
    }

    public static String summarize(String statusJson) {
        JsonObject status = objectAt(parse(statusJson), "status");
        if (status == null) {
            return "no status reported yet";
        }
        JsonArray conditions = arrayAt(status, "conditions");
        if (conditions != null && conditions.size() > 0) {
            String conditionSummary = summarizeConditions(conditions);
            if (conditionSummary != null) {
                return conditionSummary;
            }
        }
        if (status.has("desiredNumberScheduled") || status.has("numberReady")) {
            return "numberReady=" + longAt(status, "numberReady")
                    + ", desiredNumberScheduled=" + longAt(status, "desiredNumberScheduled");
        }
        String target = specReplicas(statusJson);
        return "readyReplicas=" + longAt(status, "readyReplicas")
                + ", replicas=" + longAt(status, "replicas")
                + target
                + ", observedGeneration=" + longAt(status, "observedGeneration");
    }

    private static String summarizeConditions(JsonArray conditions) {
        StringBuilder summary = new StringBuilder();
        for (JsonElement element : conditions) {
            if (!element.isJsonObject()) {
                continue;
            }
            JsonObject condition = element.getAsJsonObject();
            if (summary.length() > 0) {
                summary.append(", ");
            }
            summary.append(stringAt(condition, "type")).append('=').append(stringAt(condition, "status"));
        }
        return summary.length() > 0 ? summary.toString() : null;
    }

    /**
     * @implNote without this a caller reading the summary of a cold workload sees
     * {@code readyReplicas=0, replicas=0}, a pair that looks satisfied, next to a verdict of not
     * ready. The number that actually decides it is in the spec, and this is also the text a wait
     * timeout carries, so it has to be self-consistent on its own.
     */
    private static String specReplicas(String statusJson) {
        JsonObject spec = objectAt(parse(statusJson), "spec");
        return spec != null && spec.has("replicas") ? ", spec.replicas=" + longAt(spec, "replicas") : "";
    }

    private static JsonObject parse(String json) {
        JsonElement element = JsonParser.parseString(json);
        return element.isJsonObject() ? element.getAsJsonObject() : new JsonObject();
    }

    private static JsonObject objectAt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonObject()) {
            return null;
        }
        return object.getAsJsonObject(key);
    }

    private static JsonArray arrayAt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonArray()) {
            return null;
        }
        return object.getAsJsonArray(key);
    }

    private static String stringAt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return null;
        }
        return object.get(key).getAsString();
    }

    private static long longAt(JsonObject object, String key) {
        if (object == null || !object.has(key) || !object.get(key).isJsonPrimitive()) {
            return 0L;
        }
        return object.get(key).getAsLong();
    }
}
