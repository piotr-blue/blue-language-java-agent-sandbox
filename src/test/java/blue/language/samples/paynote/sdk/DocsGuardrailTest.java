package blue.language.samples.paynote.sdk;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;

class DocsGuardrailTest {

    @Test
    void publicDocsDoNotMentionDeprecatedRailSpecificAuthoringSurface() throws IOException {
        String readme = readIfExists(Paths.get("README.md"));
        String docs = readIfExists(Paths.get("docs.md"));
        String content = (readme + "\n" + docs).toLowerCase();

        assertFalse(content.contains("cardcapture("), "docs must not mention cardCapture()");
        assertFalse(content.contains(".attach("), "docs must not mention attach(...)");
        assertFalse(content.contains("cardtransaction("), "docs must not mention cardTransaction(...)");
    }

    private String readIfExists(Path path) throws IOException {
        if (path == null || !Files.exists(path)) {
            return "";
        }
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
