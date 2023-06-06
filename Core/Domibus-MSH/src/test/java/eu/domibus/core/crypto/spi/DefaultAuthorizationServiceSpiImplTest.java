package eu.domibus.core.crypto.spi;

import eu.domibus.api.multitenancy.Domain;
import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.pki.CertificateService;
import eu.domibus.api.pki.MultiDomainCryptoService;
import eu.domibus.api.pki.SecurityProfileService;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.api.security.SecurityProfile;
import eu.domibus.api.util.RegexUtil;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.common.model.configuration.Party;
import eu.domibus.common.model.configuration.Process;
import eu.domibus.common.model.configuration.Security;
import eu.domibus.core.converter.DomibusCoreMapper;
import eu.domibus.core.crypto.spi.model.AuthorizationError;
import eu.domibus.core.crypto.spi.model.AuthorizationException;
import eu.domibus.core.crypto.spi.model.UserMessagePmodeData;
import eu.domibus.core.message.MessageExchangeService;
import eu.domibus.core.message.pull.PullContext;
import eu.domibus.core.pki.PKIUtil;
import eu.domibus.core.pmode.provider.PModeProvider;
import eu.domibus.core.util.RegexUtilImpl;
import eu.domibus.ext.domain.SecurityProfileDTO;
import mockit.*;
import mockit.integration.junit4.JMockit;
import org.hamcrest.CoreMatchers;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.math.BigInteger;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Set;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.*;
import static eu.domibus.core.certificate.CertificateTestUtils.loadCertificateFromJKSFile;
import static eu.domibus.core.pki.PKIUtil.*;
import static org.junit.Assert.*;

/**
 * @author idragusa
 * @since 4.1
 */
@RunWith(JMockit.class)
public class DefaultAuthorizationServiceSpiImplTest {
    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/core/security/";
    private static final String TEST_KEYSTORE = "testauthkeystore.jks";
    private static final String TEST_TRUSTSTORE = "testauthtruststore.jks";
    private static final String ALIAS_CN_AVAILABLE = "blue_gw";
    private static final String ALIAS_TEST_AUTH = "test_auth";
    private static final String TEST_KEYSTORE_PASSWORD = "test123";

    @Tested
    DefaultAuthorizationServiceSpiImpl defaultAuthorizationServiceSpi;

    @Injectable
    MessageExchangeService messageExchangeService;

    @Injectable
    PModeProvider pModeProvider;

    @Injectable
    protected DomibusPropertyProvider domibusPropertyProvider;

    @Injectable
    DomainContextProvider domainProvider;

    @Injectable
    RegexUtil regexUtil;

    @Injectable
    MultiDomainCryptoService multiDomainCryptoService;

    @Injectable
    CertificateService certificateService;

    @Injectable
    SecurityProfileService securityProfileService;

    @Injectable
    DomibusCoreMapper domibusCoreMapper;

    PKIUtil pkiUtil = new PKIUtil();

    @Test
    public void testGetIdentifier() {
        Assert.assertEquals(DefaultAuthorizationServiceSpiImpl.DEFAULT_IAM_AUTHORIZATION_IDENTIFIER, defaultAuthorizationServiceSpi.getIdentifier());
    }

    @Test
    public void testFormatting() {
        X509Certificate certificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        assertNotNull(certificate);

        String excMessage = "Signing certificate and truststore certificate do not match.";
        excMessage += String.format("Truststore certificate: %s", certificate.toString());

        System.out.println(excMessage);

        String excMessage2 = String.format("Sender alias verification failed. Signing certificate CN does not contain the alias (%s): %s ", ALIAS_CN_AVAILABLE, certificate);
        System.out.println(excMessage2);

        String excMessage3 = String.format("Certificate subject [%s] does not match the regular expression configured [%s]", ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        System.out.println(excMessage3);
    }

    @Test
    public void authorizeAgainstCertificateCNMatchTestDisabled() {
        X509Certificate signingCertificate = null;
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = false;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateCNMatch(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstCertificateCNMatchTestNullCert() {
        X509Certificate signingCertificate = null;
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = true;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateCNMatch(signingCertificate, "nobodywho");
    }

    @Test(expected = AuthorizationException.class)
    public void authorizeAgainstCertificateCNMatchTestExc() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = true;
        }};
        try {
            defaultAuthorizationServiceSpi.authorizeAgainstCertificateCNMatch(signingCertificate, "nobodywho");
        } catch (AuthorizationException exc) {
            Assert.assertEquals(AuthorizationError.AUTHORIZATION_REJECTED, exc.getAuthorizationError());
            throw exc;
        }
    }

    @Test
    public void authorizeAgainstCertificateCNMatchTestOk() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = true;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateCNMatch(signingCertificate, ALIAS_CN_AVAILABLE);
    }

    @Test
    public void authorizeAgainstCertificateSubjectExpressionTestOk() {
        RegexUtil regexUtilLocal = new RegexUtilImpl();
        String regExp = ".*TEST.EU.*";
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = regExp;
            regexUtil.matches(regExp, signingCertificate.getSubjectDN().getName());
            result = regexUtilLocal.matches(regExp, signingCertificate.getSubjectDN().getName());
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateSubjectExpression(signingCertificate);
    }


    @Test
    public void authorizeAgainstCertificatePolicyMatchTestDisabled() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            result = null;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstCertificatePolicyMatchTestOK() throws Exception {
        X509Certificate signingCertificate = pkiUtil.createCertificate(BigInteger.ONE, null, Collections.singletonList(CERTIFICATE_POLICY_QCP_LEGAL));
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            result = CERTIFICATE_POLICY_QCP_LEGAL;

            certificateService.getCertificatePolicyIdentifiers((X509Certificate) any);
            result = Collections.singletonList(CERTIFICATE_POLICY_QCP_LEGAL);
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstCertificatePolicyMatchTestOneFromListOK() throws Exception {
        X509Certificate signingCertificate = pkiUtil.createCertificate(BigInteger.ONE, null, Collections.singletonList(CERTIFICATE_POLICY_QCP_LEGAL));
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            // list with spaces
            result = CERTIFICATE_POLICY_QCP_LEGAL+" , " + CERTIFICATE_POLICY_QCP_NATURAL + ", " + CERTIFICATE_POLICY_QCP_LEGAL_QSCD;

            certificateService.getCertificatePolicyIdentifiers((X509Certificate) any);
            result = Collections.singletonList(CERTIFICATE_POLICY_QCP_NATURAL);
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstCertificatePolicyMatchTestMultipletOK() throws Exception {
        X509Certificate signingCertificate = pkiUtil.createCertificate(BigInteger.ONE, null, Collections.singletonList(CERTIFICATE_POLICY_QCP_LEGAL));
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            // list with spaces
            result =CERTIFICATE_POLICY_QCP_LEGAL_QSCD;

            certificateService.getCertificatePolicyIdentifiers((X509Certificate) any);
            result = Arrays.asList(CERTIFICATE_POLICY_QCP_LEGAL_QSCD, CERTIFICATE_POLICY_QCP_LEGAL);
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstCertificatePolicyMatchTestWithEmptyCertificatePolicy() throws Exception {
        X509Certificate signingCertificate = pkiUtil.createCertificate(BigInteger.ONE, null, null);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            result = CERTIFICATE_POLICY_QCP_LEGAL_QSCD;

            certificateService.getCertificatePolicyIdentifiers((X509Certificate) any);
            result = Collections.emptyList();
        }};

        //when
        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () ->   defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho"));

        assertEquals(AuthorizationError.AUTHORIZATION_REJECTED, exception.getAuthorizationError());
        MatcherAssert.assertThat(exception.getMessage(), CoreMatchers.containsString("has empty CertificatePolicy extension"));
    }

    @Test
    public void authorizeAgainstCertificatePolicyMatchTestWithMissMatchCertificatePolicy() throws Exception {
        X509Certificate signingCertificate = pkiUtil.createCertificate(BigInteger.ONE, null, null);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_CERTIFICATE_POLICY_OIDS);
            result = CERTIFICATE_POLICY_QCP_NATURAL_QSCD+","+CERTIFICATE_POLICY_QCP_NATURAL;

            certificateService.getCertificatePolicyIdentifiers((X509Certificate) any);
            result = Arrays.asList(CERTIFICATE_POLICY_QCP_LEGAL_QSCD, CERTIFICATE_POLICY_QCP_LEGAL);
        }};

        //when
        AuthorizationException exception = assertThrows(AuthorizationException.class,
                () ->   defaultAuthorizationServiceSpi.authorizeAgainstCertificatePolicyMatch(signingCertificate, "nobodywho"));

        assertEquals(AuthorizationError.AUTHORIZATION_REJECTED, exception.getAuthorizationError());
        MatcherAssert.assertThat(exception.getMessage(),CoreMatchers.containsString("does not contain any of the required certificate policies"));
    }


    @Test
    public void authorizeAgainstCertificateSubjectExpressionTestNullCert() {
        X509Certificate signingCertificate = null;

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateSubjectExpression(signingCertificate);
        new Verifications() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            times = 0;
        }};

    }

    @Test(expected = AuthorizationException.class)
    public void authorizeAgainstCertificateSubjectExpressionTestException() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = "TEST.EU";
        }};

        try {
            defaultAuthorizationServiceSpi.authorizeAgainstCertificateSubjectExpression(signingCertificate);
        } catch (AuthorizationException exc) {
            Assert.assertEquals(AuthorizationError.AUTHORIZATION_REJECTED, exc.getAuthorizationError());
            throw exc;
        }
    }

    @Test
    public void authorizeAgainstCertificateSubjectExpressionTestDisable() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = "";
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstCertificateSubjectExpression(signingCertificate);
    }

    @Test
    public void authorizeAgainstTruststoreAliasTestDisable() {
        X509Certificate signingCertificate = null;
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = false;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstTruststoreAliasTestNullCert() {
        X509Certificate signingCertificate = null;
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = true;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, "nobodywho");
    }

    @Test
    public void authorizeAgainstTruststoreAliasDynamicDiscovery(@Injectable  X509Certificate signingCertificate) {
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = true;

            domibusPropertyProvider.getBooleanProperty(DOMIBUS_DYNAMICDISCOVERY_USE_DYNAMIC_DISCOVERY);
            result = true;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, "nobodywho");

        new FullVerifications() { };
    }

    @Test
    public void authorizeAgainstTruststoreAliasTestOK(@Mocked Domain domain) throws Exception {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = true;
            multiDomainCryptoService.getCertificateFromTruststore(domain, ALIAS_CN_AVAILABLE);
            result = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_TRUSTSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, ALIAS_CN_AVAILABLE);
    }


    @Test(expected = AuthorizationException.class)
    public void authorizeAgainstTruststoreAliasTestNullTruststore(@Mocked Domain domain) throws Exception {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = true;
            multiDomainCryptoService.getCertificateFromTruststore(domain, ALIAS_CN_AVAILABLE);
            result = null;
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, ALIAS_CN_AVAILABLE);
    }


    @Test(expected = AuthorizationException.class)
    public void authorizeAgainstTruststoreAliasTestNotOK(@Mocked Domain domain) throws Exception {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domainProvider.getCurrentDomain();
            result = domain;
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = true;
            multiDomainCryptoService.getCertificateFromTruststore(domain, ALIAS_CN_AVAILABLE);
            result = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_TRUSTSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        }};

        defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, ALIAS_CN_AVAILABLE);
    }

    @Test
    public void doAuthorizeTest() {
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = false;
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = "";
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = false;
        }};

        defaultAuthorizationServiceSpi.doAuthorize(signingCertificate, ALIAS_TEST_AUTH);

        new Verifications() {{
            defaultAuthorizationServiceSpi.authorizeAgainstTruststoreAlias(signingCertificate, ALIAS_TEST_AUTH);
            times = 1;
            defaultAuthorizationServiceSpi.authorizeAgainstCertificateSubjectExpression(signingCertificate);
            times = 1;
            defaultAuthorizationServiceSpi.authorizeAgainstCertificateCNMatch(signingCertificate, ALIAS_TEST_AUTH);
            times = 1;
        }};
    }

    @Test
    public void authorizeUserMessageTest() {
        UserMessagePmodeData userMessagePmodeData = new UserMessagePmodeData("service", "action", ALIAS_TEST_AUTH);
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = false;
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = "";
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = false;
        }};

        defaultAuthorizationServiceSpi.authorize(null, signingCertificate, null, SecurityProfileDTO.RSA, userMessagePmodeData);

        new Verifications() {{
            defaultAuthorizationServiceSpi.doAuthorize(signingCertificate, ALIAS_TEST_AUTH);
            times = 1;
        }};
    }

    @Test
    public void authorizePullTest() throws Exception {
        final String testMpc = "mpc_for_test";
        String testQualifiedMpc = "qualified_mpc_for_test";
        PullRequestPmodeData pullRequestPmodeData = new PullRequestPmodeData(testMpc);
        X509Certificate signingCertificate = loadCertificateFromJKSFile(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_TEST_AUTH, TEST_KEYSTORE_PASSWORD);
        Process process = new Process();
        Party party = new Party();
        party.setName("initiator");
        process.addInitiator(party);
        PullContext pullContext = new PullContext(process, new Party(), testQualifiedMpc);
        LegConfiguration legConfiguration = new LegConfiguration();
        legConfiguration.setName("myLegConfiguration");
        Security security = new Security();
        security.setProfile(SecurityProfile.RSA);
        legConfiguration.setSecurity(security);
        Set<LegConfiguration> legConfigurations = Collections.singleton(legConfiguration);
        Set<Party> parties = Collections.singleton(new Party());

        new Expectations() {{
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_TRUST_VALIDATION_TRUSTSTORE_ALIAS);
            result = false;
            domibusPropertyProvider.getProperty(DOMIBUS_SENDER_TRUST_VALIDATION_EXPRESSION);
            result = "";
            domibusPropertyProvider.getBooleanProperty(DOMIBUS_SENDER_CERTIFICATE_SUBJECT_CHECK);
            result = false;
            pModeProvider.findMpcUri(testMpc);
            result = testQualifiedMpc;
            messageExchangeService.extractProcessOnMpc(testQualifiedMpc);
            result = pullContext;
            messageExchangeService.extractProcessOnMpc(testQualifiedMpc).getProcess().getInitiatorParties();
            result = parties;
            messageExchangeService.extractProcessOnMpc(testQualifiedMpc).getProcess().getLegs();
            result = legConfigurations;
            legConfigurations.iterator().next().getSecurity().getProfile();
            result = SecurityProfile.RSA;
        }};

        defaultAuthorizationServiceSpi.authorize(null, signingCertificate, null, pullRequestPmodeData);

        new Verifications() {{
            defaultAuthorizationServiceSpi.doAuthorize(signingCertificate, ALIAS_TEST_AUTH);
            times = 1;
        }};
    }

    @Test(expected = AuthorizationException.class)
    public void authorizePullTestInitiatorException() throws Exception {
        final String testMpc = "mpc_for_test";
        String testQualifiedMpc = "qualified_mpc_for_test";
        PullRequestPmodeData pullRequestPmodeData = new PullRequestPmodeData(testMpc);
        Process process = new Process();
        PullContext pullContext = new PullContext(process, new Party(), testQualifiedMpc);
        new Expectations() {{
            pModeProvider.findMpcUri(testMpc);
            result = testQualifiedMpc;
            messageExchangeService.extractProcessOnMpc(testQualifiedMpc);
            result = pullContext;
        }};

        defaultAuthorizationServiceSpi.authorize(null, null, null, pullRequestPmodeData);

        new Verifications() {{
            defaultAuthorizationServiceSpi.doAuthorize(null, ALIAS_TEST_AUTH);
            times = 1;
        }};
    }

    @Test(expected = AuthorizationException.class)
    public void authorizePullTestPullContextException() throws Exception {
        final String testMpc = "mpc_for_test";
        String testQualifiedMpc = "qualified_mpc_for_test";
        PullRequestPmodeData pullRequestPmodeData = new PullRequestPmodeData(testMpc);
        new Expectations() {{
            pModeProvider.findMpcUri(testMpc);
            result = testQualifiedMpc;
            messageExchangeService.extractProcessOnMpc(testQualifiedMpc);
            result = null;
        }};

        defaultAuthorizationServiceSpi.authorize(null, null, null, pullRequestPmodeData);

        new Verifications() {{
            defaultAuthorizationServiceSpi.doAuthorize(null, ALIAS_TEST_AUTH);
            times = 1;
        }};
    }

    @Test(expected = AuthorizationException.class)
    public void authorizePullTestNullMpc() {
        PullRequestPmodeData pullRequestPmodeData = new PullRequestPmodeData(null);
        defaultAuthorizationServiceSpi.authorize(null, null, null, pullRequestPmodeData);
    }

}
