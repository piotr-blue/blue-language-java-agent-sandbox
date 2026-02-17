package blue.language.processor;

import blue.language.model.TypeBlueId;
import blue.language.processor.model.ChannelContract;
import blue.language.processor.model.HandlerContract;
import blue.language.processor.model.MarkerContract;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertSame;

class ContractProcessorRegistryTest {

    @Test
    void lookupFallsBackToAssignableHandlerProcessor() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseHandlerProcessor baseProcessor = new BaseHandlerProcessor();
        registry.registerHandler(baseProcessor);

        assertSame(baseProcessor, registry.lookupHandler(DerivedHandlerContract.class).orElse(null));
    }

    @Test
    void lookupPrefersMostSpecificAssignableHandlerProcessor() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseHandlerProcessor baseProcessor = new BaseHandlerProcessor();
        DerivedHandlerProcessor derivedProcessor = new DerivedHandlerProcessor();
        registry.registerHandler(baseProcessor);
        registry.registerHandler(derivedProcessor);

        assertSame(derivedProcessor, registry.lookupHandler(MostSpecificHandlerContract.class).orElse(null));
    }

    @Test
    void lookupPrefersMostSpecificAssignableChannelProcessor() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseChannelProcessor baseProcessor = new BaseChannelProcessor();
        DerivedChannelProcessor derivedProcessor = new DerivedChannelProcessor();
        registry.registerChannel(baseProcessor);
        registry.registerChannel(derivedProcessor);

        assertSame(derivedProcessor, registry.lookupChannel(MostSpecificChannelContract.class).orElse(null));
    }

    @Test
    void lookupFallsBackToAssignableMarkerProcessor() {
        ContractProcessorRegistry registry = new ContractProcessorRegistry();
        BaseMarkerProcessor baseProcessor = new BaseMarkerProcessor();
        registry.registerMarker(baseProcessor);

        assertSame(baseProcessor, registry.lookupMarker(DerivedMarkerContract.class).orElse(null));
    }

    @TypeBlueId("BaseHandlerContract")
    static class BaseHandlerContract extends HandlerContract {
    }

    @TypeBlueId("DerivedHandlerContract")
    static class DerivedHandlerContract extends BaseHandlerContract {
    }

    @TypeBlueId("MostSpecificHandlerContract")
    static class MostSpecificHandlerContract extends DerivedHandlerContract {
    }

    @TypeBlueId("BaseChannelContract")
    static class BaseChannelContract extends ChannelContract {
    }

    @TypeBlueId("DerivedChannelContract")
    static class DerivedChannelContract extends BaseChannelContract {
    }

    @TypeBlueId("MostSpecificChannelContract")
    static class MostSpecificChannelContract extends DerivedChannelContract {
    }

    @TypeBlueId("BaseMarkerContract")
    static class BaseMarkerContract extends MarkerContract {
    }

    @TypeBlueId("DerivedMarkerContract")
    static class DerivedMarkerContract extends BaseMarkerContract {
    }

    static class BaseHandlerProcessor implements HandlerProcessor<BaseHandlerContract> {
        @Override
        public Class<BaseHandlerContract> contractType() {
            return BaseHandlerContract.class;
        }

        @Override
        public void execute(BaseHandlerContract contract, ProcessorExecutionContext context) {
            // no-op test processor
        }
    }

    static class DerivedHandlerProcessor implements HandlerProcessor<DerivedHandlerContract> {
        @Override
        public Class<DerivedHandlerContract> contractType() {
            return DerivedHandlerContract.class;
        }

        @Override
        public void execute(DerivedHandlerContract contract, ProcessorExecutionContext context) {
            // no-op test processor
        }
    }

    static class BaseChannelProcessor implements ChannelProcessor<BaseChannelContract> {
        @Override
        public Class<BaseChannelContract> contractType() {
            return BaseChannelContract.class;
        }

        @Override
        public boolean matches(BaseChannelContract contract, ChannelEvaluationContext context) {
            return false;
        }
    }

    static class DerivedChannelProcessor implements ChannelProcessor<DerivedChannelContract> {
        @Override
        public Class<DerivedChannelContract> contractType() {
            return DerivedChannelContract.class;
        }

        @Override
        public boolean matches(DerivedChannelContract contract, ChannelEvaluationContext context) {
            return false;
        }
    }

    static class BaseMarkerProcessor implements ContractProcessor<BaseMarkerContract> {
        @Override
        public Class<BaseMarkerContract> contractType() {
            return BaseMarkerContract.class;
        }
    }
}
