package blue.language.processor;

import blue.language.model.Node;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.HandlerContract;

import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Executes channel matching and handler invocation for a scope.
 *
 * <p>Applies checkpoint gating for external channels and feeds successful
 * matches into the registered handler processors.</p>
 */
final class ChannelRunner {

    private final DocumentProcessor owner;
    private final ProcessorEngine.Execution execution;
    private final DocumentProcessingRuntime runtime;
    private final CheckpointManager checkpointManager;

    ChannelRunner(DocumentProcessor owner,
                  ProcessorEngine.Execution execution,
                  DocumentProcessingRuntime runtime,
                  CheckpointManager checkpointManager) {
        this.owner = Objects.requireNonNull(owner, "owner");
        this.execution = Objects.requireNonNull(execution, "execution");
        this.runtime = Objects.requireNonNull(runtime, "runtime");
        this.checkpointManager = Objects.requireNonNull(checkpointManager, "checkpointManager");
    }

    void runExternalChannel(String scopePath,
                            ContractBundle bundle,
                            ContractBundle.ChannelBinding channel,
                            Node event) {
        if (execution.isScopeInactive(scopePath)) {
            return;
        }
        runtime.chargeChannelMatchAttempt();
        ChannelContract contract = channel.contract();
        ProcessorEngine.ChannelMatch match = ProcessorEngine.evaluateChannel(owner, contract, bundle, scopePath, event);
        if (!match.matches) {
            return;
        }
        Node eventForHandlers = match.eventNode() != null ? match.eventNode() : event;
        checkpointManager.ensureCheckpointMarker(scopePath, bundle);
        CheckpointManager.CheckpointRecord checkpoint = checkpointManager.findCheckpoint(bundle, channel.key());
        String eventSignature = match.eventId != null
                ? match.eventId
                : ProcessorEngine.canonicalSignature(eventForHandlers);
        if (checkpointManager.isDuplicate(checkpoint, eventSignature)) {
            return;
        }
        runHandlers(scopePath, bundle, channel.key(), eventForHandlers, false);
        if (execution.isScopeInactive(scopePath)) {
            return;
        }
        checkpointManager.persist(scopePath, bundle, checkpoint, eventSignature, eventForHandlers);
    }

    void runHandlers(String scopePath,
                     ContractBundle bundle,
                     String channelKey,
                     Node event,
                     boolean allowTerminatedWork) {
        List<ContractBundle.HandlerBinding> handlers = bundle.handlersFor(channelKey);
        if (handlers.isEmpty()) {
            return;
        }
        for (ContractBundle.HandlerBinding handler : handlers) {
            if (!allowTerminatedWork && execution.isScopeInactive(scopePath)) {
                break;
            }
            if (!matchesHandlerEventFilter(handler.contract(), event)) {
                continue;
            }
            runtime.chargeHandlerOverhead();
            ProcessorExecutionContext context = execution.createContext(scopePath, bundle, event, allowTerminatedWork);
            ProcessorEngine.executeHandler(owner, handler.contract(), context);
            if (execution.isScopeInactive(scopePath) && !allowTerminatedWork) {
                break;
            }
        }
    }

    private boolean matchesHandlerEventFilter(HandlerContract handler, Node event) {
        Node expectedEvent = handler != null ? handler.getEvent() : null;
        if (expectedEvent == null) {
            return true;
        }
        if (event == null) {
            return false;
        }
        return matchesNodeFilter(event, expectedEvent);
    }

    private boolean matchesNodeFilter(Node actual, Node expected) {
        if (expected == null) {
            return true;
        }
        if (actual == null) {
            return false;
        }

        if (expected.getType() != null && !matchesType(actual.getType(), expected.getType())) {
            return false;
        }

        if (expected.getValue() != null && !Objects.equals(actual.getValue(), expected.getValue())) {
            return false;
        }

        List<Node> expectedItems = expected.getItems();
        if (expectedItems != null && !expectedItems.isEmpty()) {
            List<Node> actualItems = actual.getItems();
            if (actualItems == null || actualItems.size() < expectedItems.size()) {
                return false;
            }
            for (int i = 0; i < expectedItems.size(); i++) {
                if (!matchesNodeFilter(actualItems.get(i), expectedItems.get(i))) {
                    return false;
                }
            }
        }

        Map<String, Node> expectedProps = expected.getProperties();
        if (expectedProps != null && !expectedProps.isEmpty()) {
            Map<String, Node> actualProps = actual.getProperties();
            if (actualProps == null) {
                return false;
            }
            for (Map.Entry<String, Node> entry : expectedProps.entrySet()) {
                if (!actualProps.containsKey(entry.getKey())) {
                    return false;
                }
                if (!matchesNodeFilter(actualProps.get(entry.getKey()), entry.getValue())) {
                    return false;
                }
            }
        }

        return true;
    }

    private boolean matchesType(Node actualType, Node expectedType) {
        if (expectedType == null) {
            return true;
        }
        if (actualType == null) {
            return false;
        }
        String expectedBlueId = expectedType.getBlueId();
        if (expectedBlueId != null && !expectedBlueId.trim().isEmpty()) {
            return blueIdsEquivalent(expectedBlueId, actualType.getBlueId());
        }
        return matchesNodeFilter(actualType, expectedType);
    }

    private boolean blueIdsEquivalent(String expectedBlueId, String actualBlueId) {
        if (expectedBlueId == null || actualBlueId == null) {
            return false;
        }
        if (expectedBlueId.equals(actualBlueId)) {
            return true;
        }
        return (isAliasPair(expectedBlueId, actualBlueId, "DocumentProcessingInitiated", "Core/Document Processing Initiated")
                || isAliasPair(expectedBlueId, actualBlueId, "DocumentProcessingTerminated", "Core/Document Processing Terminated")
                || isAliasPair(expectedBlueId, actualBlueId, "DocumentProcessingFatalError", "Core/Document Processing Fatal Error")
                || isAliasPair(expectedBlueId, actualBlueId, "DocumentUpdate", "Core/Document Update"));
    }

    private boolean isAliasPair(String left, String right, String first, String second) {
        return (first.equals(left) && second.equals(right))
                || (second.equals(left) && first.equals(right));
    }
}
