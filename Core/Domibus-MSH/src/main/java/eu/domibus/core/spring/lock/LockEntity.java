package eu.domibus.core.spring.lock;

import eu.domibus.api.model.AbstractBaseEntity;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

/**
 * @author Ion Perpegel
 * @since 5.0
 */
@Entity
@Table(name = "TB_LOCK",
        uniqueConstraints = {
                @UniqueConstraint(
                        columnNames = {"LOCK_KEY"},
                        name = "UK_LOCK_KEY"
                )
        }
)
public class LockEntity extends AbstractBaseEntity {

    @NotNull
    @Column(name = "LOCK_KEY")
    private String lockKey;

    public String getLockKey() {
        return lockKey;
    }

    public void setLockKey(String lockKey) {
        this.lockKey = lockKey;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;

        if (o == null || getClass() != o.getClass()) return false;

        LockEntity lock = (LockEntity) o;

        return new EqualsBuilder()
                .appendSuper(super.equals(o))
                .append(lockKey, lock.lockKey)
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(lockKey)
                .toHashCode();
    }
}
