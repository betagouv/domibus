package eu.domibus.core.message.pull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.Security;
import eu.domibus.core.ebms3.EbMS3Exception;
import eu.domibus.test.common.PojoInstaciatorUtil;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import static eu.domibus.core.message.pull.PullProcessStatus.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class PullProcessValidatorTest {

    @Tested
    PullProcessValidator pullProcessValidator;

    @Injectable
    PullProcessValidator getPullProcessValidator;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Test
    public void checkProcessValidityWithMoreThanOneLegAndDifferentResponder() throws Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}");
        Set<PullProcessStatus> pullProcessStatuses = getProcessStatuses(process);
        assertEquals(2, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
        assertTrue(pullProcessStatuses.contains(NO_RESPONDER));
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}", "initiatorParties{[name:resp1];[name:resp2]}");
        pullProcessStatuses = getProcessStatuses(process);
        assertEquals(2, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
        assertTrue(pullProcessStatuses.contains(TOO_MANY_RESPONDER));
        process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1];[name:leg2]}", "initiatorParties{[name:resp1]}");
        pullProcessStatuses = getProcessStatuses(process);
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(MORE_THAN_ONE_LEG_FOR_THE_SAME_MPC));
    }

    @Test
    public void checkEmptyProcessStatus() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class));
        assertTrue(processStatuses.contains(NO_PROCESS_LEG));
        assertTrue(processStatuses.contains(NO_RESPONDER));
    }


    @Test
    public void checkProcessWithNoLegs() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "initiatorParties{[name:resp1]}"));
        assertEquals(1, processStatuses.size());
        assertTrue(processStatuses.contains(NO_PROCESS_LEG));
    }

    @Test
    public void checkTooManyProcesses() throws Exception {
        Process p1 = PojoInstaciatorUtil.instanciate(Process.class, "p1", "mep[name:oneway]", "legs{[name:leg1]}", "responderParties{[name:resp1]}");
        //p1.setName("p1");
        Process p2 = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg2]}", "responderParties{[name:resp2]}");
        p2.setName("p2");
        List<Process> processes = Lists.newArrayList(p1, p2);
        Set<PullProcessStatus> pullProcessStatuses = pullProcessValidator.verifyPullProcessStatus(new HashSet<>(processes));
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(TOO_MANY_PROCESSES));
    }

    @Test
    public void checkProcessValidityWithOneLeg() throws Exception {
        Set<PullProcessStatus> processStatuses = getProcessStatuses(PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "legs{[name:leg1]}", "initiatorParties{[name:resp1]}"));
        assertEquals(1, processStatuses.size());
        assertTrue(processStatuses.contains(ONE_MATCHING_PROCESS));
    }

    @Test
    public void checkNoProcess() throws Exception {
        Set<PullProcessStatus> pullProcessStatuses = pullProcessValidator.verifyPullProcessStatus(Sets.<Process>newHashSet());
        assertEquals(1, pullProcessStatuses.size());
        assertTrue(pullProcessStatuses.contains(NO_PROCESSES));
    }

    @Test
    public void createProcessWarningMessage() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class);
        try {
            pullProcessValidator.validatePullProcess(Lists.newArrayList(process));
            assertTrue(false);
        } catch (PModeException e) {
            assertTrue(e.getMessage().contains("No leg configuration found"));
            assertTrue(e.getMessage().contains("No responder configured"));
        }

    }

    @Test
    public void testOneWayPullOnlySupported() throws EbMS3Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:twoway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "responderParties{[name:resp1]}");
        try {
            pullProcessValidator.validatePullProcess(Lists.newArrayList(process));
            assertTrue(false);
        } catch (PModeException e) {
            assertTrue(e.getMessage().contains("Invalid mep. Only one way supported"));
        }
    }

    @Test
    public void testcheckMpcConfigurationSameSecurityPolicy() throws EbMS3Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qntest]];[name:leg2,defaultMpc[name:test2,qualifiedName:qntest]]}", "responderParties{[name:resp1]}");
        Assertions.assertEquals(PullProcessStatus.ONE_MATCHING_PROCESS, pullProcessValidator.checkMpcConfigurationSameSecurityPolicy(process));
    }

    @Test
    public void testcheckMpcConfigurationDifferentSecurityPolicy() throws EbMS3Exception {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "mep[name:oneway]", "mepBinding[name:pull]", "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qntest]];[name:leg2,defaultMpc[name:test2,qualifiedName:qntest]]}", "responderParties{[name:resp1]}");
        Iterator<LegConfiguration> iterator = process.getLegs().iterator();
        Security security1 = new Security();
        security1.setName("eDeliveryAS4");
        iterator.next().setSecurity(security1);
        Security security2 = new Security();
        security2.setName("eDeliveryAS4_BST");
        iterator.next().setSecurity(security2);
        Assertions.assertEquals(PullProcessStatus.MULTIPLE_LEGS_DIFFERENT_SECURITY, pullProcessValidator.checkMpcConfigurationSameSecurityPolicy(process));
    }

    private Set<PullProcessStatus> getProcessStatuses(Process process) {
        return pullProcessValidator.verifyPullProcessStatus(Sets.newHashSet(process));
    }

}
