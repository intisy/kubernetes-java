package io.github.intisy.kubernetes;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Abstract base class for platform-specific Kubernetes providers.
 * Handles Minikube installation, cluster startup, and provides a client for interacting with Kubernetes.
 * <p>
 * Each provider instance is isolated and can run simultaneously with other instances.
 * Use {@link #getInstanceId()} to get the unique identifier for this instance.
 * <p>
 * The base directory for storing Kubernetes data can be configured using {@link #setBaseDirectory(Path)}
 * before creating any providers. By default, it uses {@code ~/.kubernetes-java/}.
 *
 * @author Finn Birich
 */
public abstract class KubernetesProvider {
    private static final Path DEFAULT_KUBERNETES_DIR = Paths.get(System.getProperty("user.home"), ".kubernetes-java");

    private static Path baseDirectory = DEFAULT_KUBERNETES_DIR;

    /**
     * Set the base directory for storing Kubernetes data and instances.
     * This must be called before creating any KubernetesProvider instances.
     * <p>
     * Example:
     * <pre>{@code
     * KubernetesProvider.setBaseDirectory(Paths.get("/custom/path/kubernetes-java"));
     * KubernetesProvider provider = KubernetesProvider.get();
     * }</pre>
     *
     * @param path The base directory path
     */
    public static void setBaseDirectory(Path path) {
        baseDirectory = path;
    }

    /**
     * Get the current base directory for storing Kubernetes data.
     *
     * @return The base directory path
     */
    public static Path getBaseDirectory() {
        return baseDirectory;
    }

    /**
     * Reset the base directory to the default ({@code ~/.kubernetes-java/}).
     */
    public static void resetBaseDirectory() {
        baseDirectory = DEFAULT_KUBERNETES_DIR;
    }

    /**
     * @deprecated Use {@link #getBaseDirectory()} instead
     */
    @Deprecated
    protected static final Path KUBERNETES_DIR = DEFAULT_KUBERNETES_DIR;

    /**
     * Get the appropriate KubernetesProvider for the current operating system.
     */
    public static KubernetesProvider get() {
        String os = System.getProperty("os.name").toLowerCase();
        if (os.contains("win")) {
            return new WindowsKubernetesProvider();
        } else if (os.contains("nix") || os.contains("nux") || os.contains("aix")) {
            return new LinuxKubernetesProvider();
        } else if (os.contains("mac")) {
            return new MacKubernetesProvider();
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + os);
        }
    }

    /**
     * Get the unique instance ID for this provider.
     * Each provider instance has a unique ID to allow multiple instances to run simultaneously.
     *
     * @return The unique instance identifier
     */
    public abstract String getInstanceId();

    /**
     * Start the Kubernetes cluster via Minikube.
     * This will always start a new managed Minikube profile.
     * The cluster will use isolated paths to avoid conflicts with other instances.
     */
    public abstract void start() throws IOException, InterruptedException;

    /**
     * Get a KubernetesClient for interacting with the Kubernetes API server.
     * The client can be used to manage pods, deployments, services, etc.
     */
    public abstract KubernetesClient getClient();

    /**
     * Stop the Kubernetes cluster if it was started by this provider.
     * This will also clean up the instance-specific profile.
     */
    public abstract void stop();

    /**
     * Ensure Minikube is installed.
     * Downloads and installs Minikube if necessary.
     */
    public abstract void ensureInstalled() throws IOException;
}
