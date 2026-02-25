package blue.language.samples.paynote.sdk;

import blue.language.model.Node;
import blue.language.samples.paynote.dsl.TypeAliases;
import blue.language.samples.paynote.types.common.CommonTypes;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SimpleDocBuilderTest {

    @Test
    void buildsGenericDocumentAndBootstrapsBindingsSeparately() {
        Node document = SimpleDocBuilder.name("Counter #1")
                .type("MyCompany/Counter")
                .description("A simple counter")
                .participants("owner", "observer")
                .set("/count", 0)
                .operation("increment")
                    .channel("owner")
                    .description("Increment counter")
                    .requestType(Integer.class)
                    .steps(steps -> steps
                            .replaceExpression("IncrementValue", "/count", "document('/count') + event.message.request")
                            .emitType("CounterChanged", CommonTypes.NamedEvent.class,
                                    payload -> payload.put("name", "CounterChanged")))
                    .done()
                .buildDocument();

        assertEquals("MyCompany/Counter", document.getAsText("/type/value"));
        assertEquals(0, document.getAsInteger("/count/value").intValue());
        assertEquals(TypeAliases.CONVERSATION_TIMELINE_CHANNEL, document.getAsText("/contracts/owner/type/value"));
        assertEquals(TypeAliases.CONVERSATION_OPERATION, document.getAsText("/contracts/increment/type/value"));

        Node bootstrap = SimpleDocBuilder.name("Counter #1")
                .type("MyCompany/Counter")
                .participants("owner", "observer")
                .buildDocument();
        bootstrap = blue.language.samples.paynote.dsl.MyOsDsl.bootstrap(bootstrap)
                .bind("owner").email("alice@gmail.com")
                .bind("observer").accountId("acc_observer_123")
                .build();

        assertEquals(TypeAliases.MYOS_DOCUMENT_SESSION_BOOTSTRAP, bootstrap.getAsText("/type/value"));
        assertEquals("alice@gmail.com", bootstrap.getAsText("/channelBindings/owner/email/value"));
    }
}
