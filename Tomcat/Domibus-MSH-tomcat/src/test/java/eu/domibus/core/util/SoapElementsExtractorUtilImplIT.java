package eu.domibus.core.util;

import eu.domibus.AbstractIT;
import eu.domibus.core.ebms3.SoapElementsExtractorUtilImpl;
import org.apache.commons.io.IOUtils;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.cxf.message.MessageImpl;
import org.apache.wss4j.common.WSS4JConstants;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import javax.mail.util.ByteArrayDataSource;
import java.io.InputStream;
import java.util.Objects;

/**
 * @author Lucian Furca
 * @since 5.2
 */
@Transactional
public class SoapElementsExtractorUtilImplIT extends AbstractIT {
    public static String ENCRYPTION_METHOD_ALGORITHM_RSA = "http://www.w3.org/2009/xmlenc11#rsa-oaep";

    @Autowired
    private SoapElementsExtractorUtilImpl soapElementsExtractorUtil;

    private SoapMessage soapMessage;

    @BeforeEach
    void beforeAll() throws Exception {
        soapMessage = new SoapMessage(new MessageImpl());
        final byte[] bytes = IOUtils.toByteArray(Objects.requireNonNull(this.getClass().getClassLoader()
                .getResourceAsStream("dataset/as4/soapMessageWithSecurityAlgorithms.xml")));
        ByteArrayDataSource bads = new ByteArrayDataSource(bytes, "test/xml");
        soapMessage.setContent(InputStream.class, bads.getInputStream());
    }

    @Test
    public void testGetSignatureAlgorithm() {
        String signatureAlgorithm = soapElementsExtractorUtil.getSignatureAlgorithm(soapMessage);
        Assertions.assertEquals(WSS4JConstants.RSA_SHA256, signatureAlgorithm);
    }

    @Test
    public void testGetEncryptionAlgorithm() {
        String encryptionAlgorithm = soapElementsExtractorUtil.getEncryptionAlgorithm(soapMessage);
        Assertions.assertEquals(ENCRYPTION_METHOD_ALGORITHM_RSA, encryptionAlgorithm);
    }
}
