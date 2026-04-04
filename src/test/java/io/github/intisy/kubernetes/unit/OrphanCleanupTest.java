package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.KubernetesClient;
import io.github.intisy.kubernetes.KubernetesProvider;
import org.junit.jupiter.api.*;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.channels.FileLock;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Unit tests for orphan cleanup and instance lock management in KubernetesProvider.
 * No Docker/Minikube required — uses temp directories.
 *
 * @author Finn Birich
 */
public class OrphanCleanupTest {

    private Path originalBaseDir;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        originalBaseDir = KubernetesProvider.getBaseDirectory();
        tempDir = Files.createTempDirectory("k8s-orphan-test");
        KubernetesProvider.setBaseDirectory(tempDir);
    }

    @AfterEach
    void tearDown() {
        KubernetesProvider.setBaseDirectory(originalBaseDir);
        deleteRecursive(tempDir.toFile());
    }

    @Test
    @DisplayName("acquireInstanceLock writes profile name and holds lock")
    void testLockAcquisitionAndRelease() throws Exception {
        TestableProvider provider = new TestableProvider();
        Path instanceDir = tempDir.resolve("instances").resolve(provider.getInstanceId());

        boolean acquired = provider.doAcquireInstanceLock(instanceDir, "test-profile");
        assertTrue(acquired, "Lock should be acquired successfully");

        // Release the lock first — on Windows, file locks are mandatory
        provider.doReleaseInstanceLock();

        Path lockFile = instanceDir.resolve("instance.lock");
        assertTrue(Files.exists(lockFile), "Lock file should exist");

        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "r");
        String content = raf.readUTF();
        raf.close();
        assertEquals("test-profile", content);
    }

    @Test
    @DisplayName("cleanupOrphanProfiles detects orphan without lock file")
    void testOrphanDetectionWithoutLock() throws Exception {
        Path instancesDir = tempDir.resolve("instances");
        Path orphanDir = instancesDir.resolve("orphan-1");
        Files.createDirectories(orphanDir);

        TestableProvider provider = new TestableProvider();
        provider.doCleanupOrphanProfiles(tempDir.resolve("minikube"));

        assertFalse(Files.exists(orphanDir), "Orphan directory should be deleted");
    }

    @Test
    @DisplayName("cleanupOrphanProfiles detects orphan with released lock")
    void testOrphanDetectionWithReleasedLock() throws Exception {
        Path instancesDir = tempDir.resolve("instances");
        Path orphanDir = instancesDir.resolve("orphan-2");
        Files.createDirectories(orphanDir);

        Path lockFile = orphanDir.resolve("instance.lock");
        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
        raf.writeUTF("orphan-profile");
        raf.close();

        TestableProvider provider = new TestableProvider();
        provider.doCleanupOrphanProfiles(tempDir.resolve("minikube"));

        assertFalse(Files.exists(orphanDir), "Orphan directory with released lock should be deleted");
    }

    @Test
    @DisplayName("cleanupOrphanProfiles skips active instance with held lock")
    void testActiveInstanceNotCleaned() throws Exception {
        Path instancesDir = tempDir.resolve("instances");
        Path activeDir = instancesDir.resolve("active-1");
        Files.createDirectories(activeDir);

        Path lockFile = activeDir.resolve("instance.lock");
        RandomAccessFile raf = new RandomAccessFile(lockFile.toFile(), "rw");
        FileLock lock = raf.getChannel().lock();
        raf.writeUTF("active-profile");

        try {
            TestableProvider provider = new TestableProvider();
            provider.doCleanupOrphanProfiles(tempDir.resolve("minikube"));

            assertTrue(Files.exists(activeDir), "Active instance directory should NOT be deleted");
        } finally {
            lock.release();
            raf.close();
        }
    }

    @Test
    @DisplayName("cleanupOrphanProfiles handles multiple orphans")
    void testMultipleOrphans() throws Exception {
        Path instancesDir = tempDir.resolve("instances");
        Path orphan1 = instancesDir.resolve("multi-orphan-1");
        Path orphan2 = instancesDir.resolve("multi-orphan-2");
        Path orphan3 = instancesDir.resolve("multi-orphan-3");
        Files.createDirectories(orphan1);
        Files.createDirectories(orphan2);
        Files.createDirectories(orphan3);

        TestableProvider provider = new TestableProvider();
        provider.doCleanupOrphanProfiles(tempDir.resolve("minikube"));

        assertFalse(Files.exists(orphan1), "Orphan 1 should be deleted");
        assertFalse(Files.exists(orphan2), "Orphan 2 should be deleted");
        assertFalse(Files.exists(orphan3), "Orphan 3 should be deleted");
    }

    @Test
    @DisplayName("cleanupOrphanProfiles handles missing instances directory")
    void testNoInstancesDir() {
        TestableProvider provider = new TestableProvider();
        assertDoesNotThrow(() -> provider.doCleanupOrphanProfiles(tempDir.resolve("minikube")));
    }

    @Test
    @DisplayName("cleanupOrphanProfiles handles empty instances directory")
    void testEmptyInstancesDir() throws Exception {
        Files.createDirectories(tempDir.resolve("instances"));

        TestableProvider provider = new TestableProvider();
        assertDoesNotThrow(() -> provider.doCleanupOrphanProfiles(tempDir.resolve("minikube")));
    }

    @Test
    @DisplayName("cleanupOrphanProfiles handles corrupted lock file")
    void testCorruptedLockFile() throws Exception {
        Path instancesDir = tempDir.resolve("instances");
        Path corruptDir = instancesDir.resolve("corrupt-1");
        Files.createDirectories(corruptDir);

        Path lockFile = corruptDir.resolve("instance.lock");
        Files.write(lockFile, new byte[]{0x00, 0x01, 0x02});

        TestableProvider provider = new TestableProvider();
        provider.doCleanupOrphanProfiles(tempDir.resolve("minikube"));

        assertFalse(Files.exists(corruptDir), "Directory with corrupted lock file should be deleted");
    }

    private static void deleteRecursive(File file) {
        if (file == null || !file.exists()) return;
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursive(child);
                }
            }
        }
        file.delete();
    }

    /**
     * Testable subclass that exposes protected methods and stubs abstract methods.
     */
    static class TestableProvider extends KubernetesProvider {

        private final String instanceId = "test-" + System.nanoTime();

        @Override
        public String getInstanceId() {
            return instanceId;
        }

        @Override
        public void start() throws IOException, InterruptedException {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public KubernetesClient createClient() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public KubernetesClient getClient() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public void stop() {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        @Override
        public void ensureInstalled() throws IOException {
            throw new UnsupportedOperationException("Not used in unit tests");
        }

        public void doCleanupOrphanProfiles(Path minikubePath) {
            cleanupOrphanProfiles(minikubePath);
        }

        public boolean doAcquireInstanceLock(Path instanceDir, String profileName) {
            return acquireInstanceLock(instanceDir, profileName);
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
}
