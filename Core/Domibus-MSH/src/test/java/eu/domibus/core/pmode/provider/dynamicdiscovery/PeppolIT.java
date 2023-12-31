package eu.domibus.core.pmode.provider.dynamicdiscovery;

import eu.domibus.core.exception.ConfigurationException;
import network.oxalis.vefa.peppol.common.lang.EndpointNotFoundException;
import network.oxalis.vefa.peppol.common.lang.PeppolLoadingException;
import network.oxalis.vefa.peppol.common.lang.PeppolParsingException;
import network.oxalis.vefa.peppol.common.model.*;
import network.oxalis.vefa.peppol.lookup.LookupClient;
import network.oxalis.vefa.peppol.lookup.LookupClientBuilder;
import network.oxalis.vefa.peppol.lookup.api.LookupException;
import network.oxalis.vefa.peppol.lookup.locator.BusdoxLocator;
import network.oxalis.vefa.peppol.mode.Mode;
import network.oxalis.vefa.peppol.security.lang.PeppolSecurityException;
import network.oxalis.vefa.peppol.security.util.EmptyCertificateValidator;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.HttpHost;
import org.apache.http.auth.AuthScope;
import org.apache.http.auth.UsernamePasswordCredentials;
import org.apache.http.client.CredentialsProvider;
import org.apache.http.impl.client.BasicCredentialsProvider;

import javax.naming.InvalidNameException;
import javax.naming.ldap.LdapName;
import javax.naming.ldap.Rdn;
import java.security.cert.X509Certificate;

/**
 * @author idragusa
 * @since 6/13/18.
 */

//@ExtendWith(JMockitExtension.class)
public class PeppolIT {

    //The (sub)domain of the SML, e.g. acc.edelivery.tech.ec.europa.eu
    //private static final String TEST_SML_ZONE = "isaitb.acc.edelivery.tech.ec.europa.eu";
    private static final String TEST_SML_ZONE = "acc.edelivery.tech.ec.europa.eu";

    private Boolean useProxy = false;

    private String httpProxyHost = "";

    private String httpProxyPort = "";

    private String httpProxyUser = "";

    private String httpProxyPassword = "";

    public static void main(String[] args) throws Exception {
        new PeppolIT().testLookupInformation();
    }

    public void testLookupInformation() throws Exception {
        EndpointInfo endpointNew = testLookupInformation("0088:112244", "iso6523-actorid-upis", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-12::Invoice##urn:www.cenbii.eu:transaction:biicoretrdm010:ver1.0:#urn:www.peppol.eu:bis:peppol4a:ver1.0::2.0", "cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0", "");
        EndpointInfo endpointOld = testLookupInformation("0088:112233", "iso6523-actorid-upis", "urn:oasis:names:specification:ubl:schema:xsd:Invoice-12::Invoice##urn:www.cenbii.eu:transaction:biicoretrdm010:ver1.0:#urn:www.peppol.eu:bis:peppol4a:ver1.0::2.0", "cenbii-procid-ubl::urn:www.cenbii.eu:profile:bii04:ver1.0", "");

        System.out.println(endpointOld.getAddress());
        System.out.println(endpointNew.getAddress());

        System.out.println(endpointNew.getCertificate().getIssuerDN().getName());
        System.out.println(extractCommonName(endpointNew.getCertificate()));

        System.out.println(endpointOld.getCertificate().getIssuerDN().getName());
        System.out.println(extractCommonName(endpointOld.getCertificate()));

    }

    private EndpointInfo testLookupInformation(final String participantId, final String participantIdScheme, final String documentId, final String processId, final String processIdScheme) {
        try {
            final LookupClientBuilder lookupClientBuilder = LookupClientBuilder.forMode(Mode.TEST);
            lookupClientBuilder.locator(new BusdoxLocator(TEST_SML_ZONE));
            /* DifiCertificateValidator.validate fails when proxy is enabled */
            if(useProxy) {
                lookupClientBuilder.fetcher(new ApacheFetcherForTest(getConfiguredProxy(), getConfiguredCredentialsProvider()));
                lookupClientBuilder.certificateValidator(EmptyCertificateValidator.INSTANCE);
            } else {
                lookupClientBuilder.fetcher(new ApacheFetcherForTest(null, null));
            }

            final LookupClient smpClient = lookupClientBuilder.build();
            final ParticipantIdentifier participantIdentifier = ParticipantIdentifier.of(participantId, Scheme.of(participantIdScheme));
            final DocumentTypeIdentifier documentIdentifier = DocumentTypeIdentifier.of(documentId);

            final ProcessIdentifier processIdentifier = ProcessIdentifier.parse(processId);
            final ServiceMetadata sm = smpClient.getServiceMetadata(participantIdentifier, documentIdentifier);
            final Endpoint endpoint = sm.getServiceInformation().getEndpoint(processIdentifier, TransportProfile.AS4);

            if (endpoint == null || endpoint.getAddress() == null) {
                throw new ConfigurationException("Could not fetch metadata from SMP for documentId " + documentId + " processId " + processId);
            }
            return new EndpointInfo(endpoint.getAddress().toString(), endpoint.getCertificate());
        } catch (final PeppolParsingException | PeppolLoadingException | PeppolSecurityException | LookupException | EndpointNotFoundException | IllegalStateException e) {
            throw new ConfigurationException("Could not fetch metadata from SMP for documentId " + documentId + " processId " + processId, e);
        }
    }

    private HttpHost getConfiguredProxy() {
        return new HttpHost(httpProxyHost, Integer.parseInt(httpProxyPort));
    }

    private CredentialsProvider getConfiguredCredentialsProvider() {
        CredentialsProvider credsProvider = new BasicCredentialsProvider();
        credsProvider.setCredentials(new AuthScope(httpProxyHost, Integer.parseInt(httpProxyPort)),
                new UsernamePasswordCredentials(httpProxyUser, httpProxyPassword));

        return credsProvider;
    }

    public String extractCommonName(final X509Certificate certificate) throws InvalidNameException {

        final String dn = certificate.getSubjectDN().getName();
        final LdapName ln = new LdapName(dn);
        for (final Rdn rdn : ln.getRdns()) {
            if (StringUtils.equalsIgnoreCase(rdn.getType(), "CN")) {
                return rdn.getValue().toString();
            }
        }
        System.out.println("The certificate does not contain a common name (CN): " + certificate.getSubjectDN().getName());
        return "";
    }
}
