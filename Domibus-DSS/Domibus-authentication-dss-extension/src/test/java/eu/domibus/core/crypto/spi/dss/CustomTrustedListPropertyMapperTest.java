package eu.domibus.core.crypto.spi.dss;

import eu.domibus.ext.services.DomibusPropertyExtService;
import eu.europa.esig.dss.tsl.source.TLSource;
import mockit.Expectations;
import mockit.Injectable;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static eu.domibus.core.crypto.spi.dss.DssExtensionPropertyManager.*;

/**
 * @author Thomas Dussart
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
class CustomTrustedListPropertyMapperTest {

    @Test
    void map(@Injectable DomibusPropertyExtService domibusPropertyExtService) {
        String list1 = "list1";
        String list2 = "list2";
        List<String> customListSuffixes = new ArrayList<>(Arrays.asList(list1, list2));
        List<String> customTrustedListProperties = new ArrayList<>(Arrays.asList("url", "code"));
        String keystorePath = getClass().getClassLoader().getResource("gateway_keystore.jks").getPath();
        String keystoreType = "JKS";
        String keystorePasswd = "test123";
        String customList1Url = "firstUrl";
        String customList1Code = "CX";
        String customList2Url = "secondUrl";
        String customList2Code = "CUST";
        CustomTrustedListPropertyMapper customTrustedListPropertyMapper = new CustomTrustedListPropertyMapper(domibusPropertyExtService);
        new Expectations() {{

            domibusPropertyExtService.getNestedProperties(CUSTOM_TRUSTED_LISTS_PREFIX);
            result = customListSuffixes;

            domibusPropertyExtService.getNestedProperties(CUSTOM_TRUSTED_LISTS_PREFIX + "." + list1);
            result = customTrustedListProperties;

            domibusPropertyExtService.getNestedProperties(CUSTOM_TRUSTED_LISTS_PREFIX + "." + list2);
            result = customTrustedListProperties;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_KEYSTORE_TYPE);
            result = keystoreType;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_KEYSTORE_PATH);

            this.result = keystorePath;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_KEYSTORE_PASSWORD);
            this.result = keystorePasswd;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_1_URL);
            this.result = customList1Url;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_1_CODE);
            this.result = customList1Code;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_2_URL);
            this.result = customList2Url;

            domibusPropertyExtService.getProperty(DSS_CUSTOM_TRUSTED_LIST_2_CODE);
            this.result = customList2Code;

        }};
        List<TLSource> otherTrustedLists = customTrustedListPropertyMapper.map();
        Assertions.assertEquals(2, otherTrustedLists.size());
        TLSource otherTrustedList = otherTrustedLists.get(0);
        Assertions.assertEquals(customList1Url, otherTrustedList.getUrl());

        otherTrustedList = otherTrustedLists.get(1);
        Assertions.assertEquals(customList2Url, otherTrustedList.getUrl());
    }
}
