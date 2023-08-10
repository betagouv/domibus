package eu.domibus.api.model;

import io.hypersistence.tsid.TSID;
import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.id.Configurable;
import org.hibernate.id.IdentifierGenerator;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;

import java.io.Serializable;
import java.util.Properties;

/**
 * Domibus primary key generator. Generates TSID(Time-Sorted Unique Identifiers (TSID)) primary keys keys.
 *
 * <p>
 * TSID has 2 components:
 * <ul
 * <li>
 * Time component (42 bits). This is the count of milliseconds since 2020-01-01 00:00:00 UTC.
 * </li>
 * <li>
 * Random component (22 bits). The Random component has 2 sub-parts: Node ID (0 to 20 bits) and Counter (2 to 22 bits)
 * </li>
 * </ul>
 * </p>
 * <p>
 * For more details see https://github.com/vladmihalcea/hypersistence-tsid for more details.
 *
 * @author François Gautier
 * @author Cosmin Baciu
 * @since 5.0
 */
public class DatePrefixedGenericSequenceIdGenerator implements IdentifierGenerator, Configurable {

    @Override
    public void configure(Type type, Properties params,
                          ServiceRegistry serviceRegistry) throws MappingException {
        //add initialization logic
    }

    /**
     * Generates a TSID id. Eg: 477188111301737216
     * It creates a TSID factory for each generation to minimize the risk of collisions
     */
    @Override
    public Serializable generate(SharedSessionContractImplementor session, Object object) throws HibernateException {
        TSID.Factory tsidFactory = TSID.Factory.builder()
                .withRandomFunction(TSID.Factory.THREAD_LOCAL_RANDOM_FUNCTION)
                .build();
        final TSID tsid = tsidFactory.generate();
        final long generatedNumber = tsid.toLong();
        return generatedNumber;
    }
}
