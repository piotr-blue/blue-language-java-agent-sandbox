package blue.language.v2;

import blue.language.blueid.v2.CanonicalizerV2;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class V2Spec_CanonicalizerBlueIdDisciplineTest {

    @Test
    void pureReferenceKeepsBlueId() {
        Node reference = new Node().blueId("ReferenceOnly");
        Object canonical = CanonicalizerV2.toCanonicalObject(reference);

        assertTrue(canonical instanceof Map);
        Map<?, ?> map = (Map<?, ?>) canonical;
        assertEquals(1, map.size());
        assertEquals("ReferenceOnly", map.get("blueId"));
    }

    @Test
    void nonReferenceNodeDropsBlueIdField() {
        Node mixed = new Node()
                .blueId("NotAllowedOnPayloadNode")
                .properties("x", new Node().value(1));

        Object canonical = CanonicalizerV2.toCanonicalObject(mixed);
        assertTrue(canonical instanceof Map);
        Map<?, ?> map = (Map<?, ?>) canonical;
        assertFalse(map.containsKey("blueId"));
        assertTrue(map.containsKey("x"));
    }
}
