package eu.domibus.plugin.ws;

import eu.domibus.plugin.ws.backend.WSBackendMessageLogDao;
import eu.domibus.plugin.ws.message.WSMessageLogDao;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * @author François Gautier
 * @since 5.0
 */
@Configuration
public class WSPluginDaoTestConfig {

    @Bean
    public WSBackendMessageLogDao wsBackendMessageLogDao(){
        return new WSBackendMessageLogDao();
    }

    @Bean
    public WSMessageLogDao wsMessageLogDao(){
        return new WSMessageLogDao();
    }

}