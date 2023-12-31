package eu.domibus.core.ebms3.sender.client;

import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.api.property.DomibusConfigurationService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

/**
 * @author Sebastian-Ion TINCU
 */
@ExtendWith(JMockitExtension.class)
public class TLSReaderTest {

    public static final String CONFIG_LOCATION = "configLocation";

    @Injectable
    private Path domainSpecificPath;

    @Injectable
    private Path defaultPath;

    @Injectable
    private DomibusConfigurationService domibusConfigurationService;

    @Injectable
    DomibusLocalCacheService domibusLocalCacheService;

    @Tested
    private TLSReaderServiceImpl tlsReader;

    private String domainCode;

    private Optional<Path> clientAuthenticationPath;

    boolean domainSpecificPathExists, defaultPathExists;

    @BeforeEach
    public void setUp() {
        new Expectations() {{
            domibusConfigurationService.getConfigLocation();
            result = CONFIG_LOCATION;
        }};
        givenPathsMocks();
    }

    @Test
    public void returnsTheClientAuthenticationFromTheDomainSpecificPathIfPresent() {
        givenDomainCode("TAXUD");
        givenDomainSpecificPathFound();

        whenRetrievingTheClientAuthenticationPath();

        Assertions.assertSame(clientAuthenticationPath.get(), domainSpecificPath, "Should have returned the domain specific path if present");
    }

    @Test
    public void returnsTheClientAuthenticationFromTheDefaultPathIfPresentWhenTheDomainSpecificPathDoesNotExist() {
        givenDomainCode("TAXUD");
        givenDomainSpecificPathNotFound();
        givenDefaultPathFound();

        whenRetrievingTheClientAuthenticationPath();

        Assertions.assertSame(clientAuthenticationPath.get(), defaultPath, "Should have returned the default path if present when the domain specific path is missing");
    }

    @Test
    public void returnsNoClientAuthenticationWhenTheDefaultPathAndTheDomainSpecificPathDoNotExist() {
        givenDomainCode("TAXUD");
        givenDomainSpecificPathNotFound();
        givenDefaultPathNotFound();

        whenRetrievingTheClientAuthenticationPath();

        Assertions.assertFalse(clientAuthenticationPath.isPresent(), "Should have returned no path when the domain specific and the default paths are both missing");
    }

    @Test
    public void stripsTheDomainCodeForWhitespacesBeforeLookingUpTheClientAuthenticationFromTheDomainSpecificPath() {
        givenDomainCode("   TAXUD\t ");
        givenDomainSpecificPathFound();
        new MockUp<Paths>() {
            @Mock
            public Path get(String first, String... more) {
                if(!isDomainSpecificScenario(more)) {
                    throw new IllegalArgumentException("The domain code should have been stripped down of whitespace characters");
                }
                return domainSpecificPath;
            }
        };

        whenRetrievingTheClientAuthenticationPath();
    }


    private void givenDomainCode(String domainCode) {
        this.domainCode = domainCode;
    }

    private void givenDomainSpecificPathFound() {
        domainSpecificPathExists = true;
    }

    private void givenDomainSpecificPathNotFound() {
        domainSpecificPathExists = false;
    }

    private void givenDefaultPathFound() {
        defaultPathExists = true;
    }

    private void givenDefaultPathNotFound() {
        defaultPathExists = false;
    }

    // There is no nicer way to mock static methods even in JMockit and new MockUp definitions overwrite previous ones so they need to be defined once per mocked up class
    private void givenPathsMocks() {
        new MockUp<Paths>() {
            @Mock
            public Path get(String first, String... more) {
                return isDomainSpecificScenario(more) ? domainSpecificPath : defaultPath;
            }
        };

        new MockUp<Files>() {
            @Mock
            public boolean exists(Path path, LinkOption... options) {
                if(path == domainSpecificPath) {
                    return domainSpecificPathExists;
                }else if(path == defaultPath) {
                    return defaultPathExists;
                }else {
                    throw new IllegalArgumentException("Should have been invoked with the domain specific path or the default path");
                }
            }
        };
    }

    private boolean isDomainSpecificScenario(String... more) {
        return Arrays.stream(more).anyMatch(el-> el.startsWith(StringUtils.stripToEmpty(domainCode)));
    }

    private void whenRetrievingTheClientAuthenticationPath() {
        clientAuthenticationPath = tlsReader.getClientAuthenticationPath(domainCode);
    }
}
