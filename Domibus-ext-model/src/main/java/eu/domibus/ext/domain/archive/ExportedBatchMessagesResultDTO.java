package eu.domibus.ext.domain.archive;

import java.util.List;

/**
 * @author Joze Rihtarsic
 * @since 5.0
 */
public class ExportedBatchMessagesResultDTO {

    String batchId;
    PaginationDTO pagination;
    List<String> messages;

    public ExportedBatchMessagesResultDTO(String batchId, Integer pageStart, Integer pageSize) {
        this.batchId = batchId;
        this.pagination = new PaginationDTO(pageStart, pageSize);
    }

    public String getBatchId() {
        return batchId;
    }

    public void setBatchId(String batchId) {
        this.batchId = batchId;
    }

    public PaginationDTO getPagination() {
        return pagination;
    }

    public void setPagination(PaginationDTO pagination) {
        this.pagination = pagination;
    }

    public List<String> getMessages() {
        return messages;
    }

    public void setMessages(List<String> messages) {
        this.messages = messages;
    }
}
