package eu.domibus.plugin.ws.backend.dispatch;

import eu.domibus.plugin.ws.AbstractBackendWSIT;
import eu.domibus.plugin.ws.backend.WSBackendMessageLogEntity;
import eu.domibus.plugin.ws.backend.WSBackendMessageType;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import javax.xml.soap.SOAPMessage;
import java.util.Base64;
import java.util.UUID;

/**
 * @author François Gautier
 * @since 5.0
 */
public class WSPluginDispatcherIT extends AbstractBackendWSIT {

    private int backendPort;

    @Autowired
    private WSPluginMessageBuilder wsPluginMessageBuilder;

    @Autowired
    private WSPluginDispatcher wsPluginDispatcher;

    @Test
    @Disabled("[EDELIVERY-8828] WSPLUGIN: tests for rest methods ignored")
    public void sendSuccess() {
        WSBackendMessageLogEntity wsBackendMessageLogEntity = new WSBackendMessageLogEntity();
        wsBackendMessageLogEntity.setMessageId(UUID.randomUUID().toString());
        wsBackendMessageLogEntity.setType(WSBackendMessageType.SEND_SUCCESS);
        SOAPMessage msgToSend = wsPluginMessageBuilder.buildSOAPMessage(wsBackendMessageLogEntity);
        wsPluginDispatcher.dispatch(
                msgToSend,
                "http://localhost:" + backendPort + "/backend");

        String[] authHeader = msgToSend.getMimeHeaders().getHeader("Authorization");
        Assertions.assertNotNull(authHeader);
        String decodedCredentials = new String(Base64.getDecoder().decode(authHeader[0].replace("Basic ", "")));
        Assertions.assertTrue(decodedCredentials.contains("usertest"));
        Assertions.assertTrue(decodedCredentials.contains("passwordtest"));
    }
}
