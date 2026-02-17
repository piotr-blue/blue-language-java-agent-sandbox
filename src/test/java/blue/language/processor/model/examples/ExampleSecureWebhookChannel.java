package blue.language.processor.model.examples;

import blue.language.model.TypeBlueId;

@TypeBlueId("Example.Channel.Webhook.Secure")
public class ExampleSecureWebhookChannel extends ExampleWebhookChannel {
    public String signatureAlgorithm;
}
