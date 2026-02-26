package blue.language.sdk.dsl;

import blue.language.Blue;
import blue.language.model.Node;
import blue.language.sdk.DocBuilder;
import blue.language.types.core.Channel;
import blue.language.types.myos.MyOsTimelineChannel;
import com.fasterxml.jackson.databind.JsonNode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static blue.language.utils.UncheckedObjectMapper.JSON_MAPPER;

class DocBuilderChannelsDslParityTest {

    private static final Blue BLUE = new Blue();

    @Test
    void channelDefaultMatchesYamlDefinition() {
        Node fromDsl = DocBuilder.doc()
                .name("Channel parity")
                .channel("ownerChannel")
                .buildDocument();

        assertDslMatchesYaml(fromDsl, """
                name: Channel parity
                contracts:
                  ownerChannel:
                    type: Conversation/Timeline Channel
                """);
    }

    @Test
    void channelBeanMatchesYamlDefinition() {
        Node fromDsl = DocBuilder.doc()
                .name("Provided channel parity")
                .channel("ownerChannel", new MyOsTimelineChannel()
                        .timelineId("timeline-1")
                        .accountId("acc-1")
                        .email("owner@example.com"))
                .buildDocument();

        assertDslMatchesYaml(fromDsl, """
                name: Provided channel parity
                contracts:
                  ownerChannel:
                    type: MyOS/MyOS Timeline Channel
                    timelineId: timeline-1
                    accountId: acc-1
                    email: owner@example.com
                """);
    }

    @Test
    void channelsTwoNamesMatchYamlDefinition() {
        Node fromDsl = DocBuilder.doc()
                .name("Channels parity")
                .channels("nameA", "nameB")
                .buildDocument();

        assertDslMatchesYaml(fromDsl, """
                name: Channels parity
                contracts:
                  nameA:
                    type: Conversation/Timeline Channel
                  nameB:
                    type: Conversation/Timeline Channel
                """);
    }

    @Test
    void compositeChannelMatchesYamlDefinition() {
        Node fromDsl = DocBuilder.doc()
                .name("Composite channel parity")
                .channels("payerChannel", "payeeChannel")
                .compositeChannel("participantUnionChannel", "payerChannel", "payeeChannel")
                .buildDocument();

        assertDslMatchesYaml(fromDsl, """
                name: Composite channel parity
                contracts:
                  payerChannel:
                    type: Conversation/Timeline Channel
                  payeeChannel:
                    type: Conversation/Timeline Channel
                  participantUnionChannel:
                    type: Conversation/Composite Timeline Channel
                    channels:
                      - payerChannel
                      - payeeChannel
                """);
    }

    @Test
    void onChannelEventMatchesYamlDefinition() {
        Node fromDsl = DocBuilder.doc()
                .name("Channel event parity")
                .channel("ownerChannel")
                .set("/counter", 0)
                .onChannelEvent("onIncrementEvent", "ownerChannel", Integer.class,
                        steps -> steps.replaceValue("SetCounter", "/counter", 1))
                .buildDocument();

        assertDslMatchesYaml(fromDsl, """
                name: Channel event parity
                counter: 0
                contracts:
                  ownerChannel:
                    type: Conversation/Timeline Channel
                  onIncrementEvent:
                    type: Conversation/Sequential Workflow
                    channel: ownerChannel
                    event:
                      type: Integer
                    steps:
                      - name: SetCounter
                        type: Conversation/Update Document
                        changeset:
                          - op: replace
                            path: /counter
                            val: 1
                """);
    }

    @Test
    void channelTemplateCanBeSpecializedToMyOsAdminChannel() {
        Node template = DocBuilder.doc()
                .name("Channel template")
                .channel("adminChannel")
                .buildDocument();

        Node specialized = DocBuilder.from(template)
                .channel("adminChannel", new MyOsTimelineChannel()
                        .timelineId("session-42")
                        .accountId("acc-42")
                        .email("admin@company.com"))
                .buildDocument();

        assertSame(template, specialized);
        assertDslMatchesYaml(specialized, """
                name: Channel template
                contracts:
                  adminChannel:
                    type: MyOS/MyOS Timeline Channel
                    timelineId: session-42
                    accountId: acc-42
                    email: admin@company.com
                """);
    }

    private static void assertDslMatchesYaml(Node fromDsl, String yaml) {
        Node fromYaml = BLUE.preprocess(BLUE.yamlToNode(yaml).clone());
        Node normalizedDsl = BLUE.preprocess(fromDsl.clone());
        String expectedBlueId = BLUE.calculateBlueId(fromYaml);
        String actualBlueId = BLUE.calculateBlueId(normalizedDsl);
        assertNotNull(expectedBlueId);
        assertNotNull(actualBlueId);
        JsonNode expectedTree = JSON_MAPPER.readTree(BLUE.nodeToSimpleJson(fromYaml));
        JsonNode actualTree = JSON_MAPPER.readTree(BLUE.nodeToSimpleJson(normalizedDsl));
        assertEquals(expectedTree, actualTree);
    }
}
