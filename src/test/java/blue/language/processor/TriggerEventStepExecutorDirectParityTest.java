package blue.language.processor;

import blue.language.model.Node;
import blue.language.processor.model.SequentialWorkflow;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.steps.TriggerEventStepExecutor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class TriggerEventStepExecutorDirectParityTest {

    @Test
    void emitsResolvedEventPayloadAndChargesTriggerGas() {
        TriggerEventStepExecutor executor = new TriggerEventStepExecutor();

        Node document = new Node();
        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");

        Node event = new Node()
                .type(new Node().blueId("TestEvent"))
                .properties("kind", new Node().value("from-event"));
        Node step = new Node()
                .name("Emit")
                .type(new Node().blueId("Conversation/Trigger Event"))
                .properties("event", new Node()
                        .properties("type", new Node().blueId("Conversation/Chat Message"))
                        .properties("message", new Node().value("${event.kind}")));

        ProcessorExecutionContext context = execution.createContext(
                "/",
                execution.bundleForScope("/"),
                event,
                false,
                false);
        StepExecutionArgs args = new StepExecutionArgs(
                new SequentialWorkflow(),
                step,
                event,
                context,
                new LinkedHashMap<String, Object>(),
                0);

        long beforeGas = execution.runtime().totalGas();
        executor.execute(args);
        long afterGas = execution.runtime().totalGas();

        Node emitted = execution.runtime().scope("/").triggeredQueue().peekFirst();
        assertNotNull(emitted);
        assertEquals("from-event", String.valueOf(emitted.getProperties().get("message").getValue()));
        assertTrue(afterGas > beforeGas);
    }

    @Test
    void preservesNestedDocumentExpressionsInEmittedPayload() {
        TriggerEventStepExecutor executor = new TriggerEventStepExecutor();

        Node document = new Node();
        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");

        Node event = new Node().type(new Node().blueId("TestEvent"));
        Node step = new Node()
                .name("Emit")
                .type(new Node().blueId("Conversation/Trigger Event"))
                .properties("event", new Node()
                        .properties("type", new Node().blueId("Conversation/Chat Message"))
                        .properties("document", new Node()
                                .properties("contracts", new Node().properties("x", new Node()))
                                .properties("counterExpr", new Node().value("${document('/counter') + 1}"))));

        ProcessorExecutionContext context = execution.createContext(
                "/",
                execution.bundleForScope("/"),
                event,
                false,
                false);
        StepExecutionArgs args = new StepExecutionArgs(
                new SequentialWorkflow(),
                step,
                event,
                context,
                new LinkedHashMap<String, Object>(),
                0);

        executor.execute(args);

        Node emitted = execution.runtime().scope("/").triggeredQueue().peekFirst();
        assertNotNull(emitted);
        Node preserved = emitted.getProperties()
                .get("document")
                .getProperties()
                .get("counterExpr");
        assertEquals("${document('/counter') + 1}", String.valueOf(preserved.getValue()));
    }

    @Test
    void throwsFatalWhenEventPayloadIsMissing() {
        TriggerEventStepExecutor executor = new TriggerEventStepExecutor();

        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, new Node());
        execution.loadBundles("/");

        Node event = new Node().type(new Node().blueId("TestEvent"));
        Node step = new Node()
                .name("Emit")
                .type(new Node().blueId("Conversation/Trigger Event"));

        ProcessorExecutionContext context = execution.createContext(
                "/",
                execution.bundleForScope("/"),
                event,
                false,
                false);
        StepExecutionArgs args = new StepExecutionArgs(
                new SequentialWorkflow(),
                step,
                event,
                context,
                new LinkedHashMap<String, Object>(),
                0);

        assertThrows(ProcessorFatalException.class, () -> executor.execute(args));
    }
}
