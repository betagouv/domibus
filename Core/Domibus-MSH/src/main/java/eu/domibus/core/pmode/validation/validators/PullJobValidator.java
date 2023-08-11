package eu.domibus.core.pmode.validation.validators;

import eu.domibus.api.pmode.ValidationIssue;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.common.model.configuration.Configuration;
import eu.domibus.common.model.configuration.LegConfiguration;
import eu.domibus.core.pmode.validation.PModeValidationHelper;
import eu.domibus.core.pmode.validation.PModeValidator;
import org.springframework.core.annotation.Order;
import org.springframework.scheduling.support.CronExpression;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static eu.domibus.api.property.DomibusPropertyMetadataManagerSPI.DOMIBUS_MSH_PULL_CRON;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * @author François Gautier
 * @since 5.2
 * <p>
 * Pull job might run too late in comparison to the reception awareness of the leg.
 * A warning is created in case the cron interval between two runs of the the pull job is superior to the retryTimout
 * of a pull leg.
 * <p>
 */
@Component
@Order(11)
public class PullJobValidator implements PModeValidator {


    private final PModeValidationHelper pModeValidationHelper;

    protected final DomibusPropertyProvider domibusPropertyProvider;

    public PullJobValidator(PModeValidationHelper pModeValidationHelper, DomibusPropertyProvider domibusPropertyProvider) {
        this.pModeValidationHelper = pModeValidationHelper;
        this.domibusPropertyProvider = domibusPropertyProvider;
    }

    @Override
    public List<ValidationIssue> validate(Configuration configuration) {
        List<ValidationIssue> issues = new ArrayList<>();
        String pullCron = domibusPropertyProvider.getProperty(DOMIBUS_MSH_PULL_CRON);

        if (isBlank(pullCron)) {
            return issues;
        }
        long durationBetweenExecutions = getDurationBetweenExecutions(pullCron);
        configuration.getBusinessProcesses().getProcesses().stream()
                .filter(pModeValidationHelper::isPullProcess)
                .flatMap(process -> process.getLegs().stream())
                .filter(leg -> leg.getReceptionAwareness() != null)
                .filter(leg -> durationBetweenExecutions > leg.getReceptionAwareness().getRetryTimeout())
                .forEach(leg -> issues.add(createWarning(durationBetweenExecutions, leg)));
        return Collections.unmodifiableList(issues);
    }

    protected long getDurationBetweenExecutions(String pullCron) {
        CronExpression cronExpression = CronExpression.parse(pullCron);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime firstRun = getNextRun(cronExpression, now);
        LocalDateTime secondRun = getNextRun(cronExpression, firstRun);
        return Duration.between(firstRun, secondRun).toMinutes();
    }

    protected LocalDateTime getNextRun(CronExpression cronExpression, LocalDateTime from) {
        LocalDateTime firstRun = cronExpression.next(from);
        if (firstRun == null) {
            firstRun = from;
        }
        return firstRun;
    }

    protected ValidationIssue createWarning(long interval, LegConfiguration leg) {
        String message = String.format("Leg [%s] retryTimout [%d min] is inferior to Pull cron job interval [%s]: [%d min]",
                leg.getName(),
                leg.getReceptionAwareness().getRetryTimeout(),
                DOMIBUS_MSH_PULL_CRON,
                interval);
        return new ValidationIssue(message, ValidationIssue.Level.WARNING);
    }
}
