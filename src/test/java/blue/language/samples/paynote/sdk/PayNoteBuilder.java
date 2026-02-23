package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.BlueDocumentBuilder;
import blue.language.samples.paynote.dsl.ChannelKey;
import blue.language.samples.paynote.dsl.DocPath;
import blue.language.samples.paynote.dsl.DocTemplate;
import blue.language.samples.paynote.dsl.DocTemplates;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.MyOsBootstrapBuilder;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteEvents;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.samples.paynote.types.paynote.PayNoteV2Types;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PayNoteBuilder {

    private final BlueDocumentBuilder document;
    private DocPath cardTransactionDetailsPath;
    private IsoCurrency configuredCurrency;

    private PayNoteBuilder(String name) {
        this.document = BlueDocDsl.document(PayNoteV2Types.PayNoteDocument.class)
                .name(name)
                .putNode("amount", new Node().properties("total", new Node().value(0)));
        ensureImplicitParticipants();
    }

    public static PayNoteBuilder payNote(String name) {
        return new PayNoteBuilder(name);
    }

    public PayNoteBuilder attach(CardTransaction rail) {
        if (rail == null) {
            throw new IllegalArgumentException("card transaction rail is required");
        }
        this.cardTransactionDetailsPath = rail.detailsPath();
        return this;
    }

    public PayNoteBuilder attach(BankTransfer rail) {
        if (rail == null) {
            throw new IllegalArgumentException("bank transfer rail is required");
        }
        document.putValue("bankTransferDetailsPath", rail.detailsPath().pointer());
        return this;
    }

    public PayNoteBuilder currency(IsoCurrency currency) {
        this.configuredCurrency = currency;
        document.putValue("currency", currency.code());
        return this;
    }

    public PayNoteBuilder amountTotalMinor(long amountMinor) {
        document.putNode("amount", new Node().properties("total", new Node().value(amountMinor)));
        return this;
    }

    public PayNoteBuilder amountTotalMajor(BigDecimal amountMajor) {
        if (configuredCurrency == null) {
            throw new IllegalStateException("currency(...) must be set before amountTotalMajor(...)");
        }
        return money(Money.ofMajor(configuredCurrency, amountMajor));
    }

    public PayNoteBuilder amountTotalMajor(String amountMajor) {
        return amountTotalMajor(new BigDecimal(amountMajor));
    }

    public PayNoteBuilder money(Money money) {
        currency(money.currency());
        return amountTotalMinor(money.minor());
    }

    public PayNoteBuilder participant(PayNoteRole role) {
        if (role == null) {
            return this;
        }
        document.contracts(c -> c.timelineChannel(role.channelKey().value()));
        return this;
    }

    public PayNoteBuilder participants(Consumer<ParticipantsBuilder> customizer) {
        customizer.accept(new ParticipantsBuilder(this));
        return this;
    }

    public PayNoteBuilder participantsUnion(String compositeChannelKey, PayNoteRole... roles) {
        return participantsUnion(ChannelKey.of(compositeChannelKey), roles);
    }

    public PayNoteBuilder participantsUnion(ChannelKey compositeChannelKey, PayNoteRole... roles) {
        List<String> channels = new ArrayList<String>();
        if (roles != null) {
            for (PayNoteRole role : roles) {
                if (role != null) {
                    channels.add(role.channelKey().value());
                }
            }
        }
        document.contracts(c -> c.compositeTimelineChannel(
                compositeChannelKey.value(),
                channels.toArray(new String[0])));
        return this;
    }

    public CardCaptureBuilder cardCapture() {
        return new CardCaptureBuilder(this);
    }

    public PayNoteBuilder reserveOnInit() {
        document.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitReserve", "initLifecycleChannel", steps -> steps
                    .triggerEvent("ReserveFundsRequested",
                            PayNoteEvents.reserveFundsRequested(new Node().value(
                                    BlueDocDsl.expr("document('/amount/total')")))));
        });
        return this;
    }

    public PayNoteBuilder reserveAndCaptureImmediatelyOnInit() {
        document.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitReserveAndCapture", "initLifecycleChannel", steps -> steps
                    .triggerEvent("ReserveAndCaptureImmediately",
                            PayNoteEvents.reserveFundsAndCaptureImmediatelyRequested(new Node().value(
                                    BlueDocDsl.expr("document('/amount/total')")))));
        });
        return this;
    }

    public PayNoteBuilder operation(String key,
                                    String channelKey,
                                    String description,
                                    Consumer<StepsBuilder> implementation) {
        document.contracts(c -> {
            c.operation(key, channelKey, description);
            c.implementOperation(key + "Impl", key, implementation);
        });
        return this;
    }

    public OperationBuilder operation(String key) {
        return new OperationBuilder(this, key);
    }

    public PayNoteBuilder directChangeWithAllowList(String operationName,
                                                    String channelKey,
                                                    String description,
                                                    String... allowedPaths) {
        operation(operationName, channelKey, description, steps -> steps
                .js("CollectChangeset", BlueDocDsl.js(js -> js
                        .readRequest("request")
                        .returnOutput(JsOutputBuilder.output()
                                .changesetRaw("request.changeset ?? []")
                                .emptyEvents())))
                .updateDocumentFromExpression("ApplyChangeset", "steps.CollectChangeset.changeset"));

        document.policies(p -> p
                .contractsChangePolicy("allow-listed-direct-change", "operation constrained by explicit allow list")
                .changesetAllowList(operationName, allowedPaths));
        return this;
    }

    public PayNoteBuilder directChangeWithAllowList(String operationName,
                                                    PayNoteRole role,
                                                    String description,
                                                    String... allowedPaths) {
        return directChangeWithAllowList(operationName, role.channelKey().value(), description, allowedPaths);
    }

    public PayNoteBuilder captureOnEvent(Class<?> triggerEventType, String workflowKey) {
        return captureOnEvent(triggerEventType, workflowKey, null);
    }

    public PayNoteBuilder captureOnEvent(Class<?> triggerEventType,
                                         String workflowKey,
                                         Consumer<StepsBuilder> captureHook) {
        document.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
            steps.triggerEvent("RequestCapture", PayNoteEvents.captureFundsRequested(
                    new Node().value(BlueDocDsl.expr("document('/amount/total')"))));
            if (captureHook != null) {
                captureHook.accept(steps);
            }
        }));
        return this;
    }

    public PayNoteBuilder refundFullOperation(String channelKey) {
        return operation("refundFull",
                channelKey,
                "Request full reservation release.",
                steps -> steps.triggerEvent("RequestReservationRelease", PayNoteEvents.reservationReleaseRequested(
                        new Node().value(BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilder refundFullOperation(PayNoteRole role) {
        return refundFullOperation(role.channelKey().value());
    }

    public PayNoteBuilder releaseOperation(String operationKey, String channelKey) {
        return operation(operationKey,
                channelKey,
                "Release reservation.",
                steps -> steps.triggerEvent("ReleaseReservation", PayNoteEvents.reservationReleaseRequested(
                        new Node().value(BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilder releaseOperation(String operationKey, PayNoteRole role) {
        return releaseOperation(operationKey, role.channelKey().value());
    }

    public PayNoteBuilder requestCancellationOperation(String channelKey) {
        return operation("requestCancellation",
                channelKey,
                "Request paynote cancellation.",
                steps -> steps.replaceValue("SetCancellationRequested", "/status", "cancellation-requested"));
    }

    public PayNoteBuilder requestCancellationOperation(PayNoteRole role) {
        return requestCancellationOperation(role.channelKey().value());
    }

    public PayNoteBuilder issueChildPayNoteOnEvent(String workflowKey,
                                                   Class<?> triggerEventType,
                                                   String childPointerExpression) {
        document.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> steps
                .emitType("IssueChildPayNote", PayNoteTypes.IssueChildPayNoteRequested.class,
                        payload -> payload.putExpression("childPayNote", childPointerExpression))));
        return this;
    }

    public PayNoteBuilder onFundsReserved(String workflowKey, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.FundsReserved.class, customizer));
        return this;
    }

    public PayNoteBuilder onCaptureRequested(String workflowKey, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.CaptureFundsRequested.class, customizer));
        return this;
    }

    public PayNoteBuilder onFundsCaptured(String workflowKey, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.FundsCaptured.class, customizer));
        return this;
    }

    public PayNoteBuilder onReleased(String workflowKey, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.ReservationReleased.class, customizer));
        return this;
    }

    public PayNoteBuilder once(String workflowKey,
                               Class<?> triggerEventType,
                               String guardPath,
                               Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
            steps.js("OnceGuard", BlueDocDsl.js(js -> js
                    .constVar("alreadyDone", "document('" + guardPath + "') === true")
                    .ifBlock("alreadyDone", done -> done.returnOutput(JsOutputBuilder.output().emptyEvents()
                            .changesetRaw("[]")))
                    .returnOutput(JsOutputBuilder.output()
                            .changesetRaw(JsPatchBuilder.patch().replaceValue(guardPath, "true").build())
                            .emptyEvents())));
            steps.updateDocumentFromExpression("PersistOnceGuard", "steps.OnceGuard.changeset");
            customizer.accept(steps);
        }));
        return this;
    }

    public PayNoteBuilder barrier(String workflowKey,
                                  Class<?> triggerEventType,
                                  String statePath,
                                  String signalExpression,
                                  List<String> requiredSignals,
                                  Consumer<StepsBuilder> onSatisfied) {
        document.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
            steps.js("CollectBarrierSignal", BlueDocDsl.js(js -> js
                    .constVar("signal", signalExpression)
                    .constVar("state", "document('" + statePath + "') ?? {}")
                    .constVar("nextState", "{ ...state, [signal]: true }")
                    .constVar("requiredSignals", "'" + String.join(",", requiredSignals) + "'.split(',')")
                    .constVar("allSatisfied", "requiredSignals.every(s => nextState[s] === true)")
                    .returnOutput(JsOutputBuilder.output()
                            .changesetRaw(JsPatchBuilder.patch().replaceValue(statePath, "nextState").build())
                            .eventsArray(JsArrayBuilder.array()
                                    .itemRaw("...(allSatisfied ? [" + JsObjectBuilder.object()
                                            .propString("type", TypeAliases.COMMON_NAMED_EVENT)
                                            .propString("name", "barrier-satisfied")
                                            .build() + "] : [])")))));
            steps.updateDocumentFromExpression("PersistBarrierState", "steps.CollectBarrierSignal.changeset");
            onSatisfied.accept(steps);
        }));
        return this;
    }

    public PayNoteBuilder initialStateDescription(String summary, String details) {
        document.putNode("payNoteInitialStateDescription", new Node()
                .properties("summary", new Node().value(summary))
                .properties("details", new Node().value(details)));
        return this;
    }

    public Node buildDocument() {
        return document.build();
    }

    public Node build() {
        return buildDocument();
    }

    public MyOsBootstrapBuilder bootstrap() {
        return MyOsDsl.bootstrap(buildDocument());
    }

    public DocTemplate template() {
        return DocTemplates.template(bootstrap().build());
    }

    public static List<String> signals(String... values) {
        List<String> out = new ArrayList<String>();
        if (values != null) {
            for (String value : values) {
                out.add(value);
            }
        }
        return out;
    }

    private PayNoteBuilder lockCardCaptureOnInit(DocPath cardDetailsPath) {
        document.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitLockCardCapture", "initLifecycleChannel", steps -> steps
                    .emitType("RequestCaptureLock", PayNoteTypes.CardTransactionCaptureLockRequested.class,
                            payload -> payload.putExpression("cardTransactionDetails",
                                    "document('" + cardDetailsPath.pointer() + "')")));
        });
        return this;
    }

    private PayNoteBuilder unlockCardCaptureWhen(Class<?> eventTypeClass, DocPath cardDetailsPath) {
        document.contracts(c -> c.onTriggered("unlockCardCaptureWhenEvent", eventTypeClass, steps -> steps
                .emitType("RequestCaptureUnlock", PayNoteTypes.CardTransactionCaptureUnlockRequested.class,
                        payload -> payload.putExpression("cardTransactionDetails",
                                "document('" + cardDetailsPath.pointer() + "')"))));
        return this;
    }

    private PayNoteBuilder guarantorConfirmCaptureLockedOp() {
        return operation("confirmCardTransactionCaptureLocked",
                PayNoteRole.GUARANTOR.channelKey().value(),
                "Confirm card transaction capture lock.",
                steps -> steps.emitType("ConfirmCaptureLocked", PayNoteTypes.CardTransactionCaptureLocked.class, null));
    }

    private PayNoteBuilder guarantorConfirmCaptureUnlockedOp() {
        return operation("confirmCardTransactionCaptureUnlocked",
                PayNoteRole.GUARANTOR.channelKey().value(),
                "Confirm card transaction capture unlock.",
                steps -> steps.emitType("ConfirmCaptureUnlocked", PayNoteTypes.CardTransactionCaptureUnlocked.class, null));
    }

    private void ensureImplicitParticipants() {
        document.contracts(c -> c.timelineChannels(
                PayNoteRole.PAYER.channelKey().value(),
                PayNoteRole.PAYEE.channelKey().value(),
                PayNoteRole.GUARANTOR.channelKey().value()));
    }

    private DocPath requireAttachedCardPath() {
        if (cardTransactionDetailsPath == null) {
            throw new IllegalStateException("No card transaction rail attached. Use attach(CardTransaction.at(...)) first.");
        }
        return cardTransactionDetailsPath;
    }

    public static final class ParticipantsBuilder {
        private final PayNoteBuilder parent;

        private ParticipantsBuilder(PayNoteBuilder parent) {
            this.parent = parent;
        }

        public ParticipantsBuilder add(PayNoteRole role) {
            parent.participant(role);
            return this;
        }

        public ParticipantsBuilder shipper() {
            return add(PayNoteRole.SHIPPER);
        }
    }

    public static final class CardCaptureBuilder {
        private final PayNoteBuilder parent;

        private CardCaptureBuilder(PayNoteBuilder parent) {
            this.parent = parent;
        }

        public PayNoteBuilder lockOnInit() {
            return parent.lockCardCaptureOnInit(parent.requireAttachedCardPath());
        }

        public PayNoteBuilder lockOnInit(DocPath path) {
            return parent.lockCardCaptureOnInit(path);
        }

        public PayNoteBuilder unlockWhen(Class<?> eventTypeClass) {
            return parent.unlockCardCaptureWhen(eventTypeClass, parent.requireAttachedCardPath());
        }

        public PayNoteBuilder unlockWhen(Class<?> eventTypeClass, DocPath path) {
            return parent.unlockCardCaptureWhen(eventTypeClass, path);
        }

        public PayNoteBuilder guarantorConfirmCaptureLockedOp() {
            return parent.guarantorConfirmCaptureLockedOp();
        }

        public PayNoteBuilder guarantorConfirmCaptureUnlockedOp() {
            return parent.guarantorConfirmCaptureUnlockedOp();
        }
    }

    public static final class OperationBuilder {
        private final PayNoteBuilder parent;
        private final String key;
        private String channelKey;
        private String description;
        private Consumer<StepsBuilder> implementation;

        private OperationBuilder(PayNoteBuilder parent, String key) {
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

        public OperationBuilder channel(PayNoteRole role) {
            this.channelKey = role.channelKey().value();
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

        public PayNoteBuilder done() {
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
