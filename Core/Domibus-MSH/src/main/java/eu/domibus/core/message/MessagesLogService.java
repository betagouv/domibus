package eu.domibus.core.message;

import eu.domibus.api.model.MessageType;
import eu.domibus.web.rest.ro.MessageLogRO;
import eu.domibus.web.rest.ro.MessageLogResultRO;

import java.util.List;
import java.util.Map;

/**
 * @author Federico Martini
 * @since 3.2
 */
public interface MessagesLogService {
    long countMessages(MessageType messageType, Map<String, Object> filters);

    MessageLogResultRO countAndFindPaged(MessageType messageType, int from, int max, String orderByColumn, boolean asc, Map<String, Object> filters, List<String> fields);

    List<MessageLogInfo> findAllInfoCSV(MessageType messageType, int max, String orderByColumn, boolean asc, Map<String, Object> filters, List<String> fields);

}
