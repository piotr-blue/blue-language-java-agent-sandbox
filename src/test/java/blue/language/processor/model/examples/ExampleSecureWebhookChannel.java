package blue.language.processor.model.examples;

import blue.language.model.BlueType;

@BlueType("Example.Channel.Webhook.Secure")
public class ExampleSecureWebhookChannel extends ExampleWebhookChannel {
    public String signatureAlgorithm;
}
