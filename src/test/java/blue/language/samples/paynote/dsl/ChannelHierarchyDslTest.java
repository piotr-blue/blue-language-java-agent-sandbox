package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelHierarchyDslTest {

    @Test
    void supportsPortableTimelineChannelsThenMyOsConcreteBindings() {
        Node portableDocument = BlueDocDsl.document(MyOsTypes.Agent.class)
                .name("Portable Participant Doc")
                .contracts(c -> c
                        .channel("payerChannel", ConversationTypes.TimelineChannel.class)
                        .channel("payeeChannel", ConversationTypes.TimelineChannel.class)
                        .channel("guarantorChannel", ConversationTypes.TimelineChannel.class))
                .build();

        Node bootstrap = MyOs.bootstrap(portableDocument)
                .bindTimeline("payerChannel", MyOsTimeline.email("alice@gmail.com"))
                .bindTimeline("payeeChannel", MyOsTimeline.accountId("1234"))
                .bindTimeline("guarantorChannel", MyOsTimeline.accountId("0"))
                .build();

        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/payerChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/payeeChannel/type/value"));
        assertEquals(TypeAliases.MYOS_TIMELINE_CHANNEL,
                bootstrap.getAsText("/channelBindings/payerChannel/type/value"));
        assertEquals("alice@gmail.com",
                bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("1234",
                bootstrap.getAsText("/channelBindings/payeeChannel/accountId/value"));
    }
}
