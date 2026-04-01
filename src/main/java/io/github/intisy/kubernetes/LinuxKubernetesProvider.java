package io.github.intisy.kubernetes;

import io.github.intisy.docker.DockerProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static io.github.intisy.kubernetes.IOUtils.readAllBytes;

/**
 * Linux-specific Kubernetes provider using Minikube.
 * Self-contained — does NOT require Docker Desktop.
 * <p>
 * Uses docker-java's {@link DockerProvider} to bootstrap a self-contained
 * Docker Engine (rootless when not root, direct when root), then runs
 * Minikube with the docker driver via DOCKER_HOST.
 * <p>
 * Supports multiple simultaneous instances via Minikube profiles.
 * <p>
 * Cleanup guarantees:
 * <ul>
 *   <li>JVM shutdown hook calls {@link #stop()} on all active providers (even on Ctrl+C).</li>
 *   <li>File-lock based orphan detection cleans up profiles from crashed JVMs on next start.</li>
 * </ul>
 *
 * @author Finn Birich
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
public class LinuxKubernetesProvider extends KubernetesProvider {
    private static final Logger log = LoggerFactory.getLogger(LinuxKubernetesProvider.class);

    private static final String MINIKUBE_DOWNLOAD_URL = "https://storage.googleapis.com/minikube/releases/latest/minikube-linux-%s";
    private static final String KUBECTL_DOWNLOAD_URL = "https://dl.k8s.io/release/stable.txt";
    private static final String KUBECTL_BINARY_URL = "https://dl.k8s.io/release/%s/bin/linux/%s/kubectl";

    private final String instanceId;
    private final String profileName;
    private final Path instanceDir;
    private Path minikubePath;
    private Path kubectlPath;

    private KubernetesClient kubernetesClient;
    private boolean clusterStartedByUs = false;
    private DockerProvider dockerProvider;
    private String dockerHost;

    public LinuxKubernetesProvider() {
        this.instanceId = UUID.randomUUID().toString().substring(0, 8);
        this.profileName = "kubernetes-java-" + instanceId;
        Path baseDir = getBaseDirectory();
        this.instanceDir = baseDir.resolve("instances").resolve(instanceId);
        this.minikubePath = baseDir.resolve("bin").resolve("minikube");
        this.kubectlPath = baseDir.resolve("bin").resolve("kubectl");
        log.debug("Created LinuxKubernetesProvider with instance ID: {}", instanceId);
    }

    @Override
    public String getInstanceId() {
        return instanceId;
    }

    @Override
    public void ensureInstalled() throws IOException {
        Path binDir = getBaseDirectory().resolve("bin");
        Files.createDirectories(binDir);

        if (!Files.exists(minikubePath) || !Files.isExecutable(minikubePath)) {
            log.info("Minikube not found. Downloading...");
            String arch = getArch();
            String url = String.format(MINIKUBE_DOWNLOAD_URL, arch);
            downloadFile(url, minikubePath);
            minikubePath.toFile().setExecutable(true);
            log.info("Minikube installed successfully");
        } else {
            log.info("Minikube is already installed");
        }

        if (!Files.exists(kubectlPath) || !Files.isExecutable(kubectlPath)) {
            log.info("kubectl not found. Downloading...");
            String arch = getArch();
            String stableVersion = getKubectlStableVersion();
            String url = String.format(KUBECTL_BINARY_URL, stableVersion, arch);
            downloadFile(url, kubectlPath);
            kubectlPath.toFile().setExecutable(true);
            log.info("kubectl installed successfully");
        } else {
            log.info("kubectl is already installed");
        }
    }

    private String getArch() {
        String osArch = System.getProperty("os.arch");
        switch (osArch) {
            case "amd64":
            case "x86_64":
                return "amd64";
            case "aarch64":
            case "arm64":
                return "arm64";
            default:
                throw new UnsupportedOperationException("Unsupported architecture: " + osArch);
        }
    }

    private String getKubectlStableVersion() throws IOException {
        @SuppressWarnings("deprecation")
        URL url = new URL(KUBECTL_DOWNLOAD_URL);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");

        try (InputStream is = connection.getInputStream()) {
            return new String(readAllBytes(is)).trim();
        }
    }

    @Override
    public void start() throws IOException, InterruptedException {
        log.info("Starting Kubernetes cluster via Minikube (instance: {})...", instanceId);

        ensureInstalled();
        Files.createDirectories(instanceDir);

        cleanupOrphanProfiles(minikubePath);
        acquireInstanceLock(instanceDir, profileName);

        String status = runMinikubeCommand("status", "--profile", profileName, "--format", "{{.Host}}");
        if ("Running".equals(status.trim())) {
            log.info("Minikube profile {} is already running", profileName);
            clusterStartedByUs = false;
            registerInstance();
            return;
        }

        log.info("Using docker-java to bootstrap Docker Engine...");
        dockerProvider = DockerProvider.get();
        dockerProvider.start();
        dockerHost = dockerProvider.getDockerHost();
        log.info("Docker Engine bootstrapped at: {}", dockerHost);

        log.info("Starting Minikube with profile: {} (driver: docker, DOCKER_HOST: {})",
                profileName, dockerHost);

        ProcessBuilder pb = new ProcessBuilder(
                minikubePath.toString(),
                "start",
                "--profile", profileName,
                "--driver", "docker",
                "--cpus", "2",
                "--memory", "2048",
                "--wait", "apiserver",
                "--interactive=false"
        );
        pb.environment().put("MINIKUBE_HOME", getBaseDirectory().toString());
        pb.environment().put("DOCKER_HOST", dockerHost);
        pb.environment().put("LANG", "en_US.UTF-8");
        pb.environment().put("LC_ALL", "en_US.UTF-8");
        pb.redirectErrorStream(true);
        Process process = pb.start();

        final Process minikubeProcess = process;
        Thread outputDrainer = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(minikubeProcess.getInputStream()));
                    String line;
                    while ((line = reader.readLine()) != null) {
                        log.info("[minikube] {}", line);
                    }
                } catch (IOException e) {
                    // Process ended or stream closed
                }
            }
        });
        outputDrainer.setDaemon(true);
        outputDrainer.start();

        boolean completed = process.waitFor(20, TimeUnit.MINUTES);
        if (!completed) {
            process.destroyForcibly();
            throw new RuntimeException("Minikube start timed out after 20 minutes");
        }

        if (process.exitValue() != 0) {
            throw new RuntimeException("Minikube failed to start. Exit code: " + process.exitValue());
        }

        clusterStartedByUs = true;

        if (!waitForCluster()) {
            throw new RuntimeException("Kubernetes cluster failed to become ready");
        }

        registerInstance();

        log.info("Kubernetes cluster started (instance: {}, profile: {})", instanceId, profileName);
    }

    @Override
    public KubernetesClient createClient() {
        try {
            String kubectlConfig = runMinikubeCommand("kubectl", "--profile", profileName,
                    "--", "config", "view", "--minify", "-o",
                    "jsonpath={.clusters[0].cluster.server}");
            String apiServerUrl = "https://localhost:8443";
            if (kubectlConfig != null && !kubectlConfig.trim().isEmpty()
                    && kubectlConfig.contains("http")) {
                apiServerUrl = kubectlConfig.trim();
            }

            Path minikubeHome = getBaseDirectory();
            Path minikubeProfileDir = minikubeHome.resolve("profiles").resolve(profileName);
            Path caCertPath = minikubeHome.resolve("certs").resolve("ca.pem");
            if (!Files.exists(caCertPath)) {
                caCertPath = minikubeHome.resolve("ca.crt");
            }
            Path clientCertPath = minikubeProfileDir.resolve("client.crt");
            Path clientKeyPath = minikubeProfileDir.resolve("client.key");

            if (Files.exists(clientCertPath) && Files.exists(clientKeyPath)
                    && Files.exists(caCertPath)) {
                return KubernetesClient.builder()
                        .withApiServer(apiServerUrl)
                        .withCaCert(caCertPath.toString())
                        .withClientCert(clientCertPath.toString())
                        .withClientKey(clientKeyPath.toString())
                        .build();
            } else {
                return KubernetesClient.builder()
                        .withApiServer(apiServerUrl)
                        .build();
            }
        } catch (Exception e) {
            log.warn("Failed to get API server details, using default: {}", e.getMessage());
            return KubernetesClient.builder()
                    .withApiServer("https://localhost:8443")
                    .build();
        }
    }

    @Override
    public KubernetesClient getClient() {
        if (this.kubernetesClient == null) {
            this.kubernetesClient = createClient();
        }
        return this.kubernetesClient;
    }

    @Override
    public void stop() {
        unregisterInstance();

        log.info("Stopping Kubernetes cluster (instance: {})...", instanceId);

        if (kubernetesClient != null) {
            try {
                kubernetesClient.close();
            } catch (Exception e) {
                log.debug("Error closing Kubernetes client: {}", e.getMessage());
            }
            kubernetesClient = null;
        }

        if (clusterStartedByUs) {
            try {
                log.info("Deleting Minikube profile {}...", profileName);
                ProcessBuilder pb = new ProcessBuilder(
                        minikubePath.toString(), "delete", "--profile", profileName
                );
                pb.environment().put("MINIKUBE_HOME", getBaseDirectory().toString());
                pb.environment().put("LANG", "en_US.UTF-8");
                pb.environment().put("LC_ALL", "en_US.UTF-8");
                if (dockerHost != null) {
                    pb.environment().put("DOCKER_HOST", dockerHost);
                }
                pb.redirectErrorStream(true);
                Process process = pb.start();

                final Process deleteProcess = process;
                Thread drainer = new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            BufferedReader reader = new BufferedReader(new InputStreamReader(deleteProcess.getInputStream()));
                            String line;
                            while ((line = reader.readLine()) != null) {
                                log.debug("[minikube delete] {}", line);
                            }
                        } catch (IOException e) {
                            // Process ended
                        }
                    }
                });
                drainer.setDaemon(true);
                drainer.start();

                boolean completed = process.waitFor(60, TimeUnit.SECONDS);
                if (!completed) {
                    process.destroyForcibly();
                    log.warn("Timed out deleting Minikube profile: {}", profileName);
                }
            } catch (IOException | InterruptedException e) {
                log.warn("Failed to delete Minikube profile: {}", e.getMessage());
            }
        }

        if (dockerProvider != null) {
            dockerProvider.stop();
            dockerProvider = null;
        }

        releaseInstanceLock();
        cleanupInstanceDirectory();
        log.info("Kubernetes cluster stopped (instance: {})", instanceId);
    }

    private String runMinikubeCommand(String... args) {
        try {
            String[] fullArgs = new String[args.length + 1];
            fullArgs[0] = minikubePath.toString();
            System.arraycopy(args, 0, fullArgs, 1, args.length);

            ProcessBuilder pb = new ProcessBuilder(fullArgs);
            pb.environment().put("MINIKUBE_HOME", getBaseDirectory().toString());
            pb.environment().put("LANG", "en_US.UTF-8");
            pb.environment().put("LC_ALL", "en_US.UTF-8");
            if (dockerHost != null) {
                pb.environment().put("DOCKER_HOST", dockerHost);
            }
            pb.redirectErrorStream(true);
            Process process = pb.start();
            byte[] output = readAllBytes(process.getInputStream());
            process.waitFor(30, TimeUnit.SECONDS);
            return new String(output).trim();
        } catch (IOException | InterruptedException e) {
            log.debug("Minikube command failed: {}", e.getMessage());
            return "";
        }
    }

    @SuppressWarnings("deprecation")
    private void downloadFile(String urlString, Path destinationPath) throws IOException {
        log.debug("Downloading {} to {}", urlString, destinationPath);
        URL url = new URL(urlString);
        HttpURLConnection connection = (HttpURLConnection) url.openConnection();
        connection.setInstanceFollowRedirects(true);
        connection.setRequestMethod("GET");
        connection.connect();

        int responseCode = connection.getResponseCode();
        if (responseCode >= 400) {
            throw new IOException("Failed to download file: " + responseCode);
        }

        try (InputStream in = connection.getInputStream()) {
            Files.copy(in, destinationPath, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    @SuppressWarnings("BusyWait")
    private boolean waitForCluster() throws InterruptedException {
        log.debug("Waiting for Kubernetes cluster to be ready...");
        long timeoutMillis = TimeUnit.SECONDS.toMillis(120);
        long startTime = System.currentTimeMillis();

        while (System.currentTimeMillis() - startTime < timeoutMillis) {
            String clusterStatus = runMinikubeCommand("status", "--profile", profileName,
                    "--format", "{{.Host}}");
            if ("Running".equals(clusterStatus.trim())) {
                String kubeletStatus = runMinikubeCommand("status", "--profile", profileName,
                        "--format", "{{.Kubelet}}");
                if ("Running".equals(kubeletStatus.trim())) {
                    log.debug("Kubernetes cluster is ready");
                    return true;
                }
            }
            Thread.sleep(2000);
        }

        log.error("Timed out waiting for Kubernetes cluster to be ready");
        return false;
    }

    private void cleanupInstanceDirectory() {
        if (!Files.exists(instanceDir)) {
            return;
        }

        try {
            ProcessBuilder pb = new ProcessBuilder("rm", "-rf", instanceDir.toString());
            pb.redirectErrorStream(true);
            Process process = pb.start();
            boolean completed = process.waitFor(30, TimeUnit.SECONDS);
            if (!completed) {
                process.destroyForcibly();
                log.warn("Cleanup timed out for instance directory: {}", instanceDir);
            }
        } catch (IOException | InterruptedException e) {
            log.warn("Failed to clean up instance directory: {}", e.getMessage());
            try {
                if (Files.exists(instanceDir)) {
                    try (java.util.stream.Stream<Path> walk = Files.walk(instanceDir)) {
                        walk.sorted(java.util.Comparator.reverseOrder())
                                .forEach(path -> {
                                    try {
                                        Files.deleteIfExists(path);
                                    } catch (IOException ignored) {
                                    }
                                });
                    }
                }
            } catch (IOException ignored) {
            }
        }
    }
}
