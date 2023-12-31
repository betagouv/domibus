package eu.domibus.web.rest;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.party.Party;
import eu.domibus.api.party.PartyService;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.DomibusCertificateException;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.security.TrustStoreEntry;
import eu.domibus.core.converter.PartyCoreMapper;
import eu.domibus.core.party.*;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.web.rest.ro.PartyFilterRequestRO;
import eu.domibus.web.rest.ro.TrustStoreRO;
import eu.domibus.web.rest.ro.ValidationResponseRO;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.security.KeyStoreException;
import java.security.cert.X509Certificate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author Thomas Dussart
 * @author Ion Perpegel
 * @since 4.0
 */
@RestController
@RequestMapping(value = "/rest/party")
@Validated
public class PartyResource extends BaseResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PartyResource.class);
    private static final String DELIMITER = ", ";

    private final PartyCoreMapper partyCoreMapper;

    private final PartyService partyService;

    private final CertificateService certificateService;

    private final PModeValidationHelper pModeValidationHelper;

    private final MultiDomainCryptoService multiDomainCertificateProvider;

    private final DomainContextProvider domainProvider;

    public PartyResource(PartyCoreMapper partyCoreMapper,
                         PartyService partyService,
                         CertificateService certificateService,
                         PModeValidationHelper pModeValidationHelper,
                         MultiDomainCryptoService multiDomainCertificateProvider,
                         DomainContextProvider domainProvider) {
        this.partyCoreMapper = partyCoreMapper;
        this.partyService = partyService;
        this.certificateService = certificateService;
        this.pModeValidationHelper = pModeValidationHelper;
        this.multiDomainCertificateProvider = multiDomainCertificateProvider;
        this.domainProvider = domainProvider;
    }

    @GetMapping(value = {"/list"})
    public List<PartyResponseRo> listParties(@Valid PartyFilterRequestRO request) {
        // basic user input sanitizing; pageSize = 0 means no pagination.
        if (request.getPageStart() <= 0) {
            request.setPageStart(0);
        }
        if (request.getPageSize() <= 0) {
            request.setPageSize(Integer.MAX_VALUE);
        }
        LOG.debug("Searching party with parameters name [{}], endPoint [{}], partyId [{}], processName [{}], pageStart [{}], pageSize [{}]",
                request.getName(), request.getEndPoint(), request.getPartyId(), request.getProcess(), request.getPageStart(), request.getPageSize());

        List<PartyResponseRo> partyResponseRos = partyCoreMapper.partyListToPartyResponseRoList(
                partyService.getParties(request.getName(), request.getEndPoint(), request.getPartyId(), request.getProcess(), request.getPageStart(), request.getPageSize()));

        flattenIdentifiers(partyResponseRos);

        flattenProcesses(partyResponseRos);

        partyResponseRos.forEach(partyResponseRo -> {
            final List<ProcessRo> processesWithPartyAsInitiator = partyResponseRo.getProcessesWithPartyAsInitiator();
            final List<ProcessRo> processesWithPartyAsResponder = partyResponseRo.getProcessesWithPartyAsResponder();

            final Set<ProcessRo> processRos = new HashSet<>(processesWithPartyAsInitiator);
            processRos.addAll(processesWithPartyAsResponder);

            processRos.stream()
                    .map(item -> new PartyProcessLinkRo(item.getName(), processesWithPartyAsInitiator.contains(item), processesWithPartyAsResponder.contains(item)))
                    .collect(Collectors.toSet());
        });

        return partyResponseRos;
    }

    /**
     * This method returns a CSV file with the contents of Party table
     *
     * @return CSV file with the contents of Party table
     */
    @GetMapping(path = "/csv")
    public ResponseEntity<String> getCsv(@Valid PartyFilterRequestRO request) {
        request.setPageStart(0);
        request.setPageSize(0); // no pagination
        final List<PartyResponseRo> partyResponseRoList = listParties(request);
        getCsvService().validateMaxRows(partyResponseRoList.size());

        return exportToCSV(partyResponseRoList,
                PartyResponseRo.class,
                ImmutableMap.of(
                        "Name".toUpperCase(), "Party name",
                        "EndPoint".toUpperCase(), "End point",
                        "JoinedIdentifiers".toUpperCase(), "Party id",
                        "JoinedProcesses".toUpperCase(), "Process(I=Initiator, R= Responder, IR=Both)"
                ),
                Arrays.asList("entityId", "identifiers", "userName", "processesWithPartyAsInitiator", "processesWithPartyAsResponder", "certificateContent"),
                "pmodeparties");
    }

    @PutMapping(value = {"/update"})
    public ValidationResponseRO updateParties(@RequestBody List<PartyResponseRo> partiesRo) {
        LOG.debug("Updating parties [{}]", Arrays.toString(partiesRo.toArray()));

        List<Party> partyList = partyCoreMapper.partyResponseRoListToPartyList(partiesRo);
        LOG.debug("Updating partyList [{}]", partyList.toArray());

        Map<String, String> certificates = partiesRo.stream()
                .filter(party -> party.getCertificateContent() != null)
                .collect(Collectors.toMap(PartyResponseRo::getName, PartyResponseRo::getCertificateContent));

        List<ValidationIssue> pModeUpdateIssues = partyService.updateParties(partyList, certificates);

        return pModeValidationHelper.getValidationResponse(pModeUpdateIssues, "PMode parties have been successfully updated.");
    }

    /**
     * Flatten the list of identifiers of each party into a comma separated list
     * for displaying in the console.
     *
     * @param partyResponseRos the list of party to be adapted.
     */
    protected void flattenIdentifiers(List<PartyResponseRo> partyResponseRos) {
        partyResponseRos.forEach(
                partyResponseRo -> {
                    String joinedIdentifiers = partyResponseRo.getIdentifiers().
                            stream().
                            map(IdentifierRo::getPartyId).
                            sorted().
                            collect(Collectors.joining(DELIMITER));
                    partyResponseRo.setJoinedIdentifiers(joinedIdentifiers);
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flatten identifiers for [{}]=[{}]", partyResponseRo.getName(), partyResponseRo.getJoinedIdentifiers());
                    }
                });
    }

    /**
     * Flatten the list of processes of each party into a comma separated list
     * for displaying in the console.
     *
     * @param partyResponseRos the list of party to be adapted.
     */
    protected void flattenProcesses(List<PartyResponseRo> partyResponseRos) {
        partyResponseRos.forEach(
                partyResponseRo -> {

                    List<ProcessRo> processesWithPartyAsInitiator = partyResponseRo.getProcessesWithPartyAsInitiator();
                    List<ProcessRo> processesWithPartyAsResponder = partyResponseRo.getProcessesWithPartyAsResponder();

                    List<ProcessRo> processesWithPartyAsInitiatorAndResponder
                            = processesWithPartyAsInitiator.
                            stream().
                            filter(processesWithPartyAsResponder::contains).
                            collect(Collectors.toList());

                    List<ProcessRo> processWithPartyAsInitiatorOnly = processesWithPartyAsInitiator
                            .stream()
                            .filter(processRo -> !processesWithPartyAsInitiatorAndResponder.contains(processRo))
                            .collect(Collectors.toList());

                    List<ProcessRo> processWithPartyAsResponderOnly = processesWithPartyAsResponder
                            .stream()
                            .filter(processRo -> !processesWithPartyAsInitiatorAndResponder.contains(processRo))
                            .collect(Collectors.toList());

                    String joinedProcessesWithMeAsInitiatorOnly = processWithPartyAsInitiatorOnly.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(I)")).
                            collect(Collectors.joining(DELIMITER));

                    String joinedProcessesWithMeAsResponderOnly = processWithPartyAsResponderOnly.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(R)")).
                            collect(Collectors.joining(DELIMITER));

                    String joinedProcessesWithMeAsInitiatorAndResponder = processesWithPartyAsInitiatorAndResponder.
                            stream().
                            map(ProcessRo::getName).
                            map(name -> name.concat("(IR)")).
                            collect(Collectors.joining(DELIMITER));

                    List<String> joinedProcess = Lists.newArrayList();

                    if (StringUtils.isNotEmpty(joinedProcessesWithMeAsInitiatorOnly)) {
                        joinedProcess.add(joinedProcessesWithMeAsInitiatorOnly);
                    }

                    if (StringUtils.isNotEmpty(joinedProcessesWithMeAsResponderOnly)) {
                        joinedProcess.add(joinedProcessesWithMeAsResponderOnly);
                    }

                    if (StringUtils.isNotEmpty(joinedProcessesWithMeAsInitiatorAndResponder)) {
                        joinedProcess.add(joinedProcessesWithMeAsInitiatorAndResponder);
                    }

                    partyResponseRo.setJoinedProcesses(
                            StringUtils.join(joinedProcess, DELIMITER));
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Flatten processes for [{}]=[{}]", partyResponseRo.getName(), partyResponseRo.getJoinedProcesses());
                    }
                });
    }

    @GetMapping(value = {"/processes"})
    public List<ProcessRo> listProcesses() {
        return partyCoreMapper.processAPIListToProcessRoList(partyService.getAllProcesses());
    }

    @GetMapping(value = "/{partyName}/certificate")
    public ResponseEntity<TrustStoreRO> getCertificateForParty(@PathVariable(name = "partyName") String partyName) {
        try {
            X509Certificate cert = multiDomainCertificateProvider.getCertificateFromTruststore(domainProvider.getCurrentDomain(), partyName);
            TrustStoreEntry entry = certificateService.createTrustStoreEntry(cert, partyName);
            if (entry == null) {
                LOG.debug("Certificate entry not found for party name [{}].", partyName);
                return ResponseEntity.notFound().build();
            }
            TrustStoreRO res = partyCoreMapper.trustStoreEntryToTrustStoreRO(entry);
            return ResponseEntity.ok(res);
        } catch (KeyStoreException e) {
            LOG.error("Failed to get certificate from truststore", e);
            return ResponseEntity.badRequest().build();
        }
    }

    @PutMapping(value = "/{partyName}/certificate")
    public TrustStoreRO convertCertificateContent(@PathVariable(name = "partyName") String partyName,
                                                  @RequestBody CertificateContentRo certificate) {
        if (certificate == null) {
            throw new IllegalArgumentException("Certificate parameter must be provided");
        }

        String content = certificate.getContent();
        LOG.debug("certificate base 64 received [{}] ", content);

        TrustStoreEntry cert = null;
        try {
            cert = certificateService.convertCertificateContent(content);
        } catch (DomibusCertificateException e) {
            throw new IllegalArgumentException("Certificate could not be parsed", e);
        }
        if (cert == null) {
            throw new IllegalArgumentException("Certificate could not be parsed");
        }

        return partyCoreMapper.trustStoreEntryToTrustStoreRO(cert);
    }

}
