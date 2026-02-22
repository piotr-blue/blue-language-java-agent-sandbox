package blue.language.samples.paynote.sdk.vnext;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.BlueDocDsl;
import blue.language.samples.paynote.dsl.DocumentBuilder;
import blue.language.samples.paynote.dsl.JsArrayBuilder;
import blue.language.samples.paynote.dsl.JsObjectBuilder;
import blue.language.samples.paynote.dsl.JsOutputBuilder;
import blue.language.samples.paynote.dsl.JsPatchBuilder;
import blue.language.samples.paynote.dsl.MyOsDsl;
import blue.language.samples.paynote.dsl.PayNoteEvents;
import blue.language.samples.paynote.dsl.StepsBuilder;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public final class PayNoteBuilderVNext {

    private final DocumentBuilder bootstrap;

    PayNoteBuilderVNext(String name, String typeAlias) {
        this.bootstrap = MyOsDsl.bootstrap()
                .documentName(name)
                .documentType(typeAlias)
                .putDocumentObject("amount", amount -> amount.put("total", 0));
    }

    public PayNoteBuilderVNext currency(String currency) {
        bootstrap.putDocumentValue("currency", currency);
        return this;
    }

    public PayNoteBuilderVNext amountTotal(int amountMinor) {
        bootstrap.putDocumentObject("amount", amount -> amount.put("total", amountMinor));
        return this;
    }

    public PayNoteBuilderVNext participants(Consumer<ParticipantsBuilder> customizer) {
        customizer.accept(new ParticipantsBuilder(this));
        return this;
    }

    public PayNoteBuilderVNext abstractParticipants(String... channelKeys) {
        bootstrap.contracts(c -> c.timelineChannels(channelKeys));
        return this;
    }

    public PayNoteBuilderVNext compositeParticipants(String compositeChannelKey, String... channels) {
        bootstrap.contracts(c -> c.compositeTimelineChannel(compositeChannelKey, channels));
        return this;
    }

    public PayNoteBuilderVNext reserveOnInit() {
        bootstrap.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitReserve", "initLifecycleChannel", steps -> steps
                    .triggerEvent("ReserveFundsRequested",
                            PayNoteEvents.reserveFundsRequested(new Node().value(
                                    BlueDocDsl.expr("document('/amount/total')")))));
        });
        return this;
    }

    public PayNoteBuilderVNext reserveAndCaptureImmediatelyOnInit() {
        bootstrap.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitReserveAndCapture", "initLifecycleChannel", steps -> steps
                    .triggerEvent("ReserveAndCaptureImmediately",
                            PayNoteEvents.reserveFundsAndCaptureImmediatelyRequested(new Node().value(
                                    BlueDocDsl.expr("document('/amount/total')")))));
        });
        return this;
    }

    public PayNoteBuilderVNext lockCardCaptureOnInit(String cardTransactionDetailsPath) {
        bootstrap.contracts(c -> {
            c.lifecycleEventChannel("initLifecycleChannel", TypeAliases.CORE_DOCUMENT_PROCESSING_INITIATED);
            c.onLifecycle("onInitLockCardCapture", "initLifecycleChannel", steps -> steps
                    .emitType("RequestCaptureLock", PayNoteTypes.CardTransactionCaptureLockRequested.class,
                            payload -> payload.putExpression("cardTransactionDetails",
                                    "document('" + cardTransactionDetailsPath + "')")));
        });
        return this;
    }

    public PayNoteBuilderVNext unlockCardCaptureWhen(Class<?> eventTypeClass, String cardTransactionDetailsPath) {
        bootstrap.contracts(c -> c.onTriggered("unlockCardCaptureWhenEvent", eventTypeClass, steps -> steps
                .emitType("RequestCaptureUnlock", PayNoteTypes.CardTransactionCaptureUnlockRequested.class,
                        payload -> payload.putExpression("cardTransactionDetails",
                                "document('" + cardTransactionDetailsPath + "')"))));
        return this;
    }

    public PayNoteBuilderVNext confirmLockOperation(String channelKey) {
        return operation("confirmCardTransactionCaptureLocked",
                channelKey,
                "Confirm card transaction capture lock.",
                steps -> steps.emitType("ConfirmCaptureLocked", PayNoteTypes.CardTransactionCaptureLocked.class, null));
    }

    public PayNoteBuilderVNext confirmUnlockOperation(String channelKey) {
        return operation("confirmCardTransactionCaptureUnlocked",
                channelKey,
                "Confirm card transaction capture unlock.",
                steps -> steps.emitType("ConfirmCaptureUnlocked", PayNoteTypes.CardTransactionCaptureUnlocked.class, null));
    }

    public PayNoteBuilderVNext operation(String key,
                                         String channelKey,
                                         String description,
                                         Consumer<StepsBuilder> implementation) {
        bootstrap.contracts(c -> {
            c.operation(key, channelKey, description);
            c.implementOperation(key + "Impl", key, implementation);
        });
        return this;
    }

    public PayNoteBuilderVNext directChangeWithAllowList(String operationName,
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

        bootstrap.policies(p -> p
                .contractsChangePolicy("allow-listed-direct-change", "operation constrained by explicit allow list")
                .changesetAllowList(operationName, allowedPaths));
        return this;
    }

    public PayNoteBuilderVNext captureOnEvent(Class<?> triggerEventType, String workflowKey) {
        return captureOnEvent(triggerEventType, workflowKey, null);
    }

    public PayNoteBuilderVNext captureOnEvent(Class<?> triggerEventType,
                                              String workflowKey,
                                              Consumer<StepsBuilder> captureHook) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
            steps.triggerEvent("RequestCapture", PayNoteEvents.captureFundsRequested(
                    new Node().value(BlueDocDsl.expr("document('/amount/total')"))));
            if (captureHook != null) {
                captureHook.accept(steps);
            }
        }));
        return this;
    }

    public PayNoteBuilderVNext refundFullOperation(String channelKey) {
        return operation("refundFull",
                channelKey,
                "Request full reservation release.",
                steps -> steps.triggerEvent("RequestReservationRelease", PayNoteEvents.reservationReleaseRequested(
                        new Node().value(BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilderVNext releaseOperation(String operationKey, String channelKey) {
        return operation(operationKey,
                channelKey,
                "Release reservation.",
                steps -> steps.triggerEvent("ReleaseReservation", PayNoteEvents.reservationReleaseRequested(
                        new Node().value(BlueDocDsl.expr("document('/amount/total')")))));
    }

    public PayNoteBuilderVNext requestCancellationOperation(String channelKey) {
        return operation("requestCancellation",
                channelKey,
                "Request paynote cancellation.",
                steps -> steps.replaceValue("SetCancellationRequested", "/status", "cancellation-requested"));
    }

    public PayNoteBuilderVNext issueChildPayNoteOnEvent(String workflowKey,
                                                         Class<?> triggerEventType,
                                                         String childPointerExpression) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> steps
                .emitType("IssueChildPayNote", PayNoteTypes.IssueChildPayNoteRequested.class,
                        payload -> payload.putExpression("childPayNote", childPointerExpression))));
        return this;
    }

    public PayNoteBuilderVNext onFundsReserved(String workflowKey, Consumer<StepsBuilder> customizer) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.FundsReserved.class, customizer));
        return this;
    }

    public PayNoteBuilderVNext onCaptureRequested(String workflowKey, Consumer<StepsBuilder> customizer) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.CaptureFundsRequested.class, customizer));
        return this;
    }

    public PayNoteBuilderVNext onFundsCaptured(String workflowKey, Consumer<StepsBuilder> customizer) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.FundsCaptured.class, customizer));
        return this;
    }

    public PayNoteBuilderVNext onReleased(String workflowKey, Consumer<StepsBuilder> customizer) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, PayNoteTypes.ReservationReleased.class, customizer));
        return this;
    }

    public PayNoteBuilderVNext once(String workflowKey,
                                    Class<?> triggerEventType,
                                    String guardPath,
                                    Consumer<StepsBuilder> customizer) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
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

    public PayNoteBuilderVNext barrier(String workflowKey,
                                       Class<?> triggerEventType,
                                       String statePath,
                                       String signalExpression,
                                       List<String> requiredSignals,
                                       Consumer<StepsBuilder> onSatisfied) {
        bootstrap.contracts(c -> c.onTriggered(workflowKey, triggerEventType, steps -> {
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

    public PayNoteBuilderVNext initialStateDescription(String summary, String details) {
        bootstrap.putDocumentObject("payNoteInitialStateDescription", description -> description
                .put("summary", summary)
                .put("details", details));
        return this;
    }

    public PayNoteBuilderVNext bindRoleAccount(String role, String accountId) {
        bootstrap.bindRoleAccount(role, accountId);
        return this;
    }

    public PayNoteBuilderVNext bindRoleEmail(String role, String email) {
        bootstrap.bindRoleEmail(role, email);
        return this;
    }

    public Node build() {
        return bootstrap.build();
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

    public static final class ParticipantsBuilder {
        private final PayNoteBuilderVNext parent;

        private ParticipantsBuilder(PayNoteBuilderVNext parent) {
            this.parent = parent;
        }

        public ParticipantsBuilder payer() {
            parent.bootstrap.role("payer", "payerChannel");
            parent.bootstrap.contracts(c -> c.timelineChannel("payerChannel"));
            return this;
        }

        public ParticipantsBuilder payee() {
            parent.bootstrap.role("payee", "payeeChannel");
            parent.bootstrap.contracts(c -> c.timelineChannel("payeeChannel"));
            return this;
        }

        public ParticipantsBuilder guarantor() {
            parent.bootstrap.role("guarantor", "guarantorChannel");
            parent.bootstrap.contracts(c -> c.timelineChannel("guarantorChannel"));
            return this;
        }

        public ParticipantsBuilder shipper(String channelKey) {
            String normalizedChannel = channelKey == null || channelKey.trim().isEmpty()
                    ? "shipmentCompanyChannel"
                    : channelKey;
            parent.bootstrap.role("shipmentCompany", normalizedChannel);
            parent.bootstrap.role("shipper", normalizedChannel);
            parent.bootstrap.contracts(c -> c.timelineChannel(normalizedChannel));
            return this;
        }
    }
}
