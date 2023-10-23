package eu.domibus.core.message;

import eu.domibus.common.ErrorResult;
import eu.domibus.common.ErrorResultImpl;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

/**
 * @author Ion Perpegel
 * @since 5.1
 * <p>
 * Spin-off from UserMessageHandlerServiceImpl to break a cyclic dependency
 */
@Service
public class UserMessageErrorCreator {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageErrorCreator.class);

    public ErrorResult createErrorResult(EbMS3Exception ebm3Exception) {
        ErrorResultImpl result = new ErrorResultImpl();
        result.setMshRole(eu.domibus.common.MSHRole.RECEIVING);
        result.setMessageInErrorId(ebm3Exception.getRefToMessageId());
        result.setErrorCodeAsString(ebm3Exception.getErrorCode());
        try {
            result.setErrorCode(ebm3Exception.getErrorCodeObject());
        } catch (IllegalArgumentException e) {
            LOG.warn("Could not find error code for [" + ebm3Exception.getEbMS3ErrorCode() + "]");
        }
        result.setErrorDetail(ebm3Exception.getErrorDetail());
        return result;
    }

}
