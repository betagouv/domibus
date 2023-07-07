package eu.domibus.plugin.fs;

import eu.domibus.common.MSHRole;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

/**
 * Helper to create error messages
 *
 * @author Ion Perpegel
 * @since 5.2
 */
@Service
public class FSErrorMessageHelper {
    private static final String LS = System.lineSeparator();
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FSErrorMessageHelper.class);

    public StringBuilder buildErrorMessage(String errorDetail) {
        return buildErrorMessage(null, errorDetail, null, null, null, null);
    }

    public StringBuilder buildErrorMessage(String errorCode, String errorDetail, String messageId, String mshRole, String notified, String timestamp) {
        StringBuilder sb = new StringBuilder();
        if (errorCode != null) {
            sb.append("errorCode: ").append(errorCode).append(LS);
        }
        sb.append("errorDetail: ").append(errorDetail).append(LS);
        if (messageId != null) {
            sb.append("messageInErrorId: ").append(messageId).append(LS);
        }
        if (mshRole != null) {
            sb.append("mshRole: ").append(mshRole).append(LS);
        } else {
            sb.append("mshRole: ").append(MSHRole.SENDING).append(LS);
        }
        if (notified != null) {
            sb.append("notified: ").append(notified).append(LS);
        }
        if (timestamp != null) {
            sb.append("timestamp: ").append(timestamp).append(LS);
        } else {
            sb.append("timestamp: ").append(LocalDateTime.now()).append(LS);
        }

        return sb;
    }
}
