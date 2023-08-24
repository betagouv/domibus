package eu.domibus.core.earchive.eark;

import com.codahale.metrics.MetricRegistry;
import eu.domibus.api.earchive.DomibusEArchiveException;
import eu.domibus.api.util.FileServiceUtil;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.core.metrics.Counter;
import eu.domibus.core.metrics.Timer;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang3.StringUtils;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.mets_v1_12.beans.DivType;
import org.roda_project.commons_ip2.mets_v1_12.beans.FileType;
import org.roda_project.commons_ip2.mets_v1_12.beans.MetsType;
import org.roda_project.commons_ip2.model.IPConstants;
import org.roda_project.commons_ip2.model.MetsWrapper;
import org.roda_project.commons_ip2.model.impl.eark.EARKMETSUtils;
import org.roda_project.commons_ip2.model.impl.eark.EARKSIP;
import org.roda_project.commons_ip2.utils.METSUtils;
import org.roda_project.commons_ip2.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import javax.xml.bind.JAXBException;
import javax.xml.datatype.DatatypeFactory;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.util.GregorianCalendar;
import java.util.Optional;

import static com.codahale.metrics.MetricRegistry.name;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class EARKSIPFileService {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(EARKSIPFileService.class);

    private static final String SHA256_CHECKSUMTYPE = "SHA-256";
    public static final String SHA_256 = "sha256:";

    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    protected XMLUtil xmlUtil;

    @Autowired
    protected FileServiceUtil fileServiceUtil;

    public void setMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }

    @Timer(clazz = EARKSIPFileService.class, value = "earchive21_getMetsWrapper")
    @Counter(clazz = EARKSIPFileService.class, value = "earchive21_getMetsWrapper")
    public MetsWrapper getMetsWrapper(String artifactName, String displayVersion, String batchId) throws IPException {
        EARKSIP sip = new EARKSIP();
        sip.addCreatorSoftwareAgent(artifactName, displayVersion);

        MetsWrapper mainMETSWrapper = EARKMETSUtils.generateMETS(StringUtils.join(sip.getIds(), " "),
                displayVersion,
                sip.getProfile(),
                true,
                Optional.ofNullable(sip.getAncestors()),
                null,
                sip.getHeader(),
                sip.getType(),
                sip.getContentType(),
                null);
        setMetsDocumentID(mainMETSWrapper, batchId);
        return mainMETSWrapper;
    }

    @Timer(clazz = EARKSIPFileService.class, value = "earchive_createDataFile")
    @Counter(clazz = EARKSIPFileService.class, value = "earchive_createDataFile")
    public void createDataFile(Path path, InputStream value) {
        try {
            fileServiceUtil.copyToFile(value, path.toFile());
        } catch (IOException e) {
            throw new DomibusEArchiveException("Could not create file [" + path.toFile().getAbsolutePath() + "]", e);
        }
    }

    @Timer(clazz = EARKSIPFileService.class, value = "earchive23_getChecksum")
    @Counter(clazz = EARKSIPFileService.class, value = "earchive23_getChecksum")
    public String getChecksum(Path path) {
        try {
            return SHA_256 + getChecksumSHA256(path);
        } catch (IOException e) {
            throw new DomibusEArchiveException("Could not calculate the checksum of the manifest for path [" + path + "]", e);
        }
    }

    private void setMetsDocumentID(MetsWrapper mainMETSWrapper, String batchId) {
        MetsType.MetsHdr.MetsDocumentID value = new MetsType.MetsHdr.MetsDocumentID();
        value.setValue(batchId);
        mainMETSWrapper.getMets().getMetsHdr().setMetsDocumentID(value);
    }

    @Timer(clazz = EARKSIPFileService.class, value = "earchive22_addMetsFileToFolder")
    @Counter(clazz = EARKSIPFileService.class, value = "earchive22_addMetsFileToFolder")
    protected Path addMetsFileToFolder(Path destinationDirectory, MetsWrapper mainMETSWrapper) throws IPException {
        try {
            return METSUtils.marshallMETS(mainMETSWrapper.getMets(), Paths.get(destinationDirectory.toFile().getAbsolutePath(), IPConstants.METS_FILE), true);
        } catch (JAXBException | IOException e) {
            throw new DomibusEArchiveException("Could not create METS.xml to [" + destinationDirectory.toFile().getName() + "]", e);
        }
    }

    public void addBatchJsonToMETS(MetsWrapper representationMETS, String pathFromData) {
        addDataFileInfoToMETS(representationMETS, pathFromData, null);
    }

    public void addDataFileInfoToMETS(MetsWrapper representationMETS, String pathFromData, ArchivingFileDTO archivingFileDTO) {
        com.codahale.metrics.Timer.Context crtFile = metricRegistry.timer(name("addDataFileInfoToMETS", "generateRandomAndPrefixedUUID", "timer")).time();
        FileType file = new FileType();
        file.setID(Utils.generateRandomAndPrefixedUUID());
        crtFile.stop();

        com.codahale.metrics.Timer.Context setBF = metricRegistry.timer(name("addDataFileInfoToMETS", "setFileBasicInformation", "timer")).time();
        // set mimetype, date creation, etc.
        setFileBasicInformation(archivingFileDTO, file);
        setBF.stop();

        // add to file section
        com.codahale.metrics.Timer.Context filLoc = metricRegistry.timer(name("addDataFileInfoToMETS", "fileLocation", "timer")).time();
        FileType.FLocat fileLocation = METSUtils.createFileLocation(pathFromData);
        file.getFLocat().add(fileLocation);
        representationMETS.getDataFileGroup().getFile().add(file);
        filLoc.stop();

        com.codahale.metrics.Timer.Context strMap = metricRegistry.timer(name("addDataFileInfoToMETS", "structMap", "timer")).time();
        // add to struct map
        if (representationMETS.getDataDiv().getFptr().isEmpty()) {
            DivType.Fptr fptr = new DivType.Fptr();
            fptr.setFILEID(representationMETS.getDataFileGroup());
            representationMETS.getDataDiv().getFptr().add(fptr);
        }
        strMap.close();
    }

    public void setFileBasicInformation(ArchivingFileDTO archivingFileDTO, FileType fileType) {
        initMimeTypeInfo(archivingFileDTO, fileType);
        initDateCreation(fileType);
        if (archivingFileDTO != null) {
            initSizeInfo(archivingFileDTO, fileType);
            initChecksum(archivingFileDTO, fileType);
        }
    }

    private String getFileName(@Nullable ArchivingFileDTO archivingFileDTO) {
        return (archivingFileDTO == null || archivingFileDTO.getPath() == null) ? FileSystemEArchivePersistence.BATCH_JSON : archivingFileDTO.getPath().toFile().getName();
    }

    private void initSizeInfo(ArchivingFileDTO archivingFileDTO, FileType fileType) {

        LOG.debug("Setting file size [{}]", archivingFileDTO);
        fileType.setSIZE(archivingFileDTO.getSize());
        LOG.debug("Done setting file size");

    }

    private void initDateCreation(FileType fileType) {
        final DatatypeFactory datatypeFactory = xmlUtil.getDatatypeFactory();
        fileType.setCREATED(datatypeFactory.newXMLGregorianCalendar(GregorianCalendar.from(ZonedDateTime.now(ZoneOffset.UTC))));
    }

    private void initMimeTypeInfo(ArchivingFileDTO archivingFileDTO, FileType fileType) {
        try {
            LOG.debug("Setting mimetype [{}]", archivingFileDTO);
            fileType.setMIMETYPE(getFileMimetype(archivingFileDTO));
            LOG.debug("Done setting mimetype");
        } catch (IOException e) {
            throw new DomibusEArchiveException("Error probing content-type [" + getFileName(archivingFileDTO) + "]", e);
        }
    }

    private void initChecksum(ArchivingFileDTO archivingFileDTO, FileType fileType) {
        // checksum
        String checksumSHA256;
        try {
            checksumSHA256 = getChecksumSHA256(archivingFileDTO.getPath());
            LOG.debug("checksumSHA256 [{}] for file [{}]", checksumSHA256, archivingFileDTO.getPath().toFile().getName());
            fileType.setCHECKSUM(checksumSHA256);
            fileType.setCHECKSUMTYPE(SHA256_CHECKSUMTYPE);
        } catch (IOException e) {
            fileType.setCHECKSUM("ERROR");
            LOG.error("Exception while calculating [{}] hash", SHA256_CHECKSUMTYPE, e);
        }
    }

    private String getChecksumSHA256(Path path) throws IOException {
        com.codahale.metrics.Timer.Context crtFile = metricRegistry.timer(name("getChecksumSHA256", "openInputStream", "timer")).time();
        try (final FileInputStream inputStream = FileUtils.openInputStream(path.toFile())) {
            return DigestUtils.sha256Hex(inputStream);
        } finally {
            crtFile.stop();
        }

    }

    private String getFileMimetype(@Nullable ArchivingFileDTO archivingFileDTO) throws IOException {
        if (archivingFileDTO == null || StringUtils.isBlank(archivingFileDTO.getMimeType())) {
            return "application/octet-stream";
        }
        return archivingFileDTO.getMimeType();
    }

}
