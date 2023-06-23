package eu.domibus.web.rest;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.property.DomibusProperty;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.csv.CsvServiceImpl;
import eu.domibus.core.property.DomibusPropertiesFilter;
import eu.domibus.core.property.DomibusPropertyMetadataMapper;
import eu.domibus.core.property.DomibusPropertyResourceHelper;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.ro.DomibusPropertyRO;
import eu.domibus.web.rest.ro.PropertyFilterRequestRO;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Mocked;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ion Perpegel
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class DomibusPropertyResourceTest {

    @Tested
    DomibusPropertyResource domibusPropertyResource;

    @Injectable
    private DomibusPropertyResourceHelper domibusPropertyResourceHelper;

    @Injectable
    private ErrorHandlerService errorHandlerService;

    @Injectable
    private DomainContextProvider domainContextProvider;

    @Injectable
    private DomibusCoreMapper coreMapper;

    @Injectable
    CsvServiceImpl csvService;

    @Injectable
    DomibusPropertyMetadataMapper domibusPropertyMetadataMapper;

    @Test
    public void getProperty(@Mocked DomibusProperty prop, @Mocked DomibusPropertyRO convertedProp) {
        new Expectations() {{
            domibusPropertyResourceHelper.getProperty("propertyName");
            result = prop;
//            coreMapper.propertyApiToPropertyRO(prop);
//            result = convertedProp;
        }};

        DomibusPropertyRO res = domibusPropertyResource.getProperty("propertyName");

        Assertions.assertEquals(convertedProp, res);
    }

    @Test
    public void getCsv(@Injectable DomibusProperty prop, @Injectable DomibusPropertyRO convertedProp,
                       @Injectable DomibusPropertiesFilter filter,
                       @Injectable PropertyFilterRequestRO request) {

        List<DomibusPropertyRO> convertedItems = new ArrayList<>();
        convertedItems.add(convertedProp);
        List<DomibusProperty> items = new ArrayList<>();
        items.add(prop);

        new Expectations() {{
//            coreMapper.domibusPropertyFilterRequestTOdomibusPropertiesFilter(request);
//            result = filter;
            domibusPropertyResourceHelper.getAllProperties(filter);
            result = items;
//            coreMapper.domibusPropertyListToDomibusPropertyROList(items);
//            result = convertedItems;
        }};

        ResponseEntity<String> res = domibusPropertyResource.getCsv(request);

        Assertions.assertEquals(HttpStatus.OK, res.getStatusCode());
    }
}
