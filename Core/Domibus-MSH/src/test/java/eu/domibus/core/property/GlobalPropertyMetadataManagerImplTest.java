package eu.domibus.core.property;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.property.DomibusPropertyException;
import eu.domibus.api.property.DomibusPropertyMetadata;
import eu.domibus.api.property.DomibusPropertyMetadataManagerSPI;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.ext.domain.DomibusPropertyMetadataDTO;
import eu.domibus.ext.services.DomibusPropertyManagerExt;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Spy;

import java.util.*;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;

@ExtendWith(JMockitExtension.class)
@Disabled("EDELIVERY-6896")
public class GlobalPropertyMetadataManagerImplTest {
    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(GlobalPropertyMetadataManagerImplTest.class);

    @Tested
    GlobalPropertyMetadataManagerImpl globalPropertyMetadataManager;

    @Injectable
    List<DomibusPropertyMetadataManagerSPI> propertyMetadataManagers;

    @Injectable
    DomibusCoreMapper coreMapper;

    @Injectable
    private List<DomibusPropertyManagerExt> extPropertyManagers;

    @Injectable
    private NestedPropertiesManager nestedPropertiesManager;

    @Mocked
    @Spy
    private DomibusPropertyManagerExt propertyManager1;

    @Mocked
    private DomibusPropertyManagerExt propertyManager2;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    protected DomibusPropertyMetadataMapper domibusPropertyMetadataMapper;

    Map<String, DomibusPropertyMetadataDTO> props1;
    Map<String, DomibusPropertyMetadata> props2;
    String domainCode = "domain1";
    Domain domain = new Domain(domainCode, "DomainName1");

    @BeforeEach
    public void setUp() {
        props1 = Arrays.stream(new DomibusPropertyMetadataDTO[]{
                new DomibusPropertyMetadataDTO(DOMIBUS_UI_TITLE_NAME, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
                new DomibusPropertyMetadataDTO(DOMIBUS_SEND_MESSAGE_MESSAGE_ID_PATTERN, DomibusPropertyMetadataDTO.Usage.DOMAIN, false),
                new DomibusPropertyMetadataDTO(DOMIBUS_PLUGIN_PASSWORD_POLICY_PATTERN, DomibusPropertyMetadataDTO.Usage.DOMAIN, true),
        }).collect(Collectors.toMap(x -> x.getName(), x -> x));

        props2 = Arrays.stream(new DomibusPropertyMetadata[]{
                new DomibusPropertyMetadata(DOMIBUS_UI_SUPPORT_TEAM_NAME, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PLUGIN_PASSWORD_POLICY_VALIDATION_MESSAGE, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DEFAULT_PASSWORD_EXPIRATION, DomibusPropertyMetadata.Usage.DOMAIN, true),
                new DomibusPropertyMetadata(DOMIBUS_PASSWORD_POLICY_PLUGIN_DONT_REUSE_LAST, DomibusPropertyMetadata.Usage.DOMAIN, true),
        }).collect(Collectors.toMap(x -> x.getName(), x -> x));

        extPropertyManagers = Arrays.asList(propertyManager1, propertyManager2);
    }

    @Test
    public void getPropertyMetadataNotNullPropertyTest(@Injectable DomibusPropertyMetadata prop,
                                                       @Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                                       @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;
        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);
            allPropertyMetadataMap.get(propertyName);
            result = prop;
        }};
        globalPropertyMetadataManager.getPropertyMetadata(propertyName);
        new Verifications() {{
            allPropertyMetadataMap.get(propertyName);
            times = 1;
            allPropertyMetadataMap.values().stream().filter(p -> p.isComposable() && propertyName.startsWith(p.getName())).findAny();
            times = 0;
        }};
    }

    @Test
    public void getPropertyMetadataWithComposeablePropertyTest(@Injectable DomibusPropertyMetadata propMeta,
                                                               @Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                                               @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);
            times = 1;
            allPropertyMetadataMap.get(anyString);
            result = null;
            globalPropertyMetadataManager.getComposablePropertyMetadata(allPropertyMetadataMap, propertyName);
            result = propMeta;
//            coreMapper.clonePropertyMetadata(propMeta);
//            result = propMeta;
        }};
        DomibusPropertyMetadata meta = globalPropertyMetadataManager.getPropertyMetadata(propertyName);
        Assertions.assertNotNull(meta);
        new Verifications() {{
            allPropertyMetadataMap.get(anyString);
            times = 1;
            propMeta.setName(propertyName);
            times = 1;
        }};
    }

    @Test
    public void initializeIfNeededTest_loadExternalProps(@Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                                         @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadInternalProperties();
            times = 0;
        }};

        globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);

        new Verifications() {{
            globalPropertyMetadataManager.loadExternalProperties();
            times = 1;
        }};
    }

    @Test
    public void initializeIfNeededTest_loadInternalProps(@Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadInternalProperties();
            globalPropertyMetadataManager.hasProperty(allPropertyMetadataMap, propertyName);
            result = true;
        }};

        globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);

        new Verifications() {{
            globalPropertyMetadataManager.loadInternalProperties();
            times = 1;
            globalPropertyMetadataManager.loadExternalPropertiesIfNeeded();
            times = 0;
        }};
    }

    @Test
    public void initializeIfNeededNullPropertyMetadataMapTest(@Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                                              @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;
        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.hasProperty(allPropertyMetadataMap, propertyName);
            result = false;
        }};
        globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);
        new Verifications() {{
            globalPropertyMetadataManager.loadExternalProperties();
            times = 1;
        }};
    }

    @Test
    public void getManagerForProperty_internal(@Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                               @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);

            globalPropertyMetadataManager.hasProperty(internalPropertyMetadataMap, propertyName);
            result = true;
        }};

        DomibusPropertyManagerExt manager = globalPropertyMetadataManager.getManagerForProperty(propertyName);

        Assertions.assertEquals(null, manager);
    }

    @Test
    public void getManagerForProperty_external(@Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                               @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);

            globalPropertyMetadataManager.hasProperty(internalPropertyMetadataMap, propertyName);
            result = false;
            propertyManager1.hasKnownProperty(propertyName);
            result = false;
            propertyManager2.hasKnownProperty(propertyName);
            result = true;
        }};

        DomibusPropertyManagerExt manager = globalPropertyMetadataManager.getManagerForProperty(propertyName);

        Assertions.assertEquals(propertyManager2, manager);
    }

    @Test
    void getManagerForProperty_not_found(@Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap,
                                         @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap) {
        String propertyName = DOMIBUS_UI_TITLE_NAME;
        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.loadPropertiesIfNotFound(propertyName);

            globalPropertyMetadataManager.hasProperty(internalPropertyMetadataMap, propertyName);
            result = false;
            propertyManager1.hasKnownProperty(propertyName);
            result = false;
            propertyManager2.hasKnownProperty(propertyName);
            result = false;
        }};

        Assertions.assertThrows(DomibusPropertyException.class, () -> globalPropertyMetadataManager.getManagerForProperty(propertyName));
    }

    @Test
    public void loadExternalPropertiesIfNeeded() {
        globalPropertyMetadataManager.loadExternalPropertiesIfNeeded();
        globalPropertyMetadataManager.loadExternalPropertiesIfNeeded();
        new Verifications() {{
            globalPropertyMetadataManager.loadExternalProperties();
            times = 1;
        }};
    }

    @Test
    public void loadExternalPropertiesTest(@Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap,
                                           @Injectable DomibusPropertyMetadata propMeta) {
        new Expectations(globalPropertyMetadataManager) {{
            propertyManager1.getKnownProperties();
            result = props1;
//            coreMapper.propertyMetadataDTOTopropertyMetadata((DomibusPropertyMetadataDTO) any);
//            result = propMeta;
        }};

        globalPropertyMetadataManager.loadExternalProperties(propertyManager1);

        new Verifications() {{
//            coreMapper.propertyMetadataDTOTopropertyMetadata((DomibusPropertyMetadataDTO) any);
//            times = props1.size();
            allPropertyMetadataMap.put(anyString, propMeta);
            times = props1.size();
        }};
    }

    @Test
    public void loadPropertiesTest(@Mocked DomibusPropertyMetadataManagerSPI propertyManager,
                                   @Injectable Map<String, DomibusPropertyMetadata> allPropertyMetadataMap,
                                   @Injectable Map<String, DomibusPropertyMetadata> internalPropertyMetadataMap) {
        new Expectations(globalPropertyMetadataManager) {{
            propertyManager.getKnownProperties();
            result = props2;
        }};

        globalPropertyMetadataManager.loadProperties(propertyManager, "tomcatManager");

        new Verifications() {{
            allPropertyMetadataMap.put(anyString, (DomibusPropertyMetadata) any);
            times = props2.size();
            internalPropertyMetadataMap.put(anyString, (DomibusPropertyMetadata) any);
            times = props2.size();
        }};
    }

    @Test
    public void testSynchronizedBlocksWhenAddingPropertiesOnTheFly() {
        new Expectations(globalPropertyMetadataManager) {{
//            coreMapper.clonePropertyMetadata((DomibusPropertyMetadata)any);
//            result = DomibusPropertyMetadata.getReadOnlyGlobalProperty("dummy");
        }};

        // When multiple properties are added to the properties map at the same time,
        // concurrent access to the map may result in ConcurrentModificationException.
        // This test verifies that this situation does not occur (anymore).

        ExecutorService ex = Executors.newFixedThreadPool(3);

        Map<String, Future<DomibusPropertyMetadata>> futures = new HashMap();
        for (int i = 0; i < 100; i++) {
            String newPropertyName = "propertyName" + new Random().nextInt();
            Future<DomibusPropertyMetadata> get = ex.submit(() -> globalPropertyMetadataManager.getPropertyMetadata(newPropertyName));
            futures.put(newPropertyName, get);
        }
        futures.forEach((newPropertyName, future) -> {
            try {
                DomibusPropertyMetadata metadata = future.get();
                Assertions.assertNotNull(metadata);
            } catch (InterruptedException e) {
                LOG.debug("Interrupted", e);
            } catch (ExecutionException e) {
                LOG.error("Unexpected error", e);
                Assertions.fail(e.getClass().getSimpleName() + " caught");
            }
        });
    }

    @Test
    public void hasComposableProperty(@Injectable DomibusPropertyMetadata propMeta,
                                      @Injectable Map<String, DomibusPropertyMetadata> map) {
        String propertyName = "domibus.composable.property.suffix1";
        String compPropertyName = "domibus.composable.property";

        List<String> nestedProps = Arrays.asList("suffix1");

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.getComposablePropertyMetadata(map, propertyName);
            result = propMeta;
            propMeta.getName();
            result = compPropertyName;
            nestedPropertiesManager.getNestedProperties(propMeta);
            result = nestedProps;
        }};

        boolean result = globalPropertyMetadataManager.hasComposableProperty(map, propertyName);
        Assertions.assertTrue(result);
    }

    @Test
    public void hasComposablePropertyNegative(@Injectable DomibusPropertyMetadata propMeta,
                                              @Injectable Map<String, DomibusPropertyMetadata> map) {
        String propertyName = "domibus.composable.property.suffix1";

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.getComposablePropertyMetadata(map, propertyName);
            result = null;
        }};

        boolean result = globalPropertyMetadataManager.hasComposableProperty(map, propertyName);
        Assertions.assertEquals(false, result);
    }

    @Test
    public void hasComposablePropertyNegative2(@Injectable DomibusPropertyMetadata propMeta,
                                               @Injectable Map<String, DomibusPropertyMetadata> map) {
        String propertyName = "domibus.composable.property.suffix1";
        String compPropertyName = "domibus.composable.property";

        List<String> nestedProps = Arrays.asList();

        new Expectations(globalPropertyMetadataManager) {{
            globalPropertyMetadataManager.getComposablePropertyMetadata(map, propertyName);
            result = propMeta;
            propMeta.getName();
            result = compPropertyName;
            nestedPropertiesManager.getNestedProperties(propMeta);
            result = nestedProps;
        }};

        boolean result = globalPropertyMetadataManager.hasComposableProperty(map, propertyName);
        Assertions.assertFalse(result);
    }
}
