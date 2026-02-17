package blue.language.processor.model;

import blue.language.model.BlueType;

@BlueType("AdvancedTestEventChannel")
public class AdvancedTestEventChannel extends TestEventChannel {
    private String route;

    public String getRoute() {
        return route;
    }

    public void setRoute(String route) {
        this.route = route;
    }
}
