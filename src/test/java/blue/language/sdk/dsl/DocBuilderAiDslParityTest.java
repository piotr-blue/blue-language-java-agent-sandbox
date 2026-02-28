package blue.language.sdk.dsl;

import blue.language.model.Node;
import blue.language.sdk.DocBuilder;
import blue.language.types.conversation.ChatMessage;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class DocBuilderAiDslParityTest {

    @Test
    void aiBuilderGeneratesPermissionSubscriptionAndStatusContracts() {
        Node built = DocBuilder.doc()
                .name("AI integration parity")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmProviderSessionId", "session-llm-1")
                .ai("mealAI")
                    .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
                    .permissionFrom("ownerChannel")
                    .statusPath("/mealAI/status")
                    .contextPath("/mealAI/context")
                    .requesterId("MEAL_PLANNER")
                    .done()
                .buildDocument();

        assertEquals("pending", built.getAsText("/mealAI/status/value"));
        assertEquals(0, built.getAsNode("/mealAI/context").getProperties().size());

        assertEquals("MyOS/Single Document Permission Grant Requested",
                built.getAsText("/contracts/aiMEALAIRequestPermission/steps/0/event/type/value"));
        assertEquals("ownerChannel",
                built.getAsText("/contracts/aiMEALAIRequestPermission/steps/0/event/onBehalfOf/value"));
        assertEquals("REQ_MEALAI",
                built.getAsText("/contracts/aiMEALAIRequestPermission/steps/0/event/requestId/value"));
        assertEquals("${document('/llmProviderSessionId')}",
                built.getAsText("/contracts/aiMEALAIRequestPermission/steps/0/event/targetSessionId/value"));

        assertEquals("MyOS/Subscribe to Session Requested",
                built.getAsText("/contracts/aiMEALAISubscribe/steps/0/event/type/value"));
        assertEquals("SUB_MEALAI",
                built.getAsText("/contracts/aiMEALAISubscribe/steps/0/event/subscription/id/value"));

        assertEquals("/mealAI/status",
                built.getAsText("/contracts/aiMEALAISubscriptionReady/steps/0/changeset/0/path/value"));
        assertEquals("ready",
                built.getAsText("/contracts/aiMEALAISubscriptionReady/steps/0/changeset/0/val/value"));
    }

    @Test
    void askAIGeneratesCallOperationRequestedWithContextAndPromptExpression() {
        Node built = DocBuilder.doc()
                .name("AI ask parity")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmProviderSessionId", "session-llm-2")
                .set("/prompt", "Return JSON only")
                .set("/maxCalories", 3000)
                .ai("mealAI")
                    .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
                    .permissionFrom("ownerChannel")
                    .statusPath("/mealAI/status")
                    .contextPath("/mealAI/context")
                    .requesterId("MEAL_PLANNER")
                    .done()
                .operation("requestMealPlan")
                    .channel("ownerChannel")
                    .description("Request meal plan")
                    .steps(steps -> steps.askAI("mealAI", "GeneratePlan", prompt -> prompt
                            .text(DocBuilder.expr("document('/prompt')"))
                            .text("Keep total calories <= ${document('/maxCalories')}")
                            .text("Meal request: ${event.message.request}")))
                    .done()
                .buildDocument();

        String eventPath = "/contracts/requestMealPlanImpl/steps/0/event";
        assertEquals("MyOS/Call Operation Requested", built.getAsText(eventPath + "/type/value"));
        assertEquals("ownerChannel", built.getAsText(eventPath + "/onBehalfOf/value"));
        assertEquals("${document('/llmProviderSessionId')}", built.getAsText(eventPath + "/targetSessionId/value"));
        assertEquals("provideInstructions", built.getAsText(eventPath + "/operation/value"));
        assertEquals("MEAL_PLANNER", built.getAsText(eventPath + "/request/requester/value"));
        assertEquals("${document('/mealAI/context')}", built.getAsText(eventPath + "/request/context/value"));

        String instructions = built.getAsText(eventPath + "/request/instructions/value");
        assertTrue(instructions.contains("document('/prompt')"), instructions);
        assertTrue(instructions.contains("document('/maxCalories')"), instructions);
        assertTrue(instructions.contains("event.message.request"), instructions);
    }

    @Test
    void onAIResponseBuildsSubscriptionMatcherAndAutoContextSave() {
        Node built = DocBuilder.doc()
                .name("AI response parity")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmProviderSessionId", "session-llm-3")
                .ai("mealAI")
                    .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
                    .permissionFrom("ownerChannel")
                    .statusPath("/mealAI/status")
                    .contextPath("/mealAI/context")
                    .requesterId("MEAL_PLANNER")
                    .done()
                .onAIResponse("mealAI", "onMealPlan", steps -> steps
                        .replaceValue("MarkDone", "/mealAI/lastResult", "processed"))
                .buildDocument();

        String workflow = "/contracts/onMealPlan";
        assertEquals("triggeredEventChannel", built.getAsText(workflow + "/channel/value"));
        assertEquals("MyOS/Subscription Update", built.getAsText(workflow + "/event/type/value"));
        assertEquals("SUB_MEALAI", built.getAsText(workflow + "/event/subscriptionId/value"));
        assertEquals("Conversation/Response", built.getAsText(workflow + "/event/update/type/value"));
        assertEquals("MEAL_PLANNER",
                built.getAsText(workflow + "/event/update/inResponseTo/incomingEvent/requester/value"));

        assertEquals("/mealAI/context",
                built.getAsText(workflow + "/steps/0/changeset/0/path/value"));
        assertEquals("${event.update.context}",
                built.getAsText(workflow + "/steps/0/changeset/0/val/value"));
        assertEquals("/mealAI/lastResult",
                built.getAsText(workflow + "/steps/1/changeset/0/path/value"));
    }

    @Test
    void multipleAiIntegrationsStayIndependent() {
        Node built = DocBuilder.doc()
                .name("Two AI parity")
                .channels("aliceChannel", "bobChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/aliceSessionId", "session-a")
                .set("/bobSessionId", "session-b")
                .ai("analyst")
                    .sessionId(DocBuilder.expr("document('/aliceSessionId')"))
                    .permissionFrom("aliceChannel")
                    .contextPath("/integrations/analyst/context")
                    .statusPath("/integrations/analyst/status")
                    .requesterId("ALICE_ANALYST")
                    .done()
                .ai("validator")
                    .sessionId(DocBuilder.expr("document('/bobSessionId')"))
                    .permissionFrom("bobChannel")
                    .contextPath("/integrations/validator/context")
                    .statusPath("/integrations/validator/status")
                    .requesterId("BOB_VALIDATOR")
                    .done()
                .onInit("kickoff", steps -> steps
                        .askAI("analyst", "AskAnalyst", prompt -> prompt.text("Analyze"))
                        .askAI("validator", "AskValidator", prompt -> prompt.text("Validate")))
                .buildDocument();

        String first = "/contracts/kickoff/steps/0/event";
        assertEquals("aliceChannel", built.getAsText(first + "/onBehalfOf/value"));
        assertEquals("${document('/aliceSessionId')}", built.getAsText(first + "/targetSessionId/value"));
        assertEquals("ALICE_ANALYST", built.getAsText(first + "/request/requester/value"));
        assertEquals("${document('/integrations/analyst/context')}", built.getAsText(first + "/request/context/value"));

        String second = "/contracts/kickoff/steps/1/event";
        assertEquals("bobChannel", built.getAsText(second + "/onBehalfOf/value"));
        assertEquals("${document('/bobSessionId')}", built.getAsText(second + "/targetSessionId/value"));
        assertEquals("BOB_VALIDATOR", built.getAsText(second + "/request/requester/value"));
        assertEquals("${document('/integrations/validator/context')}", built.getAsText(second + "/request/context/value"));
    }

    @Test
    void onAIResponseSupportsExplicitResponseType() {
        Node built = DocBuilder.doc()
                .name("AI explicit type parity")
                .channel("ownerChannel")
                .myOsAdmin("myOsAdminChannel")
                .set("/llmProviderSessionId", "session-llm-4")
                .ai("mealAI")
                    .sessionId(DocBuilder.expr("document('/llmProviderSessionId')"))
                    .permissionFrom("ownerChannel")
                    .statusPath("/mealAI/status")
                    .contextPath("/mealAI/context")
                    .requesterId("MEAL_PLANNER")
                    .done()
                .onAIResponse("mealAI", "onChatMessage", ChatMessage.class, steps -> steps
                        .replaceValue("MarkSeen", "/mealAI/seen", true))
                .buildDocument();

        assertEquals("Conversation/Chat Message",
                built.getAsText("/contracts/onChatMessage/event/update/type/value"));
        assertEquals("MEAL_PLANNER",
                built.getAsText("/contracts/onChatMessage/event/update/inResponseTo/incomingEvent/requester/value"));
    }

    @Test
    void askAIRejectsUnknownIntegration() {
        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () ->
                DocBuilder.doc()
                        .name("AI unknown integration")
                        .operation("ask")
                            .channel("ownerChannel")
                            .steps(steps -> steps.askAI("missing", prompt -> prompt.text("hello")))
                            .done()
                        .buildDocument());
        assertTrue(ex.getMessage().contains("Unknown AI integration"));
    }
}
