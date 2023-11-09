package eu.domibus.api.util;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.exceptions.XmlProcessingException;
import eu.domibus.api.security.SecurityProfileException;
import org.apache.cxf.binding.soap.SoapMessage;
import org.w3c.dom.Element;

/**
 * Utility class for extracting various elements from a cxf SoapMessage
 *
 * @author Lucian Furca
 * @since 5.2
 */
public interface SoapElementsExtractorUtil {

    String ENCRYPTION_METHOD_ALGORITHM_RSA = "http://www.w3.org/2009/xmlenc11#rsa-oaep";
    String ENCRYPTION_METHOD_ALGORITHM_ECC = "http://www.w3.org/2001/04/xmlenc#kw-aes128";

    /**
     * Extracts the encryption method algorithm string from a cxf SoapMessage
     *
     * @param soapMessage the SoapMessage from which the data is extracted
     * @throws SecurityProfileException if the encryption algorithm does not correspond to any security profile(RSA, ECC)
     * @throws XmlProcessingException if parsing the xml document corresponding to the SoapMessage does not work correctly
     * @return the Encryption Algorithm
     */
    String getEncryptionAlgorithm(SoapMessage soapMessage) throws SecurityProfileException, XmlProcessingException;

    /**
     * Extracts the signature method algorithm string from a SoapMessage
     *
     * @param soapMessage the SoapMessage from which the data is extracted
     * @throws SecurityProfileException if the signature method algorithm does not correspond to any security profile(RSA, ECC)
     * @throws XmlProcessingException if parsing the xml document corresponding to the SoapMessage does not work correctly
     * @return the Signature Algorithm
     */
    String getSignatureAlgorithm(SoapMessage soapMessage) throws SecurityProfileException, XmlProcessingException;

    /**
     * Extracts the Security Header element from a SoapMessage
     *
     * @param soapMessage the SoapMessage from which the security header is extracted
     * @throws XmlProcessingException if the security header can't be extracted, or if the obtained security header is null
     * @return the Security Header Element
     */
    Element extractSecurityHeaderElement(SoapMessage soapMessage) throws XmlProcessingException;

    /**
     * Reads a cxf SoapMessage and returns the entire content of the message as XML
     *
     * @param soapMessage the SoapMessage that will be transformed to XML
     * @throws DomibusCoreException conversion exception, if the cxf SoapMessage can't be transformed into XML
     * @return the entire XML content of the SoapMessage
     */
    String getSoapMessageAsString(SoapMessage soapMessage) throws DomibusCoreException;
}
