package blue.language.samples.paynote.codegen;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RepositorySnapshotTypeGeneratorTest {

    @Test
    void generatesTypedBeansForCoreConversationMyOsAndPayNoteFromPinnedSnapshot() throws IOException {
        String snapshot = new String(Files.readAllBytes(
                Paths.get("src/test/resources/samples/paynote/repo-snapshot-pinned.yaml")),
                StandardCharsets.UTF_8);

        RepositorySnapshotTypeGenerator generator = new RepositorySnapshotTypeGenerator();
        Map<String, String> generated = generator.generate(snapshot);

        assertEquals(4, generated.size());
        assertTrue(generated.containsKey("blue.language.samples.paynote.generated.core.CoreChannel"));
        assertTrue(generated.containsKey("blue.language.samples.paynote.generated.conversation.SequentialWorkflow"));
        assertTrue(generated.containsKey("blue.language.samples.paynote.generated.myos.DocumentSessionBootstrap"));
        assertTrue(generated.containsKey("blue.language.samples.paynote.generated.paynote.ReserveFundsRequested"));

        String myOsSource = generated.get("blue.language.samples.paynote.generated.myos.DocumentSessionBootstrap");
        assertTrue(myOsSource.contains("@TypeAlias(\"MyOS/Document Session Bootstrap\")"));
        assertTrue(myOsSource.contains("@TypeBlueId(\"84xMEnEYr3DPBuYZL3JtcsZBBTtRH9fEEJiPnk7ASj1o\")"));
        assertTrue(myOsSource.contains("public blue.language.model.Node document;"));
    }
}
