package eu.domibus.core.pmode;

import com.ctc.wstx.exc.WstxParsingException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Mpcs;
import eu.domibus.core.util.xml.XMLUtilImpl;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_SCHEMAFACTORY;
import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by Cosmin Baciu on 16-Sep-16.
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class SamplePModeTestIT {

    @Configuration
    static class ContextConfiguration {

        @Bean
        public DomibusPropertyProvider domibusPropertyProvider() {
            return Mockito.mock(DomibusPropertyProvider.class);
        }

        @Bean
        public XMLUtil xmlUtil() {
            return new XMLUtilImpl(domibusPropertyProvider());
        }

        @Bean
        public JAXBContext createJaxbContent() throws JAXBException {
            return JAXBContext.newInstance(PModeBeanConfiguration.COMMON_MODEL_CONFIGURATION_JAXB_CONTEXT_PATH);
        }
    }

    @Autowired
    XMLUtil xmlUtil;

    @Autowired
    JAXBContext jaxbContext;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void testRetentionValuesForBluePmode() throws Exception {
        testRetentionUndownloadedIsBiggerThanZero("src/main/conf/pmodes/domibus-gw-sample-pmode-blue.xml");
    }

    @Test
    public void testRetentionValuesForRedPmode() throws Exception {
        testRetentionUndownloadedIsBiggerThanZero("src/main/conf/pmodes/domibus-gw-sample-pmode-red.xml");
    }

    protected void testRetentionUndownloadedIsBiggerThanZero(String location) throws Exception {
        eu.domibus.common.model.configuration.Configuration  bluePmode = readPMode(location);
        assertNotNull(bluePmode);
        Mpcs mpcsXml = bluePmode.getMpcsXml();
        assertNotNull(mpcsXml);
        List<Mpc> mpcList = mpcsXml.getMpc();
        assertNotNull(mpcList);
        for (Mpc mpc : mpcList) {
            assertTrue(mpc.getRetentionUndownloaded() > 0);
            assertEquals(0, mpc.getRetentionDownloaded());
        }
    }

    protected eu.domibus.common.model.configuration.Configuration  readPMode(String location) throws Exception {
        File pmodeFile = new File(location);
        String pmodeContent = FileUtils.readFileToString(pmodeFile, "UTF-8");

        UnmarshallerResult unmarshal = xmlUtil.unmarshal(false, jaxbContext, IOUtils.toInputStream(pmodeContent, "UTF-8"), null);
        return unmarshal.getResult();
    }

    public static final String SCHEMAS_DIR = "schemas/";
    public static final String DOMIBUS_PMODE_XSD = "domibus-pmode.xsd";

    @Test
    public void testMarshalling() throws Exception {
        Mockito.when(domibusPropertyProvider.getProperty(DOMIBUS_SCHEMAFACTORY)).thenReturn("com.sun.org.apache.xerces.internal.jaxp.validation.XMLSchemaFactory");

        InputStream xsdStream = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);
        InputStream xsdStream2 = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);
        InputStream xsdStream3 = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);
        InputStream xsdStream4 = getClass().getClassLoader().getResourceAsStream(SCHEMAS_DIR + DOMIBUS_PMODE_XSD);

        InputStream is = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-configuration-valid.xml");
        byte[] bytes = IOUtils.toByteArray(is);

        ByteArrayInputStream xmlStream = new ByteArrayInputStream(bytes);

        UnmarshallerResult unmarshallerResult = xmlUtil.unmarshal(false, jaxbContext, xmlStream, xsdStream);
        eu.domibus.common.model.configuration.Configuration configuration = unmarshallerResult.getResult();

        byte[] bytes2 = xmlUtil.marshal(jaxbContext, configuration, xsdStream2);
        xmlStream = new ByteArrayInputStream(bytes2);
        unmarshallerResult = xmlUtil.unmarshal(false, jaxbContext, xmlStream, xsdStream3);
        eu.domibus.common.model.configuration.Configuration configuration2 = unmarshallerResult.getResult();

        byte[] bytes3 = xmlUtil.marshal(jaxbContext, configuration2, xsdStream4);

        assertArrayEquals(bytes2, bytes3);
        assertNotNull(configuration2.getBusinessProcesses());
    }

    @Test
    public void testUnmarshal_PreventXxeAttack() {
        try {
            readPMode("src/test/resources/pmodes/domibus-pmode-red-xxe-vulnerability.xml");
            fail("Should have prevented the external entity since DTDs should be disabled");
        } catch (Exception e) {
            Throwable cause = ExceptionUtils.getRootCause(e);
            Assertions.assertTrue(cause instanceof WstxParsingException && cause.getMessage().startsWith("Undeclared general entity \"xxe\""),
                    "Should have thrown the correct exception indicating the XML entity is unknown");
        }
    }
}
