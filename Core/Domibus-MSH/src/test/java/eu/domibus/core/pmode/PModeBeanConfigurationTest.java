package eu.domibus.core.pmode;

import mockit.Mocked;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

/**
 * @author Cosmin Baciu
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PModeBeanConfigurationTest {

    @Tested
    PModeBeanConfiguration pModeBeanConfiguration;

    @Mocked
    JAXBContext jaxbContext;

    @Test
    public void jaxbContextConfig() throws JAXBException {

        pModeBeanConfiguration.jaxbContextConfig();

        new Verifications() {{
            JAXBContext.newInstance(PModeBeanConfiguration.COMMON_MODEL_CONFIGURATION_JAXB_CONTEXT_PATH);
            times = 1;
        }};
    }
}
