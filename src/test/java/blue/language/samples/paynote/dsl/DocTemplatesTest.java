package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class DocTemplatesTest {

    @Test
    void extendsTemplateWithoutMutatingOriginal() {
        Node baseTemplate = MyOsDsl.bootstrap()
                .documentName("Base Template")
                .documentType(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE)
                .putDocumentValue("currency", "EUR")
                .contracts(c -> c.timelineChannel("payerChannel"))
                .build();

        Node extended = DocTemplates.extend(baseTemplate, mutator -> mutator
                .putDocumentValue("currency", "CHF")
                .bindAccount("payerChannel", "acc-alice"));

        assertEquals("EUR", baseTemplate.getAsText("/document/currency/value"));
        assertEquals("CHF", extended.getAsText("/document/currency/value"));
        assertEquals("acc-alice", extended.getAsText("/channelBindings/payerChannel/accountId/value"));
    }

    @Test
    void appliesJsonPatchEntriesForSpecializationStage() {
        Node base = MyOsDsl.bootstrap()
                .documentName("Patchable Template")
                .documentType(PayNoteAliases.SHIPMENT_CAPTURE_PAYNOTE)
                .putDocumentValue("currency", "EUR")
                .putDocumentObject("amount", amount -> amount.put("total", 0))
                .build();

        Node specialized = DocTemplates.applyPatch(base, Arrays.asList(
                JsonPatch.replace("/document/amount/total", new Node().value(20000)),
                JsonPatch.replace("/document/currency", new Node().value("EUR")),
                JsonPatch.add("/document/funding/sourceCurrency", new Node().value("CHF"))
        ));

        assertEquals(0, base.getAsInteger("/document/amount/total/value").intValue());
        assertEquals(20000, specialized.getAsInteger("/document/amount/total/value").intValue());
        assertEquals("CHF", specialized.getAsText("/document/funding/sourceCurrency/value"));
        assertThrows(IllegalArgumentException.class,
                () -> base.getAsText("/document/funding/sourceCurrency/value"));
    }
}
