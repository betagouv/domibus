package eu.domibus.api.message.validation;



import eu.domibus.api.model.PartInfo;
import eu.domibus.api.model.UserMessage;

import javax.xml.soap.SOAPMessage;
import java.util.List;

public interface UserMessageValidatorSpiService {

    void validate(SOAPMessage request, UserMessage userMessage, List<PartInfo> partInfos);

    void validate(UserMessage userMessage, List<PartInfo> partInfos);
}
