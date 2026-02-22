package blue.language.processor;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.processor.contracts.SetPropertyOnEventContractProcessor;
import blue.language.processor.contracts.TestEventChannelProcessor;
import blue.language.utils.UncheckedObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ParityFixturesTest {

    @Test
    void parityFixturesProduceExpectedDocumentAndEmissions() throws IOException {
        List<Path> fixtures = fixtureFiles();
        assertNotNull(fixtures, "Fixture file list must not be null");

        for (Path fixturePath : fixtures) {
            runFixture(fixturePath);
        }
    }

    @SuppressWarnings("unchecked")
    private void runFixture(Path fixturePath) throws IOException {
        Map<String, Object> fixture = readFixture(fixturePath);
        String fixtureName = stringValue(fixture.get("name"), fixturePath.getFileName().toString());
        String documentYaml = stringValue(fixture.get("document"), null);
        String eventYaml = stringValue(fixture.get("event"), null);
        Map<String, Object> expected = mapValue(fixture.get("expected"));
        Map<String, Object> expectedPaths = mapValue(expected.get("paths"));
        List<String> expectedPresentPaths = listOfStrings(expected.get("presentPaths"));
        List<String> expectedNotNullPaths = listOfStrings(expected.get("notNullPaths"));
        List<String> expectedTriggeredKinds = listOfStrings(expected.get("triggeredKinds"));
        int expectedTriggeredEvents = intValue(expected.get("triggeredEventsCount"), 0);
        boolean expectedCapabilityFailure = boolValue(expected.get("capabilityFailure"), false);
        boolean expectedInitFailure = boolValue(expected.get("initFailure"), false);
        String initFailureMessageContains = stringValue(expected.get("initFailureMessageContains"), null);

        Blue blue = new Blue();
        blue.registerContractProcessor(new TestEventChannelProcessor());
        blue.registerContractProcessor(new SetPropertyOnEventContractProcessor());

        Node document = blue.yamlToNode(documentYaml);
        if (expectedInitFailure) {
            Throwable thrown = assertThrows(RuntimeException.class, () -> blue.initializeDocument(document));
            if (initFailureMessageContains != null && !initFailureMessageContains.trim().isEmpty()) {
                String actualMessage = thrown.getMessage() != null ? thrown.getMessage() : String.valueOf(thrown);
                assertTrue(actualMessage.contains(initFailureMessageContains),
                        fixtureName + " expected init failure containing: " + initFailureMessageContains
                                + " but got: " + actualMessage);
            }
            return;
        }

        DocumentProcessingResult initialized = blue.initializeDocument(document);
        DocumentProcessingResult result = initialized;
        if (eventYaml != null && !eventYaml.trim().isEmpty()) {
            Node event = blue.yamlToNode(eventYaml);
            result = blue.processDocument(initialized.document(), event);
        }

        for (Map.Entry<String, Object> entry : expectedPaths.entrySet()) {
            String pointer = entry.getKey();
            Node actualNode = ProcessorEngine.nodeAt(result.document(), pointer);
            assertNotNull(actualNode, fixtureName + " expected node missing at " + pointer);
            assertEquals(String.valueOf(entry.getValue()),
                    String.valueOf(actualNode.getValue()),
                    fixtureName + " value mismatch at " + pointer);
        }
        for (String pointer : expectedPresentPaths) {
            Node actualNode = ProcessorEngine.nodeAt(result.document(), pointer);
            assertNotNull(actualNode, fixtureName + " expected present node missing at " + pointer);
        }
        for (String pointer : expectedNotNullPaths) {
            Node actualNode = ProcessorEngine.nodeAt(result.document(), pointer);
            assertNotNull(actualNode, fixtureName + " expected non-null node missing at " + pointer);
            assertNotNull(actualNode.getValue(), fixtureName + " expected non-null value at " + pointer);
        }
        assertEquals(expectedTriggeredEvents,
                result.triggeredEvents().size(),
                fixtureName + " unexpected triggered events count");
        for (String expectedKind : expectedTriggeredKinds) {
            boolean present = false;
            for (Node emitted : result.triggeredEvents()) {
                if (emitted == null || emitted.getProperties() == null) {
                    continue;
                }
                Node kindNode = emitted.getProperties().get("kind");
                if (kindNode == null || kindNode.getValue() == null) {
                    continue;
                }
                if (expectedKind.equals(String.valueOf(kindNode.getValue()))) {
                    present = true;
                    break;
                }
            }
            assertTrue(present, fixtureName + " expected triggered kind not found: " + expectedKind);
        }
        assertEquals(expectedCapabilityFailure,
                result.capabilityFailure(),
                fixtureName + " unexpected capabilityFailure flag");
    }

    private List<Path> fixtureFiles() throws IOException {
        Path fixtureDir = Paths.get("parity-fixtures");
        List<Path> paths = new ArrayList<>();
        try (java.util.stream.Stream<Path> stream = Files.list(fixtureDir)) {
            stream.filter(path -> path.getFileName().toString().endsWith(".yaml"))
                    .sorted(Comparator.comparing(Path::toString))
                    .forEach(paths::add);
        }
        return paths;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> readFixture(Path fixturePath) throws IOException {
        try (InputStream input = Files.newInputStream(fixturePath)) {
            Object parsed = UncheckedObjectMapper.YAML_MAPPER.readValue(input, Map.class);
            if (parsed instanceof Map) {
                return new LinkedHashMap<>((Map<String, Object>) parsed);
            }
            return new LinkedHashMap<>();
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> mapValue(Object value) {
        if (value instanceof Map) {
            return new LinkedHashMap<>((Map<String, Object>) value);
        }
        return new LinkedHashMap<>();
    }

    @SuppressWarnings("unchecked")
    private List<String> listOfStrings(Object value) {
        if (!(value instanceof List)) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (Object item : (List<Object>) value) {
            if (item != null) {
                result.add(String.valueOf(item));
            }
        }
        return result;
    }

    private String stringValue(Object value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return String.valueOf(value);
    }

    private int intValue(Object value, int fallback) {
        if (value instanceof Number) {
            return ((Number) value).intValue();
        }
        if (value instanceof String) {
            try {
                return Integer.parseInt((String) value);
            } catch (NumberFormatException ignored) {
                return fallback;
            }
        }
        return fallback;
    }

    private boolean boolValue(Object value, boolean fallback) {
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.parseBoolean((String) value);
        }
        return fallback;
    }
}
