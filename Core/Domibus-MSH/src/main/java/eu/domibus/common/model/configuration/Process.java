package eu.domibus.common.model.configuration;

import eu.domibus.api.model.AbstractBaseEntity;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;

import javax.persistence.*;
import javax.xml.bind.annotation.*;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import static eu.domibus.common.model.configuration.Process.*;

/**
 * @author Christian Koch, Stefan Mueller
 */
@XmlAccessorType(XmlAccessType.FIELD)
@XmlType(name = "", propOrder = {
        "initiatorPartiesXml",
        "responderPartiesXml",
        "legsXml"
})
@Entity
@Table(name = "TB_PM_PROCESS")
@NamedQueries({
        @NamedQuery(name = RETRIEVE_PULL_PROCESS_FROM_MESSAGE_CONTEXT, query = "SELECT p FROM Process as p left join p.legs as l left join p.initiatorParties init left join p.responderParties resp  where p.mepBinding.value=:mepBinding and l.name=:leg and init.name=:initiatorName and resp.name=:responderName"),
        @NamedQuery(name = FIND_PULL_PROCESS_TO_INITIATE, query = "SELECT p FROM Process as p join p.initiatorParties as resp WHERE p.mepBinding.value=:mepBinding and resp in(:initiator)"),
        @NamedQuery(name = FIND_PULL_PROCESS_FROM_MPC, query = "SELECT p FROM Process as p left join p.legs as l where p.mepBinding.value=:mepBinding and l.defaultMpc.qualifiedName=:mpcName"),
        @NamedQuery(name = FIND_PULL_PROCESS_FROM_LEG_NAME, query = "SELECT p FROM Process as p left join p.legs as l where p.mepBinding.value=:mepBinding and l.name=:legName"),
        @NamedQuery(name = FIND_PROCESS_FROM_LEG_NAME, query = "SELECT p FROM Process as p left join p.legs as l where l.name=:legName"),
        @NamedQuery(name = FIND_ALL_PROCESSES, query = "SELECT p FROM Process p"),
})
public class Process extends AbstractBaseEntity {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(Process.class);
    @Transient
    @XmlTransient
    public static final String RETRIEVE_PULL_PROCESS_FROM_MESSAGE_CONTEXT = "Process.retrievePullProcessFromMessageContext";
    public static final String FIND_PULL_PROCESS_TO_INITIATE = "Process.findPullProcessToInitiate";
    public static final String FIND_PULL_PROCESS_FROM_MPC = "Process.findPullProcessFromMpc";
    public static final String FIND_PULL_PROCESS_FROM_LEG_NAME = "Process.findPullProcessFromLegName";
    public static final String FIND_PROCESS_FROM_LEG_NAME = "Process.findProcessFromLegName";
    public static final String FIND_ALL_PROCESSES = "Process.findAllProcesses";
    @XmlAttribute(name = "name", required = true)
    @Column(name = "NAME")
    protected String name;

    @XmlElement(required = true, name = "initiatorParties")
    @Transient
    protected InitiatorParties initiatorPartiesXml; //NOSONAR
    @XmlElement(required = true, name = "responderParties")
    @Transient
    protected ResponderParties responderPartiesXml; //NOSONAR
    @XmlElement(required = true, name = "legs")
    @Transient
    protected Legs legsXml; //NOSONAR
    @XmlAttribute(name = "initiatorRole", required = true)
    @Transient
    protected String initiatorRoleXml;
    @Transient
    @XmlAttribute(name = "responderRole", required = true)
    protected String responderRoleXml;
    @XmlAttribute(name = "agreement", required = true)
    @Transient
    protected String agreementXml;
    @XmlAttribute(name = "mep", required = true)
    @Transient
    protected String mepXml;
    @XmlAttribute(name = "binding", required = true)
    @Transient
    protected String bindingXml;
    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_PM_JOIN_PROCESS_INIT_PARTY", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "PARTY_FK"))
    protected Set<Party> initiatorParties;
    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_PM_JOIN_PROCESS_RESP_PARTY", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "PARTY_FK"))
    private Set<Party> responderParties;
    @XmlTransient
    @ManyToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinTable(name = "TB_PM_JOIN_PROCESS_LEG", joinColumns = @JoinColumn(name = "PROCESS_FK"), inverseJoinColumns = @JoinColumn(name = "LEG_FK"))
    private Set<LegConfiguration> legs;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_INITIATOR_ROLE")
    private Role initiatorRole;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_RESPONDER_ROLE")
    private Role responderRole;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_AGREEMENT")
    private Agreement agreement;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_MEP")
    private Mep mep;
    @XmlTransient
    @ManyToOne(cascade = CascadeType.ALL)
    @JoinColumn(name = "FK_MEP_BINDING")
    private Binding mepBinding;
    @XmlTransient
    @Column(name = "USE_DYNAMIC_RESPONDER")
    private boolean dynamicResponder;
    @XmlTransient
    @Column(name = "USE_DYNAMIC_INITIATOR")
    private boolean dynamicInitiator;


    public void init(final Configuration configuration) {
        this.initiatorParties = new HashSet<>();
        if (initiatorPartiesXml != null) { // empty means dynamic discovery is used
            for (final InitiatorParty ini : this.initiatorPartiesXml.getInitiatorParty()) {
                for (final Party party : configuration.getBusinessProcesses().getParties()) {
                    if (party.getName().equalsIgnoreCase(ini.getName())) {
                        this.initiatorParties.add(party);
                        break;
                    }
                }
            }
        } else {
            this.dynamicInitiator = true;
        }

        this.responderParties = new HashSet<>();
        if (responderPartiesXml != null) { // empty means dynamic discovery is used
            for (final ResponderParty res : this.responderPartiesXml.getResponderParty()) {
                for (final Party party : configuration.getBusinessProcesses().getParties()) {
                    if (party.getName().equalsIgnoreCase(res.getName())) {
                        this.responderParties.add(party);
                        break;
                    }
                }
            }
        } else {
            this.dynamicResponder = true;
        }

        this.legs = new HashSet<>();
        for (final Leg leg : this.legsXml.getLeg()) {
            for (final LegConfiguration legConfiguration : configuration.getBusinessProcesses().getLegConfigurations()) {
                if (legConfiguration.getName().equalsIgnoreCase(leg.getName())) {
                    this.legs.add(legConfiguration);
                    break;
                }
            }
        }

        for (final Role role : configuration.getBusinessProcesses().getRoles()) {
            if (role.getName().equalsIgnoreCase(this.initiatorRoleXml)) {
                this.initiatorRole = role;
            }
            if (role.getName().equalsIgnoreCase(this.responderRoleXml)) {
                this.responderRole = role;
            }
        }

        for (final Agreement agreement1 : configuration.getBusinessProcesses().getAgreements()) {
            if (agreement1.getName().equalsIgnoreCase(this.agreementXml)) {
                this.agreement = agreement1;
                break;
            }
        }
        for (final Mep mep1 : configuration.getBusinessProcesses().getMeps()) {
            if (mep1.getName().equalsIgnoreCase(this.mepXml)) {
                this.mep = mep1;
                break;
            }
        }
        for (final Binding binding : configuration.getBusinessProcesses().getMepBindings()) {
            if (binding.getName().equalsIgnoreCase(this.bindingXml)) {
                this.mepBinding = binding;
                break;
            }
        }

    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Set<Party> getInitiatorParties() {
        return this.initiatorParties;
    }

    public Set<Party> getResponderParties() {
        return this.responderParties;
    }

    public void setResponderParties(Set<Party> responderParties) {
        this.responderParties = responderParties;
    }

    /**
     * Method changes Set from internal hibernate PersistentSet to HashSet. Due to
     * hibernate session state, the PersistentSet.contain function return false even-thought the object is in a list
     * and values which contributes to hash have the same value. This issue will be tackled in Domibus 5.0
     * in a general manner
     */
    public void detachParties() {

        Set<Party> initiatorPartiesNew = new HashSet<>();
        initiatorPartiesNew.addAll(initiatorParties);
        initiatorParties.clear();
        initiatorParties = initiatorPartiesNew;

        Set<Party> responderPartiesNew = new HashSet<>();
        responderPartiesNew.addAll(responderParties);
        responderParties.clear();
        responderParties = responderPartiesNew;
    }

    @Override
    public boolean equals(final Object o) {
        if (this == o) return true;
        if (!(o instanceof Process)) return false;
        if (!super.equals(o)) return false;

        final Process process = (Process) o;

        return name.equalsIgnoreCase(process.name);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + name.hashCode();
        return result;
    }

    public Set<LegConfiguration> getLegs() {
        return this.legs;
    }

    public Role getInitiatorRole() {
        return this.initiatorRole;
    }

    public Role getResponderRole() {
        return this.responderRole;
    }

    public Agreement getAgreement() {
        return this.agreement;
    }

    public Mep getMep() {
        return this.mep;
    }

    public Binding getMepBinding() {
        return this.mepBinding;
    }

    public boolean isDynamicResponder() {
        return dynamicResponder;
    }

    public boolean isDynamicInitiator() {
        return dynamicInitiator;
    }

    public void addInitiator(Party party) {
        if (this.initiatorParties == null) {
            initiatorParties = new HashSet<>();
            initiatorParties.add(party);
        }
    }

    public void addResponder(Party party) {
        if (this.responderParties == null) {
            responderParties = new HashSet<>();
            responderParties.add(party);
        }
    }

    public Party removeResponder(String partyName) {
        if (CollectionUtils.isEmpty(responderParties)) {
            return null;
        }
        final Iterator<Party> iterator = responderParties.iterator();
        while (iterator.hasNext()) {
            Party party = iterator.next();
            if (StringUtils.equalsIgnoreCase(partyName, party.getName())) {
                LOG.debug("Removing responder [{}] from process [{}]", partyName, name);
                iterator.remove();
                return party;
            }
        }
        return null;
    }

    public InitiatorParties getInitiatorPartiesXml() {
        return this.initiatorPartiesXml;
    }

    public void setInitiatorPartiesXml(InitiatorParties initiatorPartiesXml) {
        this.initiatorPartiesXml = initiatorPartiesXml;
    }

    public ResponderParties getResponderPartiesXml() {
        return this.responderPartiesXml;
    }

    public void setResponderPartiesXml(ResponderParties responderPartiesXml) {
        this.responderPartiesXml = responderPartiesXml;
    }

    public void setLegs(Set<LegConfiguration> legs) {
        this.legs = legs;
    }
}
