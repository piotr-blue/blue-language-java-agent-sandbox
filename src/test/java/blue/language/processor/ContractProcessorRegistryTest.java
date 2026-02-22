package blue.language.processor;

import blue.language.model.TypeBlueId;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.MarkerContract;
import blue.language.processor.model.MyOSTimelineChannel;
import blue.language.processor.registry.processors.TimelineChannelProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ContractProcessorRegistryTest {

    @Test
    void lookupByClassFallsBackToAssignableRegisteredProcessor() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseHandlerProcessor processor = new BaseHandlerProcessor();
        registry.registerHandler(processor);

        assertSame(processor, registry.lookupHandler(BaseHandler.class).orElse(null));
        assertSame(processor, registry.lookupHandler(DerivedHandler.class).orElse(null));
    }

    @Test
    void lookupByBlueIdAndContractInstanceUsesDeclaredTypeBlueId() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseHandlerProcessor processor = new BaseHandlerProcessor();
        registry.registerHandler(processor);

        assertSame(processor, registry.lookupHandler("BaseHandler").orElse(null));
        assertSame(processor, registry.lookupHandler("Core/Base Handler").orElse(null));
        assertSame(processor, registry.lookupHandler(new DerivedHandler()).orElse(null));
    }

    @Test
    void channelAndMarkerLookupsSupportBlueIdsAndAssignableTypes() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseChannelProcessor channelProcessor = new BaseChannelProcessor();
        BaseMarkerProcessor markerProcessor = new BaseMarkerProcessor();
        registry.registerChannel(channelProcessor);
        registry.registerMarker(markerProcessor);

        assertSame(channelProcessor, registry.lookupChannel(DerivedChannel.class).orElse(null));
        assertSame(channelProcessor, registry.lookupChannel("BaseChannel").orElse(null));
        assertSame(markerProcessor, registry.lookupMarker(DerivedMarker.class).orElse(null));
        assertSame(markerProcessor, registry.lookupMarker("BaseMarker").orElse(null));
        assertTrue(registry.processors().containsKey("BaseChannel"));
    }

    @Test
    void lookupChannelFallsBackToTimelineProcessorForMyOSTypeHierarchy() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        TimelineChannelProcessor timelineProcessor = new TimelineChannelProcessor();
        registry.registerChannel(timelineProcessor);

        MyOSTimelineChannel myosChannel = new MyOSTimelineChannel();
        assertSame(timelineProcessor, registry.lookupChannel(MyOSTimelineChannel.class).orElse(null));
        assertSame(timelineProcessor, registry.lookupChannel(myosChannel).orElse(null));
    }

    @TypeBlueId({"BaseHandler", "Core/Base Handler"})
    static class BaseHandler extends HandlerContract {
    }

    @TypeBlueId("DerivedHandler")
    static class DerivedHandler extends BaseHandler {
    }

    static final class BaseHandlerProcessor implements HandlerProcessor<BaseHandler> {
        @Override
        public Class<BaseHandler> contractType() {
            return BaseHandler.class;
        }

        @Override
        public void execute(BaseHandler contract, ProcessorExecutionContext context) {
            // no-op
        }
    }

    @TypeBlueId("BaseChannel")
    static class BaseChannel extends ChannelContract {
    }

    @TypeBlueId("DerivedChannel")
    static class DerivedChannel extends BaseChannel {
    }

    static final class BaseChannelProcessor implements ChannelProcessor<BaseChannel> {
        @Override
        public Class<BaseChannel> contractType() {
            return BaseChannel.class;
        }

        @Override
        public boolean matches(BaseChannel contract, ChannelEvaluationContext context) {
            return true;
        }
    }

    @TypeBlueId("BaseMarker")
    static class BaseMarker extends MarkerContract {
    }

    @TypeBlueId("DerivedMarker")
    static class DerivedMarker extends BaseMarker {
    }

    static final class BaseMarkerProcessor implements ContractProcessor<BaseMarker> {
        @Override
        public Class<BaseMarker> contractType() {
            return BaseMarker.class;
        }
    }
}
