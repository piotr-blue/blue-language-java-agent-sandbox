package blue.language.processor.model.examples;

import blue.language.model.BlueType;

@BlueType("Example.Handler.SetCounterIfKind")
public class ExampleSetCounterIfKindHandler extends ExampleSetCounterHandler {
    public String expectedKind;
}
