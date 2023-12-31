package eu.domibus.core.security;

import eu.domibus.api.multitenancy.DomainContextProvider;
import eu.domibus.api.multitenancy.UserDomainService;
import eu.domibus.api.security.*;
import eu.domibus.api.util.DatabaseUtil;
import eu.domibus.core.certificate.CertificateServiceImpl;
import eu.domibus.core.user.plugin.AuthenticationDefaultService;
import mockit.Expectations;
import mockit.Injectable;
import mockit.Tested;
import mockit.Verifications;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;

import javax.ws.rs.HttpMethod;
import java.io.UnsupportedEncodingException;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import static eu.domibus.core.certificate.CertificateTestUtils.loadCertificateFromJKSFile;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.x509;

/**
 * @author idragusa
 * @since 4.0
 */
@ExtendWith(JMockitExtension.class)
public class AuthenticationDefaultServiceTest {

    private static final String DOMIBUS_URL = "https://localhost:8080/domibus/services/backend";
    private static final String RESOURCE_PATH = "src/test/resources/eu/domibus/ebms3/common/dao/DynamicDiscoveryPModeProviderTest/";
    private static final String TEST_KEYSTORE = "testkeystore.jks";
    private static final String ALIAS_CN_AVAILABLE = "cn_available";
    private static final String TEST_KEYSTORE_PASSWORD = "1234";

    @Injectable
    private AuthenticationProvider securityCustomAuthenticationProvider;

    private MockHttpServletRequest request;

    @Injectable
    private CertificateServiceImpl certificateService;

    @Injectable
    private AuthUtils authUtils;

    @Injectable
    UserDomainService userDomainService;

    @Injectable
    DomainContextProvider domainContextProvider;

    @Injectable
    DatabaseUtil databaseUtil;

    @Tested
    AuthenticationService authenticationService = new AuthenticationDefaultService();

    @Test
    public void authenticateX509Test() {
        X509Certificate certificate = createCertificate(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        MockHttpServletRequest postProcessedRequest = createHttpRequest(certificate, "https");

        new Expectations() {{
            authUtils.isUnsecureLoginAllowed();
            result = false;
            X509Certificate[] certs = new X509Certificate[1];
            certs[0] = certificate;
            Authentication authentication = new X509CertificateAuthentication(certs);
            authentication.setAuthenticated(true);
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            result = authentication;
        }};

        authenticationService.authenticate(postProcessedRequest);
    }

    @Test
    void authenticateX509MissingTest() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST, DOMIBUS_URL);
        request.setScheme("https");

        Assertions.assertThrows(AuthenticationException. class,() -> authenticationService.authenticate(request));

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 0;
        }};
    }

    @Test
    void authenticateX509InvalidTest() {
        X509Certificate certificate = createCertificate(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);

        new Expectations() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            result = new AuthenticationCredentialsNotFoundException(anyString);
        }};

        MockHttpServletRequest postProcessedRequest = createHttpRequest(certificate, "https");
        Assertions.assertThrows(AuthenticationException. class,() -> authenticationService.authenticate(postProcessedRequest));

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 1;
        }};
    }

    @Test
    public void authenticateDisabledTest() {
        X509Certificate certificate = createCertificate(RESOURCE_PATH + TEST_KEYSTORE, ALIAS_CN_AVAILABLE, TEST_KEYSTORE_PASSWORD);
        MockHttpServletRequest postProcessedRequest = createHttpRequest(certificate, "https");

        new Expectations() {{
            authUtils.isUnsecureLoginAllowed();
            result = true;
        }};

        authenticationService.authenticate(postProcessedRequest);

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 0;
        }};
    }

    @Test
    public void authenticateBasicValidTest() {
        MockHttpServletRequest request = createHttpBasicAuthRequest();

        new Expectations() {{
            Authentication authentication = new BasicAuthentication(AuthRole.ROLE_USER);
            authentication.setAuthenticated(true);
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            result = authentication;
            userDomainService.getDomainForUser(anyString);
            result = "default";
        }};

        authenticationService.authenticate(request);

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 1;
        }};
    }

    @Test
    void authenticateBasicInvalidTest() {
        MockHttpServletRequest request = createHttpBasicAuthRequest();

        new Expectations() {{
            Authentication authentication = new BasicAuthentication(AuthRole.ROLE_USER);
            authentication.setAuthenticated(false);
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            result = authentication;
            userDomainService.getDomainForUser(anyString);
            result = "default";
        }};

        Assertions.assertThrows(AuthenticationException. class,() -> authenticationService.authenticate(request));

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 1;
        }};
    }

    @Test
    public void authenticateBasic_wrongHeaderTest() {
        String headerValueToTest = "Basicx YWRtaW46MTIzNDU2";
        MockHttpServletRequest request = createHttpBasicAuthRequest(headerValueToTest);

        try {
            authenticationService.authenticate(request);
            Assertions.fail("Authorization header [" + headerValueToTest + "] should not pass authentication");
        } catch (AuthenticationException ex) {
            // the exception is expected,
            // but we need to verify the auth provider is not called at all
            new Verifications() {{
                securityCustomAuthenticationProvider.authenticate((Authentication) any);
                times = 0;
            }};
        }
    }

    @Test
    void authenticateBasic_wrongEncodingTest() {
        String headerValueToTest = "Basic aaa";
        MockHttpServletRequest request = createHttpBasicAuthRequest(headerValueToTest);

        Assertions.assertThrows(AuthenticationException. class,() -> authenticationService.authenticate(request));
    }

    @Test
    public void authenticateBLueCoatValidTest() throws UnsupportedEncodingException, ParseException {
        String certHeaderValue = createCertHeaderValue();
        MockHttpServletRequest request = createHttpBlueCoatRequest(certHeaderValue);
        new Expectations() {{
            Authentication authentication = new BlueCoatClientCertificateAuthentication(certHeaderValue);
            authentication.setAuthenticated(true);
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            result = authentication;
        }};

        authenticationService.authenticate(request);

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 1;
        }};
    }

    @Test
    void authenticateEnabledMissingTest() {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST, DOMIBUS_URL);

        new Expectations() {{
            authUtils.isUnsecureLoginAllowed();
            result = false;
        }};

        Assertions.assertThrows(AuthenticationException. class,() -> authenticationService.authenticate(request));

        new Verifications() {{
            securityCustomAuthenticationProvider.authenticate((Authentication) any);
            times = 0;
        }};
    }


    private MockHttpServletRequest createHttpRequest(X509Certificate certificate) {
        return createHttpRequest(certificate, null);

    }

    private MockHttpServletRequest createHttpRequest(X509Certificate certificate, String scheme) {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST, DOMIBUS_URL);
        if (scheme != null) {
            request.setScheme(scheme);
        }

        MockHttpServletRequest postProcessedRequest = x509(certificate).postProcessRequest(request);

        return postProcessedRequest;
    }

    private X509Certificate createCertificate(String keystore, String alias, String password) {
        X509Certificate certificate = loadCertificateFromJKSFile(keystore, alias, password);
        assertNotNull(certificate);
        return certificate;
    }

    private MockHttpServletRequest createHttpBasicAuthRequest() {
        return createHttpBasicAuthRequest("Basic YWRtaW46MTIzNDU2");
    }

    private MockHttpServletRequest createHttpBasicAuthRequest(String basicAuthCredentials) {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST, DOMIBUS_URL);
        request.addHeader(AuthenticationDefaultService.BASIC_AUTH_HEADER_KEY, basicAuthCredentials);
        return request;
    }

    private String createCertHeaderValue() throws ParseException {
        String serial = "123ABCD";
        String issuer = "CN=PEPPOL SERVICE METADATA PUBLISHER TEST CA,OU=FOR TEST PURPOSES ONLY,O=NATIONAL IT AND TELECOM AGENCY,C=DK";
        String subject = "O=DG-DIGIT,CN=SMP_1000000007,C=BE";
        DateFormat df = new SimpleDateFormat("MMM d hh:mm:ss yyyy zzz", Locale.US);
        Date validFrom = df.parse("Jun 01 10:37:53 2015 CEST");
        Date validTo = df.parse("Jun 01 10:37:53 2035 CEST");

        String certHeaderValue = "serial=" + serial + "&subject=" + subject + "&validFrom=" + df.format(validFrom) + "&validTo=" + df.format(validTo) + "&issuer=" + issuer;
        return certHeaderValue;
    }

    private MockHttpServletRequest createHttpBlueCoatRequest(String certHeaderValue) throws ParseException {
        MockHttpServletRequest request = new MockHttpServletRequest(HttpMethod.POST, DOMIBUS_URL);
        request.addHeader(AuthenticationDefaultService.CLIENT_CERT_HEADER_KEY, certHeaderValue);

        return request;
    }
}
