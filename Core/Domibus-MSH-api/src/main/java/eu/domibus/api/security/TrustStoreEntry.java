package eu.domibus.api.security;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import java.util.Date;

/**
 * @author Thomas Dussart
 * @since 3.3
 */

public class TrustStoreEntry {

    private String name;
    private String subject;
    private String issuer;
    private Date validFrom;
    private Date validUntil;
    private String fingerprints;
    private int certificateExpiryAlertDays;

    public TrustStoreEntry(String name, String subject, String issuer, Date validFrom, Date validUntil) {
        this.name = name;
        this.subject = subject;
        this.issuer = issuer;
        this.validFrom = validFrom;
        this.validUntil = validUntil;
    }

    public TrustStoreEntry() {
    }

    public String getName() {
        return name;
    }

    public String getSubject() {
        return subject;
    }

    public String getIssuer() {
        return issuer;
    }

    public Date getValidFrom() {
        return validFrom;
    }

    public Date getValidUntil() { return validUntil; }

    public String getFingerprints() {
        return fingerprints;
    }

    public void setFingerprints(String fingerprints) {
        this.fingerprints = fingerprints;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }

    public void setValidFrom(Date validFrom) {
        this.validFrom = validFrom;
    }

    public void setValidUntil(Date validUntil) {
        this.validUntil = validUntil;
    }

    public int getCertificateExpiryAlertDays() {
        return certificateExpiryAlertDays;
    }

    public void setCertificateExpiryAlertDays(int certificateExpiryAlertDays) {
        this.certificateExpiryAlertDays = certificateExpiryAlertDays;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof TrustStoreEntry)) return false;

        final TrustStoreEntry entry = (TrustStoreEntry) o;

        return new EqualsBuilder()
                .append(name, entry.name)
                .append(subject, entry.subject)
                .append(issuer, entry.issuer)
                .append(validFrom, entry.validFrom)
                .append(validUntil, entry.validUntil)
                .append(fingerprints, entry.fingerprints)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(name)
                .append(subject)
                .append(issuer)
                .append(validFrom)
                .append(validUntil)
                .append(fingerprints)
                .toHashCode();
    }

    @Override
    public String toString() {
        return "storeEntry{" +
                "name='" + name + '\'' +
                ", subject='" + subject + '\'' +
                ", issuer='" + issuer + '\'' +
                ", validFrom=" + validFrom +
                ", validUntil=" + validUntil +
                ", fingerprints='" + fingerprints + '\'' +
                '}';
    }
}
