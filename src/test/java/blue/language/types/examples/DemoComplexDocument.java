package blue.language.types.examples;

import blue.language.model.TypeBlueId;

import java.util.List;

@TypeBlueId("DemoComplexDocument")
public class DemoComplexDocument {
    public DemoSimpleDocument root;
    public DemoConstrainedC constrained;
    public List<DemoContract> contracts;
}
