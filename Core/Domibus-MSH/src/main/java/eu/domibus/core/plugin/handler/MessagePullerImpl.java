package eu.domibus.core.plugin.handler;

import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.plugin.handler.MessagePuller;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service used for initiating pull requests through this interface (split from DatabaseMessageHandler)
 *
 * @author Ion Perpegel
 * @since 5.0
 */
@Service
public class MessagePullerImpl implements MessagePuller {

    private final MessageExchangeService messageExchangeService;

    public MessagePullerImpl(MessageExchangeService messageExchangeService) {
        this.messageExchangeService = messageExchangeService;
    }

    @Override
    @Transactional
    @Timer(clazz = MessagePullerImpl.class, value = "initiatePull")
    @Counter(clazz = MessagePullerImpl.class, value = "initiatePull")
    public void initiatePull(String mpc) {
        messageExchangeService.initiatePullRequest(mpc);
    }

}
