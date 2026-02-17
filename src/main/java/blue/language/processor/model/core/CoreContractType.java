package blue.language.processor.model.core;

import blue.language.model.TypeBlueId;

@TypeBlueId("Core.Contract")
public class CoreContractType {

    private String key;
    private Integer order;

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public Integer getOrder() {
        return order;
    }

    public void setOrder(Integer order) {
        this.order = order;
    }
}
