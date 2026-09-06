package io.github.intisy.kubernetes.unit;

import io.github.intisy.kubernetes.apply.ManifestApply;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ManifestApplyTest {
    @Test
    void buildsACoreGroupPathWithoutAGroupSegment() {
        assertEquals("/api/v1/namespaces/spisor-data/pods/probe-writer",
                ManifestApply.resourcePath("v1", "pods", "spisor-data", "probe-writer"));
    }

    @Test
    void buildsANamedGroupPath() {
        assertEquals("/apis/postgresql.cnpg.io/v1/namespaces/spisor-data/clusters/spisor-pg",
                ManifestApply.resourcePath("postgresql.cnpg.io/v1", "clusters", "spisor-data", "spisor-pg"));
    }

    @Test
    void buildsAClusterScopedPathWhenThereIsNoNamespace() {
        assertEquals("/api/v1/namespaces/seaweedfs",
                ManifestApply.resourcePath("v1", "namespaces", null, "seaweedfs"));
    }

    @Test
    void sendsAnApplyPatchWithAFieldManager() {
        assertEquals("application/apply-patch+yaml", ManifestApply.CONTENT_TYPE);
        assertEquals("kubernetes-java", ManifestApply.applyQuery().get("fieldManager"));
        assertEquals("true", ManifestApply.applyQuery().get("force"));
    }

    @Test
    void splitsAMultiDocumentManifest() {
        String yaml = "apiVersion: v1\nkind: Namespace\nmetadata:\n  name: a\n"
                + "---\n"
                + "apiVersion: v1\nkind: Namespace\nmetadata:\n  name: b\n";

        List<ManifestApply.Document> documents = ManifestApply.documents(yaml);

        assertEquals(2, documents.size());
        assertEquals("a", documents.get(0).name());
        assertEquals("b", documents.get(1).name());
        assertEquals("Namespace", documents.get(0).kind());
    }
}
