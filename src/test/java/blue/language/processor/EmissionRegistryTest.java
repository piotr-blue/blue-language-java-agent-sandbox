package blue.language.processor;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
    void scopeRejectsNonPointerInputs() {
        EmissionRegistry registry = new EmissionRegistry();
        assertThrows(IllegalArgumentException.class, () -> registry.scope("root"));
        assertThrows(IllegalArgumentException.class, () -> registry.existingScope("root"));
        assertThrows(IllegalArgumentException.class, () -> registry.clearScope("root"));
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
