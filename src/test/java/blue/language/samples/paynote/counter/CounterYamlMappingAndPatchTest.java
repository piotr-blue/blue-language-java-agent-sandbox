package blue.language.samples.paynote.counter;

import blue.language.Blue;
import blue.language.mapping.NodeToObjectConverter;
import blue.language.model.Node;
import blue.language.processor.model.JsonPatch;
import blue.language.snapshot.ResolvedSnapshot;
import blue.language.snapshot.WorkingDocument;
import blue.language.utils.TypeClassResolver;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class CounterYamlMappingAndPatchTest {

    private final Blue blue = new Blue();

    @Test
    void mapsCounterYamlToObjectAndMutatesWithWorkingDocument() throws IOException {
        String yaml = loadResource("samples/paynote/counter/counter.yaml");
        Node counterNode = blue.yamlToNode(yaml);

        NodeToObjectConverter converter = new NodeToObjectConverter(
                new TypeClassResolver("blue.language.samples.paynote.counter"));
        CounterDocument mapped = converter.convert(counterNode, CounterDocument.class);

        assertNotNull(mapped);
        assertEquals("Counter", mapped.name);
        assertEquals(Integer.valueOf(0), mapped.counter);
        assertNotNull(mapped.contracts);
        assertEquals("timeline-demo-001", mapped.contracts.ownerChannel.timelineId);
        assertEquals("ownerChannel", mapped.contracts.increment.channel);
        assertEquals("increment", mapped.contracts.incrementImpl.operation);
        assertEquals("decrement", mapped.contracts.decrementImpl.operation);

        Node fromObject = blue.objectToNode(mapped);
        assertEquals("Counter-Demo-BlueId", fromObject.getType().getBlueId());
        assertEquals("ownerChannel", fromObject.getProperties()
                .get("contracts")
                .getProperties()
                .get("increment")
                .getProperties()
                .get("channel")
                .getValue());

        ResolvedSnapshot snapshot = blue.resolveToSnapshot(counterNode);
        WorkingDocument workingDocument = WorkingDocument.forSnapshot(blue, snapshot);
        workingDocument.applyPatch(JsonPatch.replace("/counter", new Node().value(5)));
        ResolvedSnapshot committed = workingDocument.commit();

        assertEquals(Integer.valueOf(5), committed.resolvedRoot().toNode().getAsInteger("/counter/value"));
    }

    private String loadResource(String path) throws IOException {
        InputStream inputStream = Thread.currentThread().getContextClassLoader().getResourceAsStream(path);
        if (inputStream == null) {
            throw new IllegalStateException("Missing resource: " + path);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int read;
        while ((read = inputStream.read(buffer)) != -1) {
            output.write(buffer, 0, read);
        }
        return new String(output.toByteArray(), "UTF-8");
    }
}
