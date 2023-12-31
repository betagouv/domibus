package eu.domibus.core.util;

import eu.domibus.api.model.PartyInfo;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.test.common.PojoInstaciatorUtil;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * @author Thomas Dussart
 * @since 3.3
 */
public class PojoInstaciatorUtilTest {

    @Test
    public void testSetUpParameterForRootEntity() throws Exception {
        Map<String, Object> entityMap = PojoInstaciatorUtil.setUpParameters(" [name:oneway]");
        assertEquals("oneway", entityMap.get("name"));
    }

    @Test
    public void testSetUpParameterForEntity() throws Exception {
        Map<String, Object> entityMap = PojoInstaciatorUtil.setUpParameters("mep[name:oneway,value:http]");
        Map<String, Object> fields = (Map<String, Object>) entityMap.get("mep");
        assertEquals("oneway", fields.get("name"));
        assertEquals("http", fields.get("value"));
    }

    @Test
    public void testSetUpParameterForCollection() throws Exception {
        Map<String, Object> entityMap = PojoInstaciatorUtil.setUpParameters("responderParties{[name:oneway,userName:http];[name:cool,userName:test]}");
        Map<String, Object> entities = (Map<String, Object>) entityMap.get("responderParties");
        Map<String, Object> party1 = (Map<String, Object>) entities.get("responderParties_0");
        Map<String, Object> party2 = (Map<String, Object>) entities.get("responderParties_1");
        assertEquals("oneway", party1.get("name"));
        assertEquals("http", party1.get("userName"));
        assertEquals("cool", party2.get("name"));
        assertEquals("test", party2.get("userName"));
    }

    @Test
    public void testSetUpClassInsideCollectionClass() throws Exception {
        Map<String, Object> entityMap = PojoInstaciatorUtil.setUpParameters("legs{[name:leg1,reliabilityXml:test,defaultMpc[name:mpcName,qualifiedName:qualifiedName]]}");
        Map<String, Object> entities = (Map<String, Object>) entityMap.get("legs");
        Map<String, Object> leg = (Map<String, Object>) entities.get("legs_0");
        Map<String, Object> defaultMpc = (Map<String, Object>) leg.get("defaultMpc");
        assertEquals("leg1", leg.get("name"));
        assertEquals("test", leg.get("reliabilityXml"));
        assertEquals("mpcName", defaultMpc.get("name"));
        assertEquals("qualifiedName", defaultMpc.get("qualifiedName"));
    }

    @Test
    public void testInstanciateProcess() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, " [name:test]", "mep[name:oneway,value:test]", "mepBinding[name:push]", "responderParties{[name:oneway,userName:http];[name:cool,userName:test]}");
        Assertions.assertEquals("test", process.getName());
        Assertions.assertEquals("oneway", process.getMep().getName());
        Assertions.assertEquals("test", process.getMep().getValue());
        Assertions.assertEquals("push", process.getMepBinding().getName());
        Set<Party> responderParties = process.getResponderParties();
        assertEquals(2, responderParties.size());
        boolean onePartyFound = false;
        boolean secondPartyFound = false;
        for (Party responderParty : responderParties) {
            if ("oneway".equals(responderParty.getName()) && "http".equals(responderParty.getUserName()))
                onePartyFound = true;
            if ("cool".equals(responderParty.getName()) && "test".equals(responderParty.getUserName()))
                secondPartyFound = true;
        }
        assertTrue(onePartyFound && secondPartyFound);
    }

    @Test
    public void testInstanciateProcesWithSubClassesInCollections() {
        Process process = PojoInstaciatorUtil.instanciate(Process.class, "legs{[name:leg1,defaultMpc[name:mpcName,qualifiedName:qualifiedName]]}", "responderParties{[name:endPoint1];[name:endPoint2]}");
        Mpc defaultMpc = process.getLegs().iterator().next().getDefaultMpc();
        assertEquals("mpcName", defaultMpc.getName());
        assertEquals("qualifiedName", defaultMpc.getQualifiedName());
    }

    @Test
    public void testMultipleSubClasesWithCollection() {
        PartyInfo instanciate = PojoInstaciatorUtil.instanciate(PartyInfo.class, "to[role:test,partyId{[value:testParty]}]");
        assertEquals("Mock", instanciate.getTo().getRoleValue());
        assertEquals("Mock", instanciate.getTo().getToPartyId().getValue());
    }

}
