package io.github.intisy.kubernetes.exec;

import java.io.IOException;

/**
 * Thrown when a pod exec session ended, by timeout, interruption, or an early socket close,
 * before a terminal status frame was ever parsed. The real exit code is unknowable at that
 * point; this exists so that case can never be mistaken for a genuine exit 0.
 */
public class ExecIncompleteException extends IOException {
    private final String partialStdout;
    private final String partialStderr;

    public ExecIncompleteException(String message, String partialStdout, String partialStderr) {
        super(message);
        this.partialStdout = partialStdout;
        this.partialStderr = partialStderr;
    }

    public String partialStdout() {
        return partialStdout;
    }

    public String partialStderr() {
        return partialStderr;
    }
}
