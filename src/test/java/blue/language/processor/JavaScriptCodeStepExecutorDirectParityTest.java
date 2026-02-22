package blue.language.processor;

import blue.language.model.Node;
import blue.language.processor.model.SequentialWorkflow;
import blue.language.processor.workflow.StepExecutionArgs;
import blue.language.processor.workflow.steps.JavaScriptCodeStepExecutor;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JavaScriptCodeStepExecutorDirectParityTest {

    @Test
    void executesStepDirectlyAndChargesWasmGas() {
        JavaScriptCodeStepExecutor executor = new JavaScriptCodeStepExecutor();

        Node document = new Node().properties("counter", new Node().value(5));
        DocumentProcessor owner = new DocumentProcessor();
        ProcessorEngine.Execution execution = new ProcessorEngine.Execution(owner, document.clone());
        execution.loadBundles("/");

        Node event = new Node()
                .type(new Node().blueId("TestEvent"))
                .properties("x", new Node().value(7));
        Node step = new Node()
                .name("Compute")
                .type(new Node().blueId("Conversation/JavaScript Code"))
                .properties("code", new Node().value("return { result: document('/counter') + event.x };"));

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
        Object result = executor.execute(args);
        long afterGas = execution.runtime().totalGas();

        assertTrue(result instanceof Map);
        @SuppressWarnings("unchecked")
        Map<String, Object> resultMap = (Map<String, Object>) result;
        assertEquals("12", String.valueOf(resultMap.get("result")));
        assertTrue(afterGas > beforeGas);
    }
}
