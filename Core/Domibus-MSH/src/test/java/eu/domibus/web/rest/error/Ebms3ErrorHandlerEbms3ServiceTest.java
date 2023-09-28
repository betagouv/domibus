package eu.domibus.web.rest.error;

import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.web.rest.ro.ErrorRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import javax.validation.Path;
import java.util.Arrays;
import java.util.Iterator;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Ion Perpegel
 * @since 4.1
 */
@SuppressWarnings("ResultOfMethodCallIgnored")
@ExtendWith(JMockitExtension.class)
public class Ebms3ErrorHandlerEbms3ServiceTest {

    @Tested
    ErrorHandlerService errorHandlerService;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void testCreateResponseWithStatus() {
        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String errorMessage = "Error occurred";
        Exception ex = new Exception(errorMessage);
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty("domibus.exceptions.rest.enable");
            result = true;
        }};

        ResponseEntity<ErrorRO> result = errorHandlerService.createResponse(ex, status);

        assertEquals(status, result.getStatusCode());
        assertEquals("close", Objects.requireNonNull(result.getHeaders().get(HttpHeaders.CONNECTION)).get(0));
        assertEquals(errorMessage, Objects.requireNonNull(result.getBody()).getMessage());
    }

    @Test
    public void testCreateResponse() {
        String errorMessage = "Error occurred";
        Exception ex = new Exception(errorMessage);
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty("domibus.exceptions.rest.enable");
            result = true;
        }};

        ResponseEntity<ErrorRO> result = errorHandlerService.createResponse(ex);

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, result.getStatusCode());
        assertEquals("close", Objects.requireNonNull(result.getHeaders().get(HttpHeaders.CONNECTION)).get(0));
        assertEquals(errorMessage, Objects.requireNonNull(result.getBody()).getMessage());
    }

    @Test
    public void getLast(@Injectable Path propertyPath, @Injectable Path.Node pn1, @Injectable Path.Node pn2) {
        Iterator<Path.Node> it = Arrays.asList(pn1,pn2).iterator();
        new Expectations() {{
            propertyPath.iterator();
            result = it;
            pn2.toString();
            result = "pn2_value";
        }};
        String res = errorHandlerService.getLast(propertyPath);
        assertEquals("pn2_value", res);
    }
}
