package eu.domibus.plugin.ws.webservice.deprecated.mapper;

import eu.domibus.plugin.ws.generated.header.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.InputStream;

/**
 * @author François Gautier
 * @since 5.0
 */
@Deprecated
public class WSPluginMessagingMapperTest {

    public static final String SUBMIT_MESSAGE_MESSAGING_XML = "submitMessage_messaging.xml";

    @Test
    public void testConversion() throws JAXBException {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream(SUBMIT_MESSAGE_MESSAGING_XML);
        JAXBContext jaxbContext = JAXBContext.newInstance(eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging.class.getPackage().getName());

        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging messagingFromFile =
                ((JAXBElement<eu.domibus.common.model.org.oasis_open.docs.ebxml_msg.ebms.v3_0.ns.core._200704.Messaging>) unmarshaller.unmarshal(xmlStream)).getValue();


        Messaging messaging = new WSPluginMessagingMapperImpl().messagingToEntity(messagingFromFile);

        Assertions.assertNotNull(messaging.getUserMessage());

    }


}
