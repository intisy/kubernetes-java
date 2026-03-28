package io.github.intisy.kubernetes.exception;

/**
 * Exception thrown when a requested resource is not found (HTTP 404).
 *
 * @author Finn Birich
 */
public class NotFoundException extends KubernetesException {

    public NotFoundException(String message) {
        super(message, 404);
    }

    public NotFoundException(String message, Throwable cause) {
        super(message, 404, cause);
    }
}
