package eu.domibus.ext.services;

import eu.domibus.ext.domain.TrustStoreDTO;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.ResponseEntity;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * All operations related to truststore files
 * @author Soumya Chandran
 * @since5.1
 */
public interface TruststoreExtService {

    /**
     * Download the truststore file
     * @return ResponseEntity<ByteArrayResource>
     */
    ResponseEntity<ByteArrayResource> downloadTruststoreContent();

    /**
     * Returns PMode current file information
     * @return list of {@code TrustStoreDTO}
     */
    List<TrustStoreDTO> getTrustStoreEntries();

    /**
     * Upload a new version of the truststore file
     * @param file     truststore file wrapping class
     * @param password of the truststore uploaded
     * @return String as error
     */
    String uploadTruststoreFile(MultipartFile file, String password);
}

