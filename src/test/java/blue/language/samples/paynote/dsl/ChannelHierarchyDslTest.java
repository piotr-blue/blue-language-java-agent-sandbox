package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ChannelHierarchyDslTest {

    @Test
    void supportsPortableTimelineChannelsThenMyOsConcreteBindings() {
        Node portableDocument = BlueDocDsl.document(MyOsTypes.Agent.class)
                .name("Portable Participant Doc")
                .contracts(c -> c
                        .timelineChannels("payerChannel", "payeeChannel", "guarantorChannel")
                        .compositeTimelineChannel("allParticipantsChannel",
                                "payerChannel", "payeeChannel", "guarantorChannel"))
                .build();

        Node bootstrap = MyOsDsl.bootstrap(portableDocument)
                .bindRole("payer").email("alice@gmail.com")
                .bindRole("payee").accountId("1234")
                .bindRole("guarantor").accountId("0")
                .build();

        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/payerChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/payeeChannel/type/value"));
        assertEquals(TypeAliases.CONVERSATION_COMPOSITE_TIMELINE_CHANNEL,
                bootstrap.getAsText("/document/contracts/allParticipantsChannel/type/value"));
        assertEquals(TypeAliases.MYOS_TIMELINE_CHANNEL,
                bootstrap.getAsText("/channelBindings/payerChannel/type/value"));
        assertEquals("alice@gmail.com",
                bootstrap.getAsText("/channelBindings/payerChannel/email/value"));
        assertEquals("1234",
                bootstrap.getAsText("/channelBindings/payeeChannel/accountId/value"));
    }

    @Test
    void supportsInDocumentChannelSourceBindingContracts() {
        Node document = BlueDocDsl.document(MyOsTypes.Agent.class)
                .name("Source Binding Demo")
                .contracts(c -> c
                        .timelineChannels("payerChannel", "payeeChannel")
                        .myOsTimelineChannels("alice", "bob")
                        .channelSourceBinding("roleSourceBinding", bindings -> bindings
                                .bind("payerChannel", "alice")
                                .bind("payeeChannel", "bob")))
                .build();

        assertEquals(TypeAliases.CONVERSATION_CHANNEL_SOURCE_BINDING,
                document.getAsText("/contracts/roleSourceBinding/type/value"));
        assertEquals("alice",
                document.getAsText("/contracts/roleSourceBinding/bindings/payerChannel/value"));
        assertEquals("bob",
                document.getAsText("/contracts/roleSourceBinding/bindings/payeeChannel/value"));
    }
}
