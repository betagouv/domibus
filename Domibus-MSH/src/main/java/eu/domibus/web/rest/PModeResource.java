package eu.domibus.web.rest;

import eu.domibus.api.pmode.PModeArchiveInfo;
import eu.domibus.common.model.configuration.ConfigurationRaw;
import eu.domibus.common.services.AuditService;
import eu.domibus.core.converter.DomainCoreConverter;
import eu.domibus.core.csv.CsvExcludedItems;
import eu.domibus.core.csv.CsvService;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.pmode.PModeProvider;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import eu.domibus.messaging.XmlProcessingException;
import eu.domibus.web.rest.ro.PModeResponseRO;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.QueryParam;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * @author Mircea Musat
 * @author Tiago Miguel
 * @since 3.3
 */
@RestController
@RequestMapping(value = "/rest/pmode")
@Validated
public class PModeResource extends BaseResource {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(PModeResource.class);

    @Autowired
    private PModeProvider pModeProvider;

    @Autowired
    private DomainCoreConverter domainConverter;

    @Autowired
    private CsvServiceImpl csvServiceImpl;

    @Autowired
    private AuditService auditService;

    @GetMapping(path = "{id}", produces = "application/xml")
    public ResponseEntity<? extends Resource> downloadPmode(
            @PathVariable(value = "id") int id,
            @DefaultValue("false") @QueryParam("noAudit") boolean noAudit) {

        final byte[] rawConfiguration = pModeProvider.getPModeFile(id);
        ByteArrayResource resource = new ByteArrayResource(new byte[0]);
        if (rawConfiguration != null) {
            resource = new ByteArrayResource(rawConfiguration);
        }

        HttpStatus status = HttpStatus.OK;
        if (resource.getByteArray().length == 0) {
            status = HttpStatus.NO_CONTENT;
        } else if (!noAudit) {
            auditService.addPModeDownloadedAudit(Integer.toString(id));
        }

        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header("content-disposition", "attachment; filename=Pmodes.xml")
                .body(resource);
    }

    @GetMapping(path = "current")
    public PModeResponseRO getCurrentPMode() {
        final PModeArchiveInfo currentPmode = pModeProvider.getCurrentPmode();
        if (currentPmode != null) {
            final PModeResponseRO convert = domainConverter.convert(currentPmode, PModeResponseRO.class);
            convert.setCurrent(true);
            return convert;
        }
        return null;

    }

    @PostMapping
    public ResponseEntity<String> uploadPMode(
            @RequestPart("file") MultipartFile pmode,
            @RequestParam("description") @Valid String pModeDescription) {
        if (pmode.isEmpty()) {
            return ResponseEntity.badRequest().body("Failed to upload the PMode file since it was empty.");
        }
        try {
            byte[] bytes = pmode.getBytes();

            List<String> pmodeUpdateMessage = pModeProvider.updatePModes(bytes, pModeDescription);
            String message = "PMode file has been successfully uploaded";
            if (pmodeUpdateMessage != null && !pmodeUpdateMessage.isEmpty()) {
                message += " but some issues were detected: \n" + StringUtils.join(pmodeUpdateMessage, "\n");
            }
            return ResponseEntity.ok(message);
        } catch (XmlProcessingException e) {
            LOG.error("Error uploading the PMode", e);
            String message = "Failed to upload the PMode file due to: " + ExceptionUtils.getRootCauseMessage(e);
            if (CollectionUtils.isNotEmpty(e.getErrors())) {
                message += ";" + StringUtils.join(e.getErrors(), ";");

            }
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(message);
        } catch (Exception e) {
            LOG.error("Error uploading the PMode", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to upload the PMode file due to: " + ExceptionUtils.getRootCauseMessage(e));
        }
    }

    @DeleteMapping
    public ResponseEntity<String> deletePModes(@RequestParam("ids") List<String> pModeIds) {
        if (pModeIds.isEmpty()) {
            LOG.error("Failed to delete PModes since the list of ids was empty.");
            return ResponseEntity.badRequest().body("Failed to delete PModes since the list of ids was empty.");
        }
        try {
            for (String pModeId : pModeIds) {
                pModeProvider.removePMode(Integer.parseInt(pModeId));
            }
        } catch (Exception ex) {
            LOG.error("Impossible to delete PModes", ex);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Impossible to delete PModes due to \n" + ex.getMessage());
        }
        LOG.debug("PModes {} were deleted", pModeIds);
        return ResponseEntity.ok("PModes were deleted\n");
    }

    @PutMapping(value = {"/restore/{id}"})
    public ResponseEntity<String> uploadPmode(@PathVariable(value = "id") Integer id) {
        ConfigurationRaw existingRawConfiguration = pModeProvider.getRawConfiguration(id);
        ConfigurationRaw newRawConfiguration = new ConfigurationRaw();
        newRawConfiguration.setEntityId(0);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MM-yyyy HH:mm:ssO");
        ZonedDateTime confDate = ZonedDateTime.ofInstant(existingRawConfiguration.getConfigurationDate().toInstant(), ZoneId.systemDefault());
        newRawConfiguration.setDescription("Restored version of " + confDate.format(formatter));

        newRawConfiguration.setConfigurationDate(new Date());
        newRawConfiguration.setXml(existingRawConfiguration.getXml());

        String message = "PMode was successfully uploaded";
        try {
            List<String> pmodeUpdateMessage = pModeProvider.updatePModes(newRawConfiguration.getXml(), newRawConfiguration.getDescription());

            if (pmodeUpdateMessage != null && !pmodeUpdateMessage.isEmpty()) {
                message += " but some issues were detected: \n" + StringUtils.join(pmodeUpdateMessage, "\n");
            }
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Impossible to upload PModes due to \n" + e.getMessage());
        }
        return ResponseEntity.ok(message);
    }

    @GetMapping(value = {"/list"})
    public List<PModeResponseRO> pmodeList() {
        return domainConverter.convert(pModeProvider.getRawConfigurationList(), PModeResponseRO.class);
    }

    /**
     * This method returns a CSV file with the contents of PMode Archive table
     *
     * @return CSV file with the contents of PMode Archive table
     */
    @GetMapping(path = "/csv")
    public ResponseEntity<String> getCsv() {

        // get list of archived pmodes
        List<PModeResponseRO> pModeResponseROList = new ArrayList();
        pModeResponseROList.addAll(pmodeList());

        // set first PMode as current
        if (!pModeResponseROList.isEmpty()) {
            pModeResponseROList.get(0).setCurrent(true);
        }

        return exportToCSV(pModeResponseROList,
                PModeResponseRO.class,
                new HashMap<>(),
                CsvExcludedItems.PMODE_RESOURCE.getExcludedItems(),
                "pmodearchive");
    }

    @Override
    public CsvService getCsvService() {
        return csvServiceImpl;
    }
}
