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
     * StatefulSet). A missing pair reads as zero on both sides, which this treats as incomplete
     * rather than complete, since otherwise a freshly created object with no status yet would
     * report a finished rollout the instant it appeared.
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
            long desired = longAt(status, "desiredNumberScheduled");
            long ready = longAt(status, "numberReady");
            return desired > 0 && ready >= desired;
        }
        long replicas = longAt(status, "replicas");
        long readyReplicas = longAt(status, "readyReplicas");
        return replicas > 0 && readyReplicas >= replicas;
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
        return "readyReplicas=" + longAt(status, "readyReplicas")
                + ", replicas=" + longAt(status, "replicas")
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
