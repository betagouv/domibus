package eu.domibus.core.message.pull;

import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.test.common.PojoInstaciatorUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
class PullContextTest {


    @Test
    void filterLegOnMpc() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "initiatorParties{[name:resp1]}");
        PullContext pullContext = new PullContext(process, new Party(), "qn1");
        LegConfiguration legConfiguration = pullContext.filterLegOnMpc();
        assertEquals("qn1", legConfiguration.getDefaultMpc().getQualifiedName());
    }

    @Test
    void testInstanciationWithIllegalMpc() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1,defaultMpc[name:test1,qualifiedName:qn1]];[name:leg2,defaultMpc[name:test2,qualifiedName:qn2]]}", "responderParties{[name:resp1]}");
        Assertions.assertThrows(NullPointerException.class,
                () -> new PullContext(process, new Party(), null));
    }

}


