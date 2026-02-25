package blue.language.samples.paynote.types.myos;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.dsl.TypeAlias;
import blue.language.samples.paynote.types.conversation.ConversationTypes;

import java.util.Map;

public final class MyOsTypes {

    private MyOsTypes() {
    }

    @TypeBlueId("84xMEnEYr3DPBuYZL3JtcsZBBTtRH9fEEJiPnk7ASj1o")
    public static class DocumentSessionBootstrap {
        public Node document;
        public Map<String, Node> channelBindings;
        public Node initialMessages;
        public Node capabilities;
        public String bootstrapStatus;
        public String bootstrapError;
        public Node initiatorSessionIds;
        public Node participantsState;
        public Node contracts;
    }

    @TypeBlueId("4cmrbevB6K23ZenjqwmNxpnaw6RF4VB3wkP7XB59V7W5")
    public static class DocumentLinks {
    }

    @TypeBlueId("d1vQ8ZTPcQc5KeuU6tzWaVukWRVtKjQL4hbvbpC22rB")
    public static class MyOsSessionLink {
        public String anchor;
        public String sessionId;
    }

    @TypeBlueId("HCF8mXnX3dFjQ8osjxb4Wzm2Nm1DoXnTYuA5sPnV7NTs")
    public static class MyOsTimelineChannel extends ConversationTypes.TimelineChannel {
        public String accountId;
        public String email;
    }

    @TypeBlueId("8s2rAFDtiB6sCwqeURkT4Lq7fcc2FXBkmX9B9p7R4Boc")
    public static class Agent {
        public String agentId;
    }

    @TypeBlueId("EVX6nBdHdVEBH9Gbthpd2eqpxaxS4bb9wM55QNdZmcBy")
    public static class CallOperationRequested {
        public String onBehalfOf;
        public String targetSessionId;
        public String operation;
        public Node request;
    }

    @TypeAlias("MyOS/Call Operation Responded")
    @TypeBlueId("MyOS-Call-Operation-Responded-Placeholder-BlueId")
    public static class CallOperationResponded {
        public Node result;
        public Node inResponseTo;
    }

    @TypeAlias("MyOS/Call Operation Failed")
    @TypeBlueId("MyOS-Call-Operation-Failed-Placeholder-BlueId")
    public static class CallOperationFailed {
        public String reason;
        public Node inResponseTo;
    }

    @TypeBlueId("BnrAcFrEHzoARE2yqKmRv7jrPWCbJsVBqSoXwWCaTtrk")
    public static class SubscribeToSessionRequested {
        public String targetSessionId;
        public Node subscription;
    }

    @TypeBlueId("2gc8djtKGGRPjGfMQzvJZMviaXm4ytM1nA4DVbfyjkrW")
    public static class SubscriptionUpdate {
        public String subscriptionId;
        public String targetSessionId;
        public Node update;
    }

    @TypeBlueId("GZPDibWTKDudqwPufgmNo7AHMLwY5FGeeHFx3EkegzLj")
    public static class SubscriptionToSessionInitiated {
        public String subscriptionId;
        public String targetSessionId;
        public String at;
    }

    @TypeBlueId("Ef7EvcR5He11JtgBFtswYTHEfUKnTHmFysMTo3ZsoQby")
    public static class SingleDocumentPermissionGrantRequested {
        public String onBehalfOf;
        public String requestId;
        public String targetSessionId;
        public Node permissions;
    }

    @TypeBlueId("DBv2TLwytwBgvrSVeauLjTZYycf8hiXgdadoyRVDfjhS")
    public static class LinkedDocumentsPermissionGrantRequested {
        public String onBehalfOf;
        public String requestId;
        public String targetSessionId;
        public Node links;
    }

    @TypeBlueId("8XYzJ3BrgB5uoAWU5HvZ7Gej9RXNG5r52ccneLZxMAQd")
    public static class SingleDocumentPermissionGranted {
        public String targetSessionId;
        public Node permissions;
    }

    @TypeBlueId("9CvxqAMJhqcFoLr5nXSEdWDZUMD383xhJtyFwXsCqD9E")
    public static class SessionEpochAdvanced {
        public String sessionId;
        public String timestamp;
        public Integer epoch;
        public Node document;
    }

    @TypeBlueId("AZEL7GJEXVcSPp3mgbRtqHYCHAvfBpqqc1k8b2HhQh4T")
    public static class AddingParticipantRequested {
        public String channelKey;
        public String email;
    }

    @TypeAlias("MyOS/Adding Participant Responded")
    @TypeBlueId("MyOS-Adding-Participant-Responded-Placeholder-BlueId")
    public static class AddingParticipantResponded {
        public String channelKey;
        public String email;
    }

    @TypeBlueId("Hfoh2g4jJo8Tmk43YX34wVW5YXtL1ncZs7weXVKtTm4b")
    public static class RemovingParticipantRequested {
        public String channelKey;
    }

    @TypeBlueId("3MNb8B84b9CkT5LY2qvxG9k86f5osYQwJ9TK4FFfCKmX")
    public static class StartWorkerSessionRequested {
        public String agentChannelKey;
        public Node config;
    }
}
