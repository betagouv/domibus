package eu.domibus.core.pmode.validation;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.DomainService;
import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.property.DomibusConfigurationService;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.property.encryption.PasswordEncryptionService;
import eu.domibus.api.util.ClassUtil;
import eu.domibus.api.util.xml.UnmarshallerResult;
import eu.domibus.api.util.xml.XMLUtil;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.api.cache.DomibusLocalCacheService;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.multitenancy.DomainContextProviderImpl;
import eu.domibus.core.multitenancy.DomainServiceImpl;
import eu.domibus.core.multitenancy.dao.DomainDao;
import eu.domibus.core.multitenancy.dao.DomainDaoImpl;
import eu.domibus.core.pmode.PModeBeanConfiguration;
import eu.domibus.core.pmode.validation.validators.OneWayMepValidator;
import eu.domibus.core.pmode.validation.validators.TwoWayMepValidator;
import eu.domibus.core.property.*;
import eu.domibus.core.property.encryption.PasswordEncryptionContextFactory;
import eu.domibus.core.util.xml.XMLUtilImpl;
import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.AnnotationConfigContextLoader;
import org.xml.sax.SAXException;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.stream.XMLStreamException;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class)
public class PModeValidationServiceImplIT {


    @org.springframework.context.annotation.Configuration
    public static class ContextConfiguration {
        @Bean
        public XMLUtil xmlUtil() {
            return new XMLUtilImpl(domibusPropertyProvider());
        }

        @Bean
        public PModeValidationService pModeValidationService() {
            return new PModeValidationServiceImpl();
        }

        @Bean
        public DomibusPropertyProvider domibusPropertyProvider() {
            return Mockito.mock(DomibusPropertyProviderImpl.class);
        }

        @Bean
        public DomibusRawPropertyProvider domibusRawPropertyProvider() {
            return Mockito.mock(DomibusRawPropertyProvider.class);
        }

        @Bean
        public GlobalPropertyMetadataManager domibusPropertyMetadataManager() {
            return Mockito.mock(GlobalPropertyMetadataManagerImpl.class);
        }

        @Bean
        public NestedPropertiesManager domibusNestedPropertiesManager() {
            return Mockito.mock(NestedPropertiesManager.class);
        }

        @Bean
        public PropertyProviderHelper domibusPropertyProviderHelper() {
            return Mockito.mock(PropertyProviderHelper.class);
        }

        @Bean(name = "domibusDefaultProperties")
        public Properties domibusDefaultProperties() {
            return Mockito.mock(Properties.class);
        }

        @Bean(name = "domibusProperties")
        public Properties domibusProperties() {
            return Mockito.mock(Properties.class);
        }

        @Bean
        public DomainContextProvider domainContextProvider() {
            return Mockito.mock(DomainContextProviderImpl.class);
        }

        @Bean
        public DomainService domainService() {
            return Mockito.mock(DomainServiceImpl.class);
        }

        @Bean
        public DomainDao domainDao() {
            return Mockito.mock(DomainDaoImpl.class);
        }

        @Bean
        public DomibusConfigurationService domibusConfigurationService() {
            return Mockito.mock(DefaultDomibusConfigurationService.class);
        }

        @Bean
        public PasswordEncryptionService passwordEncryptionService() {
            return Mockito.mock(PasswordEncryptionService.class);
        }

        @Bean
        public PasswordEncryptionContextFactory passwordEncryptionContextFactory() {
            return Mockito.mock(PasswordEncryptionContextFactory.class);
        }

        @Bean
        public JAXBContext jaxbContextConfig() throws JAXBException {
            return JAXBContext.newInstance(PModeBeanConfiguration.COMMON_MODEL_CONFIGURATION_JAXB_CONTEXT_PATH);
        }

        @Bean
        public List<PModeValidator> pModeValidatorList() {
            return Arrays.asList(new TwoWayMepValidator(), new OneWayMepValidator());
        }

        @Bean
        public List<DomibusPropertyMetadataManagerSPI> propertyMetadataManagers() {
            return Arrays.asList(Mockito.mock(DomibusPropertyMetadataManagerSPI.class));
        }

        @Bean
        public DomibusCoreMapper coreMapper() {
            return Mockito.mock(DomibusCoreMapper.class);
        }

        @Bean
        public PropertyProviderDispatcher domibusPropertyProviderDispatcher() {
            return Mockito.mock(PropertyProviderDispatcher.class);
        }

        @Bean
        public ClassUtil classUtil() {
            return Mockito.mock(ClassUtil.class);
        }

        @Bean
        public PropertyChangeManager domibusPropertyChangeManager() {
            return Mockito.mock(PropertyChangeManager.class);
        }

        @Bean
        public PrimitivePropertyTypesManager primitivePropertyTypesManager() {
            return Mockito.mock(PrimitivePropertyTypesManager.class);
        }

        @Bean
        public DomibusLocalCacheService domibusCacheService() {
            return Mockito.mock(DomibusLocalCacheService.class);
        }

    }

    @Autowired
    PModeValidationService pModeValidationService;

    @Autowired
    XMLUtil xmlUtil;

    @Autowired
    JAXBContext jaxbContext;

    @Autowired
    DomibusPropertyProvider domibusPropertyProvider;

    @Autowired
    DomibusRawPropertyProvider domibusRawPropertyProvider;

    @Test
    public void validate() throws IOException, SAXException, XMLStreamException, ParserConfigurationException, JAXBException {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("samplePModes/domibus-pmode-validation-tests.xml");
        byte[] pModeBytes = IOUtils.toByteArray(xmlStream);
        UnmarshallerResult unmarshallerResult = xmlUtil.unmarshal(true, jaxbContext, new ByteArrayInputStream(pModeBytes), null);
        Configuration configuration = unmarshallerResult.getResult();

        List<ValidationIssue> issues = pModeValidationService.validate(configuration);

        Assertions.assertFalse(issues.isEmpty());
    }
}
