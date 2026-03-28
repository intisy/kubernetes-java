package io.github.intisy.kubernetes.exception;

/**
 * Exception thrown when a resource already exists (HTTP 409).
 *
 * @author Finn Birich
 */
public class ConflictException extends KubernetesException {

    public ConflictException(String message) {
        super(message, 409);
    }

    public ConflictException(String message, Throwable cause) {
        super(message, 409, cause);
    }
}
