package eu.domibus.core.message.reliability;

import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.core.message.nonrepudiation.NonRepudiationConstants;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import org.junit.jupiter.api.extension.ExtendWith;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.soap.SOAPException;
import javax.xml.soap.SOAPHeader;
import javax.xml.soap.SOAPMessage;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;


/**
 * @author Joze Rihtarsic
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class ReliabilityCheckerTest {


    ReliabilityChecker testInstance = new ReliabilityChecker();

    

    @Test
    @Disabled("EDELIVERY-6896")
    public void getNonRepudiationDetailsNodeFromReceiptEmpty(@Mocked SOAPMessage response,
                                                             @Mocked SOAPHeader header,
                                                             @Mocked NodeList nodelist) throws SOAPException, EbMS3Exception {
        String messageId = "TestMessageId";
        new Expectations() {{
            response.getSOAPHeader();
            result = header;
            header.getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN);
            result = nodelist;
            nodelist.getLength();
            result = 0;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> testInstance.getNonRepudiationDetailsNodeFromReceipt(response, messageId));
        assertThat(ebMS3Exception.getMessage(), containsString("Invalid NonRepudiationInformation: No element found"));

    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void getNonRepudiationDetailsNodeFromReceiptNull(@Mocked SOAPMessage response,
                                                            @Mocked SOAPHeader header,
                                                            @Mocked NodeList nodelist) throws SOAPException, EbMS3Exception {
        String messageId = "TestMessageId";
        new Expectations() {{
            response.getSOAPHeader();
            result = header;
            header.getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN);
            result = nodelist;
            nodelist.getLength();
            result = 1;
            nodelist.item(0);
            result = null;
        }};

        EbMS3Exception ebMS3Exception = Assertions.assertThrows(EbMS3Exception.class, () -> testInstance.getNonRepudiationDetailsNodeFromReceipt(response, messageId));
        assertThat(ebMS3Exception.getMessage(), containsString("Invalid NonRepudiationInformation: No element found"));

    }

    @Test
    public void getNonRepudiationDetailsNodeFromReceiptExists(@Mocked SOAPMessage response,
                                                              @Mocked SOAPHeader header,
                                                              @Mocked NodeList nodelist,
                                                              @Mocked Node node) throws SOAPException, EbMS3Exception {

        String messageId = "TestMessageId";
        new Expectations() {{
            response.getSOAPHeader();
            result = header;
            header.getElementsByTagNameNS(NonRepudiationConstants.NS_NRR, NonRepudiationConstants.NRR_LN);
            result = nodelist;
            nodelist.getLength();
            result = 1;
            nodelist.item(0);
            result = node;
        }};
        Node result = testInstance.getNonRepudiationDetailsNodeFromReceipt(response, messageId);
        assertEquals(node, result);
    }
}
