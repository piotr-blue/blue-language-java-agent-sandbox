package blue.language.samples.paynote.counter;

import blue.language.model.Node;
import blue.language.model.TypeBlueId;
import blue.language.samples.paynote.types.core.CoreTypes;

import java.util.List;

@TypeBlueId("Counter-Demo-BlueId")
public class CounterDocument {
    public String name;
    public Integer counter;
    public CounterContracts contracts;

    @Override
    public String toString() {
        return "CounterDocument{" +
                "name='" + name + '\'' +
                ", counter=" + counter +
                ", contracts=" + contracts +
                '}';
    }

    public static class CounterContracts {
        public CounterTimelineChannel ownerChannel;
        public CounterOperation increment;
        public CounterSequentialWorkflowOperation incrementImpl;
        public CounterOperation decrement;
        public CounterSequentialWorkflowOperation decrementImpl;

        // Added in Java-authored extension examples.
        public CoreTypes.DocumentUpdateChannel counterChanged;
        public CounterSequentialWorkflow onCounterChanged;
        public CounterOperation say;
        public CounterSequentialWorkflowOperation sayImpl;

        @Override
        public String toString() {
            return "CounterContracts{" +
                    "ownerChannel=" + ownerChannel +
                    ", increment=" + increment +
                    ", incrementImpl=" + incrementImpl +
                    ", decrement=" + decrement +
                    ", decrementImpl=" + decrementImpl +
                    ", counterChanged=" + counterChanged +
                    ", onCounterChanged=" + onCounterChanged +
                    ", say=" + say +
                    ", sayImpl=" + sayImpl +
                    '}';
        }
    }

    public static class CounterTimelineChannel {
        public String timelineId;

        @Override
        public String toString() {
            return "CounterTimelineChannel{" +
                    "timelineId='" + timelineId + '\'' +
                    '}';
        }
    }

    public static class CounterOperation {
        public String description;
        public String channel;
        public Node request;

        @Override
        public String toString() {
            return "CounterOperation{" +
                    "description='" + description + '\'' +
                    ", channel='" + channel + '\'' +
                    ", request=" + request +
                    '}';
        }
    }

    public static class CounterSequentialWorkflowOperation {
        public String operation;
        public List<Node> steps;

        @Override
        public String toString() {
            return "CounterSequentialWorkflowOperation{" +
                    "operation='" + operation + '\'' +
                    ", steps=" + steps +
                    '}';
        }
    }

    public static class CounterSequentialWorkflow {
        public String channel;
        public Node event;
        public List<Node> steps;

        @Override
        public String toString() {
            return "CounterSequentialWorkflow{" +
                    "channel='" + channel + '\'' +
                    ", event=" + event +
                    ", steps=" + steps +
                    '}';
        }
    }
}
