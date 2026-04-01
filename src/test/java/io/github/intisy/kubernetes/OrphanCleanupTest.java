package io.github.intisy.kubernetes;

import org.junit.jupiter.api.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Comparator;

/**
 * Tests for the file-lock based orphan detection and cleanup mechanism.
 * <p>
 * Verifies that:
 * <ul>
 *   <li>Instance directories without lock files are detected as orphans</li>
 *   <li>Instance directories with released locks (crashed JVM) are detected as orphans</li>
 *   <li>Instance directories with held locks (active JVM) are NOT cleaned up</li>
 *   <li>Orphan directories are deleted after cleanup</li>
 *   <li>Lock acquisition and release works correctly</li>
 *   <li>Profile name is persisted in the lock file and recoverable</li>
 * </ul>
 * <p>
 * Does NOT require Docker or Minikube — uses a temp directory and dummy minikube path.
 *
 * @author Finn Birich
 */
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
public class OrphanCleanupTest {

    private static final Logger log = LoggerFactory.getLogger(OrphanCleanupTest.class);

    private Path tempBaseDir;
    private Path originalBaseDir;

    /**
     * Minimal concrete provider subclass that exposes protected methods for testing.
     */
    private static class TestableProvider extends KubernetesProvider {
        private final String id;

        TestableProvider(String id) {
            this.id = id;
        }

        @Override
        public String getInstanceId() {
            return id;
        }

        @Override
        public void start() {
            // no-op for testing
        }

        @Override
        public KubernetesClient createClient() {
            return null;
        }

        @Override
        public KubernetesClient getClient() {
            return null;
        }

        @Override
        public void stop() {
            unregisterInstance();
            releaseInstanceLock();
        }

        @Override
        public void ensureInstalled() {
            // no-op for testing
        }

        // Expose protected methods for testing

        public void doCleanupOrphanProfiles(Path minikubePath) {
            cleanupOrphanProfiles(minikubePath);
        }

        public boolean doAcquireInstanceLock(Path dir, String profile) {
            return acquireInstanceLock(dir, profile);
        }

        public void doReleaseInstanceLock() {
            releaseInstanceLock();
        }

        public void doRegisterInstance() {
            registerInstance();
        }

        public void doUnregisterInstance() {
            unregisterInstance();
        }
    }

    @BeforeEach
    void setUp() throws Exception {
        tempBaseDir = Files.createTempDirectory("k8s-orphan-test");
        originalBaseDir = KubernetesProvider.getBaseDirectory();
        KubernetesProvider.setBaseDirectory(tempBaseDir);
    }

    @AfterEach
    void tearDown() throws Exception {
        KubernetesProvider.setBaseDirectory(originalBaseDir);
        if (tempBaseDir != null && Files.exists(tempBaseDir)) {
            try (java.util.stream.Stream<Path> walk = Files.walk(tempBaseDir)) {
                walk.sorted(Comparator.reverseOrder())
                        .forEach(path -> {
                            try {
                                Files.deleteIfExists(path);
                            } catch (IOException e) {
                                // best effort
                            }
                        });
            }
        }
    }

    @Test
    @Order(1)
    void testLockAcquisitionAndRelease() throws Exception {
        log.info("Testing lock acquisition and release...");

        Path instanceDir = tempBaseDir.resolve("instances").resolve("test-lock");
        TestableProvider provider = new TestableProvider("test-lock");

        boolean acquired = provider.doAcquireInstanceLock(instanceDir, "kubernetes-java-test-lock");
        Assertions.assertTrue(acquired, "Should acquire lock on fresh directory");

        // Verify lock file was created
        Path lockFile = instanceDir.resolve("instance.lock");
        Assertions.assertTrue(Files.exists(lockFile), "Lock file should exist");

        // Release the lock first — on Windows, mandatory file locks prevent
        // opening a second handle while the lock is held
        provider.doReleaseInstanceLock();

        // Verify profile name was persisted in the lock file
        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "r");
        String profileName = raf.readUTF();
        raf.close();
        Assertions.assertEquals("kubernetes-java-test-lock", profileName,
                "Profile name should be persisted in lock file");
        log.info("Lock acquisition and release verified");
    }

    @Test
    @Order(2)
    void testOrphanDetectionWithoutLockFile() throws Exception {
        log.info("Testing orphan detection for directories without lock files...");

        Path orphanDir = tempBaseDir.resolve("instances").resolve("no-lock-orphan");
        Files.createDirectories(orphanDir);

        Files.write(orphanDir.resolve("marker.txt"), "orphan".getBytes());
        Assertions.assertTrue(Files.exists(orphanDir), "Orphan directory should exist before cleanup");

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner");
        provider.doCleanupOrphanProfiles(dummyMinikube);

        Assertions.assertFalse(Files.exists(orphanDir),
                "Orphan directory without lock file should be cleaned up");
        log.info("Orphan without lock file detected and cleaned up");
    }

    @Test
    @Order(3)
    void testOrphanDetectionWithReleasedLock() throws Exception {
        log.info("Testing orphan detection for directories with released locks (crashed JVM)...");

        // Simulate crashed JVM: create lock file, write profile, close (releasing lock)
        Path orphanDir = tempBaseDir.resolve("instances").resolve("crashed-jvm");
        Files.createDirectories(orphanDir);
        Path lockFile = orphanDir.resolve("instance.lock");

        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
        raf.writeUTF("kubernetes-java-crashed-jvm");
        raf.close();

        Assertions.assertTrue(Files.exists(orphanDir), "Orphan directory should exist before cleanup");

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner2");
        provider.doCleanupOrphanProfiles(dummyMinikube);

        Assertions.assertFalse(Files.exists(orphanDir),
                "Orphan directory with released lock should be cleaned up");
        log.info("Orphan with released lock detected and cleaned up");
    }

    @Test
    @Order(4)
    void testActiveInstanceNotCleaned() throws Exception {
        log.info("Testing that active instances (held locks) are NOT cleaned up...");

        Path activeDir = tempBaseDir.resolve("instances").resolve("active-inst");
        TestableProvider activeProvider = new TestableProvider("active-inst");
        boolean acquired = activeProvider.doAcquireInstanceLock(activeDir, "kubernetes-java-active-inst");
        Assertions.assertTrue(acquired, "Should acquire lock for active instance");

        Path orphanDir = tempBaseDir.resolve("instances").resolve("should-be-cleaned");
        Files.createDirectories(orphanDir);

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider cleanerProvider = new TestableProvider("cleaner3");
        cleanerProvider.doCleanupOrphanProfiles(dummyMinikube);

        Assertions.assertTrue(Files.exists(activeDir),
                "Active instance directory should NOT be cleaned up");

        Assertions.assertFalse(Files.exists(orphanDir),
                "Orphan without lock should still be cleaned even when active instances exist");

        activeProvider.doReleaseInstanceLock();
        log.info("Active instance correctly preserved during orphan cleanup");
    }

    @Test
    @Order(5)
    void testMultipleOrphansCleaned() throws Exception {
        log.info("Testing cleanup of multiple orphan directories...");

        for (int i = 1; i <= 3; i++) {
            Path orphanDir = tempBaseDir.resolve("instances").resolve("orphan-" + i);
            Files.createDirectories(orphanDir);

            RandomAccessFile raf = new RandomAccessFile(
                    orphanDir.resolve("instance.lock").toFile(), "rw");
            raf.writeUTF("kubernetes-java-orphan-" + i);
            raf.close();
        }

        for (int i = 1; i <= 3; i++) {
            Assertions.assertTrue(
                    Files.exists(tempBaseDir.resolve("instances").resolve("orphan-" + i)),
                    "Orphan-" + i + " should exist before cleanup");
        }

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner4");
        provider.doCleanupOrphanProfiles(dummyMinikube);

        for (int i = 1; i <= 3; i++) {
            Assertions.assertFalse(
                    Files.exists(tempBaseDir.resolve("instances").resolve("orphan-" + i)),
                    "Orphan-" + i + " should be cleaned up");
        }
        log.info("All 3 orphan directories cleaned up successfully");
    }

    @Test
    @Order(6)
    void testCleanupWithNoInstancesDirectory() {
        log.info("Testing cleanup when instances directory does not exist...");

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner5");
        Assertions.assertDoesNotThrow(() -> provider.doCleanupOrphanProfiles(dummyMinikube),
                "Cleanup should handle missing instances directory gracefully");
        log.info("Cleanup handled missing instances directory gracefully");
    }

    @Test
    @Order(7)
    void testCleanupWithEmptyInstancesDirectory() throws Exception {
        log.info("Testing cleanup when instances directory is empty...");

        Files.createDirectories(tempBaseDir.resolve("instances"));

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner6");
        Assertions.assertDoesNotThrow(() -> provider.doCleanupOrphanProfiles(dummyMinikube),
                "Cleanup should handle empty instances directory gracefully");
        log.info("Cleanup handled empty instances directory gracefully");
    }

    @Test
    @Order(8)
    void testProfileNameRecoveredFromCorruptLockFile() throws Exception {
        log.info("Testing cleanup with corrupted lock file (falls back to derived profile name)...");

        Path orphanDir = tempBaseDir.resolve("instances").resolve("corrupt-lock");
        Files.createDirectories(orphanDir);

        Files.write(orphanDir.resolve("instance.lock"), new byte[]{0x00, 0x01, 0x02});

        Path dummyMinikube = tempBaseDir.resolve("bin").resolve("minikube-nonexistent");
        TestableProvider provider = new TestableProvider("cleaner7");

        Assertions.assertDoesNotThrow(() -> provider.doCleanupOrphanProfiles(dummyMinikube),
                "Cleanup should handle corrupted lock file gracefully");

        Assertions.assertFalse(Files.exists(orphanDir),
                "Orphan with corrupted lock file should be cleaned up");
        log.info("Corrupted lock file handled gracefully, orphan cleaned up");
    }
}
