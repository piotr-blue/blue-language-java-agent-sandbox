package blue.language.samples.paynote.dsl;

public final class TypeAliases {

    private TypeAliases() {
    }

    // Core
    public static final String CORE_DOCUMENT_PROCESSING_INITIATED = "Core/Document Processing Initiated";
    public static final String CORE_DOCUMENT_UPDATE = "Core/Document Update";
    public static final String CORE_DOCUMENT_UPDATE_CHANNEL = "Core/Document Update Channel";
    public static final String CORE_LIFECYCLE_EVENT_CHANNEL = "Core/Lifecycle Event Channel";
    public static final String CORE_TRIGGERED_EVENT_CHANNEL = "Core/Triggered Event Channel";

    // Conversation
    public static final String CONVERSATION_OPERATION = "Conversation/Operation";
    public static final String CONVERSATION_SEQUENTIAL_WORKFLOW = "Conversation/Sequential Workflow";
    public static final String CONVERSATION_SEQUENTIAL_WORKFLOW_OPERATION = "Conversation/Sequential Workflow Operation";
    public static final String CONVERSATION_JAVASCRIPT_CODE = "Conversation/JavaScript Code";
    public static final String CONVERSATION_UPDATE_DOCUMENT = "Conversation/Update Document";
    public static final String CONVERSATION_TRIGGER_EVENT = "Conversation/Trigger Event";
    public static final String CONVERSATION_RESPONSE = "Conversation/Response";
    public static final String CONVERSATION_CHAT_MESSAGE = "Conversation/Chat Message";
    public static final String CONVERSATION_EVENT = "Conversation/Event";

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

    // Basic types
    public static final String TEXT = "Text";
    public static final String INTEGER = "Integer";
}
