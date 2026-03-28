package io.github.intisy.kubernetes.exception;

/**
 * Base exception for Kubernetes API errors.
 *
 * @author Finn Birich
 */
public class KubernetesException extends RuntimeException {
    private final int statusCode;

    public KubernetesException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public KubernetesException(String message, Throwable cause) {
        super(message, cause);
        this.statusCode = 0;
    }

    public KubernetesException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
