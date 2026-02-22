package blue.language.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EmissionRegistryTest {

    @Test
    void scopeTreatsNullAndEmptyAsRoot() {
        EmissionRegistry registry = new EmissionRegistry();
        ScopeRuntimeContext root = registry.scope("/");
        assertNotNull(root);
        assertSame(root, registry.scope(null));
        assertSame(root, registry.scope(""));
    }

    @Test
    void scopeNormalizesNonPointerInputs() {
        EmissionRegistry registry = new EmissionRegistry();
        ScopeRuntimeContext normalized = registry.scope("root");
        assertSame(normalized, registry.scope("/root"));
        assertSame(normalized, registry.existingScope("root"));
        registry.clearScope("root");
        assertNull(registry.existingScope("/root"));
    }

    @Test
    void terminationLookupAndClearUseNormalizedScope() {
        EmissionRegistry registry = new EmissionRegistry();
        ScopeRuntimeContext root = registry.scope("/");
        root.finalizeTermination(ScopeRuntimeContext.TerminationKind.GRACEFUL, "done");

        assertTrue(registry.isScopeTerminated(null));
        assertTrue(registry.isScopeTerminated(""));

        registry.clearScope("");
        assertNull(registry.existingScope("/"));
        assertFalse(registry.isScopeTerminated("/"));
    }
}
