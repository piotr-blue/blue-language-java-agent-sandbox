package blue.language.processor.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ProcessEmbeddedModelTest {

    @Test
    void addPathTrimsAndSkipsBlankValues() {
        ProcessEmbedded embedded = new ProcessEmbedded()
                .addPath("  /a  ")
                .addPath("   ")
                .addPath(null)
                .addPath("/b");

        assertEquals(Arrays.asList("/a", "/b"), embedded.getPaths());
    }

    @Test
    void setPathsFiltersBlankEntries() {
        ProcessEmbedded embedded = new ProcessEmbedded();
        embedded.setPaths(Arrays.asList(" /x ", "", "   ", null, "/y"));

        assertEquals(Arrays.asList("/x", "/y"), embedded.getPaths());
    }
}
