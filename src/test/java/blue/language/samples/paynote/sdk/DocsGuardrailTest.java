package blue.language.samples.paynote.sdk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DocsGuardrailTest {

    @Test
    void publicDocsDoNotMentionDeprecatedRailSpecificAuthoringSurface() throws IOException {
        String readme = new String(Files.readAllBytes(Paths.get("README.md")), StandardCharsets.UTF_8);
        String docs = new String(Files.readAllBytes(Paths.get("docs.md")), StandardCharsets.UTF_8);
        String content = (readme + "\n" + docs).toLowerCase();

        assertFalse(content.contains("cardcapture("), "docs must not mention cardCapture()");
        assertFalse(content.contains(".attach("), "docs must not mention attach(...)");
        assertFalse(content.contains("cardtransaction("), "docs must not mention cardTransaction(...)");
    }
}
