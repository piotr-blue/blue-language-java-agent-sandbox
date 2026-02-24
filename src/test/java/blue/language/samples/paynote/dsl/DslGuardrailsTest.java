package blue.language.samples.paynote.dsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DslGuardrailsTest {

    @Test
    void rejectsMutatingReservedCheckpointPathInChangesetBuilder() {
        ChangesetBuilder changesetBuilder = new ChangesetBuilder();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> changesetBuilder.replaceValue("/contracts/checkpoint", "forbidden"));

        assertTrue(exception.getMessage().contains("reserved processor contract path"));
    }

    @Test
    void rejectsMutatingReservedEmbeddedPathInChangesetBuilder() {
        ChangesetBuilder changesetBuilder = new ChangesetBuilder();

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> changesetBuilder.addValue("/contracts/embedded/paths/0", "/child"));

        assertTrue(exception.getMessage().contains("reserved processor contract path"));
    }
}
