package eu.domibus.api.util;

import eu.domibus.api.exceptions.XmlProcessingException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.apache.wss4j.common.ext.WSSecurityException;
import org.w3c.dom.Element;

/**
 * Utility class for extracting various elements from a cxf SoapMessage
 *
 * @author Lucian Furca
 * @since 5.2
 */
public interface SoapElementsExtractorUtil {

    /**
     * Extracts the encryption method algorithm string from a cxf SoapMessage
     *
     * @param soapMessage the SoapMessage from which the data is extracted
     * @throws WSSecurityException if the encryption algorithm does not correspond to any known encryption type(RSA, ECC)
     * @throws XmlProcessingException if parsing the xml document corresponding to the SoapMessage does not work correctly
     * @return the Encryption Algorithm
     */
    String getEncryptionAlgorithm(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException;

    /**
     * Extracts the signature method algorithm string from a SoapMessage
     *
     * @param soapMessage the SoapMessage from which the data is extracted
     * @throws WSSecurityException if the signature method algorithm does not correspond to any known signature type(RSA, ECC)
     * @throws XmlProcessingException if parsing the xml document corresponding to the SoapMessage does not work correctly
     * @return the Signature Algorithm
     */
    String getSignatureAlgorithm(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException;

    /**
     * Extracts the Security Header element from a SoapMessage
     *
     * @param soapMessage the SoapMessage from which the security header is extracted
     * @throws WSSecurityException if the security header can't be extracted
     * @throws XmlProcessingException if the obtained security header is null
     * @return the Security Header Element
     */
    Element extractSecurityHeaderElement(SoapMessage soapMessage) throws WSSecurityException, XmlProcessingException;

    /**
     * Reads a cxf SoapMessage and returns the entire content of the message as XML
     *
     * @param soapMessage the SoapMessage that will be transformed to XML
     * @return the entire XML content of the SoapMessage
     */
    String getSoapMessageAsString(SoapMessage soapMessage);
}
