package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.dsl.DocumentBuilder;
import blue.language.samples.paynote.dsl.MyOsBootstrapBuilder;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.StepsBuilder;

import java.util.function.Consumer;

public final class BlueDoc {

    private final DocumentBuilder bootstrap;

    private BlueDoc() {
        this.bootstrap = MyOsDsl.bootstrap();
    }

    public static BlueDoc doc() {
        return new BlueDoc();
    }

    public BlueDoc type(String documentTypeAlias) {
        bootstrap.documentType(documentTypeAlias);
        return this;
    }

    public BlueDoc name(String name) {
        bootstrap.documentName(name);
        return this;
    }

    public BlueDoc description(String description) {
        bootstrap.documentDescription(description);
        return this;
    }

    public BlueDoc participant(String channelKey) {
        bootstrap.contracts(c -> c.timelineChannel(channelKey));
        return this;
    }

    public BlueDoc participant(String channelKey, String label) {
        participant(channelKey);
        if (label != null && !label.trim().isEmpty()) {
            bootstrap.putDocumentObject("participantLabels", labels -> labels.put(channelKey, label));
        }
        return this;
    }

    public BlueDoc participants(String... channelKeys) {
        if (channelKeys == null) {
            return this;
        }
        for (String channelKey : channelKeys) {
            participant(channelKey);
        }
        return this;
    }

    public BlueDoc operation(String key,
                             String channelKey,
                             String description,
                             Consumer<StepsBuilder> implementation) {
        bootstrap.contracts(c -> {
            c.operation(key, channelKey, description);
            c.implementOperation(key + "Impl", key, implementation);
        });
        return this;
    }

    public OperationBuilder operation(String key) {
        return new OperationBuilder(this, key);
    }

    public Node buildDocument() {
        return bootstrap.build().getAsNode("/document").clone();
    }

    public Node buildBootstrap() {
        return bootstrap.build();
    }

    public MyOsBootstrapBuilder bootstrap() {
        return MyOsDsl.bootstrap(buildDocument());
    }

    public static final class OperationBuilder {
        private final BlueDoc parent;
        private final String key;
        private String channelKey;
        private String description;
        private Consumer<StepsBuilder> implementation;

        private OperationBuilder(BlueDoc parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        public OperationBuilder channel(String channelKey) {
            this.channelKey = channelKey;
            return this;
        }

        public OperationBuilder channel(ChannelKey channelKey) {
            this.channelKey = channelKey.value();
            return this;
        }

        public OperationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OperationBuilder steps(Consumer<StepsBuilder> implementation) {
            this.implementation = implementation;
            return this;
        }

        public BlueDoc done() {
            if (channelKey == null || channelKey.trim().isEmpty()) {
                throw new IllegalStateException("Operation channel must be configured for: " + key);
            }
            if (implementation == null) {
                throw new IllegalStateException("Operation steps must be configured for: " + key);
            }
            return parent.operation(key, channelKey, description, implementation);
        }
    }
}
