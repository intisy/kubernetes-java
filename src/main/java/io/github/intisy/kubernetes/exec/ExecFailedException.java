package io.github.intisy.kubernetes.exec;

import io.github.intisy.kubernetes.exception.KubernetesException;

/**
 * Thrown by {@link io.github.intisy.kubernetes.KubernetesClient#exec} when the executed command
 * exits non-zero.
 *
 * @implNote Extends {@link KubernetesException} for consistency with the rest of this library,
 * but deliberately never populates {@code statusCode} with the process exit code: every existing
 * caller of {@code getStatusCode()} expects an HTTP status, so a process exit code there could
 * easily be mistaken for one. The exit code is only ever exposed through {@link #exitCode()}.
 */
public class ExecFailedException extends KubernetesException {
    private final int exitCode;

    public ExecFailedException(String message, int exitCode) {
        super(message, 0);
        this.exitCode = exitCode;
    }

    public int exitCode() {
        return exitCode;
    }
}
