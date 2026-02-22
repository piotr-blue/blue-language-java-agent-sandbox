package blue.language.processor.workflow;

import blue.language.model.Node;
import blue.language.processor.model.SequentialWorkflow;
import org.junit.jupiter.api.Test;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WorkflowStepRunnerTest {

    @Test
    void runDispatchesExecutorsAndExposesPriorStepResults() {
        WorkflowStepExecutor compute = new WorkflowStepExecutor() {
            @Override
            public Set<String> supportedBlueIds() {
                return Collections.unmodifiableSet(new LinkedHashSet<String>(Collections.singletonList("Step/Compute")));
            }

            @Override
            public Object execute(StepExecutionArgs args) {
                assertEquals(0, args.stepIndex());
                return new BigInteger("4");
            }
        };
        WorkflowStepExecutor add = new WorkflowStepExecutor() {
            @Override
            public Set<String> supportedBlueIds() {
                return Collections.unmodifiableSet(new LinkedHashSet<String>(Collections.singletonList("Step/Add")));
            }

            @Override
            public Object execute(StepExecutionArgs args) {
                assertEquals(1, args.stepIndex());
                Object prior = args.stepResults().get("Compute");
                return new BigInteger(String.valueOf(prior)).add(new BigInteger("3"));
            }
        };

        WorkflowStepRunner runner = new WorkflowStepRunner(Arrays.asList(compute, add));
        Node first = new Node().name("Compute").type(new Node().blueId("Step/Compute"));
        Node second = new Node().name("Add").type(new Node().blueId("Step/Add"));

        Map<String, Object> results = runner.run(new SequentialWorkflow(), Arrays.asList(first, second), new Node(), null);

        assertEquals(2, results.size());
        assertEquals(new BigInteger("4"), results.get("Compute"));
        assertEquals(new BigInteger("7"), results.get("Add"));
    }

    @Test
    void runUsesStepFallbackNameWhenNameMissing() {
        WorkflowStepExecutor executor = new WorkflowStepExecutor() {
            @Override
            public Set<String> supportedBlueIds() {
                return Collections.unmodifiableSet(new LinkedHashSet<String>(Collections.singletonList("Step/Only")));
            }

            @Override
            public Object execute(StepExecutionArgs args) {
                return "ok";
            }
        };

        WorkflowStepRunner runner = new WorkflowStepRunner(Collections.singletonList(executor));
        Node unnamed = new Node().type(new Node().blueId("Step/Only"));

        Map<String, Object> results = runner.run(new SequentialWorkflow(), Collections.singletonList(unnamed), new Node(), null);

        assertEquals(1, results.size());
        assertTrue(results.containsKey("Step1"));
        assertEquals("ok", results.get("Step1"));
    }

    @Test
    void runResolvesInlineDerivedStepTypeChains() {
        WorkflowStepExecutor executor = new WorkflowStepExecutor() {
            @Override
            public Set<String> supportedBlueIds() {
                return Collections.unmodifiableSet(new LinkedHashSet<String>(Collections.singletonList("Step/Only")));
            }

            @Override
            public Object execute(StepExecutionArgs args) {
                return "ok";
            }
        };

        WorkflowStepRunner runner = new WorkflowStepRunner(Collections.singletonList(executor));
        Node derived = new Node().type(new Node()
                .blueId("Step/Derived")
                .type(new Node().blueId("Step/Only")));

        Map<String, Object> results = runner.run(new SequentialWorkflow(), Collections.singletonList(derived), new Node(), null);

        assertEquals(1, results.size());
        assertEquals("ok", results.get("Step1"));
    }
}
