package blue.language.processor.harness;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.examples.paynote.PayNoteCookbookExamplesV2;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PayNoteCookbookHarnessSmokeTest {

    @Test
    void allCookbookTicketsInitializeSuccessfullyInHarness() {
        ProcessorHarness harness = new ProcessorHarness();
        for (Map.Entry<String, Node> entry : PayNoteCookbookExamplesV2.allTickets().entrySet()) {
            ProcessorSession session = harness.start(entry.getValue());
            session.init();

            Node document = session.document();
            assertEquals(PayNoteAliases.PAYNOTE, document.getAsText("/type/value"), entry.getKey());
            String name = document.getAsText("/name/value");
            assertNotNull(name, entry.getKey());
            assertFalse(name.trim().isEmpty(), entry.getKey());
            assertTrue(session.initialized(), entry.getKey());
        }
    }
}
