package eu.domibus.core.message.dictionary;

import eu.domibus.api.model.MSHRole;
import eu.domibus.api.model.MessageStatus;
import eu.domibus.api.model.NotificationStatus;
import eu.domibus.core.message.MessageStatusDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Arrays;

/**
 * @author Cosmin Baciu
 * @since 5.0.3
 */
@Service
public class StaticDictionaryServiceHelper {

    protected MessageStatusDao messageStatusDao;
    protected NotificationStatusDao notificationStatusDao;
    protected MshRoleDao mshRoleDao;

    public StaticDictionaryServiceHelper(MessageStatusDao messageStatusDao, NotificationStatusDao notificationStatusDao, MshRoleDao mshRoleDao) {
        this.messageStatusDao = messageStatusDao;
        this.notificationStatusDao = notificationStatusDao;
        this.mshRoleDao = mshRoleDao;
    }

    @Transactional
    public void createEntries() {
        Arrays.stream(MessageStatus.values()).forEach(messageStatus -> messageStatusDao.findOrCreate(messageStatus));
        Arrays.stream(NotificationStatus.values()).forEach(notificationStatus -> notificationStatusDao.findOrCreate(notificationStatus));
        Arrays.stream(MSHRole.values()).forEach(mshRole -> mshRoleDao.findOrCreate(mshRole));
    }
}