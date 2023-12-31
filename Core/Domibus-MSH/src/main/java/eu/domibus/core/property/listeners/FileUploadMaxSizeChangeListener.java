package eu.domibus.core.property.listeners;

import eu.domibus.api.property.DomibusPropertyChangeListener;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_FILE_UPLOAD_MAX_SIZE;

/**
 * Handles the change of domibus.file.upload.maxSize property
 *
 * @author Ion Perpegel
 * @since 4.2
 */
@Service
public class FileUploadMaxSizeChangeListener implements DomibusPropertyChangeListener {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FileUploadMaxSizeChangeListener.class);

    private CommonsMultipartResolver multipartResolver;

    public FileUploadMaxSizeChangeListener(CommonsMultipartResolver multipartResolver) {
        this.multipartResolver = multipartResolver;
    }

    @Override
    public boolean handlesProperty(String propertyName) {
        return StringUtils.equalsIgnoreCase(propertyName, DOMIBUS_FILE_UPLOAD_MAX_SIZE);
    }

    @Override
    public void propertyValueChanged(String domainCode, String propertyName, String propertyValue) {
        if (domainCode == null) {
            int size = Integer.parseInt(propertyValue);
            LOG.trace("Setting global max upload size to [{}]", size);
            multipartResolver.setMaxUploadSize(size);
        }
    }
}
