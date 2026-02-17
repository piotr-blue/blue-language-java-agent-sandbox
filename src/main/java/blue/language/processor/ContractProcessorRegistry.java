package blue.language.processor;

import blue.language.model.BlueType;
import blue.language.model.TypeBlueId;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.Contract;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.MarkerContract;
import blue.language.utils.BlueIdResolver;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

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
        return lookupMostSpecificProcessor(handlerProcessors, type);
    }

    public Optional<ChannelProcessor<? extends ChannelContract>> lookupChannel(Class<? extends ChannelContract> type) {
        return lookupMostSpecificProcessor(channelProcessors, type);
    }

    public Optional<ContractProcessor<? extends MarkerContract>> lookupMarker(Class<? extends MarkerContract> type) {
        return lookupMostSpecificProcessor(markerProcessors, type);
    }

    public Map<String, ContractProcessor<? extends Contract>> processors() {
        return Collections.unmodifiableMap(processorsByBlueId);
    }

    private <T extends Contract> void registerBlueIds(Class<T> contractType, ContractProcessor<T> processor) {
        Objects.requireNonNull(contractType, "contractType");

        Set<String> declaredBlueIds = collectDeclaredBlueIds(contractType);
        if (declaredBlueIds.isEmpty()) {
            String resolved = BlueIdResolver.resolveBlueId(contractType);
            if (resolved != null && !resolved.trim().isEmpty()) {
                declaredBlueIds.add(resolved.trim());
            }
        }
        if (declaredBlueIds.isEmpty()) {
            throw new IllegalArgumentException("Contract type " + contractType.getName() + " does not declare any BlueId values");
        }

        for (String blueId : declaredBlueIds) {
            processorsByBlueId.put(blueId, processor);
        }
    }

    private Set<String> collectDeclaredBlueIds(Class<? extends Contract> contractType) {
        Set<String> result = new LinkedHashSet<String>();

        BlueType blueType = contractType.getAnnotation(BlueType.class);
        if (blueType != null) {
            addDeclaredBlueIds(result, blueType.value(), blueType.defaultValue());
        }

        TypeBlueId typeBlueId = contractType.getAnnotation(TypeBlueId.class);
        if (typeBlueId == null && blueType == null) {
            throw new IllegalArgumentException("Contract type lacks @TypeBlueId/@BlueType: " + contractType.getName());
        }
        if (typeBlueId != null) {
            addDeclaredBlueIds(result, typeBlueId.value(), typeBlueId.defaultValue());
        }
        return result;
    }

    private void addDeclaredBlueIds(Set<String> sink, String[] values, String defaultValue) {
        List<String> raw = new ArrayList<String>();
        if (values != null) {
            Collections.addAll(raw, values);
        }
        if ((values == null || values.length == 0) && defaultValue != null && !defaultValue.isEmpty()) {
            raw.add(defaultValue);
        }
        for (String candidate : raw) {
            if (candidate == null) {
                continue;
            }
            String trimmed = candidate.trim();
            if (!trimmed.isEmpty()) {
                sink.add(trimmed);
            }
        }
    }

    private <T extends Contract, P extends ContractProcessor<? extends T>> Optional<P> lookupMostSpecificProcessor(
            Map<Class<? extends T>, P> processorsByType,
            Class<? extends T> requestedType
    ) {
        if (requestedType == null) {
            return Optional.empty();
        }

        P exact = processorsByType.get(requestedType);
        if (exact != null) {
            return Optional.of(exact);
        }

        List<Class<? extends T>> candidates = new ArrayList<Class<? extends T>>();
        for (Class<? extends T> supportedType : processorsByType.keySet()) {
            if (supportedType.isAssignableFrom(requestedType)) {
                candidates.add(supportedType);
            }
        }

        if (candidates.isEmpty()) {
            return Optional.empty();
        }

        Class<? extends T> selected = selectMostSpecificCandidate(requestedType, candidates);
        return Optional.ofNullable(processorsByType.get(selected));
    }

    private <T extends Contract> Class<? extends T> selectMostSpecificCandidate(
            Class<? extends T> requestedType,
            List<Class<? extends T>> candidates
    ) {
        Class<? extends T> selected = null;
        for (Class<? extends T> candidate : candidates) {
            boolean subtypeOfAll = true;
            for (Class<? extends T> other : candidates) {
                if (candidate == other) {
                    continue;
                }
                if (!other.isAssignableFrom(candidate)) {
                    subtypeOfAll = false;
                    break;
                }
            }
            if (!subtypeOfAll) {
                continue;
            }
            if (selected != null && !selected.equals(candidate)) {
                throw new IllegalStateException("Ambiguous processor match for "
                        + requestedType.getName() + ": " + selected.getName() + " and " + candidate.getName());
            }
            selected = candidate;
        }

        if (selected != null) {
            return selected;
        }
        throw new IllegalStateException("No unique most-specific processor for "
                + requestedType.getName() + " among " + candidates);
    }
}
