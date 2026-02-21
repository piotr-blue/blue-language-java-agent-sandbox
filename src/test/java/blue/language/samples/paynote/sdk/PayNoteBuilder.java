package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.processor.model.ProcessEmbedded;
import blue.language.samples.paynote.types.conversation.ConversationTypes;
import blue.language.samples.paynote.types.paynote.PayNoteTypes;
import blue.language.processor.model.EmitEvents;
import blue.language.processor.model.SetProperty;
import blue.language.processor.model.TestEventChannel;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public final class PayNoteBuilder {

    private final PayNoteDraft draft;

    private PayNoteBuilder(String name, Integer amount, String currency) {
        draft = new PayNoteDraft();
        draft.name = name;
        draft.amount = amount;
        draft.currency = currency;
    }

    public static PayNoteBuilder create(String name, Integer amount, String currency) {
        return new PayNoteBuilder(name, amount, currency);
    }

    public PayNoteBuilder withStandardParties(String payerTimelineId,
                                              String payeeTimelineId,
                                              String guarantorTimelineId) {
        draft.parties.put("payerChannel", timelineChannel(payerTimelineId));
        draft.parties.put("payeeChannel", timelineChannel(payeeTimelineId));
        draft.parties.put("guarantorChannel", timelineChannel(guarantorTimelineId));
        return this;
    }

    public PayNoteBuilder attachContract(String key, Object contract) {
        draft.contracts.put(key, contract);
        return this;
    }

    public PayNoteBuilder addChild(String key, PayNoteDraft child) {
        draft.children.put(key, child);
        return this;
    }

    public PayNoteBuilder processEmbeddedChildren() {
        ProcessEmbedded processEmbedded = new ProcessEmbedded();
        List<String> paths = new ArrayList<String>();
        for (String childKey : draft.children.keySet()) {
            paths.add("/children/" + childKey);
        }
        processEmbedded.setPaths(paths);
        draft.contracts.put("embedded", processEmbedded);
        return this;
    }

    public PayNoteBuilder captureWhenEventArrives(String triggerChannelKey) {
        return flagOnEvent(triggerChannelKey, "captureOnEvent", "/captureRequested", 1);
    }

    public PayNoteBuilder flagOnEvent(String triggerChannelKey,
                                      String handlerKey,
                                      String propertyPath,
                                      int value) {
        ensureTestEventChannel(triggerChannelKey);
        SetProperty handler = new SetProperty();
        handler.setChannel(triggerChannelKey);
        handler.setPropertyKey(propertyPath);
        handler.setPropertyValue(value);
        draft.contracts.put(handlerKey, handler);
        return this;
    }

    public PayNoteBuilder emitChildIssuedEvents(String triggerChannelKey, String handlerKey) {
        ensureTestEventChannel(triggerChannelKey);
        EmitEvents handler = new EmitEvents();
        handler.setChannel(triggerChannelKey);
        for (Map.Entry<String, PayNoteDraft> child : draft.children.entrySet()) {
            PayNoteTypes.ChildPayNoteIssued event = new PayNoteTypes.ChildPayNoteIssued();
            event.childPayNote = new Node().value("/children/" + child.getKey());
            handler.addEvent(new blue.language.Blue().objectToNode(event));
        }
        draft.contracts.put(handlerKey, handler);
        return this;
    }

    public PayNoteDraft build() {
        return draft;
    }

    private ConversationTypes.TimelineChannel timelineChannel(String timelineId) {
        ConversationTypes.TimelineChannel channel = new ConversationTypes.TimelineChannel();
        channel.timelineId = timelineId;
        return channel;
    }

    private void ensureTestEventChannel(String channelKey) {
        if (draft.contracts.containsKey(channelKey)) {
            return;
        }
        TestEventChannel channel = new TestEventChannel();
        draft.contracts.put(channelKey, channel);
    }
}
