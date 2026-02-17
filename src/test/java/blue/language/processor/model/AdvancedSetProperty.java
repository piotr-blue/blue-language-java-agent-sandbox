package blue.language.processor.model;

import blue.language.model.BlueType;

@BlueType("AdvancedSetProperty")
public class AdvancedSetProperty extends SetProperty {
    private String tag;

    public String getTag() {
        return tag;
    }

    public void setTag(String tag) {
        this.tag = tag;
    }
}
