package eu.domibus.core.property.listeners;

import mockit.FullVerifications;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_FILE_UPLOAD_MAX_SIZE;

/**
 * @author Ion Perpegel
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class FileUploadMaxSizeChangeListenerTest {

    @Mocked
    protected CommonsMultipartResolver multipartResolver;

    protected FileUploadMaxSizeChangeListener listener ;

    @BeforeEach
    public void setUp() {
        listener = new FileUploadMaxSizeChangeListener(multipartResolver);
    }

    @Test
    public void handlesProperty_true() {
        Assertions.assertTrue(listener.handlesProperty(DOMIBUS_FILE_UPLOAD_MAX_SIZE));
    }

    @Test
    public void handlesProperty_false() {
        Assertions.assertFalse(listener.handlesProperty("OTHER"));
    }

    @Test
    public void testPropertyValueChanged_domain() {
        listener.propertyValueChanged("default", DOMIBUS_FILE_UPLOAD_MAX_SIZE, "100");

        new FullVerifications() {{
            multipartResolver.setMaxUploadSize(100);
            times = 0;
        }};
    }

    @Test
    public void testPropertyValueChanged_domainInvalid() {
        listener.propertyValueChanged("default", DOMIBUS_FILE_UPLOAD_MAX_SIZE, "invalid");

        new FullVerifications() {{
            multipartResolver.setMaxUploadSize(100);
            times = 0;
        }};
    }

    @Test
    public void testPropertyValueChanged() {
        listener.propertyValueChanged(null, DOMIBUS_FILE_UPLOAD_MAX_SIZE, "100");

        new FullVerifications() {{
            multipartResolver.setMaxUploadSize(100);
            times = 1;
        }};
    }

    @Test
    void testInvalidPropertyValueChanged() {
        Assertions.assertThrows(IllegalArgumentException. class,
        () -> listener.propertyValueChanged(null, DOMIBUS_FILE_UPLOAD_MAX_SIZE, "invalid"));
    }
}
