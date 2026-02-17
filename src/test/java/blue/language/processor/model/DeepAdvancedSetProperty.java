package blue.language.processor.model;

import blue.language.model.BlueType;

@BlueType("DeepAdvancedSetProperty")
public class DeepAdvancedSetProperty extends AdvancedSetProperty {
    private String level;

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }
}
