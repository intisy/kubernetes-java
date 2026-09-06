package io.github.intisy.kubernetes.exec;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.nio.charset.StandardCharsets;

/**
 * Demultiplexes the {@code v5.channel.k8s.io} binary frame stream from a pod exec into stdout,
 * stderr and an exit code. Pure and stateful only over its own buffers, so this is testable
 * without a socket; {@link PodExec} is the thin part that actually opens one.
 */
public class ExecChannels {
    private static final int CHANNEL_STDOUT = 1;
    private static final int CHANNEL_STDERR = 2;
    private static final int CHANNEL_ERROR = 3;

    private final StringBuilder stdout = new StringBuilder();
    private final StringBuilder stderr = new StringBuilder();
    private int exitCode = 0;

    /**
     * @implNote A frame of length 1 carries only the channel byte and no payload: it is the
     * protocol's keepalive, not a zero-length write, so it must not be appended as empty data.
     */
    public void accept(byte[] frame) {
        if (frame == null || frame.length < 2) {
            return;
        }
        int channel = frame[0] & 0xFF;
        String payload = new String(frame, 1, frame.length - 1, StandardCharsets.UTF_8);
        if (channel == CHANNEL_STDOUT) {
            stdout.append(payload);
        } else if (channel == CHANNEL_STDERR) {
            stderr.append(payload);
        } else if (channel == CHANNEL_ERROR) {
            exitCode = exitCodeFromStatus(payload);
        }
    }

    public ExecResult result() {
        return new ExecResult(stdout.toString(), stderr.toString(), exitCode);
    }

    /**
     * @implNote A failure status that carries no {@code ExitCode} cause still must not read as
     * exit 0: that would report success for a command that failed, so 1 is used instead.
     */
    private static int exitCodeFromStatus(String statusJson) {
        JsonElement parsed = JsonParser.parseString(statusJson);
        if (!parsed.isJsonObject()) {
            return 1;
        }
        JsonObject status = parsed.getAsJsonObject();
        if ("Success".equals(stringAt(status, "status"))) {
            return 0;
        }
        JsonObject details = objectAt(status, "details");
        JsonArray causes = details != null ? arrayAt(details, "causes") : null;
        if (causes != null) {
            for (JsonElement element : causes) {
                if (!element.isJsonObject()) {
                    continue;
                }
                JsonObject cause = element.getAsJsonObject();
                if ("ExitCode".equals(stringAt(cause, "reason"))) {
                    Integer code = parseInt(stringAt(cause, "message"));
                    if (code != null) {
                        return code;
                    }
                }
            }
        }
        return 1;
    }

    private static Integer parseInt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return Integer.parseInt(value.trim());
        } catch (NumberFormatException e) {
            return null;
        }
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
}
