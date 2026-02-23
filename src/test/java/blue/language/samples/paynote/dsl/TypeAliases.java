package blue.language.samples.paynote.dsl;

import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.common.CommonTypes;
import blue.language.samples.paynote.types.core.CoreTypes;
import blue.language.samples.paynote.types.domain.PayNoteDemoEvents;
import blue.language.samples.paynote.types.domain.RecruitmentEvents;
import blue.language.samples.paynote.types.domain.ShippingEvents;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.samples.paynote.types.paynote.PayNoteV2Types;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

public final class TypeAliases {

    private TypeAliases() {
    }

    // Core
    public static final String CORE_CHANNEL = "Core/Channel";
    public static final String CORE_DOCUMENT_PROCESSING_INITIATED = "Core/Document Processing Initiated";
    public static final String CORE_DOCUMENT_UPDATE = "Core/Document Update";
    public static final String CORE_DOCUMENT_UPDATE_CHANNEL = "Core/Document Update Channel";
    public static final String CORE_LIFECYCLE_EVENT_CHANNEL = "Core/Lifecycle Event Channel";
    public static final String CORE_TRIGGERED_EVENT_CHANNEL = "Core/Triggered Event Channel";

    // Conversation
    public static final String CONVERSATION_OPERATION = "Conversation/Operation";
    public static final String CONVERSATION_TIMELINE_CHANNEL = "Conversation/Timeline Channel";
    public static final String CONVERSATION_COMPOSITE_TIMELINE_CHANNEL = "Conversation/Composite Timeline Channel";
    public static final String CONVERSATION_CHANNEL_SOURCE_BINDING = "Conversation/Channel Source Binding";
    public static final String CONVERSATION_SEQUENTIAL_WORKFLOW = "Conversation/Sequential Workflow";
    public static final String CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION = "Conversation/Sequential Workflow Operation";
    public static final String CONVERSATION_JAVASCRIPT_CODE = "Conversation/JavaScript Code";
    public static final String CONVERSATION_UPDATE_DOCUMENT = "Conversation/Update Document";
    public static final String CONVERSATION_TRIGGER_EVENT = "Conversation/Trigger Event";
    public static final String CONVERSATION_RESPONSE = "Conversation/Response";
    public static final String CONVERSATION_CHAT_MESSAGE = "Conversation/Chat Message";
    public static final String CONVERSATION_EVENT = "Conversation/Event";
    public static final String COMMON_NAMED_EVENT = "Common/Named Event";
    public static final String SHIPPING_SHIPMENT_CONFIRMED = "Shipping/Shipment Confirmed";
    public static final String SHIPPING_DELIVERY_REPORTED = "Shipping/Delivery Reported";
    public static final String PAYNOTE_DEMO_SUBSCRIPTION_CYCLE_STARTED = "PayNote Demo/Subscription Cycle Started";
    public static final String PAYNOTE_DEMO_MARKETPLACE_SPLIT_REQUESTED = "PayNote Demo/Marketplace Split Requested";
    public static final String PAYNOTE_DEMO_AGENT_PURCHASE_APPROVED = "PayNote Demo/Agent Purchase Approved";
    public static final String PAYNOTE_DEMO_MILESTONE_APPROVED = "PayNote Demo/Milestone Approved";
    public static final String PAYNOTE_DEMO_VOUCHER_TRIGGERED = "PayNote Demo/Voucher Triggered";
    public static final String PAYNOTE_DEMO_CAPTURE_HOOK_RAN = "PayNote Demo/Capture Hook Ran";
    public static final String RECRUITMENT_CV_CLASSIFICATION_REQUESTED = "Recruitment/CV Classification Requested";
    public static final String RECRUITMENT_SENIOR_CANDIDATE_DETECTED = "Recruitment/Senior Candidate Detected";

    // MyOS
    public static final String MYOS_DOCUMENT_SESSION_BOOTSTRAP = "MyOS/Document Session Bootstrap";
    public static final String MYOS_AGENT = "MyOS/Agent";
    public static final String MYOS_DOCUMENT_LINKS = "MyOS/Document Links";
    public static final String MYOS_SESSION_LINK = "MyOS/MyOS Session Link";
    public static final String MYOS_TIMELINE_CHANNEL = "MyOS/MyOS Timeline Channel";
    public static final String MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED = "MyOS/Single Document Permission Grant Requested";
    public static final String MYOS_LINKED_DOCUMENTS_PERMISSION_GRANT_REQUESTED = "MyOS/Linked Documents Permission Grant Requested";
    public static final String MYOS_SINGLE_DOCUMENT_PERMISSION_GRANTED = "MyOS/Single Document Permission Granted";
    public static final String MYOS_SUBSCRIBE_TO_SESSION_REQUESTED = "MyOS/Subscribe to Session Requested";
    public static final String MYOS_SUBSCRIPTION_TO_SESSION_INITIATED = "MyOS/Subscription to Session Initiated";
    public static final String MYOS_SUBSCRIPTION_UPDATE = "MyOS/Subscription Update";
    public static final String MYOS_SESSION_EPOCH_ADVANCED = "MyOS/Session Epoch Advanced";
    public static final String MYOS_CALL_OPERATION_REQUESTED = "MyOS/Call Operation Requested";
    public static final String PAYNOTE_DOCUMENT = PayNoteAliases.PAYNOTE;

    // Basic types
    public static final String TEXT = "Text";
    public static final String INTEGER = "Integer";

    private static final Map<Class<?>, String> CLASS_TO_ALIAS;

    static {
        Map<Class<?>, String> aliases = new HashMap<Class<?>, String>();

        // Conversation
        aliases.put(CoreTypes.Channel.class, CORE_CHANNEL);
        aliases.put(ConversationTypes.Operation.class, CONVERSATION_OPERATION);
        aliases.put(ConversationTypes.TimelineChannel.class, CONVERSATION_TIMELINE_CHANNEL);
        aliases.put(ConversationTypes.CompositeTimelineChannel.class, CONVERSATION_COMPOSITE_TIMELINE_CHANNEL);
        aliases.put(ConversationTypes.ChannelSourceBinding.class, CONVERSATION_CHANNEL_SOURCE_BINDING);
        aliases.put(ConversationTypes.SequentialWorkflow.class, CONVERSATION_SEQUENTIAL_WORKFLOW);
        aliases.put(ConversationTypes.SequentialWorkflowOperation.class, CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION);
        aliases.put(ConversationTypes.JavaScriptCode.class, CONVERSATION_JAVASCRIPT_CODE);
        aliases.put(ConversationTypes.UpdateDocument.class, CONVERSATION_UPDATE_DOCUMENT);
        aliases.put(ConversationTypes.TriggerEvent.class, CONVERSATION_TRIGGER_EVENT);
        aliases.put(ConversationTypes.ChatMessage.class, CONVERSATION_CHAT_MESSAGE);
        aliases.put(ConversationTypes.Response.class, CONVERSATION_RESPONSE);
        aliases.put(ConversationTypes.Event.class, CONVERSATION_EVENT);
        aliases.put(CommonTypes.NamedEvent.class, COMMON_NAMED_EVENT);
        aliases.put(ShippingEvents.ShipmentConfirmed.class, SHIPPING_SHIPMENT_CONFIRMED);
        aliases.put(ShippingEvents.DeliveryReported.class, SHIPPING_DELIVERY_REPORTED);
        aliases.put(PayNoteDemoEvents.SubscriptionCycleStarted.class, PAYNOTE_DEMO_SUBSCRIPTION_CYCLE_STARTED);
        aliases.put(PayNoteDemoEvents.MarketplaceSplitRequested.class, PAYNOTE_DEMO_MARKETPLACE_SPLIT_REQUESTED);
        aliases.put(PayNoteDemoEvents.AgentPurchaseApproved.class, PAYNOTE_DEMO_AGENT_PURCHASE_APPROVED);
        aliases.put(PayNoteDemoEvents.MilestoneApproved.class, PAYNOTE_DEMO_MILESTONE_APPROVED);
        aliases.put(PayNoteDemoEvents.VoucherTriggered.class, PAYNOTE_DEMO_VOUCHER_TRIGGERED);
        aliases.put(PayNoteDemoEvents.CaptureHookRan.class, PAYNOTE_DEMO_CAPTURE_HOOK_RAN);
        aliases.put(RecruitmentEvents.CvClassificationRequested.class, RECRUITMENT_CV_CLASSIFICATION_REQUESTED);
        aliases.put(RecruitmentEvents.SeniorCandidateDetected.class, RECRUITMENT_SENIOR_CANDIDATE_DETECTED);

        // MyOS
        aliases.put(MyOsTypes.DocumentSessionBootstrap.class, MYOS_DOCUMENT_SESSION_BOOTSTRAP);
        aliases.put(MyOsTypes.Agent.class, MYOS_AGENT);
        aliases.put(MyOsTypes.MyOsTimelineChannel.class, MYOS_TIMELINE_CHANNEL);
        aliases.put(MyOsTypes.DocumentLinks.class, MYOS_DOCUMENT_LINKS);
        aliases.put(MyOsTypes.MyOsSessionLink.class, MYOS_SESSION_LINK);
        aliases.put(MyOsTypes.SingleDocumentPermissionGrantRequested.class,
                MYOS_SINGLE_DOCUMENT_PERMISSION_GRANT_REQUESTED);
        aliases.put(MyOsTypes.LinkedDocumentsPermissionGrantRequested.class,
                MYOS_LINKED_DOCUMENTS_PERMISSION_GRANT_REQUESTED);
        aliases.put(MyOsTypes.SingleDocumentPermissionGranted.class, MYOS_SINGLE_DOCUMENT_PERMISSION_GRANTED);
        aliases.put(MyOsTypes.SubscribeToSessionRequested.class, MYOS_SUBSCRIBE_TO_SESSION_REQUESTED);
        aliases.put(MyOsTypes.SubscriptionToSessionInitiated.class, MYOS_SUBSCRIPTION_TO_SESSION_INITIATED);
        aliases.put(MyOsTypes.SubscriptionUpdate.class, MYOS_SUBSCRIPTION_UPDATE);
        aliases.put(MyOsTypes.SessionEpochAdvanced.class, MYOS_SESSION_EPOCH_ADVANCED);
        aliases.put(MyOsTypes.CallOperationRequested.class, MYOS_CALL_OPERATION_REQUESTED);

        // PayNote
        aliases.put(PayNoteTypes.ReserveFundsRequested.class, PayNoteAliases.RESERVE_FUNDS_REQUESTED);
        aliases.put(PayNoteTypes.ReserveFundsAndCaptureImmediatelyRequested.class,
                PayNoteAliases.RESERVE_FUNDS_AND_CAPTURE_IMMEDIATELY_REQUESTED);
        aliases.put(PayNoteTypes.CaptureFundsRequested.class, PayNoteAliases.CAPTURE_FUNDS_REQUESTED);
        aliases.put(PayNoteTypes.ReservationReleaseRequested.class, PayNoteAliases.RESERVATION_RELEASE_REQUESTED);
        aliases.put(PayNoteTypes.IssueChildPayNoteRequested.class, PayNoteAliases.ISSUE_CHILD_PAYNOTE_REQUESTED);
        aliases.put(PayNoteTypes.PayNoteCancellationRequested.class, PayNoteAliases.PAYNOTE_CANCELLATION_REQUESTED);
        aliases.put(PayNoteTypes.CaptureLockRequested.class, PayNoteAliases.CAPTURE_LOCK_REQUESTED);
        aliases.put(PayNoteTypes.CaptureUnlockRequested.class, PayNoteAliases.CAPTURE_UNLOCK_REQUESTED);
        aliases.put(PayNoteTypes.CaptureLocked.class, PayNoteAliases.CAPTURE_LOCKED);
        aliases.put(PayNoteTypes.CaptureUnlocked.class, PayNoteAliases.CAPTURE_UNLOCKED);
        aliases.put(PayNoteV2Types.PayNoteDocument.class, PAYNOTE_DOCUMENT);

        CLASS_TO_ALIAS = Collections.unmodifiableMap(aliases);
    }

    public static String aliasForClass(Class<?> typeClass) {
        String alias = CLASS_TO_ALIAS.get(typeClass);
        if (alias != null) {
            return alias;
        }
        return typeClass.getSimpleName();
    }
}
