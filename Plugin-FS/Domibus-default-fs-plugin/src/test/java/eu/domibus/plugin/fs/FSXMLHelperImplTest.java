package eu.domibus.plugin.fs;

import eu.domibus.plugin.fs.ebms3.*;
import eu.domibus.plugin.fs.worker.FSSendMessagesService;
import org.apache.commons.io.IOUtils;
import org.apache.commons.vfs2.FileContent;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemManager;
import org.apache.commons.vfs2.VFS;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.stream.XMLStreamException;
import java.io.IOException;
import java.io.InputStream;

/**
 * JUnit for {@link FSXMLHelperImpl} class
 *
 * @author Catalin Enache
 * @since 3.3.2
 */
public class FSXMLHelperImplTest {

    private FileObject testFolder;


    @BeforeEach
    public void setUp() throws Exception {
        FileSystemManager fileSystemManager = VFS.getManager();

        testFolder = fileSystemManager.resolveFile("ram:///FSXMLHelperTest");
        testFolder.createFolder();
    }

    @AfterEach
public void tearDown() throws Exception {
        testFolder.delete();
        testFolder.close();
    }

    @Test
    public void testParseXML() throws IOException, JAXBException, XMLStreamException {
        FileObject metadataFile;
        final String partyIdType = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";

        try (InputStream testMetadata = FSTestHelper.getTestResource(FSXMLHelper.class, "testParseXML_metadata.xml")) {
            metadataFile = testFolder.resolveFile("test_metadata.xml");
            metadataFile.createFile();
            FileContent metadataFileContent = metadataFile.getContent();
            IOUtils.copy(testMetadata, metadataFileContent.getOutputStream());
            metadataFile.close();
        }

        //tested method
        JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
        UserMessage userMessage = new FSXMLHelperImpl(jaxbContext).parseXML(metadataFile.getContent().getInputStream(), UserMessage.class);
        Assertions.assertNotNull(userMessage);
        Assertions.assertEquals(preparePartyId("domibus-blue", partyIdType), userMessage.getPartyInfo().getFrom().getPartyId());
        Assertions.assertEquals(preparePartyId("domibus-red", partyIdType), userMessage.getPartyInfo().getTo().getPartyId());
    }

    @Test
    public void testWriteXML() throws Exception {

        try (FileObject file = testFolder.resolveFile(FSSendMessagesService.METADATA_FILE_NAME);
             FileContent fileContent = file.getContent()) {

            //tested method
            JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
            new FSXMLHelperImpl(jaxbContext).writeXML(fileContent.getOutputStream(), UserMessage.class, prepareUserMessage());

            Assertions.assertNotNull(file);
            Assertions.assertTrue(FSSendMessagesService.METADATA_FILE_NAME.equals(file.getName().getBaseName()));
        }
        Assertions.assertEquals(prepareUserMessage(), FSTestHelper.getUserMessage(testFolder.resolveFile(FSSendMessagesService.METADATA_FILE_NAME).getContent().getInputStream()));

    }


    private UserMessage prepareUserMessage() {
        UserMessage userMessage = new UserMessage();
        final String partyIdType = "urn:oasis:names:tc:ebcore:partyid-type:unregistered";

        PartyInfo partyInfo = new PartyInfo();

        //from
        From from = new From();
        from.setPartyId(preparePartyId("domibus-blue", partyIdType));

        from.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/initiator");
        partyInfo.setFrom(from);

        //to
        To to = new To();
        to.setPartyId(preparePartyId("domibus-red", partyIdType));
        to.setRole("http://docs.oasis-open.org/ebxml-msg/ebms/v3.0/ns/core/200704/responder");
        partyInfo.setTo(to);

        userMessage.setPartyInfo(partyInfo);

        //collaboration info
        CollaborationInfo collaborationInfo = new CollaborationInfo();
        Service service = new Service();
        service.setValue("bdx:noprocess");
        service.setType("tc1");
        collaborationInfo.setService(service);
        collaborationInfo.setAction("TC1Leg1");
        userMessage.setCollaborationInfo(collaborationInfo);

        //message properties
        MessageProperties messageProperties = new MessageProperties();
        prepareProperty("originalSender", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C1", messageProperties);
        prepareProperty("finalRecipient", "urn:oasis:names:tc:ebcore:partyid-type:unregistered:C4", messageProperties);
        userMessage.setMessageProperties(messageProperties);

        return userMessage;
    }

    private void prepareProperty(final String name, final String value, MessageProperties messageProperties) {
        Property originalSenderPropertry = new Property();
        originalSenderPropertry.setName(name);
        originalSenderPropertry.setValue(value);
        messageProperties.getProperty().add(originalSenderPropertry);
    }

    private PartyId preparePartyId(final String value, final String type) {
        PartyId partyIdFrom = new PartyId();
        partyIdFrom.setValue(value);
        partyIdFrom.setType(type);
        return partyIdFrom;
    }

}
