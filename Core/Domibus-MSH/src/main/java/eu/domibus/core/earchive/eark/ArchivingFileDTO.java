package eu.domibus.core.earchive.eark;

import java.io.InputStream;
import java.nio.file.Path;

/**
 * @author François Gautier
 * @since 5.0
 */
public class ArchivingFileDTO {

    private InputStream inputStream;

    private Long size;

    private String mimeType;

    private String checkSum;

    private Path path;

    public ArchivingFileDTO(InputStream inputStream, Long size, String mimeType) {
        this.inputStream = inputStream;
        this.size = size;
        this.mimeType = mimeType;
    }

    public InputStream getInputStream() {
        return inputStream;
    }

    public Long getSize() {
        return size;
    }

    public String getMimeType() {
        return mimeType;
    }

    public Path getPath() {
        return path;
    }

    public void setPath(Path path) {
        this.path = path;
    }

    public String getCheckSum() {
        return checkSum;
    }

    public void setCheckSum(String checkSum) {
        this.checkSum = checkSum;
    }

    @Override
    public String toString() {
        return "ArchivingFileDTO{" +
                "inputStream=" + inputStream +
                ", size=" + size +
                ", mimeType='" + mimeType + '\'' +
                ", path=" + path +
                ", checkSum=" + checkSum +
                '}';
    }
}
