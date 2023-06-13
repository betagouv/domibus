package eu.domibus.plugin.jms.property;

import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.ext.domain.DomainDTO;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.exceptions.DomibusPropertyExtException;
import eu.domibus.ext.services.DomainExtService;
import eu.domibus.ext.services.DomibusConfigurationExtService;
import eu.domibus.ext.services.DomibusPropertyExtService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.integration.junit4.JMockit;
import org.apache.commons.lang3.StringUtils;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.Map;

import static eu.domibus.plugin.jms.JMSMessageConstants.JMSPLUGIN_DOMAIN_ENABLED;

/**
 * @author Ion Perpegel
 * @author Cosmin Baciu
 * @since 4.1.1
 */
@RunWith(JMockit.class)
public class JmsPluginPropertyManagerTest {

    @Tested
    JmsPluginPropertyManager jmsPluginPropertyManager;

    @Injectable
    protected DomibusPropertyExtService domibusPropertyExtService;

    @Injectable
    protected DomainExtService domainExtService;

    @Injectable
    DomibusConfigurationExtService domibusConfigurationExtService;

    private final String jmsProperty = "jmsplugin.fromPartyId";
    private final String testValue = "new-value";
    private final DomainDTO testDomain = new DomainDTO("default", "default");

    @Test
    public void setKnownPropertyValue() {
        new Expectations() {{
            domibusPropertyExtService.getProperty(jmsProperty);
            returns("old-value", testValue);
        }};

        final String oldValue = jmsPluginPropertyManager.getKnownPropertyValue(jmsProperty);
        jmsPluginPropertyManager.setKnownPropertyValue(jmsProperty, testValue);
        final String newValue = jmsPluginPropertyManager.getKnownPropertyValue(jmsProperty);

        Assert.assertTrue(oldValue != newValue);
        Assert.assertEquals(testValue, newValue);
    }

    @Test
    public void getKnownProperties() {
        Map<String, DomibusPropertyMetadataDTO> properties = jmsPluginPropertyManager.getKnownProperties();
        Assert.assertTrue(properties.containsKey(jmsProperty));
    }

    @Test
    public void hasKnownProperty() {
        boolean hasProperty = jmsPluginPropertyManager.hasKnownProperty(jmsProperty);
        Assert.assertTrue(hasProperty);
    }

    @Test
    public void testUnknownProperty() {
        String unknownPropertyName = "jmsplugin.unknown.property";

        String value = jmsPluginPropertyManager.getKnownPropertyValue(unknownPropertyName);
        Assert.assertTrue(StringUtils.isBlank(value));
    }

}
