package blue.language.processor.model.examples;

import blue.language.model.TypeBlueId;

@TypeBlueId("Example.Handler.SetCounterIfKind")
public class ExampleSetCounterIfKindHandler extends ExampleSetCounterHandler {
    public String expectedKind;
}
