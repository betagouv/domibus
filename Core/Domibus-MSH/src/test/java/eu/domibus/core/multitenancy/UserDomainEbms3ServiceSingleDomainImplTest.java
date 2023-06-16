package eu.domibus.core.multitenancy;

import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class UserDomainEbms3ServiceSingleDomainImplTest {

    @Tested
    UserDomainServiceSingleDomainImpl userDomainServiceSingleDomainImpl;

    @Test
    public void getDomainForUser() {
        String domainCode = userDomainServiceSingleDomainImpl.getDomainForUser("user1");
        Assertions.assertEquals("default", domainCode);
    }

    @Test
    public void getPreferredDomainForUser() {
        String domainCode = userDomainServiceSingleDomainImpl.getPreferredDomainForUser("user1");
        Assertions.assertEquals("default", domainCode);
    }

}
