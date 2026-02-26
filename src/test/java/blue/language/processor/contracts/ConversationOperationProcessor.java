package blue.language.processor.contracts;

import blue.language.model.Node;
import blue.language.processor.HandlerProcessor;
import blue.language.processor.ProcessorExecutionContext;
import blue.language.processor.model.ConversationOperation;
import blue.language.processor.model.JsonPatch;

import java.math.BigInteger;

public class ConversationOperationProcessor implements HandlerProcessor<ConversationOperation> {

    @Override
    public Class<ConversationOperation> contractType() {
        return ConversationOperation.class;
    }

    @Override
    public void execute(ConversationOperation contract, ProcessorExecutionContext context) {
        Node event = context.event();
        if (event == null || contract.getKey() == null) {
            return;
        }

        String requestedOperation = event.getAsText("/operation/value");
        if (requestedOperation == null) {
            requestedOperation = event.getAsText("/operation");
        }
        if (requestedOperation == null || !contract.getKey().equals(requestedOperation.trim())) {
            return;
        }

        Integer requestValue = event.getAsInteger("/message/request/value");
        if (requestValue == null) {
            requestValue = event.getAsInteger("/message/request");
        }
        if (requestValue == null) {
            return;
        }

        Node counterNode = context.documentAt("/counter");
        BigInteger current = BigInteger.ZERO;
        if (counterNode != null && counterNode.getValue() instanceof Number) {
            current = new BigInteger(counterNode.getValue().toString());
        }

        BigInteger delta = BigInteger.valueOf(requestValue.longValue());
        BigInteger next = "decrement".equals(contract.getKey())
                ? current.subtract(delta)
                : current.add(delta);

        context.applyPatch(JsonPatch.replace("/counter", new Node().value(next)));
    }
}
