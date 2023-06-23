package eu.domibus.ext.rest;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.ext.domain.PluginUserDTO;
import eu.domibus.ext.exceptions.PluginUserExtServiceException;
import eu.domibus.ext.rest.error.ExtExceptionHelper;
import eu.domibus.ext.services.PluginUserExtService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

@ExtendWith(JMockitExtension.class)
public class PluginUserExtResourceTest {

    @Tested
    PluginUserExtResource pluginUserExtResource;

    @Injectable
    PluginUserExtService pluginUserExtService;

    @Injectable
    ExtExceptionHelper extExceptionHelper;

    @Test
    public void testCreatePluginUser(@Mocked PluginUserDTO pluginUserDTO) {
        String userName = "testUserName";

        new Expectations(){{
            pluginUserDTO.getUserName();
            result = userName;
        }};

        pluginUserExtResource.createPluginUser(pluginUserDTO);

        new FullVerifications(){{
            pluginUserExtService.createPluginUser(pluginUserDTO);
        }};
    }

    @Test
    void testCreatePluginUserErrorHandler(@Mocked PluginUserDTO pluginUserDTO) {
        DomibusCoreException domibusCoreException = new DomibusCoreException("Test error message.");
        new Expectations(){{
            pluginUserExtService.createPluginUser(pluginUserDTO);
            result = domibusCoreException;
        }};

        Assertions.assertThrows(PluginUserExtServiceException. class,() -> pluginUserExtResource.createPluginUser(pluginUserDTO));
    }
}
