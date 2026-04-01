package io.github.intisy.kubernetes;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.channels.OverlappingFileLockException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Abstract base class for platform-specific Kubernetes providers.
 * Handles Minikube installation, cluster startup, and provides a client for interacting with Kubernetes.
 * <p>
 * Each provider instance is isolated and can run simultaneously with other instances.
 * Use {@link #getInstanceId()} to get the unique identifier for this instance.
 * <p>
 * The base directory for storing Kubernetes data can be configured using {@link #setBaseDirectory(Path)}
 * before creating any providers. By default, it uses {@code ~/.kubernetes-java/}.
 * <p>
 * Providers automatically register a JVM shutdown hook to clean up running clusters on exit.
 * On startup, orphaned Minikube profiles from crashed JVMs are detected and removed.
 *
 * @author Finn Birich
 */
public abstract class KubernetesProvider {

    private static final Logger log = LoggerFactory.getLogger(KubernetesProvider.class);
    private static final Path DEFAULT_KUBERNETES_DIR = Paths.get(System.getProperty("user.home"), ".kubernetes-java");

    private static Path baseDirectory = DEFAULT_KUBERNETES_DIR;

    /**
     * Tracks all active (started) provider instances in this JVM.
     * Used by the shutdown hook to clean up all providers on exit.
     */
    private static final Set<KubernetesProvider> activeProviders =
            Collections.synchronizedSet(new LinkedHashSet<KubernetesProvider>());

    private static volatile boolean shutdownHookRegistered = false;

    /**
     * Instance lock file handle — kept open to hold the file lock for orphan detection.
     * When the JVM exits (even on crash/kill), the OS releases the lock automatically.
     */
    private RandomAccessFile lockFileRaf;
    private FileLock instanceLock;

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
     * Create a new KubernetesClient for interacting with the Kubernetes API server.
     * Each call creates a fresh, independent client instance with its own HTTP connection.
     *
     * @return A new KubernetesClient instance
     */
    public abstract KubernetesClient createClient();

    /**
     * Get a cached KubernetesClient for interacting with the Kubernetes API server.
     * Returns the same client instance on subsequent calls.
     * Use {@link #createClient()} if you need independent client instances.
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

    // =========================================================================
    // Shutdown hooks and orphan detection
    // =========================================================================

    /**
     * Register this provider as active and ensure the JVM shutdown hook is installed.
     * <p>
     * The shutdown hook guarantees that {@link #stop()} is called on all active providers
     * when the JVM exits — whether via normal exit, {@code System.exit()}, or Ctrl+C (SIGTERM).
     * <p>
     * Call this at the end of {@code start()} in each provider, after the cluster is confirmed ready.
     */
    protected void registerInstance() {
        synchronized (KubernetesProvider.class) {
            if (!shutdownHookRegistered) {
                Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
                    @Override
                    public void run() {
                        int count = activeProviders.size();
                        if (count > 0) {
                            log.info("JVM shutdown detected, cleaning up {} active Kubernetes provider(s)...", count);
                            List<KubernetesProvider> toStop = new ArrayList<KubernetesProvider>(activeProviders);
                            for (KubernetesProvider provider : toStop) {
                                try {
                                    provider.stop();
                                } catch (Exception e) {
                                    log.warn("Failed to stop provider {} during shutdown: {}",
                                            provider.getInstanceId(), e.getMessage());
                                }
                            }
                        }
                    }
                }, "kubernetes-java-shutdown-hook"));
                shutdownHookRegistered = true;
                log.debug("Registered JVM shutdown hook for Kubernetes provider cleanup");
            }
        }
        activeProviders.add(this);
        log.debug("Registered provider instance: {}", getInstanceId());
    }

    /**
     * Unregister this provider from active tracking.
     * Call this at the beginning of {@code stop()} in each provider.
     */
    protected void unregisterInstance() {
        activeProviders.remove(this);
        log.debug("Unregistered provider instance: {}", getInstanceId());
    }

    /**
     * Acquire an instance lock file to enable orphan detection.
     * <p>
     * The lock is held for the lifetime of this provider instance. When the JVM exits
     * (even on crash or kill -9), the OS automatically releases the file lock.
     * On next startup, {@link #cleanupOrphanProfiles(Path)} detects released locks
     * and cleans up the orphaned Minikube profiles.
     *
     * @param instanceDir The instance-specific directory
     * @param profileName The Minikube profile name to write into the lock file
     * @return true if the lock was acquired, false otherwise
     */
    protected boolean acquireInstanceLock(Path instanceDir, String profileName) {
        try {
            Files.createDirectories(instanceDir);
            Path lockFile = instanceDir.resolve("instance.lock");
            lockFileRaf = new RandomAccessFile(lockFile.toFile(), "rw");
            instanceLock = lockFileRaf.getChannel().tryLock();
            if (instanceLock != null) {
                lockFileRaf.setLength(0);
                lockFileRaf.writeUTF(profileName);
                log.debug("Acquired instance lock for profile: {}", profileName);
                return true;
            }
            log.warn("Could not acquire instance lock for profile: {}", profileName);
            return false;
        } catch (IOException | OverlappingFileLockException e) {
            log.warn("Failed to acquire instance lock: {}", e.getMessage());
            return false;
        }
    }

    /**
     * Release the instance lock file.
     * Call this in {@code stop()} before cleaning up the instance directory.
     */
    protected void releaseInstanceLock() {
        try {
            if (instanceLock != null) {
                instanceLock.release();
                instanceLock = null;
            }
        } catch (IOException e) {
            log.debug("Error releasing instance lock: {}", e.getMessage());
        }
        try {
            if (lockFileRaf != null) {
                lockFileRaf.close();
                lockFileRaf = null;
            }
        } catch (IOException e) {
            log.debug("Error closing lock file: {}", e.getMessage());
        }
    }

    /**
     * Scan for orphaned Kubernetes-Java Minikube profiles and clean them up.
     * <p>
     * An orphan is a profile created by a previous JVM that has since exited (normally or crashed).
     * Detection works via file locks: each running provider holds a lock on its instance directory.
     * If the lock can be acquired by us, the original owner is dead.
     * <p>
     * Call this at the beginning of {@code start()} in each provider, after {@code ensureInstalled()}.
     *
     * @param minikubePath Path to the minikube binary (needed to run {@code minikube delete})
     */
    protected void cleanupOrphanProfiles(Path minikubePath) {
        synchronized (KubernetesProvider.class) {
            Path instancesDir = baseDirectory.resolve("instances");
            if (!Files.exists(instancesDir)) {
                return;
            }

            File[] dirs = instancesDir.toFile().listFiles();
            if (dirs == null) {
                return;
            }

            for (File dir : dirs) {
                if (!dir.isDirectory()) {
                    continue;
                }

                String dirName = dir.getName();
                String profileName = "kubernetes-java-" + dirName;
                File lockFile = new File(dir, "instance.lock");

                if (!lockFile.exists()) {
                    // No lock file — stale directory from before lock support
                    log.info("Found orphaned instance directory without lock: {}", dirName);
                    deleteOrphanProfile(profileName, minikubePath);
                    deleteDirectory(dir);
                    continue;
                }

                // Try to acquire the lock to check if the owning process is still alive
                RandomAccessFile raf = null;
                FileLock lock = null;
                try {
                    raf = new RandomAccessFile(lockFile, "rw");
                    lock = raf.getChannel().tryLock();
                    if (lock != null) {
                        String resolvedProfile = profileName;
                        try {
                            raf.seek(0);
                            resolvedProfile = raf.readUTF();
                        } catch (Exception e) {
                            // Corrupted lock file, use derived profile name
                        }

                        log.info("Found orphaned Kubernetes instance: {} (profile: {})", dirName, resolvedProfile);

                        lock.release();
                        lock = null;
                        raf.close();
                        raf = null;

                        deleteOrphanProfile(resolvedProfile, minikubePath);
                        deleteDirectory(dir);
                    } else {
                        log.debug("Instance {} is still active, skipping", dirName);
                    }
                } catch (OverlappingFileLockException e) {
                    log.debug("Instance {} is held by another thread in this JVM, skipping", dirName);
                } catch (IOException e) {
                    log.debug("Could not check lock for instance {}: {}", dirName, e.getMessage());
                } finally {
                    if (lock != null) {
                        try {
                            lock.release();
                        } catch (IOException e) {
                            // best effort
                        }
                    }
                    if (raf != null) {
                        try {
                            raf.close();
                        } catch (IOException e) {
                            // best effort
                        }
                    }
                }
            }
        }
    }

    /**
     * Delete an orphaned Minikube profile via {@code minikube delete --profile}.
     */
    private void deleteOrphanProfile(String profileName, Path minikubePath) {
        log.info("Deleting orphaned Minikube profile: {}", profileName);
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    minikubePath.toString(), "delete", "--profile", profileName
            );
            pb.environment().put("MINIKUBE_HOME", baseDirectory.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();

            final Process p = process;
            Thread drainer = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        IOUtils.readAllBytes(p.getInputStream());
                    } catch (IOException e) {
                        // ignore
                    }
                }
            });
            drainer.setDaemon(true);
            drainer.start();

            boolean completed = process.waitFor(60, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.warn("Timed out deleting orphaned profile: {}", profileName);
            } else if (process.exitValue() == 0) {
                log.info("Successfully deleted orphaned profile: {}", profileName);
            } else {
                log.warn("Failed to delete orphaned profile {} (exit code: {})", profileName, process.exitValue());
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Error deleting orphaned profile {}: {}", profileName, e.getMessage());
        }
    }

    /**
     * Recursively delete a directory and all its contents.
     */
    private static void deleteDirectory(File dir) {
        if (!dir.exists()) {
            return;
        }
        try {
            java.util.stream.Stream<Path> walk = Files.walk(dir.toPath());
            try {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // best effort — file may be locked on Windows
                            }
                        });
            } finally {
                walk.close();
            }
        } catch (IOException e) {
            // best effort
        }
    }
}
