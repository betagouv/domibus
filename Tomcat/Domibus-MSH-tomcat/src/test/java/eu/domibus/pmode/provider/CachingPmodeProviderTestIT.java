package eu.domibus.core.pmode.provider;

import eu.domibus.AbstractIT;
import eu.domibus.api.ebms3.Ebms3Constants;
import eu.domibus.api.ebms3.MessageExchangePattern;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusProperty;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Mpc;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.core.participant.FinalRecipientDao;
import eu.domibus.core.property.DomibusPropertyResourceHelperImpl;
import eu.domibus.messaging.XmlProcessingException;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;


/**
 * @author Cosmin Baciu
 * @since 5.0.2
 */
@Transactional
public class CachingPmodeProviderTestIT extends AbstractIT {

    @Autowired
    protected PModeProviderFactoryImpl pModeProviderFactory;

    @Autowired
    protected DomainContextProvider domainContextProvider;

    @Autowired
    protected FinalRecipientDao finalRecipientDao;

    @Autowired
    protected FinalRecipientService finalRecipientService;

    @Autowired
    DomibusPropertyResourceHelperImpl configurationPropertyResourceHelper;

    @Test
    public void testGetFinalParticipantEndpointFromParty() {
        final CachingPModeProvider pmodeProvider = (CachingPModeProvider) pModeProviderFactory.createDomainPModeProvider(domainContextProvider.getCurrentDomain());

        //no final recipients saved in the database
        assertTrue(finalRecipientDao.findAll().isEmpty());

        String finalRecipient = "0001:recipient1";
        String finalRecipientURL = "http://localhost:8080/domibus/services/msh?domain=domain1";
        Party party = new Party();
        party.setName("domibus-blue");
        final String partyEndpoint = "http://localhost:8080/domibus/services/msh?domain=default";
        party.setEndpoint(partyEndpoint);

        final String receiverPartyEndpoint = pmodeProvider.getReceiverPartyEndpoint(party, finalRecipient);
        //no final recipients saved in the database after we retrieved the URL
        assertTrue(finalRecipientDao.findAll().isEmpty());
        assertEquals(partyEndpoint, receiverPartyEndpoint);
    }

    @Test
    @Disabled("EDELIVERY-11795")
    public void testGetFinalParticipantEndpointFromFinalParticipantEndpointURL() {
        final CachingPModeProvider pmodeProvider = (CachingPModeProvider) pModeProviderFactory.createDomainPModeProvider(domainContextProvider.getCurrentDomain());

        //no final recipients saved in the database
        assertTrue(finalRecipientDao.findAll().isEmpty());

        String finalRecipient = "0001:recipient1";
        String finalRecipientURL = "http://localhost:8080/domibus/services/msh?domain=domain1";
        Party party = new Party();
        party.setName("domibus-blue");
        final String partyEndpoint = "http://localhost:8080/domibus/services/msh?domain=default";
        party.setEndpoint(partyEndpoint);

        //get the endpoint URL from Pmode Party
        String receiverPartyEndpoint = pmodeProvider.getReceiverPartyEndpoint(party, finalRecipient);
        assertEquals(partyEndpoint, receiverPartyEndpoint);

        pmodeProvider.setReceiverPartyEndpoint(finalRecipient, finalRecipientURL);
        //final recipient should be saved in the database after we retrieved the URL
        assertEquals(1, finalRecipientDao.findAll().size());

        //get the endpoint URL from the database
        receiverPartyEndpoint = pmodeProvider.getReceiverPartyEndpoint(party, finalRecipient);
        assertEquals(finalRecipientURL, receiverPartyEndpoint);

        //clear the cache to simulate a second server which doesn't have the endpoint URL in the cache
        finalRecipientService.clearFinalRecipientAccessPointUrls(domainContextProvider.getCurrentDomain());

        //the endpoint URL should be retrieved from the database
        receiverPartyEndpoint = pmodeProvider.getReceiverPartyEndpoint(party, finalRecipient);
        assertEquals(finalRecipientURL, receiverPartyEndpoint);
    }

    @Test
    public void testX() throws XmlProcessingException, IOException {
        String selfParty = "domibus-blue";
        uploadPMode();
        final CachingPModeProvider pmodeProvider = (CachingPModeProvider) pModeProviderFactory.createDomainPModeProvider(domainContextProvider.getCurrentDomain());

        List<String> list = pModeProvider.findPartiesByInitiatorServiceAndAction("domibus-blue", Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION, getPushMeps());
        assertEquals(1, list.size());
        assertTrue(list.contains("domibus-red"));

        List<String> list2 = pModeProvider.findPartiesByResponderServiceAndAction("domibus-red", Ebms3Constants.TEST_SERVICE, Ebms3Constants.TEST_ACTION, getPushMeps());
        assertEquals(1, list2.size());
        assertTrue(list2.contains("domibus-blue"));
    }

    private List<MessageExchangePattern> getPushMeps() {
        List<MessageExchangePattern> meps = new ArrayList<>();
        meps.add(MessageExchangePattern.ONE_WAY_PUSH);
        meps.add(MessageExchangePattern.TWO_WAY_PUSH_PUSH);
        meps.add(MessageExchangePattern.TWO_WAY_PUSH_PULL);
        meps.add(MessageExchangePattern.TWO_WAY_PULL_PUSH);
        return meps;
    }

    @Test
    public void checkMpcMismatch() {
        LegConfiguration legConfiguration = new LegConfiguration();
        final Mpc mpc = new Mpc();
        mpc.setQualifiedName("defaultMpc1");
        legConfiguration.setDefaultMpc(mpc);
        LegFilterCriteria legFilterCriteria = new LegFilterCriteria(null, null, null, null, null, null, null, null, "defaultMpc");
        final CachingPModeProvider pmodeProvider = (CachingPModeProvider) pModeProviderFactory.createDomainPModeProvider(domainContextProvider.getCurrentDomain());

        Set<String> mismatchedMPcs = new HashSet<>();
        String DOMIBUS_PMODE_LEGCONFIGURATION_MPC_VALIDATION_ENABLED = "domibus.pmode.legconfiguration.mpc.validation.enabled";
        DomibusProperty initialValue = configurationPropertyResourceHelper.getProperty(DOMIBUS_PMODE_LEGCONFIGURATION_MPC_VALIDATION_ENABLED);
        configurationPropertyResourceHelper.setPropertyValue(DOMIBUS_PMODE_LEGCONFIGURATION_MPC_VALIDATION_ENABLED, true, "false");

        boolean matchedMpc = pmodeProvider.checkMpcMismatch(legConfiguration, legFilterCriteria, mismatchedMPcs);

        assertEquals(mismatchedMPcs.size(), 1);
        assertFalse(matchedMpc);
        configurationPropertyResourceHelper.setPropertyValue(DOMIBUS_PMODE_LEGCONFIGURATION_MPC_VALIDATION_ENABLED, true, initialValue.getValue());
    }

//    @Test
    // TODO [EDELIVERY-11854] Fix merging issue
//    public void filterMatchingLegConfigurations() {
//        LegConfiguration legConfiguration1 = new LegConfiguration();
//        LegConfiguration legConfiguration2 = new LegConfiguration();
//
//        List<Process> matchingProcessesList = new ArrayList<>();
//        Process process = new Process();
//        process.setName("tc1Process");
//        LinkedHashSet<LegConfiguration> candidateLegs = new LinkedHashSet<>();
//
//        legConfiguration1.setName("leg0");
//        final Mpc mpc1 = new Mpc();
//        mpc1.setQualifiedName("defaultMpc");
//        legConfiguration1.setDefaultMpc(mpc1);
//        final Service service1 = new Service();
//        service1.setName("testService0");
//        service1.setValue("bdx:noprocess");
//        service1.setServiceType("tc0");
//        legConfiguration1.setService(service1);
//
//        Action action1 = new Action();
//        action1.setName("tc0Action");
//        legConfiguration1.setAction(action1);
//
//        legConfiguration2.setName("leg1");
//        final Mpc mpc2 = new Mpc();
//        mpc2.setQualifiedName("defaultMpc");
//        legConfiguration2.setDefaultMpc(mpc2);
//        final Service service2 = new Service();
//        service2.setName("testService");
//        service2.setValue("bdx:noprocess");
//        service2.setServiceType("tc1");
//        legConfiguration2.setService(service2);
//        Action action2 = new Action();
//        action2.setName("tc1Action");
//        legConfiguration2.setAction(action2);
//
//        candidateLegs.add(legConfiguration1);
//        candidateLegs.add(legConfiguration2);
//        process.setLegs(candidateLegs);
//        matchingProcessesList.add(process);
//
//        LegFilterCriteria legFilterCriteria = new LegFilterCriteria(null, null, null, null, null, "testService", "tc1Action", null, "defaultMpc");
//        final CachingPModeProvider pmodeProvider = (CachingPModeProvider) pModeProviderFactory.createDomainPModeProvider(domainContextProvider.getCurrentDomain());
//
//        Set<LegConfiguration> legConfigurationList = pmodeProvider.filterMatchingLegConfigurations(matchingProcessesList, legFilterCriteria);
//
//        assertEquals(1, legConfigurationList.size());
//        assertEquals("tc1Action", legConfigurationList.iterator().next().getAction().getName());
//    }
}