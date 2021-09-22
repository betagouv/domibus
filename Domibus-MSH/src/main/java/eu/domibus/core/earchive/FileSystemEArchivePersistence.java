package eu.domibus.core.earchive;

import eu.domibus.core.earchive.eark.DomibusEARKSIP;
import eu.domibus.core.earchive.eark.DomibusIPFile;
import eu.domibus.core.earchive.eark.EARKSIPBuilderService;
import eu.domibus.core.earchive.storage.EArchiveFileStorageProvider;
import eu.domibus.core.property.DomibusVersionService;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.vfs2.FileObject;
import org.apache.commons.vfs2.FileSystemException;
import org.apache.commons.vfs2.VFS;
import org.roda_project.commons_ip.utils.IPException;
import org.roda_project.commons_ip2.model.IPRepresentation;
import org.roda_project.commons_ip2.model.SIP;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.util.Map;

import static java.util.Collections.singletonList;

/**
 * @author François Gautier
 * @since 5.0
 */
@Service
public class FileSystemEArchivePersistence implements EArchivePersistence {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(FileSystemEArchivePersistence.class);
    public static final String BATCH_JSON = "batch.json";

    protected final EArchiveFileStorageProvider storageProvider;

    protected final DomibusVersionService domibusVersionService;

    private final EArchivingService eArchivingService;

    private EARKSIPBuilderService eArkSipBuilderService;


    public FileSystemEArchivePersistence(EArchiveFileStorageProvider storageProvider,
                                         DomibusVersionService domibusVersionService,
                                         EArchivingService eArchivingService,
                                         EARKSIPBuilderService eArkSipBuilderService) {
        this.storageProvider = storageProvider;
        this.domibusVersionService = domibusVersionService;
        this.eArchivingService = eArchivingService;
        this.eArkSipBuilderService = eArkSipBuilderService;
    }

    @Override
    public FileObject createEArkSipStructure(BatchEArchiveDTO batchEArchiveDTO) {
        LOG.info("Create earchive structure for batchId [{}]", batchEArchiveDTO.getBatchId());

        try (FileObject batchDirectory = VFS.getManager().resolveFile(storageProvider.getCurrentStorage().getStorageDirectory(), batchEArchiveDTO.getBatchId())) {
            batchDirectory.createFolder();

            DomibusEARKSIP sip = new DomibusEARKSIP();
            sip.setBatchId(batchEArchiveDTO.getBatchId());
            sip.addCreatorSoftwareAgent(domibusVersionService.getArtifactName(), domibusVersionService.getDisplayVersion());
            sip.setDescription(domibusVersionService.getDisplayVersion());

            LOG.debug("DomibusEARKSIP initialized [{}]", sip);
            addRepresentation1(sip, batchEArchiveDTO);
            LOG.debug("DomibusEARKSIP created [{}]", sip);

            return eArkSipBuilderService.build(sip, batchDirectory);
        } catch (IPException | FileSystemException e) {
            throw new DomibusEArchiveException("Could not create eArchiving structure for batch [" + batchEArchiveDTO + "]", e);
        }
    }

    protected void addRepresentation1(SIP sip, BatchEArchiveDTO batchEArchiveDTO) throws IPException {
        IPRepresentation representation1 = new IPRepresentation("representation1");
        sip.addRepresentation(representation1);

        LOG.debug("Add batch.json");
        InputStream batchFileJson = eArchivingService.getBatchFileJson(batchEArchiveDTO);
        representation1.addFile(new DomibusIPFile(batchFileJson, BATCH_JSON));
        for (String messageId : batchEArchiveDTO.getMessages()) {
            LOG.debug("Add messageId [{}]", messageId);
            addUserMessage(representation1, messageId);
        }
    }

    private void addUserMessage(IPRepresentation representation1, String messageId) {
        Map<String, InputStream> archivingFile = eArchivingService.getArchivingFiles(messageId);

        for (Map.Entry<String, InputStream> file : archivingFile.entrySet()) {
            LOG.debug("Process file [{}]", file.getKey());
            processFile(representation1, messageId, file);
        }
    }

    private void processFile(IPRepresentation representation1, String messageId, Map.Entry<String, InputStream> aFile) {
        DomibusIPFile soapEnvelope = new DomibusIPFile(aFile.getValue(), aFile.getKey());
        soapEnvelope.setRelativeFolders(singletonList(messageId));
        representation1.addFile(soapEnvelope);
    }
}