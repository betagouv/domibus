package eu.domibus.core.util;

import eu.domibus.api.audit.envers.RevisionLogicalName;
import eu.domibus.common.model.configuration.BusinessProcesses;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.audit.model.JmsMessageAudit;
import eu.domibus.core.audit.model.MessageAudit;
import eu.domibus.core.audit.model.PModeAudit;
import eu.domibus.core.certificate.Certificate;
import eu.domibus.core.plugin.routing.BackendFilterEntity;
import eu.domibus.core.user.ui.User;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
public class AnnotationsUtilTest {
    @Test
    public void getDefaultValue() throws Exception {
        AnnotationsUtil annotationsUtil = new AnnotationsUtil();
        assertEquals("Pmode", annotationsUtil.getValue(PModeAudit.class, RevisionLogicalName.class).get());
        assertEquals("Pmode", annotationsUtil.getValue(Configuration.class, RevisionLogicalName.class).get());
        assertEquals("Message", annotationsUtil.getValue(MessageAudit.class, RevisionLogicalName.class).get());
        assertEquals("Party", annotationsUtil.getValue(Party.class, RevisionLogicalName.class).get());
        assertEquals("User", annotationsUtil.getValue(User.class, RevisionLogicalName.class).get());
        assertEquals("Message filter", annotationsUtil.getValue(BackendFilterEntity.class, RevisionLogicalName.class).get());
        assertEquals("Jms message", annotationsUtil.getValue(JmsMessageAudit.class, RevisionLogicalName.class).get());
        assertEquals("Certificate", annotationsUtil.getValue(Certificate.class, RevisionLogicalName.class).get());
        assertEquals(false, annotationsUtil.getValue(BusinessProcesses.class, RevisionLogicalName.class).isPresent());
    }

}
