package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.PayNoteAliases;
import blue.language.samples.paynote.dsl.PayNoteEvents;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.dsl.TypeRef;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PayNoteBuilder extends DocBuilder<PayNoteBuilder> {

    private final Map<String, String> participantTypeByKey = new LinkedHashMap<String, String>();
    private IsoCurrency configuredCurrency;
    private boolean captureLockedOnInit;
    private int captureResolutionPaths;

    private PayNoteBuilder(String name) {
        super();
        type(PayNoteAliases.PAYNOTE);
        withName(name);
        ensureImplicitParticipants();
    }

    public static PayNoteBuilder payNote(String name) {
        return new PayNoteBuilder(name);
    }

    public PayNoteBuilder currency(IsoCurrency currency) {
        this.configuredCurrency = currency;
        set("/currency", currency.code());
        return this;
    }

    public PayNoteBuilder amountTotalMinor(long amountMinor) {
        set("/amount", new Node().properties("total", new Node().value(amountMinor)));
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
        set("/referenceTransactionPath", path);
        return this;
    }

    public PayNoteBuilder referenceTransaction(Node referenceTransaction) {
        if (referenceTransaction == null) {
            throw new IllegalArgumentException("reference transaction node is required");
        }
        set("/referenceTransaction", referenceTransaction);
        return this;
    }

    @Override
    public PayNoteBuilder participant(String channelKey, String description, Node channel) {
        if (channelKey == null || channelKey.trim().isEmpty()) {
            throw new IllegalArgumentException("participant channel key is required");
        }
        String normalizedChannelKey = channelKey.trim();
        String nextTypeAlias = channel == null
                ? TypeAliases.CONVERSATION_TIMELINE_CHANNEL
                : channel.getAsText("/type/value");
        String existingTypeAlias = participantTypeByKey.get(normalizedChannelKey);
        if (existingTypeAlias != null && !isTypeCompatible(existingTypeAlias, nextTypeAlias)) {
            throw new IllegalArgumentException("Participant override is not type-compatible for channel: " + normalizedChannelKey
                    + " (" + existingTypeAlias + " -> " + nextTypeAlias + ")");
        }
        participantTypeByKey.put(normalizedChannelKey, nextTypeAlias);
        return super.participant(normalizedChannelKey, description, channel);
    }

    public PayNoteBuilder acceptsEventsFrom(String participantName, Class<?>... allowedEventTypes) {
        String channelKey = toChannelKey(participantName);
        ensureParticipantChannel(channelKey);

        String participantKey = toParticipantKey(channelKey);
        String operationKey = participantKey + "EmitEvents";
        String implementationKey = operationKey + "Impl";
        String jsCode = buildAcceptEventsIngressJs(allowedEventTypes);

        contracts(c -> {
            c.operation(operationKey,
                    channelKey,
                    "Allow " + participantKey + " to emit events into this document.");
            c.implementOperation(implementationKey, operationKey, steps -> steps.jsRaw("EmitEvents", jsCode));
        });
        return this;
    }

    public CaptureBuilder capture() {
        return new CaptureBuilder(this);
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

    @Override
    protected void beforeOperation(String channelKey) {
        ensureParticipantChannel(channelKey);
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

    public PayNoteBuilder requestCancellationOperation(String channelKey) {
        return operation("requestCancellation",
                channelKey,
                "Request paynote cancellation.",
                steps -> steps.replaceValue("SetCancellationRequested", "/status", "cancellation-requested"));
    }

    public PayNoteBuilder issueChildPayNoteOnEvent(String workflowKey,
                                                   Class<?> triggerEventType,
                                                   String childPointerExpression) {
        contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> steps
                .emitType("IssueChildPayNote", PayNoteTypes.IssueChildPayNoteRequested.class,
                        payload -> payload.putExpression("childPayNote", childPointerExpression))));
        return this;
    }

    public PayNoteBuilder onFundsReserved(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onEvent(workflowKey, PayNoteTypes.FundsReserved.class, customizer);
    }

    public PayNoteBuilder onCaptureRequested(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onEvent(workflowKey, PayNoteTypes.CaptureFundsRequested.class, customizer);
    }

    public PayNoteBuilder onFundsCaptured(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onEvent(workflowKey, PayNoteTypes.FundsCaptured.class, customizer);
    }

    public PayNoteBuilder onReleased(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onEvent(workflowKey, PayNoteTypes.ReservationReleased.class, customizer);
    }

    public PayNoteBuilder onRefunded(String workflowKey, Consumer<StepsBuilder> customizer) {
        return onReleased(workflowKey, customizer);
    }

    public PayNoteBuilder onEventEmit(String workflowKey,
                                      Class<?> triggerEventTypeClass,
                                      Class<?> emittedEventTypeClass) {
        return onEvent(workflowKey,
                triggerEventTypeClass,
                steps -> steps.emitType("EmitEventAction", emittedEventTypeClass, null));
    }

    public PayNoteBuilder once(String workflowKey,
                               Class<?> triggerEventType,
                               String guardPath,
                               Consumer<StepsBuilder> customizer) {
        contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
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
        contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
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
        set("/payNoteInitialStateDescription", new Node()
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

    @Override
    public Node buildDocument() {
        validateCapturePlan();
        return super.buildDocument();
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
        contracts(c -> c.timelineChannels(
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
        String normalizedChannelKey = channelKey.trim();
        if (!participantTypeByKey.containsKey(normalizedChannelKey)) {
            participant(normalizedChannelKey);
        }
    }

    private String buildAcceptEventsIngressJs(Class<?>... allowedEventTypes) {
        if (allowedEventTypes == null || allowedEventTypes.length == 0) {
            return ""
                    + "const req = event.message.request;\n"
                    + "const events = Array.isArray(req) ? req : [req];\n"
                    + "return { events: events };";
        }
        List<String> aliases = new ArrayList<String>();
        for (Class<?> eventTypeClass : Arrays.asList(allowedEventTypes)) {
            aliases.add("'" + escapeJsString(TypeRef.of(eventTypeClass).alias()) + "'");
        }
        return ""
                + "const allowed = [" + String.join(", ", aliases) + "];\n"
                + "const req = event.message.request;\n"
                + "const events = (Array.isArray(req) ? req : [req])\n"
                + "  .filter(e => e && allowed.includes(e.type));\n"
                + "return { events: events };";
    }

    private String toChannelKey(String participantName) {
        if (participantName == null || participantName.trim().isEmpty()) {
            throw new IllegalArgumentException("participant name is required");
        }
        String key = participantName.trim();
        if (key.endsWith("Channel")) {
            return key;
        }
        return key + "Channel";
    }

    private String toParticipantKey(String channelKey) {
        if (channelKey.endsWith("Channel")) {
            return channelKey.substring(0, channelKey.length() - "Channel".length());
        }
        return channelKey;
    }

    private String escapeJsString(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("\\", "\\\\").replace("'", "\\'");
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
