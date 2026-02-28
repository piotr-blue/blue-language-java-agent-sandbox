package blue.language.sdk.internal;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.sdk.MyOsSteps;
import blue.language.types.common.NamedEvent;
import blue.language.types.conversation.DocumentBootstrapRequested;
import blue.language.types.payments.PaymentRequests;
import blue.language.types.payments.fields.AchPaymentFields;
import blue.language.types.payments.fields.CardPaymentFields;
import blue.language.types.payments.fields.CardTokenPaymentFields;
import blue.language.types.payments.fields.CreditLinePaymentFields;
import blue.language.types.payments.fields.CryptoPaymentFields;
import blue.language.types.payments.fields.LedgerPaymentFields;
import blue.language.types.payments.fields.SepaPaymentFields;
import blue.language.types.payments.fields.WirePaymentFields;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Function;

public final class StepsBuilder {

    private static final Blue BLUE = new Blue();

    private final List<Node> steps = new ArrayList<Node>();

    public StepsBuilder jsRaw(String name, String code) {
        Node step = new Node().type(TypeAliases.CONVERSATION_JAVASCRIPT_CODE);
        if (name != null) {
            step.name(name);
        }
        step.properties("code", new Node().value(code));
        steps.add(step);
        return this;
    }

    public StepsBuilder updateDocument(String name, Consumer<ChangesetBuilder> customizer) {
        ChangesetBuilder changesetBuilder = new ChangesetBuilder();
        customizer.accept(changesetBuilder);

        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().items(changesetBuilder.build()));
        steps.add(step);
        return this;
    }

    public StepsBuilder updateDocumentFromExpression(String name, String expression) {
        Node step = new Node().type(TypeAliases.CONVERSATION_UPDATE_DOCUMENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("changeset", new Node().value(expr(expression)));
        steps.add(step);
        return this;
    }

    public StepsBuilder triggerEvent(String name, Node event) {
        Node step = new Node().type(TypeAliases.CONVERSATION_TRIGGER_EVENT);
        if (name != null) {
            step.name(name);
        }
        step.properties("event", event);
        steps.add(step);
        return this;
    }

    public StepsBuilder emit(String name, Object typedEvent) {
        if (typedEvent == null) {
            throw new IllegalArgumentException("typedEvent cannot be null");
        }
        return triggerEvent(name, BLUE.objectToNode(typedEvent));
    }

    public StepsBuilder emit(Object typedEvent) {
        return emit(null, typedEvent);
    }

    public StepsBuilder emitType(String name, Class<?> eventTypeClass, Consumer<NodeObjectBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(eventTypeClass).asTypeNode());
        if (payloadCustomizer != null) {
            NodeObjectBuilder builder = NodeObjectBuilder.create();
            payloadCustomizer.accept(builder);
            Node payload = builder.build();
            if (payload.getProperties() != null) {
                for (Map.Entry<String, Node> entry : payload.getProperties().entrySet()) {
                    event.properties(entry.getKey(), entry.getValue());
                }
            }
        }
        return triggerEvent(name, event);
    }

    public StepsBuilder emitType(Class<?> eventTypeClass, Consumer<NodeObjectBuilder> payloadCustomizer) {
        return emitType(null, eventTypeClass, payloadCustomizer);
    }

    public StepsBuilder emitType(Class<?> eventTypeClass) {
        return emitType(null, eventTypeClass, null);
    }

    public StepsBuilder emitAdHocEvent(String name, String eventName, Consumer<NodeObjectBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(NamedEvent.class).asTypeNode());
        event.properties("name", new Node().value(eventName));
        if (payloadCustomizer != null) {
            NodeObjectBuilder payloadBuilder = NodeObjectBuilder.create();
            payloadCustomizer.accept(payloadBuilder);
            event.properties("payload", payloadBuilder.build());
        }
        return triggerEvent(name, event);
    }

    public StepsBuilder namedEvent(String name, String eventName, Consumer<NodeObjectBuilder> payloadCustomizer) {
        return emitAdHocEvent(name, eventName, payloadCustomizer);
    }

    public StepsBuilder namedEvent(String name, String eventName) {
        return emitAdHocEvent(name, eventName, null);
    }

    public StepsBuilder triggerPayment(String name,
                                       Class<?> paymentEventTypeClass,
                                       Consumer<PaymentRequestPayloadBuilder> payloadCustomizer) {
        return emitPaymentRequest(name, paymentEventTypeClass, payloadCustomizer);
    }

    public StepsBuilder triggerPayment(Class<?> paymentEventTypeClass,
                                       Consumer<PaymentRequestPayloadBuilder> payloadCustomizer) {
        return triggerPayment(null, paymentEventTypeClass, payloadCustomizer);
    }

    public StepsBuilder bootstrapDocument(String stepName,
                                          Node document,
                                          Map<String, String> channelBindings) {
        return bootstrapDocument(stepName, document, channelBindings, null);
    }

    public StepsBuilder bootstrapDocument(String stepName,
                                          Node document,
                                          Map<String, String> channelBindings,
                                          Consumer<BootstrapOptionsBuilder> options) {
        if (document == null) {
            throw new IllegalArgumentException("document cannot be null");
        }
        return emitType(stepName, DocumentBootstrapRequested.class, payload -> {
            payload.putNode("document", document);
            payload.putStringMap("channelBindings", channelBindings);
            applyBootstrapOptions(payload, options);
        });
    }

    public StepsBuilder bootstrapDocumentExpr(String stepName,
                                              String documentExpression,
                                              Map<String, String> channelBindings,
                                              Consumer<BootstrapOptionsBuilder> options) {
        if (documentExpression == null || documentExpression.trim().isEmpty()) {
            throw new IllegalArgumentException("documentExpression cannot be blank");
        }
        return emitType(stepName, DocumentBootstrapRequested.class, payload -> {
            payload.putExpression("document", documentExpression);
            payload.putStringMap("channelBindings", channelBindings);
            applyBootstrapOptions(payload, options);
        });
    }

    public StepsBuilder requestBackwardPayment(String name,
                                               Consumer<PaymentRequestPayloadBuilder> payloadCustomizer) {
        return emitPaymentRequest(name, PaymentRequests.BackwardPaymentRequested.class, payloadCustomizer);
    }

    public StepsBuilder requestBackwardPayment(Consumer<PaymentRequestPayloadBuilder> payloadCustomizer) {
        return requestBackwardPayment(null, payloadCustomizer);
    }

    public StepsBuilder replaceValue(String name, String path, Object value) {
        return updateDocument(name, changeset -> changeset.replaceValue(path, value));
    }

    public StepsBuilder replaceExpression(String name, String path, String expression) {
        return updateDocument(name, changeset -> changeset.replaceExpression(path, expression));
    }

    public StepsBuilder raw(Node step) {
        steps.add(step);
        return this;
    }

    public CaptureStepBuilder capture() {
        return new CaptureStepBuilder(this);
    }

    public <E> E ext(Function<StepsBuilder, E> extensionFactory) {
        if (extensionFactory == null) {
            throw new IllegalArgumentException("extensionFactory cannot be null");
        }
        E extension = extensionFactory.apply(this);
        if (extension == null) {
            throw new IllegalArgumentException("extensionFactory cannot return null");
        }
        return extension;
    }

    public MyOsSteps myOs() {
        return ext(steps -> new MyOsSteps(steps, "myOsAdminChannel"));
    }

    public MyOsSteps myOs(String adminChannelKey) {
        return ext(steps -> new MyOsSteps(steps, adminChannelKey));
    }

    private StepsBuilder emitPaymentRequest(String name,
                                            Class<?> paymentEventTypeClass,
                                            Consumer<PaymentRequestPayloadBuilder> payloadCustomizer) {
        Node event = new Node().type(TypeRef.of(paymentEventTypeClass).asTypeNode());
        PaymentRequestPayloadBuilder payloadBuilder = new PaymentRequestPayloadBuilder();
        if (payloadCustomizer != null) {
            payloadCustomizer.accept(payloadBuilder);
            Node payload = payloadBuilder.build();
            if (payload.getProperties() != null) {
                for (Map.Entry<String, Node> entry : payload.getProperties().entrySet()) {
                    event.properties(entry.getKey(), entry.getValue());
                }
            }
        }

        String processor = payloadBuilder.processor();
        if (processor == null || processor.trim().isEmpty()) {
            throw new IllegalArgumentException("triggerPayment requires non-empty processor field");
        }
        return triggerEvent(name, event);
    }

    private static void applyBootstrapOptions(NodeObjectBuilder payload,
                                              Consumer<BootstrapOptionsBuilder> options) {
        if (options == null) {
            return;
        }
        BootstrapOptionsBuilder bootstrapOptions = new BootstrapOptionsBuilder();
        options.accept(bootstrapOptions);
        bootstrapOptions.applyTo(payload);
    }

    List<Node> build() {
        return steps;
    }

    public static final class PaymentRequestPayloadBuilder {
        private final Node payload = new Node();
        private String processor;

        public PaymentRequestPayloadBuilder processor(String processor) {
            this.processor = processor;
            payload.properties("processor", new Node().value(processor));
            return this;
        }

        public PaymentRequestPayloadBuilder payer(String payerReference) {
            payload.properties("payer", new Node().value(payerReference));
            return this;
        }

        public PaymentRequestPayloadBuilder payer(Node payer) {
            payload.properties("payer", payer);
            return this;
        }

        public PaymentRequestPayloadBuilder payee(String payeeReference) {
            payload.properties("payee", new Node().value(payeeReference));
            return this;
        }

        public PaymentRequestPayloadBuilder payee(Node payee) {
            payload.properties("payee", payee);
            return this;
        }

        public PaymentRequestPayloadBuilder from(String fromReference) {
            payload.properties("from", new Node().value(fromReference));
            return this;
        }

        public PaymentRequestPayloadBuilder from(Node from) {
            payload.properties("from", from);
            return this;
        }

        public PaymentRequestPayloadBuilder to(String toReference) {
            payload.properties("to", new Node().value(toReference));
            return this;
        }

        public PaymentRequestPayloadBuilder to(Node to) {
            payload.properties("to", to);
            return this;
        }

        public PaymentRequestPayloadBuilder currency(String currency) {
            payload.properties("currency", new Node().value(currency));
            return this;
        }

        public PaymentRequestPayloadBuilder amountMinor(long amountMinor) {
            payload.properties("amountMinor", new Node().value(amountMinor));
            return this;
        }

        public PaymentRequestPayloadBuilder amountMinorExpression(String amountMinorExpression) {
            payload.properties("amountMinor", new Node().value(expr(amountMinorExpression)));
            return this;
        }

        public PaymentRequestPayloadBuilder attachPayNote(Node payNote) {
            payload.properties("attachedPayNote", payNote);
            return this;
        }

        public PaymentRequestPayloadBuilder reason(String reason) {
            payload.properties("reason", new Node().value(reason));
            return this;
        }

        public AchRailBuilder viaAch() {
            return new AchRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaAch(AchPaymentFields fields) {
            return rail(fields);
        }

        public SepaRailBuilder viaSepa() {
            return new SepaRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaSepa(SepaPaymentFields fields) {
            return rail(fields);
        }

        public WireRailBuilder viaWire() {
            return new WireRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaWire(WirePaymentFields fields) {
            return rail(fields);
        }

        public CardRailBuilder viaCard() {
            return new CardRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaCard(CardPaymentFields fields) {
            return rail(fields);
        }

        public CardTokenRailBuilder viaTokenizedCard() {
            return new CardTokenRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaTokenizedCard(CardTokenPaymentFields fields) {
            return rail(fields);
        }

        public CreditLineRailBuilder viaCreditLine() {
            return new CreditLineRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaCreditLine(CreditLinePaymentFields fields) {
            return rail(fields);
        }

        public LedgerRailBuilder viaLedger() {
            return new LedgerRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaLedger(LedgerPaymentFields fields) {
            return rail(fields);
        }

        public CryptoRailBuilder viaCrypto() {
            return new CryptoRailBuilder(this);
        }

        public PaymentRequestPayloadBuilder viaCrypto(CryptoPaymentFields fields) {
            return rail(fields);
        }

        @Deprecated
        public AchRailBuilder ach() {
            return viaAch();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder ach(AchPaymentFields fields) {
            return viaAch(fields);
        }

        @Deprecated
        public SepaRailBuilder sepa() {
            return viaSepa();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder sepa(SepaPaymentFields fields) {
            return viaSepa(fields);
        }

        @Deprecated
        public WireRailBuilder wire() {
            return viaWire();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder wire(WirePaymentFields fields) {
            return viaWire(fields);
        }

        @Deprecated
        public CardRailBuilder card() {
            return viaCard();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder card(CardPaymentFields fields) {
            return viaCard(fields);
        }

        @Deprecated
        public CardTokenRailBuilder tokenizedCard() {
            return viaTokenizedCard();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder tokenizedCard(CardTokenPaymentFields fields) {
            return viaTokenizedCard(fields);
        }

        @Deprecated
        public CreditLineRailBuilder creditLine() {
            return viaCreditLine();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder creditLine(CreditLinePaymentFields fields) {
            return viaCreditLine(fields);
        }

        @Deprecated
        public LedgerRailBuilder ledger() {
            return viaLedger();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder ledger(LedgerPaymentFields fields) {
            return viaLedger(fields);
        }

        @Deprecated
        public CryptoRailBuilder crypto() {
            return viaCrypto();
        }

        @Deprecated
        public PaymentRequestPayloadBuilder crypto(CryptoPaymentFields fields) {
            return viaCrypto(fields);
        }

        public PaymentRequestPayloadBuilder rail(Object railFieldsBean) {
            if (railFieldsBean == null) {
                throw new IllegalArgumentException("railFieldsBean cannot be null");
            }
            Node railNode = BLUE.objectToNode(railFieldsBean);
            if (railNode.getProperties() == null) {
                return this;
            }
            for (Map.Entry<String, Node> entry : railNode.getProperties().entrySet()) {
                String key = entry.getKey();
                if ("type".equals(key)) {
                    continue;
                }
                if ("processor".equals(key)) {
                    throw new IllegalArgumentException("Use processor(...) to set processor");
                }
                payload.properties(key, entry.getValue());
            }
            return this;
        }

        public <E> E ext(Function<PaymentRequestPayloadBuilder, E> extensionFactory) {
            if (extensionFactory == null) {
                throw new IllegalArgumentException("extensionFactory cannot be null");
            }
            E extension = extensionFactory.apply(this);
            if (extension == null) {
                throw new IllegalArgumentException("extensionFactory cannot return null");
            }
            return extension;
        }

        public PaymentRequestPayloadBuilder putCustom(String key, Object value) {
            if ("processor".equals(key)) {
                throw new IllegalArgumentException("Use processor(...) to set processor");
            }
            if (value instanceof Node) {
                payload.properties(key, (Node) value);
            } else {
                payload.properties(key, new Node().value(value));
            }
            return this;
        }

        public PaymentRequestPayloadBuilder putCustomExpression(String key, String expression) {
            if ("processor".equals(key)) {
                throw new IllegalArgumentException("Use processor(...) to set processor");
            }
            payload.properties(key, new Node().value(expr(expression)));
            return this;
        }

        private Node build() {
            return payload;
        }

        private String processor() {
            return processor;
        }

        public static final class AchRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private AchRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public AchRailBuilder routingNumber(String value) {
                parent.putCustom("routingNumber", value);
                return this;
            }

            public AchRailBuilder accountNumber(String value) {
                parent.putCustom("accountNumber", value);
                return this;
            }

            public AchRailBuilder accountType(String value) {
                parent.putCustom("accountType", value);
                return this;
            }

            public AchRailBuilder network(String value) {
                parent.putCustom("network", value);
                return this;
            }

            public AchRailBuilder companyEntryDescription(String value) {
                parent.putCustom("companyEntryDescription", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class SepaRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private SepaRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public SepaRailBuilder ibanFrom(String value) {
                parent.putCustom("ibanFrom", value);
                return this;
            }

            public SepaRailBuilder ibanTo(String value) {
                parent.putCustom("ibanTo", value);
                return this;
            }

            public SepaRailBuilder bicTo(String value) {
                parent.putCustom("bicTo", value);
                return this;
            }

            public SepaRailBuilder remittanceInformation(String value) {
                parent.putCustom("remittanceInformation", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class WireRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private WireRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public WireRailBuilder bankSwift(String value) {
                parent.putCustom("bankSwift", value);
                return this;
            }

            public WireRailBuilder bankName(String value) {
                parent.putCustom("bankName", value);
                return this;
            }

            public WireRailBuilder accountNumber(String value) {
                parent.putCustom("accountNumber", value);
                return this;
            }

            public WireRailBuilder beneficiaryName(String value) {
                parent.putCustom("beneficiaryName", value);
                return this;
            }

            public WireRailBuilder beneficiaryAddress(String value) {
                parent.putCustom("beneficiaryAddress", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class CardRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private CardRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public CardRailBuilder cardOnFileRef(String value) {
                parent.putCustom("cardOnFileRef", value);
                return this;
            }

            public CardRailBuilder merchantDescriptor(String value) {
                parent.putCustom("merchantDescriptor", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class CardTokenRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private CardTokenRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public CardTokenRailBuilder networkToken(String value) {
                parent.putCustom("networkToken", value);
                return this;
            }

            public CardTokenRailBuilder tokenProvider(String value) {
                parent.putCustom("tokenProvider", value);
                return this;
            }

            public CardTokenRailBuilder cryptogram(String value) {
                parent.putCustom("cryptogram", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class CreditLineRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private CreditLineRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public CreditLineRailBuilder creditLineId(String value) {
                parent.putCustom("creditLineId", value);
                return this;
            }

            public CreditLineRailBuilder merchantAccountId(String value) {
                parent.putCustom("merchantAccountId", value);
                return this;
            }

            public CreditLineRailBuilder cardholderAccountId(String value) {
                parent.putCustom("cardholderAccountId", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class LedgerRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private LedgerRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public LedgerRailBuilder ledgerAccountFrom(String value) {
                parent.putCustom("ledgerAccountFrom", value);
                return this;
            }

            public LedgerRailBuilder ledgerAccountTo(String value) {
                parent.putCustom("ledgerAccountTo", value);
                return this;
            }

            public LedgerRailBuilder memo(String value) {
                parent.putCustom("memo", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }

        public static final class CryptoRailBuilder {
            private final PaymentRequestPayloadBuilder parent;

            private CryptoRailBuilder(PaymentRequestPayloadBuilder parent) {
                this.parent = parent;
            }

            public CryptoRailBuilder asset(String value) {
                parent.putCustom("asset", value);
                return this;
            }

            public CryptoRailBuilder chain(String value) {
                parent.putCustom("chain", value);
                return this;
            }

            public CryptoRailBuilder fromWalletRef(String value) {
                parent.putCustom("fromWalletRef", value);
                return this;
            }

            public CryptoRailBuilder toAddress(String value) {
                parent.putCustom("toAddress", value);
                return this;
            }

            public CryptoRailBuilder txPolicy(String value) {
                parent.putCustom("txPolicy", value);
                return this;
            }

            public PaymentRequestPayloadBuilder done() {
                return parent;
            }
        }
    }

    public static final class CaptureStepBuilder {
        private final StepsBuilder parent;

        private CaptureStepBuilder(StepsBuilder parent) {
            this.parent = parent;
        }

        public StepsBuilder lock() {
            return parent.triggerEvent("RequestCaptureLock", PayNoteEvents.captureLockRequested());
        }

        public StepsBuilder unlock() {
            return parent.triggerEvent("RequestCaptureUnlock", PayNoteEvents.captureUnlockRequested());
        }

        public StepsBuilder markLocked() {
            return parent.triggerEvent("CaptureLocked", PayNoteEvents.captureLocked());
        }

        public StepsBuilder markUnlocked() {
            return parent.triggerEvent("CaptureUnlocked", PayNoteEvents.captureUnlocked());
        }

        public StepsBuilder requestNow() {
            return parent.triggerEvent(
                    "RequestCapture",
                    PayNoteEvents.captureFundsRequested(new Node().value(expr("document('/amount/total')"))));
        }

        public StepsBuilder requestPartial(String amountExpression) {
            return parent.triggerEvent(
                    "RequestCapture",
                    PayNoteEvents.captureFundsRequested(new Node().value(expr(amountExpression))));
        }

        public StepsBuilder releaseFull() {
            return parent.triggerEvent(
                    "RequestRelease",
                    PayNoteEvents.reservationReleaseRequested(new Node().value(expr("document('/amount/total')"))));
        }
    }

    private static String expr(String expression) {
        if (expression == null) {
            return null;
        }
        String trimmed = expression.trim();
        if (trimmed.startsWith("${") && trimmed.endsWith("}")) {
            return trimmed;
        }
        return "${" + trimmed + "}";
    }
}
