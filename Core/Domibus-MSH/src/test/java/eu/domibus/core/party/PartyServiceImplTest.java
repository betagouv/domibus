package eu.domibus.core.party;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.party.Identifier;
import eu.domibus.api.party.Party;
import eu.domibus.api.pki.CertificateEntry;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.process.Process;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.*;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.messaging.XmlProcessingException;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.security.KeyStore;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.function.Predicate;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_FILE_UPLOAD_MAX_SIZE;
import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

/**
 * @author Thomas Dussart
 * @since 4.0
 */
@SuppressWarnings({"ResultOfMethodCallIgnored", "DataFlowIssue", "UnusedAssignment", "ThrowableNotThrown", "unchecked", "OptionalGetWithoutIsPresent", "ClassEscapesDefinedScope", "SpellCheckingInspection"})
@ExtendWith(JMockitExtension.class)
public class PartyServiceImplTest {

    @Injectable
    private PartyCoreMapper partyConverter;

    @Injectable
    private PModeProvider pModeProvider;

    @Injectable
    private PartyDao partyDao;

    @Injectable
    private MultiDomainCryptoService multiDomainCertificateProvider;

    @Injectable
    private DomainContextProvider domainProvider;

    @Injectable
    private CertificateService certificateService;

    @Tested
    private PartyServiceImpl partyService;


    @Injectable
    private eu.domibus.common.model.configuration.Party gatewayParty;

    @Injectable
    private Configuration configuration;

    @Injectable
    private BusinessProcesses configurationBusinessProcesses;

    @Injectable
    private Parties configurationParties;

    @Injectable
    private PartyIdTypes configurationPartyIdTypes;

    @Injectable
    private Domain currentDomain;

    @Injectable
    PModeValidationHelper pModeValidationHelper;

    @Injectable
    DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    PartyCoreMapper partyCoreMapper;

    @Test
    public void getParties(@Injectable Party party) {
        String name = "name";
        String endPoint = "endPoint";
        String partyId = "partyId";
        String processName = "processName";
        int pageStart = 0;
        int pageSize = 10;

        new Expectations(partyService) {{
            partyService.getSearchPredicate(anyString, anyString, anyString, anyString);
            result = (Predicate<Party>) condition -> true;
            partyService.linkPartyAndProcesses();
            result = Collections.singletonList(party);
        }};

        List<Party> parties = partyService.getParties(name, endPoint, partyId, processName, pageStart, pageSize);

        assertEquals(1, parties.size());
        assertEquals(party, parties.get(0));
        new Verifications() {{
            partyService.getSearchPredicate(name, endPoint, partyId, processName);
            times = 1;
        }};
    }

    @Test
    public void linkPartyAndProcesses() {
        eu.domibus.common.model.configuration.Party partyEntity = new eu.domibus.common.model.configuration.Party();
        final String name = "name";
        partyEntity.setName(name);

        List<eu.domibus.common.model.configuration.Party> partyEntities = Lists.newArrayList(partyEntity);
        eu.domibus.common.model.configuration.Process process = new eu.domibus.common.model.configuration.Process();
        process.setName("processName");
        List<eu.domibus.common.model.configuration.Process> processEntities = Lists.newArrayList(process);

        Party party = new Party();
        party.setName(name);
        List<Party> parties = Lists.newArrayList(party);

        new Expectations(partyService) {{
            pModeProvider.findAllParties();
            result = partyEntities;
            pModeProvider.findAllProcesses();
            result = processEntities;
            partyConverter.configurationPartyListToPartyList(partyEntities);
            result = parties;
            partyService.linkProcessWithPartyAsInitiator(withAny(new HashMap<>()), processEntities);
            times = 1;
            partyService.linkProcessWithPartyAsResponder(withAny(new HashMap<>()), processEntities);
            times = 1;
        }};

        partyService.linkPartyAndProcesses();

        new Verifications() {{
            Map<String, Party> partyMap;
            partyService.linkProcessWithPartyAsInitiator(partyMap = withCapture(), processEntities);
            times = 1;
            assertEquals(partyMap.get(name), party);
            partyService.linkProcessWithPartyAsResponder(partyMap = withCapture(), processEntities);
            times = 1;
            assertEquals(partyMap.get(name), party);
        }};
    }

    @Test
    public void returnsEmptyListWhenLinkingProcessWithParty_findAllPartiesThrowsIllegalStateException() {
        new Expectations(partyService) {{
            pModeProvider.findAllParties();
            result = new IllegalStateException();
        }};

        List<Party> parties = partyService.linkPartyAndProcesses();

        assertTrue(parties.isEmpty(), "The party list should have been empty");
    }

    @Test
    public void linkProcessWithPartyAsInitiator(final @Injectable eu.domibus.common.model.configuration.Process processEntity) {
        Party party = new Party();
        party.setName("name");
        Map<String, Party> partyMap = new HashMap<>();
        partyMap.put("name", party);

        Process process = new Process();
        process.setName("p1");


        eu.domibus.common.model.configuration.Party partyEntity = new eu.domibus.common.model.configuration.Party();
        partyEntity.setName("name");

        Set<eu.domibus.common.model.configuration.Party> responderParties = Sets.newHashSet(partyEntity);
        List<eu.domibus.common.model.configuration.Process> processes = Lists.newArrayList(processEntity);

        new Expectations() {{
            processEntity.getInitiatorParties();
            result = responderParties;

            partyConverter.processToProcessAPI(processEntity);
            result = process;
        }};
        partyService.linkProcessWithPartyAsInitiator(partyMap, processes);
        assertEquals(1, party.getProcessesWithPartyAsInitiator().size());
        assertEquals("p1", party.getProcessesWithPartyAsInitiator().get(0).getName());
    }

    @Test
    public void linkProcessWithPartyAsResponder(final @Injectable eu.domibus.common.model.configuration.Process processEntity) {
        Party party = new Party();
        party.setName("name");
        Map<String, Party> partyMap = new HashMap<>();
        partyMap.put("name", party);

        Process process = new Process();
        process.setName("p1");


        eu.domibus.common.model.configuration.Party partyEntity = new eu.domibus.common.model.configuration.Party();
        partyEntity.setName("name");

        Set<eu.domibus.common.model.configuration.Party> responderParties = Sets.newHashSet(partyEntity);
        List<eu.domibus.common.model.configuration.Process> processes = Lists.newArrayList(processEntity);

        new Expectations() {{
            processEntity.getResponderParties();
            result = responderParties;

            partyConverter.processToProcessAPI(processEntity);
            result = process;
        }};
        partyService.linkProcessWithPartyAsResponder(partyMap, processes);
        assertEquals(1, party.getProcessesWithPartyAsResponder().size());
        assertEquals("p1", party.getProcessesWithPartyAsResponder().get(0).getName());

    }

    @Test
    public void getSearchPredicate() {
        final String name = "name";
        final String endPoint = "endPoint";
        final String partyId = "partyId";
        final String processName = "processName";

        partyService.getSearchPredicate(name, endPoint, partyId, processName);

        new Verifications() {{
            partyService.namePredicate(name);
            times = 1;
            partyService.endPointPredicate(endPoint);
            times = 1;
            partyService.partyIdPredicate(partyId);
            times = 1;
            partyService.processPredicate(processName);
            times = 1;
        }};
    }

    @Test
    public void testNamePredicate(@Injectable Party party) {
        final String name = "name";

        new Expectations(partyService) {{
            party.getName();
            result = name;
        }};

        assertTrue(partyService.namePredicate("").test(party));
        assertTrue(partyService.namePredicate("name").test(party));
        assertFalse(partyService.namePredicate("wrong").test(party));
        assertFalse(partyService.namePredicate("Name1").test(party));
    }

    @Test
    public void testEndPointPredicate(@Injectable Party party) {
        final String endPoint = "http://localhost:8080";

        new Expectations(partyService) {{
            party.getEndpoint();
            result = endPoint;
        }};

        assertTrue(partyService.endPointPredicate("").test(party));
        assertTrue(partyService.endPointPredicate("http://localhost:8080").test(party));
        assertFalse(partyService.endPointPredicate("8080").test(party));
        assertFalse(partyService.endPointPredicate("http://localhost:7070").test(party));
    }

    @Test
    public void testPartyIdPredicate(@Injectable Party party, @Injectable Identifier identifier) {
        final String partyId = "partyId";
        List<Identifier> identifiers = new ArrayList<>();
        identifiers.add(identifier);

        new Expectations(partyService) {{
            identifier.getPartyId();
            result = partyId;
            party.getIdentifiers();
            result = identifiers;
        }};

        assertTrue(partyService.partyIdPredicate("").test(party));
        assertTrue(partyService.partyIdPredicate("partyId").test(party));
        assertFalse(partyService.partyIdPredicate("wrong").test(party));
        assertFalse(partyService.partyIdPredicate("partyId1").test(party));
    }

    @Test
    public void testProcessPredicate(@Injectable Party party, @Injectable Process process) {
        final String processName = "tc1Process";
        List<Process> processes = new ArrayList<>();

        new Expectations(partyService) {{
            process.getName();
            result = processName;
            processes.add(process);
            party.getProcessesWithPartyAsInitiator();
            result = processes;
            party.getProcessesWithPartyAsResponder();
            result = processes;
        }};

        assertTrue(partyService.processPredicate(null).test(party));
        assertTrue(partyService.processPredicate("tc1Process").test(party));
        assertFalse(partyService.processPredicate("wrong").test(party));
        assertFalse(partyService.processPredicate("tc1ProcessIR").test(party));
    }

    @Test
    public void testGetGatewayPartyIdentifier() {
        // Given
        String expectedGatewayPartyId = "testGatewayPartyId";
        eu.domibus.common.model.configuration.Party gatewayParty = new eu.domibus.common.model.configuration.Party();
        List<eu.domibus.common.model.configuration.Identifier> identifiers = new ArrayList<>();
        eu.domibus.common.model.configuration.Identifier identifier = new eu.domibus.common.model.configuration.Identifier();
        identifier.setPartyId(expectedGatewayPartyId);
        identifiers.add(identifier);
        gatewayParty.setIdentifiers(identifiers);
        new Expectations() {{
            pModeProvider.getGatewayParty();
            result = gatewayParty;
        }};

        // When
        String gatewayPartyId = partyService.getGatewayPartyIdentifier();

        // Then
        Assertions.assertEquals(expectedGatewayPartyId, gatewayPartyId);
    }

    @Test
    public void getProcesses() {
        new Expectations() {{
            pModeProvider.findAllProcesses();
        }};

        // When
        partyService.getAllProcesses();
    }

    @Test
    public void returnsEmptyListWhenRetrievingAllProcesses_findAllProcessesThrowsIllegalStateException() {
        // Given
        new Expectations(partyService) {{
            pModeProvider.findAllProcesses();
            result = new IllegalStateException();
        }};

        // When
        List<Process> processes = partyService.getAllProcesses();

        // Then
        assertTrue(processes.isEmpty(), "The process list should have been empty");
    }

    @Test
    public void failsWhenReplacingPartiesIfTheNewReplacementPartiesDoNotContainTheGatewayPartyDefinitionAsCurrentlyPresentInConfiguration(
            @Injectable eu.domibus.common.model.configuration.Party replacement) {
        // Expected exception
        // Given
        List<Party> replacements = Lists.newArrayList();
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(replacement);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            replacement.getName();
            result = "replacementParty"; // update the replacement party

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
        }};

        // When
        DomibusCoreException domibusCoreException = assertThrows(DomibusCoreException.class, () -> partyService.replaceParties(replacements, configuration));
        assertThat(domibusCoreException.getMessage(), containsString("[DOM_003]:" + PartyServiceImpl.EXCEPTION_CANNOT_DELETE_PARTY_CURRENT_SYSTEM));
    }

    @Test
    public void addsPartyIdentifierTypesToTheOnesCurrentlyPresentInConfigurationWhenReplacingParties(@Injectable eu.domibus.common.model.configuration.Party converted,
                                                                                                     @Injectable PartyIdType partyIdType,
                                                                                                     @Injectable PartyIdType matchingConfigurationPartyIdType,
                                                                                                     @Injectable PartyIdType nonMatchingConfigurationPartyIdType,
                                                                                                     @Injectable eu.domibus.common.model.configuration.Identifier firstParty,
                                                                                                     @Injectable eu.domibus.common.model.configuration.Identifier secondParty) {
        // Given
        List<Party> replacements = Lists.newArrayList(); // ignore content, just use an empty list
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<PartyIdType> partyIdTypes = Lists.newArrayList(partyIdType);
        List<eu.domibus.common.model.configuration.Identifier> identifiers = new ArrayList<>();
        identifiers.add(firstParty);
        identifiers.add(secondParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;

            partyIdType.equals(matchingConfigurationPartyIdType);
            result = true; // invoked by List#contains
            partyIdType.equals(nonMatchingConfigurationPartyIdType);
            result = false; // invoked by List#contains

            firstParty.getPartyIdType();
            result = matchingConfigurationPartyIdType;
            secondParty.getPartyIdType();
            result = nonMatchingConfigurationPartyIdType;

            converted.getName();
            result = "gatewayParty"; // update the gateway party
            converted.getIdentifiers();
            result = identifiers;

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationPartyIdTypes.getPartyIdType();
            result = partyIdTypes;
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        new VerificationsInOrder() {{
            PartyIdType expected = new PartyIdType();

            expected.setName("id_2");
            partyIdTypes.add(withEqual(expected));

            expected.setName("id_3");
            partyIdTypes.add(withEqual(expected));
        }};
    }

    @Test
    public void removesInitiatorPartiesFromProcessConfigurationIfThePartiesBeingReplacedDoNotBelongToThoseProcessesAnymoreWhenReplacingParties(@Injectable Party replacement,
                                                                                                                                               @Injectable Process process,
                                                                                                                                               @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                                                               @Injectable InitiatorParties configurationInitiatorParties,
                                                                                                                                               @Injectable InitiatorParty configurationInitiatorParty,
                                                                                                                                               @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<InitiatorParty> configurationInitiatorPartyList = Lists.newArrayList(configurationInitiatorParty);

        new Expectations() {{
            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            replacement.getName();
            result = "gatewayParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationInitiatorParty.getName();
            result = "configurationParty";

            configurationProcess.getInitiatorPartiesXml();
            result = configurationInitiatorParties;
            configurationInitiatorParties.getInitiatorParty();
            result = configurationInitiatorPartyList;
            gatewayParty.getName();
            result = "partyName";

            replacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        new Verifications() {{
            configurationInitiatorPartyList.remove(configurationInitiatorParty);
        }};
    }

    @Test
    public void doesNotAddInitiatorPartiesIfAlreadyExistingInsideProcessConfigurationWhenReplacingParties(@Injectable Party replacement,
                                                                                                          @Injectable Process process,
                                                                                                          @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                          @Injectable InitiatorParties configurationInitiatorParties,
                                                                                                          @Injectable InitiatorParty configurationInitiatorParty,
                                                                                                          @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<InitiatorParty> configurationInitiatorPartyList = Lists.newArrayList(configurationInitiatorParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;

            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            replacement.getName();
            result = "gatewayParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationInitiatorParty.getName();
            result = "gatewayParty";

            configurationProcess.getInitiatorPartiesXml();
            result = configurationInitiatorParties;
            configurationInitiatorParties.getInitiatorParty();
            result = configurationInitiatorPartyList;

            replacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertEquals(
                1, configurationInitiatorPartyList.size(),
                "Should have not added the initiator party to the configuration process if already present");
    }


    @Test
    public void addsInitiatorPartiesIfMissingInsideProcessConfigurationWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                            @Injectable Party replacement,
                                                                                            @Injectable Process process,
                                                                                            @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                            @Injectable InitiatorParties configurationInitiatorParties,
                                                                                            @Injectable InitiatorParty configurationInitiatorParty,
                                                                                            @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement, gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<InitiatorParty> configurationInitiatorPartyList = Lists.newArrayList(configurationInitiatorParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;
            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            gatewayReplacement.getName();
            result = "gatewayParty";
            replacement.getName();
            result = "replacementParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationInitiatorParty.getName();
            result = "gatewayParty";

            configurationProcess.getInitiatorPartiesXml();
            result = configurationInitiatorParties;
            configurationInitiatorParties.getInitiatorParty();
            result = configurationInitiatorPartyList;

            gatewayReplacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList(process);
            replacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertEquals(
                2, configurationInitiatorPartyList.size(), "Should have added the initiator party to the configuration process if not already present");
    }

    @Test
    public void clearsTheInitiatorPartiesForTheConfigurationProcessIfTheReplacementPartyIsNotSetAsInitiatorWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                                                                @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                                                @Injectable InitiatorParties configurationInitiatorParties,
                                                                                                                                @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);

        new Expectations() {{
            converted.getName();
            result = "gatewayParty"; // update the gateway party

            configurationProcess.getInitiatorPartiesXml();
            result = configurationInitiatorParties;
            configurationInitiatorParties.getInitiatorParty();
            result = Lists.newArrayList();

            gatewayReplacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList();

            gatewayParty.getName();
            result = "partyName";

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertTrue(
                configurationInitiatorParties.getInitiatorParty().isEmpty(), "Should have cleared the initiator party to the configuration process if replacement party not set as its initiator");
    }

    @Test
    public void initializesTheInitiatorPartiesIfUndefinedForTheConfigurationProcessWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                                        @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                        @Injectable InitiatorParties configurationInitiatorParties,
                                                                                                        @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;


            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;
            converted.getName();
            result = "gatewayParty"; // update the gateway party

            configurationProcess.getInitiatorPartiesXml();
            returns(null, configurationInitiatorParties);
            configurationInitiatorParties.getInitiatorParty();
            result = Lists.newArrayList();

            gatewayReplacement.getProcessesWithPartyAsInitiator();
            result = Lists.newArrayList();

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        new Verifications() {{
            configurationProcess.setInitiatorPartiesXml(withInstanceLike(new InitiatorParties()));
        }};
    }

    @Test
    public void removesResponderPartiesFromProcessConfigurationIfThePartiesBeingReplacedDoNotBelongToThoseProcessesAnymoreWhenReplacingParties(@Injectable Party replacement,
                                                                                                                                               @Injectable Process process,
                                                                                                                                               @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                                                               @Injectable ResponderParties configurationResponderParties,
                                                                                                                                               @Injectable ResponderParty configurationResponderParty,
                                                                                                                                               @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<ResponderParty> configurationResponderPartyList = Lists.newArrayList(configurationResponderParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;

            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            replacement.getName();
            result = "gatewayParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationResponderParty.getName();
            result = "configurationParty";

            configurationProcess.getResponderPartiesXml();
            result = configurationResponderParties;
            configurationResponderParties.getResponderParty();
            result = configurationResponderPartyList;

            replacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        new Verifications() {{
            configurationResponderPartyList.remove(configurationResponderParty);
        }};
    }

    @Test
    public void doesNotAddResponderPartiesIfAlreadyExistingInsideProcessConfigurationWhenReplacingParties(@Injectable Party replacement,
                                                                                                          @Injectable Process process,
                                                                                                          @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                          @Injectable ResponderParties configurationResponderParties,
                                                                                                          @Injectable ResponderParty configurationResponderParty,
                                                                                                          @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<ResponderParty> configurationResponderPartyList = Lists.newArrayList(configurationResponderParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;

            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            replacement.getName();
            result = "gatewayParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationResponderParty.getName();
            result = "gatewayParty";

            configurationProcess.getResponderPartiesXml();
            result = configurationResponderParties;
            configurationResponderParties.getResponderParty();
            result = configurationResponderPartyList;

            replacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertEquals(
                1, configurationResponderPartyList.size(), "Should have not added the responder party to the configuration process if already present");
    }


    @Test
    public void addsResponderPartiesIfMissingInsideProcessConfigurationWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                            @Injectable Party replacement,
                                                                                            @Injectable Process process,
                                                                                            @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                            @Injectable ResponderParties configurationResponderParties,
                                                                                            @Injectable ResponderParty configurationResponderParty,
                                                                                            @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(replacement, gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);
        List<ResponderParty> configurationResponderPartyList = Lists.newArrayList(configurationResponderParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;
            process.getName();
            result = "process_1";
            configurationProcess.getName();
            result = "process_1";
            gatewayReplacement.getName();
            result = "gatewayParty";
            replacement.getName();
            result = "replacementParty";
            converted.getName();
            result = "gatewayParty"; // update the gateway party
            configurationResponderParty.getName();
            result = "gatewayParty";

            configurationProcess.getResponderPartiesXml();
            result = configurationResponderParties;
            configurationResponderParties.getResponderParty();
            result = configurationResponderPartyList;

            gatewayReplacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList(process);
            replacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList(process);

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertEquals(
                2, configurationResponderPartyList.size(), "Should have added the responder party to the configuration process if not already present");
    }

    @Test
    public void clearsTheResponderPartiesForTheConfigurationProcessIfTheReplacementPartyIsNotSetAsResponderWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                                                                @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                                                @Injectable ResponderParties configurationResponderParties,
                                                                                                                                @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);

        new Expectations() {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            configurationBusinessProcesses.getPartiesXml();
            result = configurationParties;
            configurationParties.getPartyIdTypes();
            result = configurationPartyIdTypes;
            converted.getName();
            result = "gatewayParty"; // update the gateway party

            configurationProcess.getResponderPartiesXml();
            result = configurationResponderParties;
            configurationResponderParties.getResponderParty();
            result = Lists.newArrayList();

            gatewayReplacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList();

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        Assertions.assertTrue(
                configurationResponderParties.getResponderParty().isEmpty(), "Should have cleared the responder party to the configuration process if replacement party not set as its responder");
    }

    @Test
    public void initializesTheResponderPartiesIfUndefinedForTheConfigurationProcessWhenReplacingParties(@Injectable Party gatewayReplacement,
                                                                                                        @Injectable eu.domibus.common.model.configuration.Process configurationProcess,
                                                                                                        @Injectable ResponderParties configurationResponderParties,
                                                                                                        @Injectable eu.domibus.common.model.configuration.Party converted) {
        // Given
        List<Party> replacements = Lists.newArrayList(gatewayReplacement);
        List<eu.domibus.common.model.configuration.Party> convertedForReplacement = Lists.newArrayList(converted);
        List<eu.domibus.common.model.configuration.Party> configurationPartyList = Lists.newArrayList(gatewayParty);

        new Expectations() {{
            converted.getName();
            result = "gatewayParty"; // update the gateway party

            configurationProcess.getResponderPartiesXml();
            returns(null, configurationResponderParties);
            configurationResponderParties.getResponderParty();
            result = Lists.newArrayList();

            gatewayReplacement.getProcessesWithPartyAsResponder();
            result = Lists.newArrayList();
            gatewayParty.getName();
            result = "partyName";

            partyConverter.partyListToConfigurationPartyList(replacements);
            result = convertedForReplacement;

            configurationParties.getParty();
            result = configurationPartyList;
            configurationBusinessProcesses.getProcesses();
            result = Lists.newArrayList(configurationProcess);
        }};

        // When
        partyService.replaceParties(replacements, configuration);

        // Then
        new Verifications() {{
            configurationProcess.setResponderPartiesXml(withInstanceLike(new ResponderParties()));
        }};
    }

    @Test
    public void throwsExceptionIfItCannotRetrieveThePModeRawConfigurationsArchiveWhenUpdatingParties() {
        // Given
        new Expectations() {{
            pModeProvider.getCurrentPmode();
            result = null;
        }};

        // When
        PModeException pModeException = assertThrows(PModeException.class, () -> partyService.updateParties(Lists.newArrayList(), Maps.newHashMap()));
        assertThat(pModeException.getMessage(), containsString("[DOM_001]:Could not update PMode parties: PMode not found!"));

    }

    @Test
    public void throwsExceptionIfItCannotRetrieveThePModeConfigurationWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                                          @Injectable ConfigurationRaw rawConfiguration) throws Exception {
        // Given

        XmlProcessingException xmlProcessingException = new XmlProcessingException("");

        new Expectations() {{
            pModeArchiveInfo.getId();
            result = anyInt;
            rawConfiguration.getXml();
            result = any;

            pModeProvider.getRawConfiguration(anyInt);
            result = rawConfiguration;
            pModeProvider.getPModeConfiguration((byte[]) any);
            result = xmlProcessingException;

            pModeValidationHelper.getPModeValidationException(xmlProcessingException, anyString);
            result = new PModeValidationException(Collections.singletonList(new ValidationIssue("TEST")));
        }};

        // When
        Assertions.assertThrows(PModeValidationException.class, () -> partyService.updateParties(Lists.newArrayList(), Maps.newHashMap()));
    }

    @Test
    public void throwsExceptionIfItCannotUpdatePModeConfigurationWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                                     @Injectable ConfigurationRaw rawConfiguration,
                                                                                     @Injectable PartyServiceImpl.ReplacementResult replacementResult) throws Exception {
        // Given
        XmlProcessingException xmlProcessingException = new XmlProcessingException("");

        new Expectations(partyService) {{

            pModeArchiveInfo.getId();
            result = anyInt;
            rawConfiguration.getXml();
            result = any;

            pModeProvider.getRawConfiguration(anyInt);
            result = rawConfiguration;
            pModeProvider.getPModeConfiguration((byte[]) any);
            result = configuration;

            partyService.replaceParties((List<Party>) any, configuration);
            result = replacementResult;
            rawConfiguration.getConfigurationDate();
            result = new Date();
            pModeProvider.serializePModeConfiguration(configuration);
            result = any;
            pModeProvider.updatePModes((byte[]) any, anyString);
            result = xmlProcessingException;

            pModeValidationHelper.getPModeValidationException(xmlProcessingException, "Error writing current PMode due to: ");
            result = new PModeValidationException(Collections.singletonList(new ValidationIssue("TEST")));
        }};

        // When
        Assertions.assertThrows(PModeValidationException.class, () -> partyService.updateParties(Lists.newArrayList(), Maps.newHashMap()));
    }

    @Test
    public void removesCertificatesInTheCurrentDomainForRemovedPartiesWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                                          @Injectable ConfigurationRaw rawConfiguration,
                                                                                          @Injectable eu.domibus.common.model.configuration.Party removedParty) throws Exception {
        // Given
        List<eu.domibus.common.model.configuration.Party> removedParties = Lists.newArrayList(removedParty);
        new Expectations(partyService) {{
            partyService.replaceParties((List<Party>) any, configuration);
            result = new PartyServiceImpl.ReplacementResult(configuration, removedParties);
        }};

        new Expectations() {{
            pModeArchiveInfo.getId();
            result = anyInt;
            rawConfiguration.getXml();
            result = any;
            removedParty.getName();
            result = "removed";

            pModeProvider.getCurrentPmode();
            result = pModeArchiveInfo;
            pModeProvider.getRawConfiguration(anyLong);
            result = rawConfiguration;
            pModeProvider.getPModeConfiguration((byte[]) any);
            result = configuration;

            rawConfiguration.getConfigurationDate();
            result = new Date();
            pModeProvider.serializePModeConfiguration(configuration);
            result = any;
            pModeProvider.updatePModes((byte[]) any, withPrefix("Updated parties to version of"));
        }};

        // When
        partyService.updateParties(Lists.newArrayList(), Maps.newHashMap());

        // Then
        new Verifications() {{
            List<String> aliases;
            multiDomainCertificateProvider.removeCertificate(currentDomain, aliases = withCapture());
            Assertions.assertEquals(
                    Lists.newArrayList("removed"), aliases, "Should have removed the certificate in the current domain for the removed parties");
        }};
    }

    @Test
    public void ignoresNullPartyCertificatesInTheCurrentDomainWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                                  @Injectable ConfigurationRaw rawConfiguration,
                                                                                  @Injectable eu.domibus.common.model.configuration.Party removedParty) throws Exception {
        // Given
        List<eu.domibus.common.model.configuration.Party> removedParties = Lists.newArrayList(removedParty);
        Map<String, String> partyToCertificateMap = Maps.newHashMap();
        partyToCertificateMap.put("party_1", null);

        new Expectations(partyService) {{
            partyService.validatePartyCertificates(partyToCertificateMap);
            partyService.replaceParties((List<Party>) any, configuration);
            result = new PartyServiceImpl.ReplacementResult(configuration, removedParties);
        }};

        new Expectations() {{
            pModeArchiveInfo.getId();
            result = anyLong;
            rawConfiguration.getXml();
            result = any;
            removedParty.getName();
            result = "removed";

            pModeProvider.getCurrentPmode();
            result = pModeArchiveInfo;
            pModeProvider.getRawConfiguration(anyLong);
            result = rawConfiguration;
            pModeProvider.getPModeConfiguration((byte[]) any);
            result = configuration;

            rawConfiguration.getConfigurationDate();
            result = new Date();
            pModeProvider.serializePModeConfiguration(configuration);
            result = any;
            pModeProvider.updatePModes((byte[]) any, withPrefix("Updated parties to version of"));
            multiDomainCertificateProvider.removeCertificate(currentDomain, (List<String>) any);
        }};

        // When
        partyService.updateParties(Lists.newArrayList(), partyToCertificateMap);

        // Then
        new Verifications() {{
            certificateService.loadCertificate((String) null);
            times = 0;
            multiDomainCertificateProvider.addCertificate(currentDomain, null, anyBoolean);
            times = 0;
        }};
    }

    @Test
    public void TrustStoreUpdateInTheCurrentDomainWhenUpdatingPartiesOnlyIfAnyChangeInCertificate(@Injectable X509Certificate x509Certificate,
                                                                                                  @Injectable PartyServiceImpl.ReplacementResult replacementResult,
                                                                                                  @Injectable eu.domibus.common.model.configuration.Party removedParty) {

        // Given
        Map<String, String> partyToCertificateMap = Maps.newHashMap();
        partyToCertificateMap.put("party_red", "certificate_1");
        List<String> aliases = new ArrayList<>();
        aliases.add("party_blue");

        new Expectations(partyService) {{
            domainProvider.getCurrentDomain();
            result = currentDomain;
            partyService.getRemovedParties(replacementResult);
            result = aliases;
            certificateService.loadCertificate("certificate_1");
            result = x509Certificate;
        }};

        // When
        partyService.updatePartyCertificate(partyToCertificateMap, replacementResult);

        // Then
        new Verifications() {{
            List<CertificateEntry> certificates;
            List<String> aliases;
            multiDomainCertificateProvider.removeCertificate(currentDomain, aliases = withCapture());
            multiDomainCertificateProvider.addCertificate(currentDomain, certificates = withCapture(), true);

            final KeyStore trustStore = multiDomainCertificateProvider.getTrustStore(domainProvider.getCurrentDomain());
            final KeyStore keyStore = multiDomainCertificateProvider.getKeyStore(domainProvider.getCurrentDomain());
            certificateService.saveCertificateAndLogRevocation(trustStore, keyStore);

            Assertions.assertTrue(aliases.size() == 1
                            && "party_blue".equals(aliases.get(0)),
                    "Should update party truststore when removing certificates of the parties");
            Assertions.assertTrue(certificates.size() == 1
                            && "party_red".equals(certificates.get(0).getAlias())
                            && x509Certificate == certificates.get(0).getCertificate(),
                    "Should update party truststore when updating certificates of the parties");
        }};

    }

    @Test
    public void addsPartyCertificatesInTheCurrentDomainWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                           @Injectable ConfigurationRaw rawConfiguration,
                                                                           @Injectable X509Certificate x509Certificate,
                                                                           @Injectable eu.domibus.common.model.configuration.Party removedParty) throws Exception {
        // Given
        List<eu.domibus.common.model.configuration.Party> removedParties = Lists.newArrayList(removedParty);
        Map<String, String> partyToCertificateMap = Maps.newHashMap();
        partyToCertificateMap.put("party_1", "certificate_1");

        new Expectations(partyService) {{
            partyService.validatePartyCertificates(partyToCertificateMap);

            partyService.replaceParties((List<Party>) any, configuration);
            result = new PartyServiceImpl.ReplacementResult(configuration, removedParties);
        }};

        new Expectations() {{
            pModeArchiveInfo.getId();
            result = anyLong;
            rawConfiguration.getXml();
            result = any;
            removedParty.getName();
            result = "removed";

            pModeProvider.getCurrentPmode();
            result = pModeArchiveInfo;
            pModeProvider.getRawConfiguration(anyLong);
            result = rawConfiguration;
            pModeProvider.getPModeConfiguration((byte[]) any);
            result = configuration;

            rawConfiguration.getConfigurationDate();
            result = new Date();
            pModeProvider.serializePModeConfiguration(configuration);
            result = any;
            pModeProvider.updatePModes((byte[]) any, withPrefix("Updated parties to version of"));
            multiDomainCertificateProvider.removeCertificate(currentDomain, (List<String>) any);
            certificateService.loadCertificate("certificate_1");
            result = x509Certificate;
        }};

        // When
        partyService.updateParties(Lists.newArrayList(), partyToCertificateMap);

        // Then
        new Verifications() {{
            List<CertificateEntry> certificates;
            multiDomainCertificateProvider.addCertificate(currentDomain, certificates = withCapture(), true);
            Assertions.assertTrue(certificates.size() == 1
                            && "party_1".equals(certificates.get(0).getAlias())
                            && x509Certificate == certificates.get(0).getCertificate(),
                    "Should have ignore party certificates that are null when updating parties");
        }};
    }


    @Test
    public void throwsExceptionIfLoadingPartyCertificatesFailsInTheCurrentDomainWhenUpdatingParties(@Injectable PModeArchiveInfo pModeArchiveInfo,
                                                                                                    @Injectable ConfigurationRaw rawConfiguration,
                                                                                                    @Injectable eu.domibus.common.model.configuration.Party removedParty) throws Exception {
        // Given
        List<eu.domibus.common.model.configuration.Party> removedParties = Lists.newArrayList(removedParty);
        Map<String, String> partyToCertificateMap = Maps.newHashMap();
        partyToCertificateMap.put("party_1", "certificate_1");

        new Expectations(partyService) {{
            partyService.validatePartyCertificates(partyToCertificateMap);
            partyService.replaceParties((List<Party>) any, configuration);
            result = new PartyServiceImpl.ReplacementResult(configuration, removedParties);
        }};

        new Expectations() {{

            pModeArchiveInfo.getId();
            result = anyInt;
            rawConfiguration.getXml();
            result = any;
            removedParty.getName();
            result = "removed";

            pModeProvider.getPModeConfiguration((byte[]) any);
            result = configuration;

            rawConfiguration.getConfigurationDate();
            result = new Date();
            pModeProvider.serializePModeConfiguration(configuration);
            result = any;
            pModeProvider.updatePModes((byte[]) any, withPrefix("Updated parties to version of"));
            multiDomainCertificateProvider.removeCertificate(currentDomain, (List<String>) any);
            certificateService.loadCertificate("certificate_1");
            result = new DomibusCertificateException();
        }};

        // When
        Assertions.assertThrows(IllegalStateException.class, () -> partyService.updateParties(Lists.newArrayList(), partyToCertificateMap));
    }

    @Test
    public void printPartyProcessesTest(@Injectable Party party,
                                        @Injectable Process process) {
        List<Process> processes = new ArrayList<>();

        new Expectations() {{
            processes.add(process);
            party.getProcessesWithPartyAsInitiator();
            result = processes;
            party.getProcessesWithPartyAsResponder();
            result = processes;
        }};

        partyService.printPartyProcesses(party);

        new Verifications() {{
            party.getProcessesWithPartyAsInitiator();
            times = 2;
            party.getProcessesWithPartyAsResponder();
            times = 2;
        }};
    }


    @Test
    public void test_createParty(final @Injectable Party party,
                                 final @Injectable eu.domibus.common.model.configuration.Party configParty,
                                 final @Injectable BusinessProcesses businessProcesses,
                                 final @Injectable Parties parties,
                                 final @Injectable Configuration configuration) {
        final String certificateContent = "test";
        final List<eu.domibus.common.model.configuration.Party> listParties = new ArrayList<>();

        new Expectations(partyService) {{
            partyService.getConfiguration();
            result = configuration;

            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getPartiesXml();
            result = parties;

            parties.getParty();
            result = listParties;

            partyConverter.partyToConfigurationParty(party);
            result = configParty;

        }};

        //tested method
        partyService.createParty(party, certificateContent);

        new FullVerifications(partyService) {{
            partyService.addPartyToConfiguration(configParty, configuration);

            partyService.updatePartyIdTypes(listParties, configuration);

            partyService.addProcessConfiguration(party, configuration);

            partyService.updateConfiguration((Date) any, configuration);

            partyService.addPartyCertificate((HashMap<String, String>) any);
        }};
    }

    @Test
    public void test_addPartyToConfiguration(final @Injectable eu.domibus.common.model.configuration.Party configParty,
                                             final @Injectable BusinessProcesses businessProcesses,
                                             final @Injectable Parties parties,
                                             final @Injectable Configuration configuration) {
        final List<eu.domibus.common.model.configuration.Party> listParties = new ArrayList<>();

        new Expectations() {{
            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getPartiesXml();
            result = parties;

            parties.getParty();
            result = listParties;
        }};

        //tested method
        partyService.addPartyToConfiguration(configParty, configuration);

        Assertions.assertEquals(1, listParties.size());
    }

    @Test
    public void test_addProcessConfiguration(final @Injectable Party party,
                                             final @Injectable eu.domibus.common.model.configuration.Process configProcess,
                                             final @Injectable Configuration configuration) {
        final List<Process> listProcesses = new ArrayList<>();
        Process process = new Process();
        process.setName("tc1Process");
        listProcesses.add(process);

        new Expectations(partyService) {{

            party.getProcessesWithPartyAsInitiator();
            result = listProcesses;
            times = 2;

            party.getProcessesWithPartyAsResponder();
            result = listProcesses;
            times = 2;

            partyService.getProcess("tc1Process", configuration);
            result = configProcess;

            configProcess.getInitiatorPartiesXml().getInitiatorParty();
            result = new ArrayList<InitiatorParty>();

            configProcess.getResponderPartiesXml().getResponderParty();
            result = new ArrayList<ResponderParty>();

        }};

        //tested method
        partyService.addProcessConfiguration(party, configuration);

        new Verifications() {{
            partyService.addProcessConfigurationInitiatorParties(party, process, configuration);
            times = 1;

            partyService.addProcessConfigurationResponderParties(party, process, configuration);
            times = 1;
        }};
    }

    @Test
    public void test_getProcess(final @Injectable Configuration configuration,
                                final @Injectable BusinessProcesses businessProcesses) {
        final String processName = "tc1Process";
        final List<eu.domibus.common.model.configuration.Process> listProcesses = new ArrayList<>();
        eu.domibus.common.model.configuration.Process process = new eu.domibus.common.model.configuration.Process();
        process.setName(processName);
        listProcesses.add(process);

        new Expectations() {{
            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getProcesses();
            result = listProcesses;
        }};

        //tested method
        eu.domibus.common.model.configuration.Process result = partyService.getProcess(processName, configuration);
        Assertions.assertNotNull(result);
        Assertions.assertEquals(processName, result.getName());
    }

    @Test
    public void test_getProcess_NotFound(final @Injectable Configuration configuration,
                                         final @Injectable BusinessProcesses businessProcesses) {
        final String processName = "tc1Process2";
        final List<eu.domibus.common.model.configuration.Process> listProcesses = new ArrayList<>();
        eu.domibus.common.model.configuration.Process process = new eu.domibus.common.model.configuration.Process();
        process.setName("tc1Process");
        listProcesses.add(process);

        new Expectations() {{
            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getProcesses();
            result = listProcesses;
        }};

        try {
            //tested method
            partyService.getProcess(processName, configuration);
            Assertions.fail("exception thrown");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof PModeException);
        }
    }


    @Test
    public void test_deleteParty(final @Injectable Configuration configuration, final @Injectable BusinessProcesses businessProcesses,
                                 final @Injectable eu.domibus.common.model.configuration.Party party,
                                 final @Injectable Parties parties) {
        final String partyName = "red-gw";
        final List<eu.domibus.common.model.configuration.Party> listParties = new ArrayList<>();
        listParties.add(party);

        new Expectations(partyService) {{

            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;
            partyService.getConfiguration();
            result = configuration;

            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getPartiesXml();
            result = parties;

            parties.getParty();
            result = listParties;

            partyService.getParty(partyName, listParties);
            result = party;
        }};

        //tested method
        partyService.deleteParty(partyName);

        new FullVerifications(partyService) {{
            partyService.checkPartyInUse(partyName);

            partyService.initConfigurationParties(configuration);

            partyService.removePartyFromConfiguration(party, configuration);

            partyService.removePartyIdTypes(party, configuration);

            partyService.removeProcessConfiguration(party, configuration);

            partyService.updateConfiguration((Date) any, configuration);

            partyService.removePartyCertificate((List<String>) any);
        }};
    }

    @Test
    public void test_removeProcessConfigurationInitiatorResponderParties(final @Injectable Party party,
                                                                         final @Injectable eu.domibus.common.model.configuration.Process process,
                                                                         final @Injectable InitiatorParties initiatorParties,
                                                                         final @Injectable ResponderParties responderParties) {
        final String partyName = "red-gw";

        InitiatorParty initiatorParty = new InitiatorParty();
        initiatorParty.setName("RED-GW");
        final List<InitiatorParty> initiatorPartyList = new ArrayList<>();
        initiatorPartyList.add(initiatorParty);

        ResponderParty responderParty = new ResponderParty();
        responderParty.setName("RED-gw");
        final List<ResponderParty> responderPartyList = new ArrayList<>();
        responderPartyList.add(responderParty);

        new Expectations() {{
            party.getName();
            result = partyName;

            process.getInitiatorPartiesXml();
            result = initiatorParties;
            times = 2;

            initiatorParties.getInitiatorParty();
            result = initiatorPartyList;

            process.getResponderPartiesXml();
            result = responderParties;

            responderParties.getResponderParty();
            result = responderPartyList;
        }};

        //tested method
        partyService.removeProcessConfigurationInitiatorResponderParties(party, process);
        assertEquals(0, initiatorPartyList.size());
        assertEquals(0, responderPartyList.size());
    }

    @Test
    public void test_updateParty(final @Injectable Configuration configuration, final @Injectable BusinessProcesses businessProcesses,
                                 final @Injectable eu.domibus.common.model.configuration.Party configParty,
                                 final @Injectable eu.domibus.common.model.configuration.Party newParty,
                                 final @Injectable Party party,
                                 final @Injectable Parties parties) {
        final String partyName = "red-gw";
        final List<eu.domibus.common.model.configuration.Party> listParties = new ArrayList<>();
        listParties.add(configParty);
        final String certificateContent = "test";

        new Expectations(partyService) {{
            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;

            gatewayParty.getName();
            result = "gatewayParty";
            pModeProvider.getGatewayParty();
            result = gatewayParty;

            configuration.getBusinessProcesses();
            result = configurationBusinessProcesses;

            party.getName();
            result = partyName;

            partyService.getConfiguration();
            result = configuration;

            configuration.getBusinessProcesses();
            result = businessProcesses;

            businessProcesses.getPartiesXml();
            result = parties;

            parties.getParty();
            result = listParties;

            partyService.getParty(partyName, listParties);
            result = configParty;

            partyConverter.partyToConfigurationParty(party);
            result = newParty;
        }};

        //tested method
        partyService.updateParty(party, certificateContent);

        new FullVerifications(partyService) {{
            partyService.checkPartyInUse(partyName);

            partyService.initConfigurationParties(configuration);

            partyService.removePartyFromConfiguration(configParty, configuration);

            partyService.removePartyIdTypes(configParty, configuration);

            partyService.removeProcessConfiguration(configParty, configuration);

            partyService.updateConfiguration((Date) any, configuration);

            partyService.removePartyCertificate((List<String>) any);

            partyService.addPartyToConfiguration(newParty, configuration);

            partyService.updatePartyIdTypes(listParties, configuration);

            partyService.addProcessConfiguration(party, configuration);

            partyService.updateConfiguration((Date) any, configuration);

            partyService.addPartyCertificate((HashMap<String, String>) any);

        }};
    }


    @Test
    public void test_getConfiguration(final @Injectable PModeArchiveInfo pModeArchiveInfo,
                                      final @Injectable ConfigurationRaw configurationRaw,
                                      final @Injectable byte[] pmodeContent,
                                      final @Injectable Configuration configuration) throws Exception {
        final long pModeId = 1;

        new Expectations(partyService) {{
            pModeProvider.getCurrentPmode();
            result = pModeArchiveInfo;
            times = 1;

            pModeArchiveInfo.getId();
            result = pModeId;

            pModeProvider.getRawConfiguration(pModeId);
            result = configurationRaw;

            configurationRaw.getXml();
            result = pmodeContent;

            pModeProvider.getPModeConfiguration(pmodeContent);
            result = configuration;
        }};

        //tested method
        partyService.getConfiguration();

        new FullVerifications() {
        };
    }

    @Test
    public void test_getConfiguration_Exception(final @Injectable PModeArchiveInfo pModeArchiveInfo,
                                                final @Injectable ConfigurationRaw configurationRaw,
                                                final @Injectable byte[] pmodeContent) throws Exception {
        final long pModeId = 1;

        new Expectations(partyService) {{
            pModeProvider.getCurrentPmode();
            result = pModeArchiveInfo;
            times = 1;

            pModeArchiveInfo.getId();
            result = pModeId;

            pModeProvider.getRawConfiguration(pModeId);
            result = configurationRaw;

            configurationRaw.getXml();
            result = pmodeContent;

            pModeProvider.getPModeConfiguration(pmodeContent);
            result = new XmlProcessingException("test");
        }};

        try {
            //tested method
            partyService.getConfiguration();
            Assertions.fail("exception expected");
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof PModeException);
        }

        new FullVerifications() {
        };
    }

    @Test
    public void test_getParty() {
        final String partyName = "red-gw";
        List<eu.domibus.common.model.configuration.Party> partyList = new ArrayList<>();
        eu.domibus.common.model.configuration.Party configParty = new eu.domibus.common.model.configuration.Party();
        configParty.setName("RED-gw");
        partyList.add(configParty);

        //tested method
        eu.domibus.common.model.configuration.Party foundParty = partyService.getParty(partyName, partyList);
        Assertions.assertNotNull(foundParty);
        Assertions.assertTrue(partyName.equalsIgnoreCase(foundParty.getName()));
    }

    @Test
    public void validatePartyCertificates_errors() {
        Map<String, String> partyToCertificateMap = new HashMap<>();
        partyToCertificateMap.put("party1", "l/kjgslkgjrlgjkerljkh");
        partyToCertificateMap.put("party2", "l/kjgslkgjrlgjkerljkhgertwerywey");
        partyToCertificateMap.put("party3", "l/kjgslkgjrlgjkerljkhsghsrtuwruw6uw65iuw5y5y");

        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_FILE_UPLOAD_MAX_SIZE);
            result = 25;
        }};

        try {
            partyService.validatePartyCertificates(partyToCertificateMap);
            Assertions.fail();
        } catch (PModeValidationException ex) {
            Assertions.assertEquals(2, ex.getIssues().size());
            Assertions.assertEquals(ValidationIssue.class, ex.getIssues().get(0).getClass());
            Assertions.assertTrue(ex.getIssues().stream().anyMatch(el -> el.getMessage().contains("party2")));
            Assertions.assertTrue(ex.getIssues().stream().anyMatch(el -> el.getMessage().contains("party3")));
        }
    }

    @Test
    public void validatePartyCertificates_OK() {
        Map<String, String> partyToCertificateMap = new HashMap<>();
        partyToCertificateMap.put("party1", "l/kjgslkgjrlgjkerljkh");
        partyToCertificateMap.put("party2", "l/kjgslkgjrlgjkerljkhgertwerywey");
        partyToCertificateMap.put("party3", "l/kjgslkgjrlgjkerljkhsghsrtuwruw6uw65iuw5y5y");

        new Expectations() {{
            domibusPropertyProvider.getIntegerProperty(DOMIBUS_FILE_UPLOAD_MAX_SIZE);
            result = 225;
        }};

        try {
            partyService.validatePartyCertificates(partyToCertificateMap);
        } catch (PModeValidationException ex) {
            Assertions.fail();
        }
    }

    @Test
    public void testSanitize() {
        String partyIdTypeName = "partyIdTypeName";
        String partyIdId = "blue";
        String partyName = "domibus-blue";

        eu.domibus.api.party.PartyIdType pIdType = new eu.domibus.api.party.PartyIdType();
        pIdType.setName(partyIdTypeName + "  ");
        pIdType.setValue("partyIdTypeValue");
        Identifier partyId = new Identifier();
        partyId.setPartyId("  " + partyIdId);
        partyId.setPartyIdType(pIdType);

        Party party = new Party();
        party.setName(" " + partyName + "  ");
        party.setIdentifiers(Collections.singletonList(partyId));

        List<Party> parties = Collections.singletonList(party);

        partyService.sanitizeParties(parties);

        Assertions.assertEquals(partyName, parties.get(0).getName());
        Identifier id1 = parties.get(0).getIdentifiers().stream().findFirst().get();
        Assertions.assertEquals(partyIdId, id1.getPartyId());
        Assertions.assertEquals(partyIdTypeName, id1.getPartyIdType().getName());
    }
}
