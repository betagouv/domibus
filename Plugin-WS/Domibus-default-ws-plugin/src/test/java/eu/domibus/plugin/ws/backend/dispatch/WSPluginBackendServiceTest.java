package eu.domibus.plugin.ws.backend.dispatch;

import eu.domibus.common.MessageDeletedBatchEvent;
import eu.domibus.common.MessageDeletedEvent;
import eu.domibus.common.MessageSendSuccessEvent;
import eu.domibus.ext.services.UserMessageExtService;
import eu.domibus.messaging.MessageConstants;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import eu.domibus.plugin.ws.backend.reliability.retry.WSPluginBackendScheduleRetryService;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRule;
import eu.domibus.plugin.ws.backend.rules.WSPluginDispatchRulesService;
import eu.domibus.plugin.ws.property.WSPluginPropertyManager;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.hamcrest.CoreMatchers;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static eu.domibus.plugin.ws.backend.WSBackendMessageType.DELETED_BATCH;
import static eu.domibus.plugin.ws.backend.WSBackendMessageType.SEND_SUCCESS;
import static eu.domibus.plugin.ws.property.WSPluginPropertyManager.PUSH_ENABLED;
import static org.apache.commons.lang3.StringUtils.equalsAnyIgnoreCase;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * @author François Gautier
 * @since 5.0
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class WSPluginBackendServiceTest {

    public static final String FINAL_RECIPIENT = "finalRecipient";
    public static final String FINAL_RECIPIENT2 = "finalRecipient2";
    public static final String FINAL_RECIPIENT3 = "finalRecipient3";
    public static final String ORIGINAL_SENDER = "originalSender";
    public static final String MESSAGE_ID = "messageId";
    public static final long MESSAGE_ENTITY_ID = 555;
    public static final String RULE_NAME = "ruleName";

    @Tested
    private WSPluginBackendService wsPluginBackendService;

    @Injectable
    private WSPluginBackendScheduleRetryService retryService;

    @Injectable
    private WSPluginDispatchRulesService wsBackendRulesService;

    @Injectable
    private UserMessageExtService userMessageExtService;

    @Injectable
    private WSPluginPropertyManager wsPluginPropertyManager;

    @Test
    public void sendSuccess(@Injectable WSPluginDispatchRule wsPluginDispatchRule) {
        MessageSendSuccessEvent messageSendSuccessEvent = getMessageSendSuccessEvent(FINAL_RECIPIENT);
        new Expectations() {{
            wsPluginPropertyManager.getKnownPropertyValue(PUSH_ENABLED);
            result = "true";

            wsBackendRulesService.getRulesByRecipient(FINAL_RECIPIENT);
            times = 1;
            result = Collections.singletonList(wsPluginDispatchRule);


            wsPluginDispatchRule.getTypes();
            result = Arrays.asList(SEND_SUCCESS, WSBackendMessageType.MESSAGE_STATUS_CHANGE);

            wsPluginDispatchRule.getRuleName();
            result = RULE_NAME;
        }};

        wsPluginBackendService.send(messageSendSuccessEvent, SEND_SUCCESS);

        new FullVerifications() {{
            retryService.schedule(MESSAGE_ID, MESSAGE_ENTITY_ID , messageSendSuccessEvent.getProps(), wsPluginDispatchRule, SEND_SUCCESS);
            times = 1;
        }};
    }

    private MessageSendSuccessEvent getMessageSendSuccessEvent(String finalRecipient) {
        HashMap<String, String> properties = new HashMap<>();
        if (StringUtils.isNotBlank(finalRecipient)) {
            properties.put(MessageConstants.FINAL_RECIPIENT, finalRecipient);
        }
        properties.put(MessageConstants.ORIGINAL_SENDER, ORIGINAL_SENDER);
        return new MessageSendSuccessEvent(MESSAGE_ENTITY_ID, MESSAGE_ID, properties);
    }

    private void testSend(String knownProperty) {
        new Expectations() {{
            wsPluginPropertyManager.getKnownPropertyValue(PUSH_ENABLED);
            result = knownProperty;
        }};
        wsPluginBackendService.send(getMessageSendSuccessEvent(""), SEND_SUCCESS);

        new FullVerifications() {
        };
    }

    @Test
    public void sendSuccess_noRecipient() {
        testSend("true");
    }

    @Test
    public void sendSuccess_disabled_wrongValue() {
        testSend("x");
    }

    @Test
    public void sendSuccess_disabled() {
        testSend("false");
    }

    @Test
    public void noRules() {
        new Expectations() {{
            wsPluginPropertyManager.getKnownPropertyValue(PUSH_ENABLED);
            result = "true";

            wsBackendRulesService.getRulesByRecipient(FINAL_RECIPIENT);
            times = 1;
            result = new ArrayList<>();
        }};

        wsPluginBackendService.send(getMessageSendSuccessEvent(FINAL_RECIPIENT), SEND_SUCCESS);

        new FullVerifications() {
        };
    }

    @Test
    public void sendNotificationsForOneRule_empty(@Injectable WSPluginDispatchRule wsPluginDispatchRule) {
        List<WSBackendMessageType> types = new ArrayList<>();
        new Expectations() {{
            wsPluginDispatchRule.getTypes();
            result = types;
        }};

        wsPluginBackendService.sendNotificationsForOneRule(
                FINAL_RECIPIENT,
                new ArrayList<>(),
                DELETED_BATCH,
                wsPluginDispatchRule);

        new FullVerifications() {
        };
    }

    @Test
    public void sendNotificationsForOneRule_1notification(@Injectable WSPluginDispatchRule wsPluginDispatchRule) {
        List<WSBackendMessageType> types = Arrays.asList(WSBackendMessageType.DELETED, DELETED_BATCH);
        ArrayList<String> messageIds = new ArrayList<>();

        new Expectations() {{
            wsPluginDispatchRule.getTypes();
            result = types;
        }};

        wsPluginBackendService.sendNotificationsForOneRule(
                FINAL_RECIPIENT,
                messageIds,
                DELETED_BATCH,
                wsPluginDispatchRule);

        new FullVerifications() {{
            retryService.schedule(messageIds, FINAL_RECIPIENT, wsPluginDispatchRule, DELETED_BATCH);
            times = 1;
        }};
    }

    @Test
    public void sendNotificationsForOneRecipient(@Injectable WSPluginDispatchRule rule1,
                                                 @Injectable WSPluginDispatchRule rule2,
                                                 @Injectable WSBackendMessageType wsBackendMessageType) {
        List<String> messageIds = new ArrayList<>();
        new Expectations(wsPluginBackendService) {{
            wsPluginBackendService.sendNotificationsForOneRule(FINAL_RECIPIENT, messageIds, wsBackendMessageType, rule1);
            times = 1;

            wsPluginBackendService.sendNotificationsForOneRule(FINAL_RECIPIENT, messageIds, wsBackendMessageType, rule2);
            times = 1;
        }};
        wsPluginBackendService.sendNotificationsForOneRecipient(FINAL_RECIPIENT, messageIds, Arrays.asList(rule1, rule2), wsBackendMessageType);
        new FullVerifications() {
        };
    }

    @Test
    public void getRulesForFinalRecipients(@Injectable WSPluginDispatchRule rule1,
                                           @Injectable WSPluginDispatchRule rule2,
                                           @Injectable WSPluginDispatchRule rule3) {
        Map<String, List<String>> messageIdsPerRecipient = new HashMap<>();
        messageIdsPerRecipient.put(FINAL_RECIPIENT, Arrays.asList("1", "2"));
        messageIdsPerRecipient.put(FINAL_RECIPIENT2, Arrays.asList("1", "2"));
        messageIdsPerRecipient.put(FINAL_RECIPIENT3, Arrays.asList("1", "2"));
        List<WSPluginDispatchRule> rulesRecipient = Arrays.asList(rule1, rule2);
        List<WSPluginDispatchRule> rulesRecipient2 = Arrays.asList(rule3, rule2);

        new Expectations() {{
            wsBackendRulesService.getRulesByRecipient(FINAL_RECIPIENT);
            this.result = rulesRecipient;

            wsBackendRulesService.getRulesByRecipient(FINAL_RECIPIENT2);
            this.result = rulesRecipient2;

            wsBackendRulesService.getRulesByRecipient(FINAL_RECIPIENT3);
            this.result = new ArrayList<>();
        }};

        List<RulesPerRecipient> rulesForFinalRecipients =
                wsPluginBackendService.getRulesForFinalRecipients(messageIdsPerRecipient);

        new FullVerifications() {
        };

        Assertions.assertEquals(3, rulesForFinalRecipients.size());

        Assertions.assertEquals(rulesRecipient, getRules(rulesForFinalRecipients, FINAL_RECIPIENT));
        Assertions.assertEquals(rulesRecipient2, getRules(rulesForFinalRecipients, FINAL_RECIPIENT2));
        Assertions.assertEquals(0, getRules(rulesForFinalRecipients, FINAL_RECIPIENT3).size());
    }

    private List<WSPluginDispatchRule> getRules(List<RulesPerRecipient> rulesForFinalRecipients, String finalRecipient) {
        return rulesForFinalRecipients.stream()
                .filter(rulesPerRecipient -> equalsAnyIgnoreCase(rulesPerRecipient.getFinalRecipient(), finalRecipient))
                .findAny()
                .map(RulesPerRecipient::getRules)
                .orElse(null);
    }

    @Test
    public void sortMessageIdsPerFinalRecipients() {
        MessageDeletedBatchEvent messageDeletedBatchEvent = new MessageDeletedBatchEvent();
        List<MessageDeletedEvent> messageIdsPerRecipient = Stream
                .of("1", "2")
                .map(s -> getMessageDeletedEvent(s, FINAL_RECIPIENT))
                .collect(Collectors.toList());
        messageIdsPerRecipient.add(getMessageDeletedEvent("3", FINAL_RECIPIENT2));
        messageDeletedBatchEvent.setMessageDeletedEvents(messageIdsPerRecipient);

        Map<String, List<String>> stringListMap = wsPluginBackendService.sortMessageIdsPerFinalRecipients(messageDeletedBatchEvent);

        assertThat(stringListMap.get(FINAL_RECIPIENT), CoreMatchers.hasItems("1", "2"));
        Assertions.assertEquals(2, stringListMap.get(FINAL_RECIPIENT).size());
        assertThat(stringListMap.get(FINAL_RECIPIENT2), CoreMatchers.hasItems("3"));
        Assertions.assertEquals(1, stringListMap.get(FINAL_RECIPIENT2).size());
        new FullVerifications() {
        };
    }

    private MessageDeletedEvent getMessageDeletedEvent(String s, String finalRecipient) {
        MessageDeletedEvent messageDeletedEvent = new MessageDeletedEvent();
        messageDeletedEvent.setMessageId(s);
        messageDeletedEvent.addProperty(MessageConstants.FINAL_RECIPIENT, finalRecipient);
        return messageDeletedEvent;
    }

    @Test
    public void addMessageIdToMap() {
        HashMap<String, List<String>> messageIdGroupedByRecipient = new HashMap<>();

        wsPluginBackendService.addMessageIdToMap(getMessageDeletedEvent(MESSAGE_ID, FINAL_RECIPIENT), messageIdGroupedByRecipient);

        Assertions.assertEquals(1, messageIdGroupedByRecipient.size());
        Assertions.assertEquals(1, messageIdGroupedByRecipient.get(FINAL_RECIPIENT).size());
        Assertions.assertEquals(MESSAGE_ID, messageIdGroupedByRecipient.get(FINAL_RECIPIENT).get(0));

    }

    @Test
    public void addMessageIdToMap_emptyRecipient() {
        HashMap<String, List<String>> messageIdGroupedByRecipient = new HashMap<>();

        wsPluginBackendService.addMessageIdToMap(new MessageDeletedEvent(), messageIdGroupedByRecipient);

        Assertions.assertEquals(0, messageIdGroupedByRecipient.size());
    }

    @Test
    public void send(@Injectable WSPluginDispatchRule rule1,
                     @Injectable WSPluginDispatchRule rule2,
                     @Injectable WSPluginDispatchRule rule3) {
        MessageDeletedBatchEvent messageDeletedBatchEvent = new MessageDeletedBatchEvent();
        Stream<String> stringStream = Stream
                .of("1", "2", "3");
        List<MessageDeletedEvent> messageIdsPerRecipient = stringStream
                .map(s -> getMessageDeletedEvent(s, FINAL_RECIPIENT))
                .collect(Collectors.toList());
        messageDeletedBatchEvent.setMessageDeletedEvents(messageIdsPerRecipient);

        Map<String, List<String>> sorted = new HashMap<>();
        List<String> msgRecipient1 = Arrays.asList("1", "2");
        List<String> msgRecipient2 = Arrays.asList("1", "2");
        sorted.put(FINAL_RECIPIENT, msgRecipient1);
        sorted.put(FINAL_RECIPIENT2, msgRecipient2);
        List<RulesPerRecipient> rules = new ArrayList<>();
        List<WSPluginDispatchRule> rulesRecipient = Arrays.asList(rule1, rule2);
        List<WSPluginDispatchRule> rulesRecipient2 = Arrays.asList(rule3, rule2);
        rules.add(new RulesPerRecipient(FINAL_RECIPIENT, rulesRecipient));
        rules.add(new RulesPerRecipient(FINAL_RECIPIENT2, rulesRecipient2));

        new Expectations(wsPluginBackendService) {{
            wsPluginBackendService.sortMessageIdsPerFinalRecipients(messageDeletedBatchEvent);
            result = sorted;

            wsPluginBackendService.getRulesForFinalRecipients(sorted);
            result = rules;

            wsPluginBackendService.sendNotificationsForOneRecipient(FINAL_RECIPIENT, msgRecipient1, rulesRecipient, DELETED_BATCH);
            times = 1;
            wsPluginBackendService.sendNotificationsForOneRecipient(FINAL_RECIPIENT2, msgRecipient2, rulesRecipient2, DELETED_BATCH);
            times = 1;
        }};
        wsPluginBackendService.send(messageDeletedBatchEvent, DELETED_BATCH);

        new FullVerifications() {
        };
    }
}
