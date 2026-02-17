package blue.language.processor.model;

import blue.language.model.TypeBlueId;

@TypeBlueId("DeepAdvancedSetProperty")
public class DeepAdvancedSetProperty extends AdvancedSetProperty {
    private String level;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
