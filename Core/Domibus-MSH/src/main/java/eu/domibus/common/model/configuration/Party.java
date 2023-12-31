package eu.domibus.common.model.configuration;

import eu.domibus.api.audit.envers.RevisionLogicalName;
import eu.domibus.api.model.AbstractBaseEntity;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.AuditJoinTable;
import org.hibernate.envers.Audited;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.util.ArrayList;
import java.util.List;

import static org.hibernate.envers.RelationTargetAuditMode.NOT_AUDITED;


/**
 * <p>Java class for anonymous complex type.
 *
 * <p>The following schema fragment specifies the expected content contained within this class.
 *
 * <pre>
 * &lt;complexType&gt;
 *   &lt;complexContent&gt;
 *     &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *       &lt;sequence&gt;
 *         &lt;element name="identifier" maxOccurs="unbounded"&gt;
 *           &lt;complexType&gt;
 *             &lt;complexContent&gt;
 *               &lt;restriction base="{http://www.w3.org/2001/XMLSchema}anyType"&gt;
 *                 &lt;attribute name="partyId" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *                 &lt;attribute name="partyIdType" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *               &lt;/restriction&gt;
 *             &lt;/complexContent&gt;
 *           &lt;/complexType&gt;
 *         &lt;/element&gt;
 *       &lt;/sequence&gt;
 *       &lt;attribute name="name" use="required" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="userName" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="password" type="{http://www.w3.org/2001/XMLSchema}string" /&gt;
 *       &lt;attribute name="endpoint" use="required" type="{http://www.w3.org/2001/XMLSchema}anyURI" /&gt;
 *     &lt;/restriction&gt;
 *   &lt;/complexContent&gt;
 * &lt;/complexType&gt;
 * </pre>
 *
 * @author Christian Koch, Stefan Mueller
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = "identifiers")
@Entity
@Table(name = "TB_PM_PARTY")
@NamedQueries({@NamedQuery(name = "Party.findPartyByIdentifier", query = "select p.name from Party p where :PARTY_IDENTIFIER member of p.identifiers"),
        @NamedQuery(name = "Party.findByName", query = "select p from Party p where p.name = :NAME"),
        @NamedQuery(name = "Party.findAll", query = "select p from Party p"),
        @NamedQuery(name = "Party.findPartyIdentifiersByEndpoint", query = "select p.identifiers from Party p where p.endpoint = :ENDPOINT"),
        @NamedQuery(name = "Party.deleteAll", query = "delete from Party")})
@Audited(withModifiedFlag = true)
@RevisionLogicalName(value = "Party", auditOrder = 1)
public class Party extends AbstractBaseEntity {

    @XmlElement(required = true, name = "identifier")
    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    @JoinColumn(name = "FK_PARTY")
    @Audited(targetAuditMode = NOT_AUDITED)
    @AuditJoinTable(name = "TB_PM_PARTY_IDENTIFIER_AUD")
    protected List<Identifier> identifiers; //NOSONAR
    @XmlAttribute(name = "name", required = true)
    @Column(name = "NAME")
    protected String name;
    @XmlAttribute(name = "userName")
    @Column(name = "USERNAME")
    protected String userName;
    @XmlAttribute(name = "password")
    @Column(name = "PASSWORD")//TODO:HASH!
    protected String password;
    @XmlAttribute(name = "endpoint", required = true)
    @XmlSchemaType(name = "anyURI")
    @Column(name = "ENDPOINT")
    protected String endpoint;

    /**
     * Gets the value of the identifier property.
     *
     * <p>
     * This accessor method returns a reference to the live list,
     * not a snapshot. Therefore any modification you make to the
     * returned list will be present inside the JAXB object.
     * This is why there is not a <CODE>set</CODE> method for the identifier property.
     *
     * <p>
     * For example, to add a new item, do as follows:
     * <pre>
     *    getIdentifier().add(newItem);
     * </pre>
     *
     *
     * <p>
     * Objects of the following type(s) are allowed in the list
     * {@link Identifier }
     *
     * @return a reference to the live list of identifiers
     */
    public List<Identifier> getIdentifiers() {
        if (this.identifiers == null) {
            this.identifiers = new ArrayList<>();
        }
        return this.identifiers;
    }

    public void setIdentifiers(List<Identifier> identifiers) {
        this.identifiers = identifiers;
    }

    /**
     * Gets the value of the name property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getName() {
        return this.name;
    }

    /**
     * Sets the value of the name property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setName(final String value) {
        this.name = value;
    }

    /**
     * Gets the value of the userName property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getUserName() {
        return this.userName;
    }

    /**
     * Sets the value of the userName property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setUserName(final String value) {
        this.userName = value;
    }

    /**
     * Gets the value of the password property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getPassword() {
        return this.password;
    }

    /**
     * Sets the value of the password property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setPassword(final String value) {
        this.password = value;
    }

    /**
     * Gets the value of the endpoint property.
     *
     * @return possible object is
     * {@link String }
     */
    public String getEndpoint() {
        return this.endpoint;
    }

    /**
     * Sets the value of the endpoint property.
     *
     * @param value allowed object is
     *              {@link String }
     */
    public void setEndpoint(final String value) {
        this.endpoint = value;
    }


    public void init(final Configuration configuration) {
        for (final Identifier identifier : this.identifiers) {
            identifier.init(configuration);
        }

    }

    @Override
    public boolean equals(Object otherParty) {
        if (this == otherParty) return true;

        if (otherParty == null || getClass() != otherParty.getClass()) return false;

        if (!super.equals(otherParty)) return false;

        Party party = (Party) otherParty;

        if (!equalIdentifiers(identifiers, party.identifiers)) return false;
        return StringUtils.equalsIgnoreCase(name, party.name);
    }

    protected boolean equalIdentifiers(List<Identifier> identifiers, List<Identifier> identifiers1) {

        if(identifiers == null && identifiers1 == null) {
            return true;
        }

        if(identifiers == null || identifiers1 == null) {
            return false;
        }

        if(identifiers.size() != identifiers1.size()) {
            return false;
        }

        for(Identifier identifier : identifiers) {
            boolean found = false;
            for (Identifier identifier1 : identifiers1) {
                if (identifier.equals(identifier1)) {
                    found = true;
                    break;
                }
            }
            if (!found) {
                return false;
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        if(identifiers != null) {
            result = 31 * result + identifiers.hashCode();
        }
        if(name != null) {
            result = 31 * result + name.hashCode();
        }
        return result;
    }
}
