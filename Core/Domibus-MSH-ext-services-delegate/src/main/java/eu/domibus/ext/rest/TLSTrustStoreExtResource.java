package eu.domibus.ext.rest;

import eu.domibus.api.exceptions.RequestValidationException;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.security.CertificatePurpose;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.api.util.DateUtil;
import eu.domibus.api.util.MultiPartFileUtil;
import eu.domibus.api.validators.SkipWhiteListed;
import eu.domibus.ext.delegate.mapper.DomibusExtMapper;
import eu.domibus.ext.domain.*;
import eu.domibus.ext.exceptions.CryptoExtException;
import eu.domibus.ext.rest.error.ExtExceptionHelper;
import eu.domibus.ext.services.DomainContextExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.TLSTrustStoreExtService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.apache.commons.lang3.StringUtils;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import java.time.LocalDateTime;
import java.util.List;

import static eu.domibus.api.crypto.TLSCertificateManager.TLS_TRUSTSTORE_NAME;
import static eu.domibus.ext.rest.TruststoreExtResource.ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD;

/**
 * TLS Truststore Domibus services.
 * Domibus expose the REST API to upload and get the TLS truststore.
 *
 * @author Soumya Chandran
 * @since 5.1
 */
@RestController
@RequestMapping(value = "/ext/tlstruststore")
@SecurityScheme(type = SecuritySchemeType.HTTP, name = "DomibusBasicAuth", scheme = "basic")
@Tag(name = "TLS truststore", description = "Domibus TLS truststore services API")
@PreAuthorize("hasAnyRole('ROLE_ADMIN','ROLE_AP_ADMIN')")
public class TLSTrustStoreExtResource {

    public static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(TLSTrustStoreExtResource.class);

    final TLSTrustStoreExtService tlsTruststoreExtService;

    final ExtExceptionHelper extExceptionHelper;

    final MultiPartFileUtil multiPartFileUtil;

    final DomainContextExtService domainContextExtService;

    final DomibusConfigurationExtService domibusConfigurationExtService;

    final SecurityProfileService securityProfileService;

    final DomibusExtMapper domibusExtMapper;

    public TLSTrustStoreExtResource(TLSTrustStoreExtService tlsTruststoreExtService,
                                    ExtExceptionHelper extExceptionHelper,
                                    MultiPartFileUtil multiPartFileUtil,
                                    DomainContextExtService domainContextExtService,
                                    DomibusConfigurationExtService domibusConfigurationExtService,
                                    SecurityProfileService securityProfileService, DomibusExtMapper domibusExtMapper) {
        this.tlsTruststoreExtService = tlsTruststoreExtService;
        this.extExceptionHelper = extExceptionHelper;
        this.multiPartFileUtil = multiPartFileUtil;
        this.domainContextExtService = domainContextExtService;
        this.domibusConfigurationExtService = domibusConfigurationExtService;
        this.securityProfileService = securityProfileService;
        this.domibusExtMapper = domibusExtMapper;
    }

    @ExceptionHandler(CryptoExtException.class)
    protected ResponseEntity<ErrorDTO> handleTrustStoreExtException(CryptoExtException e) {
        return extExceptionHelper.handleExtException(e);
    }

    @Operation(summary = "Get TLS truststore entries", description = "Get the TLS truststore details",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @GetMapping(value = {"/entries"})
    public List<TrustStoreDTO> getTLSTruststoreEntries() {
        return tlsTruststoreExtService.getTrustStoreEntries();
    }

    @Operation(summary = "Download TLS truststore", description = "Upload the TLS truststore file",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @GetMapping(value = "/download", produces = {MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaType.APPLICATION_JSON_VALUE})
    public ResponseEntity<ByteArrayResource> downloadTLSTrustStore() {
        KeyStoreContentInfoDTO contentInfoDTO;
        byte[] content;
        try {
            contentInfoDTO = tlsTruststoreExtService.downloadTruststoreContent();
            content = contentInfoDTO.getContent();
        } catch (Exception exception) {
            throw new CryptoExtException("Could not download TLS truststore.", exception);
        }
        HttpStatus status = HttpStatus.OK;
        if (content.length == 0) {
            status = HttpStatus.NO_CONTENT;
        }
        return ResponseEntity.status(status)
                .contentType(MediaType.parseMediaType("application/octet-stream"))
                .header("content-disposition", "attachment; filename=" + getFileName())
                .body(new ByteArrayResource(content));
    }

    @Operation(summary = "Upload TLS truststore", description = "Upload the TLS truststore file",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @PostMapping(consumes = {"multipart/form-data"})
    public String uploadTLSTruststoreFile(
            @RequestPart("file") MultipartFile truststoreFile,
            @SkipWhiteListed @RequestParam("password") String password) {

        byte[] truststoreFileContent = multiPartFileUtil.validateAndGetFileContent(truststoreFile);

        if (StringUtils.isBlank(password)) {
            throw new RequestValidationException(ERROR_MESSAGE_EMPTY_TRUSTSTORE_PASSWORD);
        }

        KeyStoreContentInfoDTO contentInfo = new KeyStoreContentInfoDTO(TLS_TRUSTSTORE_NAME, truststoreFileContent, truststoreFile.getOriginalFilename(), password);
        tlsTruststoreExtService.uploadTruststoreFile(contentInfo);

        return "TLS truststore file has been successfully replaced.";
    }

    //TODO: remove with EDELIVERY-11496
    @Operation(summary = "Add Certificate", description = "Add Certificate to the TLS truststore",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @PostMapping(value = "/entries", consumes = {"multipart/form-data"})
    public String addTLSCertificate(@RequestPart("file") MultipartFile certificateFile,
                                    @RequestParam("alias") @Valid @NotNull String alias) throws RequestValidationException {

        return doAddCertificate(certificateFile, alias);
    }

    @Operation(summary = "Add Certificate", description = "Add Certificate to the TLS truststore",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @PostMapping(value = "/certificates", consumes = {"multipart/form-data"})
    public String addTLSCertificate(@RequestPart("file") MultipartFile certificateFile,
                                    @RequestParam("partyName") @Valid @NotNull String partyName,
                                    @RequestParam("securityProfile") @Valid @NotNull SecurityProfileDTO securityProfileDTO,
                                    @RequestParam("certificatePurpose") @Valid @NotNull CertificatePurposeDTO certificatePurposeDTO) throws RequestValidationException {
        SecurityProfile securityProfile = domibusExtMapper.securityProfileDTOToApi(securityProfileDTO);
        CertificatePurpose certificatePurpose = domibusExtMapper.certificatePurposeDTOToApi(certificatePurposeDTO);
        String alias = securityProfileService.getCertificateAliasForPurpose(partyName, securityProfile, certificatePurpose);
        return doAddCertificate(certificateFile, alias);
    }

    protected String doAddCertificate(MultipartFile certificateFile, String alias) {
        if (StringUtils.isBlank(alias)) {
            throw new RequestValidationException("Please provide an alias for the certificate.");
        }

        byte[] fileContent = multiPartFileUtil.validateAndGetFileContent(certificateFile);

        tlsTruststoreExtService.addCertificate(fileContent, alias);

        return "Certificate [" + alias + "] has been successfully added to the TLS truststore.";
    }

    //TODO: remove with EDELIVERY-11496
    @Operation(summary = "Remove Certificate", description = "Remove Certificate from the TLS truststore",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @DeleteMapping(value = "/entries/{alias:.+}")
    public String removeTLSCertificate(@PathVariable String alias) throws RequestValidationException {
        return doRemoveCertificate(alias);
    }

    @Operation(summary = "Remove Certificate", description = "Remove Certificate from the TLS truststore",
            security = @SecurityRequirement(name = "DomibusBasicAuth"))
    @DeleteMapping(value = "/certificates/{partyName:.+}")
    public String removeTLSCertificate(@PathVariable String partyName,
                                       @RequestParam("securityProfile") @Valid @NotNull SecurityProfileDTO securityProfileDTO,
                                       @RequestParam("certificatePurpose") @Valid @NotNull CertificatePurposeDTO certificatePurposeDTO) throws RequestValidationException {
        SecurityProfile securityProfile = domibusExtMapper.securityProfileDTOToApi(securityProfileDTO);
        CertificatePurpose certificatePurpose = domibusExtMapper.certificatePurposeDTOToApi(certificatePurposeDTO);
        String alias = securityProfileService.getCertificateAliasForPurpose(partyName, securityProfile, certificatePurpose);
        return doRemoveCertificate(alias);
    }

    protected String doRemoveCertificate(String alias) {
        tlsTruststoreExtService.removeCertificate(alias);

        return "Certificate [" + alias + "] has been successfully removed from the [" + TLS_TRUSTSTORE_NAME + "].";
    }

    private String getFileName() {
        String fileName = TLS_TRUSTSTORE_NAME;
        DomainDTO domain = domainContextExtService.getCurrentDomainSafely();
        if (domibusConfigurationExtService.isMultiTenantAware() && domain != null) {
            fileName = fileName + "_" + domain.getName();
        }
        fileName = fileName + "_"
                + LocalDateTime.now().format(DateUtil.DEFAULT_FORMATTER)
                + "." + tlsTruststoreExtService.getStoreFileExtension();
        return fileName;
    }
}
