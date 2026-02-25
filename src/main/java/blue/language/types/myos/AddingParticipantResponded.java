package blue.language.types.myos;

import blue.language.model.TypeBlueId;
import blue.language.types.TypeAlias;

@TypeAlias("MyOS/Adding Participant Responded")
@TypeBlueId("MyOS-Adding-Participant-Responded-Placeholder-BlueId")
public class AddingParticipantResponded {
    public String channelKey;
    public String email;

    public AddingParticipantResponded channelKey(String channelKey) {
        this.channelKey = channelKey;
        return this;
    }

    public AddingParticipantResponded email(String email) {
        this.email = email;
        return this;
    }
}
