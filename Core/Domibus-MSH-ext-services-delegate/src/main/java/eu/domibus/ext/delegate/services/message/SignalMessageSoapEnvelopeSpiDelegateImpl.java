package eu.domibus.ext.delegate.services.message;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import eu.domibus.api.message.SignalMessageSoapEnvelopeSpiDelegate;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.spi.soapenvelope.HttpMetadata;
import eu.domibus.core.spi.soapenvelope.SignalMessageSoapEnvelopeSpi;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.MessageConstants;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import javax.xml.soap.SOAPMessage;
import java.util.List;
import java.util.Map;

import static eu.domibus.api.property.DomibusGeneralConstants.JSON_MAPPER_BEAN;

/**
 * @author Cosmin Baciu
 * @since 5.0.2
 */
@Service
public class SignalMessageSoapEnvelopeSpiDelegateImpl implements SignalMessageSoapEnvelopeSpiDelegate {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(SignalMessageSoapEnvelopeSpiDelegateImpl.class);

    protected SignalMessageSoapEnvelopeSpi soapEnvelopeSpi;
    protected DomibusPropertyProvider domibusPropertyProvider;
    protected ObjectMapper objectMapper;

    public SignalMessageSoapEnvelopeSpiDelegateImpl(@Autowired(required = false) SignalMessageSoapEnvelopeSpi soapEnvelopeSpi,
                                                    @Qualifier(JSON_MAPPER_BEAN)
                                                    ObjectMapper objectMapper,
                                                    DomibusPropertyProvider domibusPropertyProvider) {
        this.soapEnvelopeSpi = soapEnvelopeSpi;
        this.domibusPropertyProvider = domibusPropertyProvider;
        this.objectMapper = objectMapper;
    }

    @Override
    public SOAPMessage beforeSigningAndEncryption(SOAPMessage soapMessage) {
        if (!isSoapEnvelopeSpiActive()) {
            LOG.debug("BeforeSigningAndEncryption hook skipped: SPI is not active");
            return soapMessage;
        }

        LOG.debug("Executing beforeSigningAndEncryption hook");
        final SOAPMessage resultSoapMessage = soapEnvelopeSpi.beforeSigningAndEncryption(soapMessage);
        LOG.debug("Finished executing beforeSigningAndEncryption hook");

        return resultSoapMessage;
    }

    @Override
    public SOAPMessage afterReceiving(SOAPMessage responseMessage) {
        if (!isSoapEnvelopeSpiActive()) {
            LOG.debug("afterReceiving hook skipped: SPI is not active");
            return responseMessage;
        }

        final Boolean httpHeaderMetadataActive = domibusPropertyProvider.getBooleanProperty(DomibusPropertyMetadataManagerSPI.DOMIBUS_DISPATCHER_HTTP_HEADER_METADATA_ACTIVE);
        HttpMetadata httpMetadata = new HttpMetadata();
        if (httpHeaderMetadataActive) {
            final String contentType = LOG.getMDC(MessageConstants.HTTP_CONTENT_TYPE);
            httpMetadata.setContentType(contentType);
            final String httpProtocolHeaders = LOG.getMDC(MessageConstants.HTTP_PROTOCOL_HEADERS);
            try {
                Map<String, List<String>> headers = objectMapper.readValue(httpProtocolHeaders, Map.class);
                httpMetadata.setHeaders(headers);
            } catch (JsonProcessingException e) {
                LOG.error("Could not extract protocol headers from json [{}]", httpProtocolHeaders, e);
            }
        }

        LOG.debug("Executing afterReceiving with http metadata hook");
        final SOAPMessage resultSoapMessage = soapEnvelopeSpi.afterReceiving(responseMessage, httpMetadata);
        LOG.debug("Finished executing afterReceiving http metadata hook");

        return resultSoapMessage;
    }

    protected boolean isSoapEnvelopeSpiActive() {
        return soapEnvelopeSpi != null;
    }
}
