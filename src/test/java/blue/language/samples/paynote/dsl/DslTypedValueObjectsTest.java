package blue.language.samples.paynote.dsl;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DslTypedValueObjectsTest {

    @Test
    void docPathRequiresJsonPointerShape() {
        assertEquals("/a/b", DocPath.of("/a/b").pointer());
        assertThrows(IllegalArgumentException.class, () -> DocPath.of("a/b"));
        assertThrows(IllegalArgumentException.class, () -> DocPath.of("   "));
    }

    @Test
    void channelKeyRequiresNonBlankValue() {
        assertEquals("payerChannel", ChannelKey.of("payerChannel").value());
        assertThrows(IllegalArgumentException.class, () -> ChannelKey.of(""));
        assertThrows(IllegalArgumentException.class, () -> ChannelKey.of("  "));
    }
}
