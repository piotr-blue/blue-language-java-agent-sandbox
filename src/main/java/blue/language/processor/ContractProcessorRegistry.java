package blue.language.processor;

import blue.language.model.TypeBlueId;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.Contract;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.MarkerContract;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Maintains the mapping between contract BlueIds and their processors.
 */
public class ContractProcessorRegistry {

    private final Map<String, ContractProcessor<? extends Contract>> processorsByBlueId = new LinkedHashMap<>();
    private final Map<Class<? extends HandlerContract>, HandlerProcessor<? extends HandlerContract>> handlerProcessors = new LinkedHashMap<>();
    private final Map<Class<? extends ChannelContract>, ChannelProcessor<? extends ChannelContract>> channelProcessors = new LinkedHashMap<>();
    private final Map<Class<? extends MarkerContract>, ContractProcessor<? extends MarkerContract>> markerProcessors = new LinkedHashMap<>();

    public <T extends HandlerContract> void registerHandler(HandlerProcessor<T> processor) {
        Objects.requireNonNull(processor, "processor");
        registerBlueIds(processor.contractType(), processor);
        handlerProcessors.put(processor.contractType(), processor);
    }

    public <T extends ChannelContract> void registerChannel(ChannelProcessor<T> processor) {
        Objects.requireNonNull(processor, "processor");
        registerBlueIds(processor.contractType(), processor);
        channelProcessors.put(processor.contractType(), processor);
    }

    public <T extends MarkerContract> void registerMarker(ContractProcessor<T> processor) {
        Objects.requireNonNull(processor, "processor");
        registerBlueIds(processor.contractType(), processor);
        markerProcessors.put(processor.contractType(), processor);
    }

    public void register(ContractProcessor<? extends Contract> processor) {
        Objects.requireNonNull(processor, "processor");
        if (processor instanceof HandlerProcessor) {
            @SuppressWarnings("unchecked")
            HandlerProcessor<? extends HandlerContract> handler = (HandlerProcessor<? extends HandlerContract>) processor;
            registerHandler(handler);
        } else if (processor instanceof ChannelProcessor) {
            @SuppressWarnings("unchecked")
            ChannelProcessor<? extends ChannelContract> channel = (ChannelProcessor<? extends ChannelContract>) processor;
            registerChannel(channel);
        } else if (processor.contractType() != null && MarkerContract.class.isAssignableFrom(processor.contractType())) {
            @SuppressWarnings("unchecked")
            ContractProcessor<? extends MarkerContract> marker = (ContractProcessor<? extends MarkerContract>) processor;
            registerMarker(marker);
        } else {
            throw new IllegalArgumentException("Unsupported processor type: " + processor.getClass().getName());
        }
    }

    public Optional<HandlerProcessor<? extends HandlerContract>> lookupHandler(Class<? extends HandlerContract> type) {
        return Optional.ofNullable(resolveProcessor(type, handlerProcessors));
    }

    public Optional<ChannelProcessor<? extends ChannelContract>> lookupChannel(Class<? extends ChannelContract> type) {
        return Optional.ofNullable(resolveProcessor(type, channelProcessors));
    }

    public Optional<ContractProcessor<? extends MarkerContract>> lookupMarker(Class<? extends MarkerContract> type) {
        return Optional.ofNullable(resolveProcessor(type, markerProcessors));
    }

    public Map<String, ContractProcessor<? extends Contract>> processors() {
        return Collections.unmodifiableMap(processorsByBlueId);
    }

    private <T extends Contract> void registerBlueIds(Class<T> contractType, ContractProcessor<T> processor) {
        Objects.requireNonNull(contractType, "contractType");

        TypeBlueId typeBlueId = contractType.getAnnotation(TypeBlueId.class);
        if (typeBlueId == null) {
            throw new IllegalArgumentException("Contract type lacks @TypeBlueId: " + contractType.getName());
        }

        String[] declared = typeBlueId.value();
        if (declared.length == 0 && !typeBlueId.defaultValue().isEmpty()) {
            declared = new String[]{typeBlueId.defaultValue()};
        }
        if (declared.length == 0) {
            throw new IllegalArgumentException("Contract type " + contractType.getName() + " does not declare any BlueId values");
        }

        for (String blueId : declared) {
            processorsByBlueId.put(blueId, processor);
        }
    }

    private <T extends Contract, P extends ContractProcessor<? extends T>> P resolveProcessor(
            Class<? extends T> requestedType,
            Map<Class<? extends T>, P> processorsByType) {
        Objects.requireNonNull(requestedType, "requestedType");

        P exact = processorsByType.get(requestedType);
        if (exact != null) {
            return exact;
        }

        Class<? extends T> bestType = null;
        P bestProcessor = null;
        for (Map.Entry<Class<? extends T>, P> entry : processorsByType.entrySet()) {
            Class<? extends T> candidateType = entry.getKey();
            if (!candidateType.isAssignableFrom(requestedType)) {
                continue;
            }
            if (bestType == null || bestType.isAssignableFrom(candidateType)) {
                bestType = candidateType;
                bestProcessor = entry.getValue();
                continue;
            }
            if (!candidateType.isAssignableFrom(bestType) &&
                    candidateType.getName().compareTo(bestType.getName()) < 0) {
                bestType = candidateType;
                bestProcessor = entry.getValue();
            }
        }
        return bestProcessor;
    }
}
