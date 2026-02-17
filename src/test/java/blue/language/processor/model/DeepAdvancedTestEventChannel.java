package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId("DeepAdvancedTestEventChannel")
public class DeepAdvancedTestEventChannel extends AdvancedTestEventChannel {
    private String transport;

    public String getTransport() {
        return transport;
    }

    public void setTransport(String transport) {
        this.transport = transport;
    }
}
