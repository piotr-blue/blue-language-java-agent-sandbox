package blue.language.processor.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

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

    @Test
    void addPathRejectsNonPointerValues() {
        ProcessEmbedded embedded = new ProcessEmbedded();
        assertThrows(IllegalArgumentException.class, () -> embedded.addPath("x"));
    }

    @Test
    void setPathsRejectsMalformedPointerEscapes() {
        ProcessEmbedded embedded = new ProcessEmbedded();
        assertThrows(IllegalArgumentException.class, () -> embedded.setPaths(Arrays.asList("/ok", "/bad~2")));
    }
}
