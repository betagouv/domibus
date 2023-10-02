package eu.domibus.api.model;

import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Parameter;

import javax.persistence.*;
import javax.xml.bind.annotation.XmlTransient;


@XmlTransient
@MappedSuperclass
public abstract class AbstractBaseEntity extends AbstractBaseAuditEntity {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(AbstractBaseEntity.class);

    public static final String DOMIBUS_SCALABLE_SEQUENCE="DOMIBUS_SCALABLE_SEQUENCE";
    public static final String DATE_PREFIXED_SEQUENCE_ID_GENERATOR = "eu.domibus.api.model.DatePrefixedGenericSequenceIdGenerator";

    @XmlTransient
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO, generator = DOMIBUS_SCALABLE_SEQUENCE)
    @GenericGenerator(
            name = DOMIBUS_SCALABLE_SEQUENCE,
            strategy = DATE_PREFIXED_SEQUENCE_ID_GENERATOR)
    @Column(name = "ID_PK")
    protected long entityId;


    /**
     * @return the primary key of the entity
     */
    public long getEntityId() {
        return this.entityId;
    }

    public void setEntityId(long entityId) {
        this.entityId = entityId;
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this)
                .append("entityId", entityId)
                .append(super.toString())
                .toString();
    }
}
