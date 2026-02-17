package blue.language.processor;

import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocumentProcessingRuntimeScopeTest {

    @Test
    void scopeTreatsNullAndEmptyAsRoot() {
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(new Node());

        ScopeRuntimeContext root = runtime.scope("/");
        assertSame(root, runtime.scope(null));
        assertSame(root, runtime.scope(""));
    }

    @Test
    void scopesViewIsUnmodifiable() {
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(new Node());
        runtime.scope("/");

        Map<String, ScopeRuntimeContext> scopes = runtime.scopes();
        assertThrows(UnsupportedOperationException.class,
                () -> scopes.put("/x", new ScopeRuntimeContext("/x")));
    }

    @Test
    void scopeApisRejectNonPointerScopeInputs() {
        DocumentProcessingRuntime runtime = new DocumentProcessingRuntime(new Node());

        assertThrows(IllegalArgumentException.class, () -> runtime.scope("scope"));
        assertThrows(IllegalArgumentException.class, () -> runtime.existingScope("scope"));
        assertThrows(IllegalArgumentException.class, () -> runtime.isScopeTerminated("scope"));
        assertThrows(IllegalArgumentException.class, () -> runtime.chargeScopeEntry("scope"));
    }
}
