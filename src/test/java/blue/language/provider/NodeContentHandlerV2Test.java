package blue.language.provider;

import blue.language.Blue;
import blue.language.model.Node;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;

class NodeContentHandlerV2Test {

    @Test
    void semanticBlueIdNormalizesRedundantOverrides() {
        BasicNodeProvider provider = new BasicNodeProvider();
        provider.addSingleDocs(
                "name: BaseType\n" +
                        "x: 1\n"
        );
        String baseBlueId = provider.getBlueIdByName("BaseType");

        Blue blue = new Blue(provider);
        String lean = "name: Child\n" +
                "type:\n" +
                "  blueId: " + baseBlueId + "\n";
        String noisy = "name: Child\n" +
                "type:\n" +
                "  blueId: " + baseBlueId + "\n" +
                "x: 1\n";

        NodeContentHandler.ParsedContent leanParsed = NodeContentHandler.parseAndCalculateSemanticBlueId(lean, blue);
        NodeContentHandler.ParsedContent noisyParsed = NodeContentHandler.parseAndCalculateSemanticBlueId(noisy, blue);

        assertEquals(leanParsed.blueId, noisyParsed.blueId);
        assertFalse(leanParsed.isMultipleDocuments);
    }

    @Test
    void semanticBlueIdListCalculationUsesCanonicalNodes() {
        Blue blue = new Blue();
        Node nodeA = blue.yamlToNode("name: A\nv: 1\n");
        Node nodeB = blue.yamlToNode("name: B\nv: 2\n");

        NodeContentHandler.ParsedContent parsed = NodeContentHandler.parseAndCalculateSemanticBlueId(
                Arrays.asList(nodeA, nodeB),
                blue
        );

        assertNotNull(parsed.blueId);
        assertTrue(parsed.isMultipleDocuments);
        assertTrue(parsed.content.isArray());
        assertEquals(2, parsed.content.size());
    }
}
