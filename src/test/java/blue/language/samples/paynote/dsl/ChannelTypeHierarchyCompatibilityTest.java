package blue.language.samples.paynote.dsl;

import blue.language.model.Node;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.core.CoreTypes;
import blue.language.samples.paynote.types.myos.MyOsTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ChannelTypeHierarchyCompatibilityTest {

    @Test
    void channelHierarchyAlignsWithCoreConversationAndMyOsTypes() {
        assertTrue(CoreTypes.Channel.class.isAssignableFrom(ConversationTypes.TimelineChannel.class));
        assertTrue(ConversationTypes.TimelineChannel.class.isAssignableFrom(MyOsTypes.MyOsTimelineChannel.class));

        Node coreChannelType = TypeRef.of(CoreTypes.Channel.class).asTypeNode();
        Node timelineType = TypeRef.of(ConversationTypes.TimelineChannel.class).asTypeNode();
        Node myOsTimelineType = TypeRef.of(MyOsTypes.MyOsTimelineChannel.class).asTypeNode();

        assertEquals(TypeAliases.CORE_CHANNEL, coreChannelType.getValue());
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL, timelineType.getValue());
        assertEquals(TypeAliases.MYOS_TIMELINE_CHANNEL, myOsTimelineType.getValue());
    }
}
