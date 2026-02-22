package blue.language.processor;

import blue.language.mapping.NodeToObjectConverter;
import blue.language.model.Node;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.Contract;
import blue.language.processor.model.DocumentUpdateChannel;
import blue.language.processor.model.EmbeddedNodeChannel;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.MarkerContract;
import blue.language.processor.model.ProcessEmbedded;
import blue.language.processor.util.ProcessorContractConstants;
import blue.language.processor.util.PointerUtils;

import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.LinkedHashSet;

/**
 * Parses contracts under a scope and produces a {@link ContractBundle}.
 */
final class ContractLoader {

    private final ContractProcessorRegistry registry;
    private final NodeToObjectConverter converter;

    ContractLoader(ContractProcessorRegistry registry, NodeToObjectConverter converter) {
        this.registry = Objects.requireNonNull(registry, "registry");
        this.converter = Objects.requireNonNull(converter, "converter");
    }

    ContractBundle load(Node scopeNode, String scopePath) {
        ContractBundle.Builder builder = ContractBundle.builder();
        Map<String, Node> properties = scopeNode.getProperties();
        if (properties == null) {
            return builder.build();
        }
        Node contractsNode = properties.get("contracts");
        if (contractsNode == null || contractsNode.getProperties() == null) {
            return builder.build();
        }

        Set<String> normalizedKeys = new LinkedHashSet<>();
        for (Map.Entry<String, Node> entry : contractsNode.getProperties().entrySet()) {
            String key = normalizeContractKey(entry.getKey(), scopePath);
            if (!normalizedKeys.add(key)) {
                throw new IllegalStateException(
                        "Duplicate normalized contract key '" + key + "' at scope " + scopePath);
            }
            Contract contract = converter.convertWithType(entry.getValue(), Contract.class, false);
            if (contract == null) {
                continue;
            }
            contract.setKey(key);
            normalizePointerBackedContracts(contract, key, scopePath);
            if (contract instanceof ChannelContract) {
                ChannelContract channel = (ChannelContract) contract;
                if (!ProcessorContractConstants.isProcessorManagedChannel(channel)
                        && !registry.lookupChannel(channel).isPresent()) {
                    throw new MustUnderstandFailureException(
                            "Unsupported contract type: " + channel.getClass().getName());
                }
                builder.addChannel(key, channel);
            } else if (contract instanceof HandlerContract) {
                HandlerContract handler = (HandlerContract) contract;
                @SuppressWarnings("unchecked")
                HandlerProcessor<HandlerContract> handlerProcessor =
                        (HandlerProcessor<HandlerContract>) registry.lookupHandler(handler).orElse(null);
                if (handlerProcessor == null) {
                    throw new MustUnderstandFailureException(
                            "Unsupported contract type: " + handler.getClass().getName());
                }
                String channelKey = handler.getChannelKey();
                if (channelKey == null || channelKey.trim().isEmpty()) {
                    channelKey = handlerProcessor.deriveChannel(handler);
                }
                if (channelKey == null || channelKey.trim().isEmpty()) {
                    throw new IllegalStateException("Handler " + key + " must declare channel (or derive one)");
                }
                handler.setChannelKey(channelKey.trim());
                builder.addHandler(key, handler);
            } else if (contract instanceof ProcessEmbedded) {
                builder.setEmbedded((ProcessEmbedded) contract);
            } else if (contract instanceof MarkerContract) {
                builder.addMarker(key, (MarkerContract) contract);
            }
        }

        return builder.build();
    }

    private String normalizeContractKey(String key, String scopePath) {
        if (key == null || key.trim().isEmpty()) {
            throw new IllegalStateException("Contract key must not be blank at scope " + scopePath);
        }
        return key.trim();
    }

    private void normalizePointerBackedContracts(Contract contract, String key, String scopePath) {
        if (contract instanceof DocumentUpdateChannel) {
            DocumentUpdateChannel channel = (DocumentUpdateChannel) contract;
            channel.setPath(normalizeContractPointer(channel.getPath(),
                    "DocumentUpdateChannel",
                    key,
                    scopePath,
                    "path",
                    true));
            return;
        }
        if (contract instanceof EmbeddedNodeChannel) {
            EmbeddedNodeChannel channel = (EmbeddedNodeChannel) contract;
            channel.setChildPath(normalizeContractPointer(channel.getChildPath(),
                    "EmbeddedNodeChannel",
                    key,
                    scopePath,
                    "childPath",
                    true));
        }
    }

    private String normalizeContractPointer(String pointer,
                                            String contractType,
                                            String key,
                                            String scopePath,
                                            String fieldName,
                                            boolean required) {
        if (pointer == null) {
            if (required) {
                throw new IllegalStateException(contractType + " '" + key + "' at scope " + scopePath
                        + " is missing required " + fieldName);
            }
            return pointer;
        }
        String trimmed = pointer.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalStateException(contractType + " '" + key + "' at scope " + scopePath
                    + " has blank " + fieldName);
        }
        try {
            return PointerUtils.normalizeRequiredPointer(trimmed, fieldName);
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(contractType + " '" + key + "' at scope " + scopePath
                    + " has invalid " + fieldName + ": " + pointer, ex);
        }
    }
}
