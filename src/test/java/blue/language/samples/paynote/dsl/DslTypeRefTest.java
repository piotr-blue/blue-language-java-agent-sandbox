package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class DslTypeRefTest {

    @Test
    void resolvesAliasAndBlueIdFromClass() {
        TypeRef typeRef = TypeRef.of(MyOsTypes.MyOsTimelineChannel.class);
        Node typeNode = typeRef.asTypeNode();

        assertEquals(TypeAliases.MYOS_TIMELINE_CHANNEL, typeNode.getValue());
        assertEquals("HCF8mXnX3dFjQ8osjxb4Wzm2Nm1DoXnTYuA5sPnV7NTs", typeNode.getBlueId());
    }

    @Test
    void buildsTypedDocumentWithoutRawTypeStrings() {
        Node document = BlueDocDsl.document(MyOsTypes.Agent.class)
                .name("Typed Agent Doc")
                .build();

        assertNotNull(document.getType());
        assertEquals(TypeAliases.MYOS_AGENT, document.getAsText("/type/value"));
        assertEquals("8s2rAFDtiB6sCwqeURkT4Lq7fcc2FXBkmX9B9p7R4Boc", document.getAsText("/type/blueId"));
    }
}
