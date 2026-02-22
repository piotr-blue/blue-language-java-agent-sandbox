package blue.language.merge;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.provider.BasicNodeProvider;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ExpressionPreserverTest {

    @Test
    void preservesExpressionsWhenTypeResolutionWouldCoerceValueShape() {
        BasicNodeProvider nodeProvider = new BasicNodeProvider();
        nodeProvider.addSingleDocs(
                "name: Json Patch Entry\n" +
                        "val:\n" +
                        "  description: The value to be used in the operation");
        String patchEntryBlueId = nodeProvider.getBlueIdByName("Json Patch Entry");
        nodeProvider.addSingleDocs(
                "name: Update Document\n" +
                        "changeset:\n" +
                        "  type: List\n" +
                        "  itemType:\n" +
                        "    blueId: " + patchEntryBlueId
        );
        Blue blue = new Blue(nodeProvider);

        Node source = blue.yamlToNode("type:\n" +
                "  blueId: " + nodeProvider.getBlueIdByName("Update Document") + "\n" +
                "changeset: \"${steps.CreateSubscriptions.changes}\"");

        Node resolved = blue.resolve(source);
        Node changeset = resolved.getProperties().get("changeset");
        assertNotNull(changeset);
        assertEquals("${steps.CreateSubscriptions.changes}", changeset.getValue());
    }

    @Test
    void preservesNestedExpressionsAcrossObjectProperties() {
        BasicNodeProvider nodeProvider = new BasicNodeProvider();
        nodeProvider.addSingleDocs(
                "name: Complex Type\n" +
                        "config:\n" +
                        "  database:\n" +
                        "    host:\n" +
                        "      type: Text\n" +
                        "    port:\n" +
                        "      type: Integer"
        );
        Blue blue = new Blue(nodeProvider);

        Node source = blue.yamlToNode("type:\n" +
                "  blueId: " + nodeProvider.getBlueIdByName("Complex Type") + "\n" +
                "config:\n" +
                "  database:\n" +
                "    host: \"${env.DB_HOST}\"\n" +
                "    port: \"${env.DB_PORT}\"");
        Node resolved = blue.resolve(source);

        Node database = resolved.getProperties().get("config")
                .getProperties().get("database");
        assertEquals("${env.DB_HOST}", database.getProperties().get("host").getValue());
        assertEquals("${env.DB_PORT}", database.getProperties().get("port").getValue());
    }
}
