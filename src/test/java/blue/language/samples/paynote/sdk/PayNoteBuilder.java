package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.BlueDocumentBuilder;
import blue.language.samples.paynote.dsl.ChannelKey;
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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PayNoteBuilder {

    private final BlueDocumentBuilder document;
    private final Map<String, String> participantTypeByKey = new LinkedHashMap<String, String>();
    private IsoCurrency configuredCurrency;
    private boolean captureLockedOnInit;
    private int captureResolutionPaths;

    private PayNoteBuilder(String name) {
        this.document = BlueDocDsl.document(PayNoteV2Types.PayNoteDocument.class)
                .name(name);
        ensureImplicitParticipants();
    }

    public static PayNoteBuilder payNote(String name) {
        return new PayNoteBuilder(name);
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

    public PayNoteBuilder referenceTransactionPath(String path) {
        if (path == null || path.trim().isEmpty()) {
            throw new IllegalArgumentException("reference transaction path is required");
        }
        document.putValue("referenceTransactionPath", path);
        return this;
    }

    public PayNoteBuilder referenceTransaction(Node referenceTransaction) {
        if (referenceTransaction == null) {
            throw new IllegalArgumentException("reference transaction node is required");
        }
        document.putNode("referenceTransaction", referenceTransaction);
        return this;
    }

    public PayNoteBuilder participant(String channelKey) {
        return participant(channelKey, null, null);
    }

    public PayNoteBuilder participant(String channelKey, String description) {
        return participant(channelKey, description, null);
    }

    public PayNoteBuilder participant(String channelKey, String description, Node channel) {
        if (channelKey == null || channelKey.trim().isEmpty()) {
            throw new IllegalArgumentException("participant channel key is required");
        }
        String nextTypeAlias = channel == null
                ? TypeAliases.CONVERSATION_TIMELINE_CHANNEL
                : channel.getAsText("/type/value");
        String existingTypeAlias = participantTypeByKey.get(channelKey);
        if (existingTypeAlias != null && !isTypeCompatible(existingTypeAlias, nextTypeAlias)) {
            throw new IllegalArgumentException("Participant override is not type-compatible for channel: " + channelKey
                    + " (" + existingTypeAlias + " -> " + nextTypeAlias + ")");
        }

        if (channel == null) {
            document.contracts(c -> c.timelineChannel(channelKey));
        } else {
            document.contracts(c -> c.putRaw(channelKey, channel));
        }
        participantTypeByKey.put(channelKey, nextTypeAlias);
        if (description != null && !description.trim().isEmpty()) {
            document.putNode("participantLabels", new Node().properties(channelKey, new Node().value(description)));
        }
        return this;
    }

    public PayNoteBuilder participants(String... channelKeys) {
        if (channelKeys == null) {
            return this;
        }
        for (String key : channelKeys) {
            participant(key);
        }
        return this;
    }

    public PayNoteBuilder participantsUnion(String compositeChannelKey, String... channelKeys) {
        return participantsUnion(ChannelKey.of(compositeChannelKey), channelKeys);
    }

    public PayNoteBuilder participantsUnion(ChannelKey compositeChannelKey, String... channelKeys) {
        document.contracts(c -> c.compositeTimelineChannel(compositeChannelKey.value(), channelKeys));
        return this;
    }

    public CaptureBuilder capture() {
        return new CaptureBuilder(this);
    }

    public PayNoteBuilder captureLockedUntilOperation(String operationKey,
                                                      String channelKey,
                                                      String description,
                                                      Class<?> emittedEventType) {
        return capture()
                .lockOnInit()
                .unlockOnOperation(operationKey, op -> op
                        .channel(channelKey)
                        .description(description)
                        .steps(steps -> {
                            if (emittedEventType != null) {
                                steps.emitType("EmitUnlockSignal", emittedEventType, null);
                            }
                        }))
                .done();
    }

    public PayNoteBuilder captureLockedUntilEvent(Class<?> eventTypeClass) {
        return capture()
                .lockOnInit()
                .unlockOnEvent(eventTypeClass)
                .done();
    }

    public PayNoteBuilder captureLockedUntilDocPathChanges(String path) {
        return capture()
                .lockOnInit()
                .unlockOnDocPathChange(path)
                .done();
    }

    public PayNoteBuilder reserveOnInit() {
        onInit("onInitReserve", steps -> steps.triggerEvent("ReserveFundsRequested",
                PayNoteEvents.reserveFundsRequested(new Node().value(
                        BlueDocDsl.expr("document('/amount/total')")))));
        return this;
    }

    public PayNoteBuilder reserveAndCaptureImmediatelyOnInit() {
        onInit("onInitReserveAndCapture", steps -> steps
                .triggerEvent("ReserveAndCaptureImmediately",
                        PayNoteEvents.reserveFundsAndCaptureImmediatelyRequested(new Node().value(
                                BlueDocDsl.expr("document('/amount/total')")))));
        return this;
    }

    public PayNoteBuilder reserveLockedUntilOperation(String operationKey,
                                                      String channelKey,
                                                      String description,
                                                      Class<?> emittedEventType) {
        return reserveOnOperation(operationKey, channelKey, description, emittedEventType);
    }

    public PayNoteBuilder reserveLockedUntilEvent(Class<?> eventTypeClass) {
        return onEvent("reserveWhen" + sanitizeKey(eventTypeClass.getSimpleName()),
                eventTypeClass,
                steps -> steps.triggerEvent("ReserveFundsRequested",
                        PayNoteEvents.reserveFundsRequested(new Node().value(
                                BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilder reserveLockedUntilDocPathChanges(String path) {
        return onDocChange("reserveOnDocChange" + sanitizeKey(path),
                path,
                steps -> steps.triggerEvent("ReserveFundsRequested",
                        PayNoteEvents.reserveFundsRequested(new Node().value(
                                BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilder reserveOnOperation(String operationKey,
                                             String channelKey,
                                             String description) {
        return reserveOnOperation(operationKey, channelKey, description, null);
    }

    public PayNoteBuilder reserveOnOperation(String operationKey,
                                             String channelKey,
                                             String description,
                                             Class<?> emittedEventType) {
        return operation(operationKey, channelKey, description, steps -> {
            if (emittedEventType != null) {
                steps.emitType("EmitReserveSignal", emittedEventType, null);
            }
            steps.triggerEvent("ReserveFundsRequested",
                    PayNoteEvents.reserveFundsRequested(new Node().value(
                            BlueDocDsl.expr("document('/amount/total')"))));
        });
    }

    public PayNoteBuilder refundLockedUntilOperation(String operationKey,
                                                     String channelKey,
                                                     String description,
                                                     Class<?> emittedEventType) {
        return refundOnOperation(operationKey, channelKey, description, emittedEventType);
    }

    public PayNoteBuilder refundLockedUntilEvent(Class<?> eventTypeClass) {
        return onEvent("refundWhen" + sanitizeKey(eventTypeClass.getSimpleName()),
                eventTypeClass,
                steps -> steps.capture().refundFull());
    }

    public PayNoteBuilder refundLockedUntilDocPathChanges(String path) {
        return onDocChange("refundOnDocChange" + sanitizeKey(path),
                path,
                steps -> steps.capture().refundFull());
    }

    public PayNoteBuilder refundOnOperation(String operationKey,
                                            String channelKey,
                                            String description) {
        return refundOnOperation(operationKey, channelKey, description, null);
    }

    public PayNoteBuilder refundOnOperation(String operationKey,
                                            String channelKey,
                                            String description,
                                            Class<?> emittedEventType) {
        return operation(operationKey, channelKey, description, steps -> {
            if (emittedEventType != null) {
                steps.emitType("EmitRefundSignal", emittedEventType, null);
            }
            steps.capture().refundFull();
        });
    }

    public PayNoteBuilder operation(String key,
                                    String channelKey,
                                    String description,
                                    Consumer<StepsBuilder> implementation) {
        return operation(key, channelKey, null, description, implementation);
    }

    public PayNoteBuilder operation(String key,
                                    String channelKey,
                                    Class<?> requestTypeClass,
                                    String description,
                                    Consumer<StepsBuilder> implementation) {
        ensureParticipantChannel(channelKey);
        document.contracts(c -> {
            if (requestTypeClass != null) {
                c.operation(key, channelKey, requestTypeClass, description);
            } else {
                c.operation(key, channelKey, description);
            }
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

    public PayNoteBuilder allowListDirectChange(String operationName,
                                                String channelKey,
                                                String description,
                                                String... allowedPaths) {
        return directChangeWithAllowList(operationName, channelKey, description, allowedPaths);
    }

    public PayNoteBuilder captureOnEvent(Class<?> triggerEventType, String workflowKey) {
        return captureOnEvent(triggerEventType, workflowKey, null);
    }

    public PayNoteBuilder captureOnEvent(Class<?> triggerEventType,
                                         String workflowKey,
                                         Consumer<StepsBuilder> captureHook) {
        onEvent(workflowKey, triggerEventType, steps -> {
            steps.capture().requestNow();
            if (captureHook != null) {
                captureHook.accept(steps);
            }
        });
        return this;
    }

    public PayNoteBuilder refundFullOperation(String channelKey) {
        return operation("refundFull",
                channelKey,
                "Request full reservation release.",
                steps -> steps.capture().refundFull());
    }

    public PayNoteBuilder refundPartialOperation(String operationKey,
                                                 String channelKey,
                                                 String amountExpression) {
        return operation(operationKey,
                channelKey,
                "Request partial refund.",
                steps -> steps.triggerEvent("RequestPartialRefund",
                        PayNoteEvents.reservationReleaseRequested(new Node().value(BlueDocDsl.expr(amountExpression)))));
    }

    public PayNoteBuilder releaseReservationOperation(String operationKey, String channelKey) {
        return operation(operationKey,
                channelKey,
                "Release reservation.",
                steps -> steps.triggerEvent("ReleaseReservation", PayNoteEvents.reservationReleaseRequested(
                        new Node().value(BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilder releaseOperation(String operationKey, String channelKey) {
        return releaseReservationOperation(operationKey, channelKey);
    }

    public PayNoteBuilder requestCancellationOperation(String channelKey) {
        return operation("requestCancellation",
                channelKey,
                "Request paynote cancellation.",
                steps -> steps.replaceValue("SetCancellationRequested", "/status", "cancellation-requested"));
    }

    public PayNoteBuilder cancelOperation(String channelKey) {
        return requestCancellationOperation(channelKey);
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

    public PayNoteBuilder onRefunded(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onReleased(workflowKey, customizer);
    }

    public PayNoteBuilder onInit(String workflowKey, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle(workflowKey, "initLifecycleChannel", customizer);
        });
        return this;
    }

    public PayNoteBuilder onEvent(String workflowKey, Class<?> eventTypeClass, Consumer<StepsBuilder> customizer) {
        document.contracts(c -> c.onTriggered(workflowKey, eventTypeClass, customizer));
        return this;
    }

    public PayNoteBuilder onEventEmit(String workflowKey,
                                      Class<?> triggerEventTypeClass,
                                      Class<?> emittedEventTypeClass) {
        return onEvent(workflowKey,
                triggerEventTypeClass,
                steps -> steps.emitType("EmitEventAction", emittedEventTypeClass, null));
    }

    public PayNoteBuilder onDocChange(String workflowKey, String path, Consumer<StepsBuilder> customizer) {
        String channelKey = workflowKey + "Channel";
        document.contracts(c -> {
            c.documentUpdateChannel(channelKey, path);
            c.sequentialWorkflow(workflowKey,
                    channelKey,
                    new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE),
                    customizer);
        });
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

    public PayNoteBuilder withGuarantorStateOps() {
        operation("guarantorUpdateReserved",
                "guarantorChannel",
                Integer.class,
                "Guarantor updates reserved amount.",
                steps -> steps.replaceExpression("UpdateReserved", "/amount/reserved", "event.message.request"));
        operation("guarantorUpdateCaptured",
                "guarantorChannel",
                Integer.class,
                "Guarantor updates captured amount.",
                steps -> steps.replaceExpression("UpdateCaptured", "/amount/captured", "event.message.request"));
        operation("guarantorUpdateRefunded",
                "guarantorChannel",
                Integer.class,
                "Guarantor updates refunded amount.",
                steps -> steps.replaceExpression("UpdateRefunded", "/amount/refunded", "event.message.request"));
        operation("guarantorUpdateStatus",
                "guarantorChannel",
                String.class,
                "Guarantor updates paynote status.",
                steps -> steps.replaceExpression("UpdateStatus", "/status", "event.message.request"));
        return this;
    }

    public Node buildDocument() {
        validateCapturePlan();
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

    private void ensureImplicitParticipants() {
        document.contracts(c -> c.timelineChannels(
                "payerChannel",
                "payeeChannel",
                "guarantorChannel"));
        participantTypeByKey.put("payerChannel", TypeAliases.CONVERSATION_TIMELINE_CHANNEL);
        participantTypeByKey.put("payeeChannel", TypeAliases.CONVERSATION_TIMELINE_CHANNEL);
        participantTypeByKey.put("guarantorChannel", TypeAliases.CONVERSATION_TIMELINE_CHANNEL);
    }

    private void validateCapturePlan() {
        if (captureLockedOnInit && captureResolutionPaths == 0) {
            throw new IllegalStateException("Capture is locked but no unlock/request-capture resolution path is configured.");
        }
    }

    private static String sanitizeKey(String key) {
        if (key == null || key.trim().isEmpty()) {
            return "Default";
        }
        return key.replaceAll("[^a-zA-Z0-9]", "");
    }

    private boolean isTypeCompatible(String existingAlias, String nextAlias) {
        if (existingAlias == null || nextAlias == null) {
            return false;
        }
        if (existingAlias.equals(nextAlias)) {
            return true;
        }
        if (TypeAliases.CORE_CHANNEL.equals(existingAlias)) {
            return TypeAliases.CONVERSATION_TIMELINE_CHANNEL.equals(nextAlias)
                    || TypeAliases.MYOS_TIMELINE_CHANNEL.equals(nextAlias);
        }
        if (TypeAliases.CONVERSATION_TIMELINE_CHANNEL.equals(existingAlias)) {
            return TypeAliases.MYOS_TIMELINE_CHANNEL.equals(nextAlias);
        }
        return false;
    }

    private void ensureParticipantChannel(String channelKey) {
        if (channelKey == null || channelKey.trim().isEmpty()) {
            throw new IllegalArgumentException("operation channel key is required");
        }
        if (!participantTypeByKey.containsKey(channelKey)) {
            participant(channelKey);
        }
    }

    public static final class CaptureBuilder {
        private final PayNoteBuilder parent;

        private CaptureBuilder(PayNoteBuilder parent) {
            this.parent = parent;
        }

        public CaptureBuilder lockOnInit() {
            parent.captureLockedOnInit = true;
            parent.onInit("onInitCaptureLock", steps -> steps.capture().lock());
            return this;
        }

        public CaptureBuilder lockOnEvent(Class<?> eventTypeClass) {
            return onEventAction("lockCaptureWhen", eventTypeClass, steps -> steps.capture().lock(), false);
        }

        public CaptureBuilder lockOnOperation(String operationKey, Consumer<CaptureOperationBuilder> customizer) {
            return onOperationAction(operationKey, customizer, steps -> steps.capture().lock(), false);
        }

        public CaptureBuilder lockOnDocPathChange(String path) {
            return onDocPathAction("lockCaptureOnDocChange", path, steps -> steps.capture().lock(), false);
        }

        public CaptureBuilder unlockOnEvent(Class<?> eventTypeClass) {
            return onEventAction("unlockCaptureWhen", eventTypeClass, steps -> steps.capture().unlock(), true);
        }

        public CaptureBuilder unlockOnOperation(String operationKey, Consumer<CaptureOperationBuilder> customizer) {
            return onOperationAction(operationKey, customizer, steps -> steps.capture().unlock(), true);
        }

        public CaptureBuilder unlockOnDocPathChange(String path) {
            return onDocPathAction("unlockCaptureOnDocChange", path, steps -> steps.capture().unlock(), true);
        }

        public CaptureBuilder requestOnInit() {
            parent.captureResolutionPaths++;
            parent.onInit("onInitCaptureRequest", steps -> steps.capture().requestNow());
            return this;
        }

        public CaptureBuilder requestOnEvent(Class<?> eventTypeClass) {
            return onEventAction("requestCaptureWhen", eventTypeClass, steps -> steps.capture().requestNow(), true);
        }

        public CaptureBuilder requestOnOperation(String operationKey, Consumer<CaptureOperationBuilder> customizer) {
            return onOperationAction(operationKey, customizer, steps -> steps.capture().requestNow(), true);
        }

        public CaptureBuilder requestOnDocPathChange(String path) {
            return onDocPathAction("requestCaptureOnDocChange", path, steps -> steps.capture().requestNow(), true);
        }

        public CaptureBuilder requestPartialOnEvent(Class<?> eventTypeClass, String amountExpression) {
            String expression = requireAmountExpression(amountExpression);
            return onEventAction("requestPartialCaptureWhen",
                    eventTypeClass,
                    steps -> steps.capture().requestPartial(expression),
                    true);
        }

        public CaptureBuilder requestPartialOnOperation(String operationKey,
                                                        String amountExpression,
                                                        Consumer<CaptureOperationBuilder> customizer) {
            String expression = requireAmountExpression(amountExpression);
            return onOperationAction(operationKey,
                    customizer,
                    steps -> steps.capture().requestPartial(expression),
                    true);
        }

        public CaptureBuilder requestPartialOnDocPathChange(String path, String amountExpression) {
            String expression = requireAmountExpression(amountExpression);
            return onDocPathAction("requestPartialCaptureOnDocChange",
                    path,
                    steps -> steps.capture().requestPartial(expression),
                    true);
        }

        public CaptureBuilder refundOnEvent(Class<?> eventTypeClass) {
            return onEventAction("refundWhen", eventTypeClass, steps -> steps.capture().refundFull(), true);
        }

        public CaptureBuilder refundOnOperation(String operationKey, Consumer<CaptureOperationBuilder> customizer) {
            return onOperationAction(operationKey, customizer, steps -> steps.capture().refundFull(), true);
        }

        public CaptureBuilder refundOnDocPathChange(String path) {
            return onDocPathAction("refundOnDocChange", path, steps -> steps.capture().refundFull(), true);
        }

        public CaptureBuilder refundPartialOnEvent(Class<?> eventTypeClass, String amountExpression) {
            String expression = requireAmountExpression(amountExpression);
            return onEventAction("refundPartialWhen",
                    eventTypeClass,
                    partialRefundAction(expression),
                    true);
        }

        public CaptureBuilder refundPartialOnOperation(String operationKey,
                                                       String amountExpression,
                                                       Consumer<CaptureOperationBuilder> customizer) {
            String expression = requireAmountExpression(amountExpression);
            return onOperationAction(operationKey, customizer, partialRefundAction(expression), true);
        }

        public CaptureBuilder refundPartialOnDocPathChange(String path, String amountExpression) {
            String expression = requireAmountExpression(amountExpression);
            return onDocPathAction("refundPartialOnDocChange", path, partialRefundAction(expression), true);
        }

        public PayNoteBuilder done() {
            return parent;
        }

        private CaptureBuilder onEventAction(String workflowPrefix,
                                             Class<?> eventTypeClass,
                                             Consumer<StepsBuilder> action,
                                             boolean captureResolutionPath) {
            if (captureResolutionPath) {
                parent.captureResolutionPaths++;
            }
            parent.onEvent(workflowPrefix + sanitizeKey(eventTypeClass.getSimpleName()), eventTypeClass, action);
            return this;
        }

        private CaptureBuilder onDocPathAction(String workflowPrefix,
                                               String path,
                                               Consumer<StepsBuilder> action,
                                               boolean captureResolutionPath) {
            if (captureResolutionPath) {
                parent.captureResolutionPaths++;
            }
            parent.onDocChange(workflowPrefix + sanitizeKey(path), path, action);
            return this;
        }

        private CaptureBuilder onOperationAction(String operationKey,
                                                 Consumer<CaptureOperationBuilder> customizer,
                                                 Consumer<StepsBuilder> action,
                                                 boolean captureResolutionPath) {
            if (captureResolutionPath) {
                parent.captureResolutionPaths++;
            }
            CaptureOperationBuilder operationBuilder = new CaptureOperationBuilder(parent, operationKey);
            if (customizer != null) {
                customizer.accept(operationBuilder);
            }
            operationBuilder.doneWithAction(action);
            return this;
        }

        private Consumer<StepsBuilder> partialRefundAction(String amountExpression) {
            return steps -> steps.triggerEvent("RequestPartialRefund",
                    PayNoteEvents.reservationReleaseRequested(new Node().value(BlueDocDsl.expr(amountExpression))));
        }

        private String requireAmountExpression(String amountExpression) {
            if (amountExpression == null || amountExpression.trim().isEmpty()) {
                throw new IllegalArgumentException("amountExpression is required");
            }
            return amountExpression.trim();
        }
    }

    public static final class OperationBuilder {
        private final PayNoteBuilder parent;
        private final String key;
        private String channelKey;
        private String description;
        private Class<?> requestTypeClass;
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

        public OperationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public OperationBuilder requestType(Class<?> requestTypeClass) {
            this.requestTypeClass = requestTypeClass;
            return this;
        }

        public OperationBuilder noRequest() {
            this.requestTypeClass = null;
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
            return parent.operation(key, channelKey, requestTypeClass, description, implementation);
        }
    }

    public static final class CaptureOperationBuilder {
        private final PayNoteBuilder parent;
        private final String key;
        private String channelKey;
        private String description;
        private Class<?> requestTypeClass;
        private Consumer<StepsBuilder> implementation;

        private CaptureOperationBuilder(PayNoteBuilder parent, String key) {
            this.parent = parent;
            this.key = key;
        }

        public CaptureOperationBuilder channel(String channelKey) {
            this.channelKey = channelKey;
            return this;
        }

        public CaptureOperationBuilder description(String description) {
            this.description = description;
            return this;
        }

        public CaptureOperationBuilder requestType(Class<?> requestTypeClass) {
            this.requestTypeClass = requestTypeClass;
            return this;
        }

        public CaptureOperationBuilder noRequest() {
            this.requestTypeClass = null;
            return this;
        }

        public CaptureOperationBuilder steps(Consumer<StepsBuilder> implementation) {
            this.implementation = implementation;
            return this;
        }

        private PayNoteBuilder doneWithAction(Consumer<StepsBuilder> action) {
            if (channelKey == null || channelKey.trim().isEmpty()) {
                throw new IllegalStateException("Operation channel must be configured for: " + key);
            }
            Consumer<StepsBuilder> withAction = steps -> {
                if (implementation != null) {
                    implementation.accept(steps);
                }
                action.accept(steps);
            };
            return parent.operation(key, channelKey, requestTypeClass, description, withAction);
        }
    }
}
