package blue.language.samples.paynote.sdk;

import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.types.conversation.ConversationTypes;

import java.util.LinkedHashMap;
import java.util.Map;

@TypeBlueId("PayNote-Draft-Demo-BlueId")
public class PayNoteDraft {
    public String name;
    public Integer amount;
    public String currency;
    public Map<String, ConversationTypes.TimelineChannel> parties = new LinkedHashMap<String, ConversationTypes.TimelineChannel>();
    public Map<String, PayNoteDraft> children = new LinkedHashMap<String, PayNoteDraft>();
    public Map<String, Object> contracts = new LinkedHashMap<String, Object>();
}
