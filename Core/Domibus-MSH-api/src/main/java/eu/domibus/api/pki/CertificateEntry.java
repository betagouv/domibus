package eu.domibus.api.pki;

import org.apache.commons.lang3.builder.ToStringBuilder;

import java.security.cert.X509Certificate;

/**
 * @author Ion Perpegel(perpion)
 * @since 4.0
 */
public class CertificateEntry {
    String alias;
    X509Certificate certificate;

    public CertificateEntry() {
    }

    public CertificateEntry(String alias, X509Certificate certificate) {
        this.alias = alias;
        this.certificate = certificate;
    }

    public String getAlias() {
        return this.alias;
    }

    public X509Certificate getCertificate() {
        return this.certificate;
    }

    public void setAlias(String alias) {
        this.alias = alias;
    }

    public void setCertificate(X509Certificate certificate) {
        this.certificate = certificate;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("alias", alias)
                .append("certificate", certificate)
                .toString();
    }
}
