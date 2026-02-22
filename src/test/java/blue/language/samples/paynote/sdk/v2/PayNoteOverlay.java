package blue.language.samples.paynote.sdk.v2;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.ChangesetBuilder;
import blue.language.samples.paynote.dsl.ContractsBuilder;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.samples.paynote.types.paynote.PayNoteV2Types;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

public final class PayNoteOverlay {

    private final Node document = BlueDocDsl.document(PayNoteV2Types.PayNoteDocument.class).build();
    private final Map<String, Node> contracts = new LinkedHashMap<String, Node>();

    private PayNoteOverlay(String name, int amount, String currency) {
        document.name(name);
        document.properties("amount", new Node().value(amount));
        document.properties("currency", new Node().value(currency));
        document.properties("status", new Node().value("draft"));
    }

    public static PayNoteOverlay payNote(String name, int amount, String currency) {
        return new PayNoteOverlay(name, amount, currency);
    }

    public PayNoteOverlay abstractParticipants() {
        contracts().channel("payerChannel", ConversationTypes.TimelineChannel.class);
        contracts().channel("payeeChannel", ConversationTypes.TimelineChannel.class);
        contracts().channel("guarantorChannel", ConversationTypes.TimelineChannel.class);
        return this;
    }

    public PayNoteOverlay reserve(int amount) {
        contracts().operation("reserve", "payerChannel", "Reserve funds for this PayNote");
        contracts().implementOperation("reserveImpl", "reserve", steps -> steps
                .emitType("EmitReserveRequested", PayNoteTypes.ReserveFundsRequested.class, payload -> payload.put("amount", amount))
                .replaceValue("MarkReservationRequested", "/reservationRequested", true)
                .replaceValue("SetStatusReserved", "/status", "reserved"));
        return this;
    }

    public PayNoteOverlay reserveAndCaptureImmediately(int amount) {
        contracts().operation("reserveAndCaptureNow", "payerChannel", "Reserve and capture instantly");
        contracts().implementOperation("reserveAndCaptureNowImpl", "reserveAndCaptureNow", steps -> steps
                .emitType("EmitReserveAndCaptureImmediately",
                        PayNoteTypes.ReserveFundsAndCaptureImmediatelyRequested.class,
                        payload -> payload.put("amount", amount))
                .replaceValue("SetStatusCaptured", "/status", "captured"));
        return this;
    }

    public PayNoteOverlay captureOnEvent(String workflowKey, Class<?> triggerEventType, int amount) {
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, triggerEventType, steps -> steps
                .emitType("EmitCaptureRequested", PayNoteTypes.CaptureFundsRequested.class, payload -> payload.put("amount", amount))
                .replaceValue("SetCaptureRequested", "/captureRequested", true));
        return this;
    }

    public PayNoteOverlay captureAfterTimer(String timerPath, int amount) {
        String channelKey = "timerChannel";
        contracts().documentUpdateChannel(channelKey, timerPath);
        contracts().sequentialWorkflow("captureAfterTimer", channelKey, new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE), steps -> steps
                .emitType("EmitCaptureFromTimer", PayNoteTypes.CaptureFundsRequested.class, payload -> payload.put("amount", amount))
                .replaceValue("SetCaptureRequested", "/captureRequested", true));
        return this;
    }

    public PayNoteOverlay refundFullOperation() {
        contracts().operation("refundFull", "payeeChannel", "Refund full amount");
        contracts().implementOperation("refundFullImpl", "refundFull", steps -> steps
                .emitType("EmitRefundRequested", PayNoteTypes.ReservationReleaseRequested.class,
                        payload -> payload.put("amount", BlueDocDsl.expr("document('/amount')")))
                .replaceValue("SetStatusRefundRequested", "/status", "refund-requested"));
        return this;
    }

    public PayNoteOverlay issueChildOnEvent(String workflowKey, Class<?> triggerEventType, String childPointer) {
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, triggerEventType, steps -> steps
                .emitType("IssueChildPayNote", PayNoteTypes.IssueChildPayNoteRequested.class,
                        payload -> payload.put("childPayNote", new Node().value(childPointer))));
        return this;
    }

    public PayNoteOverlay once(String workflowKey,
                               Class<?> triggerEventType,
                               String flagPath,
                               Consumer<StepsBuilder> guardedSteps) {
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, triggerEventType, steps -> {
            steps.js("OnceGuard", BlueDocDsl.js(js -> js
                    .constVar("alreadyDone", "document('" + flagPath + "') === true")
                    .ifBlock("alreadyDone", b -> b.returnOutput(JsOutputBuilder.output()
                            .eventsArray(JsArrayBuilder.array())
                            .changesetRaw("[]")))
                    .returnOutput(JsOutputBuilder.output()
                            .changesetRaw(JsPatchBuilder.patch().replaceValue(flagPath, "true").build())
                            .eventsRaw("[]"))));
            steps.updateDocumentFromExpression("PersistOnceFlag", "steps.OnceGuard.changeset");
            guardedSteps.accept(steps);
        });
        return this;
    }

    public PayNoteOverlay barrier(String workflowKey,
                                  Class<?> triggerEventType,
                                  String statePath,
                                  String signalKeyExpression,
                                  List<String> requiredSignals,
                                  Consumer<StepsBuilder> onSatisfied) {
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, triggerEventType, steps -> {
            steps.js("BarrierCollect", BlueDocDsl.js(js -> js
                    .constVar("signalKey", signalKeyExpression)
                    .constVar("state", "document('" + statePath + "') ?? {}")
                    .constVar("nextState", "{ ...state, [signalKey]: true }")
                    .constVar("requiredSignals", "'" + String.join(",", requiredSignals) + "'.split(',')")
                    .constVar("allSatisfied", "requiredSignals.every(s => nextState[s] === true)")
                    .returnOutput(JsOutputBuilder.output()
                            .changesetRaw(JsPatchBuilder.patch().replaceValue(statePath, "nextState").build())
                            .eventsRaw("allSatisfied ? [{ type: '" + TypeAliases.COMMON_NAMED_EVENT + "', name: 'barrier-satisfied' }] : []"))));
            steps.updateDocumentFromExpression("PersistBarrierState", "steps.BarrierCollect.changeset");
            onSatisfied.accept(steps);
        });
        return this;
    }

    public PayNoteOverlay onEvent(String workflowKey, Class<?> triggerEventType, Consumer<StepsBuilder> customizer) {
        ensureTriggeredChannel();
        contracts().onTriggered(workflowKey, triggerEventType, customizer);
        return this;
    }

    public PayNoteOverlay onChange(String workflowKey, String path, Consumer<StepsBuilder> customizer) {
        String channelKey = workflowKey + "Channel";
        contracts().documentUpdateChannel(channelKey, path);
        contracts().sequentialWorkflow(workflowKey, channelKey, new Node().type(TypeAliases.CORE_DOCUMENT_UPDATE), customizer);
        return this;
    }

    public PayNoteOverlay withOperation(String key,
                                        String channel,
                                        String description,
                                        Consumer<ChangesetBuilder> changesetCustomizer) {
        contracts().operation(key, channel, description);
        contracts().implementOperation(key + "Impl", key, steps -> steps
                .updateDocument("ApplyOperation", changesetCustomizer));
        return this;
    }

    public PayNoteOverlay applyStandardPayNotePolicies() {
        return abstractParticipants()
                .reserve(100)
                .refundFullOperation()
                .captureOnEvent("captureOnFundsReserved", PayNoteTypes.FundsReserved.class, 100);
    }

    public Node build() {
        if (!contracts.isEmpty()) {
            document.properties("contracts", new Node().properties(contracts));
        }
        return document;
    }

    private ContractsBuilder contracts() {
        return new ContractsBuilder(contracts);
    }

    private void ensureTriggeredChannel() {
        if (!contracts.containsKey("triggeredEventChannel")) {
            contracts().triggeredEventChannel("triggeredEventChannel");
        }
    }

    public static List<String> defaultBarrierSignals(String... signals) {
        return Arrays.asList(signals);
    }
}
