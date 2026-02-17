package blue.language.provider;

import blue.language.Blue;
import blue.language.blueid.v2.BlueIdCalculatorV2;
import blue.language.model.Node;
import blue.language.preprocess.Preprocessor;
import blue.language.snapshot.v2.ResolvedSnapshotV2;
import blue.language.utils.Nodes;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;

import java.util.*;
import java.util.function.Function;
import java.util.stream.IntStream;

import static blue.language.utils.UncheckedObjectMapper.JSON_MAPPER;
import static blue.language.utils.UncheckedObjectMapper.YAML_MAPPER;

public class BasicNodeProvider extends PreloadedNodeProvider {

    private Map<String, JsonNode> blueIdToContentMap;
    private Map<String, Boolean> blueIdToMultipleDocumentsMap;
    private Map<String, ResolvedSnapshotV2> semanticBlueIdToSnapshotMap;
    private Function<Node, Node> preprocessor;

    public BasicNodeProvider(Node... nodes) {
        this(Arrays.asList(nodes));
    }

    public BasicNodeProvider(Collection<Node> nodes) {
        this.blueIdToContentMap = new HashMap<>();
        this.blueIdToMultipleDocumentsMap = new HashMap<>();
        this.semanticBlueIdToSnapshotMap = new HashMap<>();

        Preprocessor defaultPreprocessor = new Preprocessor(this);
        this.preprocessor = defaultPreprocessor::preprocessWithDefaultBlue;

        nodes.forEach(this::processNode);
    }

    private void processNode(Node node) {
        if (Nodes.hasItemsOnly(node)) {
            processNodeWithItems(node);
        } else {
            processSingleNode(node);
        }
    }

    private void processSingleNode(Node node) {
        NodeContentHandler.ParsedContent parsedContent = NodeContentHandler.parseAndCalculateBlueId(node, preprocessor);
        blueIdToContentMap.put(parsedContent.blueId, parsedContent.content);
        blueIdToMultipleDocumentsMap.put(parsedContent.blueId, parsedContent.isMultipleDocuments);
        addToNameMap(node.getName(), parsedContent.blueId);
    }

    private void processNodeWithItems(Node node) {
        List<Node> items = node.getItems();
        processNodeList(items);

        NodeContentHandler.ParsedContent parsedContent = NodeContentHandler.parseAndCalculateBlueId(items, preprocessor);
        blueIdToContentMap.put(parsedContent.blueId, parsedContent.content);
        blueIdToMultipleDocumentsMap.put(parsedContent.blueId, true);

        IntStream.range(0, items.size()).forEach(i -> {
            Node item = items.get(i);
            addToNameMap(item.getName(), parsedContent.blueId + "#" + i);
        });
    }

    public void processNodeList(List<Node> nodes) {
        String listBlueId = BlueIdCalculatorV2.calculateSemanticBlueId(nodes);
        JsonNode listContent = JSON_MAPPER.valueToTree(nodes);
        blueIdToContentMap.put(listBlueId, listContent);
        blueIdToMultipleDocumentsMap.put(listBlueId, true);
    }

    @Override
    protected JsonNode fetchContentByBlueId(String baseBlueId) {
        JsonNode content = blueIdToContentMap.get(baseBlueId);
        Boolean isMultipleDocuments = blueIdToMultipleDocumentsMap.get(baseBlueId);
        if (content != null && isMultipleDocuments != null) {
            return NodeContentHandler.resolveThisReferences(content, baseBlueId, isMultipleDocuments);
        }
        return null;
    }

    public void addSingleNodes(Node... nodes) {
        Arrays.stream(nodes).forEach(this::processNode);
    }

    public void addSingleDocs(String... docs) {
        Arrays.stream(docs)
                .map(doc -> YAML_MAPPER.readValue(doc, Node.class))
                .forEach(this::processNode);
    }

    public String addSingleDocsV2(String yaml) {
        Blue blue = new Blue(this);
        Node authoring = YAML_MAPPER.readValue(yaml, Node.class);
        ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);
        storeSemanticSnapshot(snapshot, false);
        addNodeToNameMap(JSON_MAPPER.valueToTree(snapshot.resolvedRoot().toNode()), snapshot.rootBlueId());
        return snapshot.rootBlueId();
    }

    public String addMultipleDocsV2(String yaml) {
        JsonNode jsonNode = YAML_MAPPER.readTree(yaml);
        if (!jsonNode.isArray()) {
            return addSingleDocsV2(yaml);
        }

        Blue blue = new Blue(this);
        ArrayNode canonicalNodes = JSON_MAPPER.createArrayNode();
        List<Node> canonicalNodeList = new ArrayList<Node>();
        List<JsonNode> resolvedNodeJsons = new ArrayList<JsonNode>();

        for (int i = 0; i < jsonNode.size(); i++) {
            Node authoring = JSON_MAPPER.convertValue(jsonNode.get(i), Node.class);
            ResolvedSnapshotV2 snapshot = blue.resolveToSnapshotV2(authoring);
            storeSemanticSnapshot(snapshot, false);
            resolvedNodeJsons.add(JSON_MAPPER.valueToTree(snapshot.resolvedRoot().toNode()));

            Node canonicalNode = snapshot.canonicalRoot().toNode();
            canonicalNodeList.add(canonicalNode);
            canonicalNodes.add(JSON_MAPPER.valueToTree(canonicalNode));
        }

        String listSemanticBlueId = BlueIdCalculatorV2.calculateSemanticBlueId(canonicalNodeList);
        blueIdToContentMap.put(listSemanticBlueId, canonicalNodes);
        blueIdToMultipleDocumentsMap.put(listSemanticBlueId, true);
        for (int i = 0; i < resolvedNodeJsons.size(); i++) {
            addNodeToNameMap(resolvedNodeJsons.get(i), listSemanticBlueId + "#" + i);
        }
        return listSemanticBlueId;
    }

    public String getBlueIdByName(String name) {
        return nameToBlueIdsMap.get(name).get(0);
    }

    public Node getNodeByName(String name) {
        return findNodeByName(name).orElseThrow(() -> new IllegalArgumentException("No node with name \"" + name + "\""));
    }

    public void addListAndItsItems(List<Node> list) {
        processNodeList(list);
        list.forEach(this::processNode);
    }

    public void addListAndItsItems(String doc) {
        Node listNode = YAML_MAPPER.readValue(doc, Node.class);
        addListAndItsItems(listNode.getItems());
    }

    public void addList(List<Node> list) {
        processNodeList(list);
    }

    public Optional<ResolvedSnapshotV2> findSnapshotBySemanticBlueId(String semanticBlueId) {
        return Optional.ofNullable(semanticBlueIdToSnapshotMap.get(semanticBlueId));
    }

    private void storeParsedContent(NodeContentHandler.ParsedContent parsedContent) {
        blueIdToContentMap.put(parsedContent.blueId, parsedContent.content);
        blueIdToMultipleDocumentsMap.put(parsedContent.blueId, parsedContent.isMultipleDocuments);
    }

    private void addNodeToNameMap(JsonNode node, String blueId) {
        JsonNode nameNode = node.get("name");
        if (nameNode != null && !nameNode.isNull()) {
            addToNameMap(nameNode.asText(), blueId);
        }
    }

    private void storeSemanticSnapshot(ResolvedSnapshotV2 snapshot, boolean isMultipleDocuments) {
        String semanticBlueId = snapshot.rootBlueId();
        JsonNode canonicalJson = JSON_MAPPER.valueToTree(snapshot.canonicalRoot().toNode());
        blueIdToContentMap.put(semanticBlueId, canonicalJson);
        blueIdToMultipleDocumentsMap.put(semanticBlueId, isMultipleDocuments);
        semanticBlueIdToSnapshotMap.put(semanticBlueId, snapshot);
    }
}