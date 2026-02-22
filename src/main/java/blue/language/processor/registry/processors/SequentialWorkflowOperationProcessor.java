package blue.language.processor.registry.processors;

import blue.language.model.Node;
import blue.language.processor.ContractBundle;
import blue.language.processor.HandlerProcessor;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.MarkerContract;
import blue.language.processor.model.OperationMarker;
import blue.language.processor.model.SequentialWorkflowOperation;
import blue.language.processor.workflow.WorkflowStepRunner;

import java.util.List;

public class SequentialWorkflowOperationProcessor implements HandlerProcessor<SequentialWorkflowOperation> {

    private final WorkflowStepRunner stepRunner;

    public SequentialWorkflowOperationProcessor() {
        this(WorkflowStepRunner.defaultRunner());
    }

    public SequentialWorkflowOperationProcessor(WorkflowStepRunner stepRunner) {
        this.stepRunner = stepRunner;
    }

    @Override
    public Class<SequentialWorkflowOperation> contractType() {
        return SequentialWorkflowOperation.class;
    }

    @Override
    public String deriveChannel(SequentialWorkflowOperation contract) {
        if (contract.getChannelKey() != null && !contract.getChannelKey().trim().isEmpty()) {
            return contract.getChannelKey().trim();
        }
        return null;
    }

    @Override
    public boolean matches(SequentialWorkflowOperation contract, ProcessorExecutionContext context) {
        Node eventNode = context.event();
        if (eventNode == null) {
            return false;
        }

        Node requestNode = extractOperationRequestNode(eventNode);
        if (requestNode == null) {
            return false;
        }
        if (!isOperationRequestForContract(contract, eventNode, requestNode)) {
            return false;
        }

        Node operationNode = loadOperationNode(contract, context);
        if (operationNode == null) {
            return false;
        }

        String operationChannel = extractOperationChannel(operationNode);
        String handlerChannel = normalizeChannel(contract.getChannelKey());
        if (!channelsCompatible(operationChannel, handlerChannel)) {
            return false;
        }
        if (!isRequestTypeCompatible(requestNode, operationNode)) {
            return false;
        }
        if (!isPinnedDocumentAllowed(requestNode, context)) {
            return false;
        }

        return true;
    }

    @Override
    public void execute(SequentialWorkflowOperation contract, ProcessorExecutionContext context) {
        List<Node> steps = contract.getSteps();
        if (steps == null || steps.isEmpty()) {
            return;
        }
        stepRunner.run(contract, steps, context.event(), context);
    }

    @Override
    public String deriveChannel(SequentialWorkflowOperation contract, ContractBundle scopeContracts) {
        String declared = deriveChannel(contract);
        if (declared != null) {
            return declared;
        }
        if (scopeContracts == null || contract.getOperation() == null || contract.getOperation().trim().isEmpty()) {
            return null;
        }
        String operationKey = contract.getOperation().trim();
        MarkerContract marker = scopeContracts.marker(operationKey);
        if (!(marker instanceof OperationMarker)) {
            return null;
        }
        String channel = ((OperationMarker) marker).getChannel();
        return channel != null && !channel.trim().isEmpty() ? channel.trim() : null;
    }

    private Node extractOperationRequestNode(Node eventNode) {
        if (isOperationRequestNode(eventNode)) {
            return eventNode;
        }
        if (eventNode.getProperties() == null) {
            return null;
        }
        Node message = eventNode.getProperties().get("message");
        if (isOperationRequestNode(message)) {
            return message;
        }
        return null;
    }

    private boolean isOperationRequestNode(Node node) {
        return node != null
                && node.getProperties() != null
                && node.getProperties().containsKey("operation")
                && node.getProperties().containsKey("request");
    }

    private boolean isOperationRequestForContract(SequentialWorkflowOperation contract, Node eventNode, Node requestNode) {
        String operationKey = normalize(contract.getOperation());
        if (operationKey == null) {
            return false;
        }
        String requestOperation = valueAsString(requestNode, "operation");
        if (!operationKey.equals(requestOperation)) {
            return false;
        }
        return WorkflowContractSupport.matchesEventFilter(eventNode, contract.getEvent());
    }

    private Node loadOperationNode(SequentialWorkflowOperation contract, ProcessorExecutionContext context) {
        String operationKey = normalize(contract.getOperation());
        if (operationKey == null) {
            return null;
        }
        String operationPointer = context.resolvePointer("/contracts/" + operationKey);
        Node operationNode = context.documentAt(operationPointer);
        if (operationNode == null) {
            return null;
        }
        if (!isOperationNode(operationNode)) {
            return null;
        }
        return operationNode;
    }

    private boolean isOperationNode(Node node) {
        if (node == null || node.getType() == null || node.getType().getBlueId() == null) {
            return false;
        }
        String blueId = node.getType().getBlueId();
        return blueId.endsWith("Operation")
                || blueId.endsWith("Change Operation")
                || "Operation".equals(blueId)
                || "Conversation/Operation".equals(blueId)
                || "Conversation/Change Operation".equals(blueId);
    }

    private String extractOperationChannel(Node operationNode) {
        return normalize(valueAsString(operationNode, "channel"));
    }

    private boolean channelsCompatible(String operationChannel, String handlerChannel) {
        return !(operationChannel != null
                && handlerChannel != null
                && !operationChannel.equals(handlerChannel));
    }

    private boolean isRequestTypeCompatible(Node requestNode, Node operationNode) {
        if (requestNode.getProperties() == null || operationNode.getProperties() == null) {
            return false;
        }
        Node requestPayload = requestNode.getProperties().get("request");
        Node requiredType = operationNode.getProperties().get("request");
        if (requestPayload == null || requiredType == null) {
            return false;
        }
        return WorkflowContractSupport.matchesEventFilter(requestPayload, requiredType);
    }

    private boolean isPinnedDocumentAllowed(Node requestNode, ProcessorExecutionContext context) {
        Boolean allowNewer = valueAsBoolean(requestNode, "allowNewerVersion");
        if (allowNewer == null || allowNewer.booleanValue()) {
            return true;
        }
        Node pinnedDocument = requestNode.getProperties() != null
                ? requestNode.getProperties().get("document")
                : null;
        String pinnedBlueId = resolvePinnedDocumentBlueId(pinnedDocument);
        if (pinnedBlueId == null) {
            return true;
        }

        Node root = context.documentAt("/");
        if (root == null || root.getProperties() == null) {
            return false;
        }
        Node contracts = root.getProperties().get("contracts");
        if (contracts == null || contracts.getProperties() == null) {
            return false;
        }
        Node initialized = contracts.getProperties().get("initialized");
        if (initialized == null || initialized.getProperties() == null) {
            return false;
        }
        Node documentId = initialized.getProperties().get("documentId");
        if (documentId == null || !(documentId.getValue() instanceof String)) {
            return false;
        }
        return pinnedBlueId.equals(documentId.getValue());
    }

    private String resolvePinnedDocumentBlueId(Node documentNode) {
        if (documentNode == null) {
            return null;
        }
        if (documentNode.getBlueId() != null && !documentNode.getBlueId().trim().isEmpty()) {
            return documentNode.getBlueId().trim();
        }
        if (documentNode.getProperties() == null) {
            return null;
        }
        Node blueIdNode = documentNode.getProperties().get("blueId");
        if (blueIdNode == null || !(blueIdNode.getValue() instanceof String)) {
            return null;
        }
        return ((String) blueIdNode.getValue()).trim();
    }

    private String valueAsString(Node node, String property) {
        if (node == null || node.getProperties() == null) {
            return null;
        }
        Node valueNode = node.getProperties().get(property);
        if (valueNode == null || valueNode.getValue() == null) {
            return null;
        }
        return String.valueOf(valueNode.getValue()).trim();
    }

    private Boolean valueAsBoolean(Node node, String property) {
        if (node == null || node.getProperties() == null) {
            return null;
        }
        Node valueNode = node.getProperties().get(property);
        if (valueNode == null || valueNode.getValue() == null) {
            return null;
        }
        Object value = valueNode.getValue();
        if (value instanceof Boolean) {
            return (Boolean) value;
        }
        if (value instanceof String) {
            return Boolean.valueOf((String) value);
        }
        return null;
    }

    private String normalize(String channel) {
        if (channel == null) {
            return null;
        }
        String trimmed = channel.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String normalizeChannel(String channel) {
        return normalize(channel);
    }
}
