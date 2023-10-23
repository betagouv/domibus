package eu.domibus.ext.delegate.services.message;

import eu.domibus.common.Ebms3ErrorExt;
import eu.domibus.core.spi.validation.Ebms3ErrorSpi;
import eu.domibus.core.spi.validation.UserMessageValidatorSpiException;
import eu.domibus.ext.delegate.services.interceptor.ServiceInterceptor;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.DomibusServiceExtException;
import eu.domibus.ext.exceptions.UserMessageExtException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;

@Aspect
@Component
public class UserMessageValidatorServiceInterceptor extends ServiceInterceptor {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(UserMessageValidatorServiceInterceptor.class);

    @Around(value = "execution(public * eu.domibus.ext.delegate.services.message.UserMessageValidatorServiceDelegateImpl.*(..))")
    @Override
    public Object intercept(ProceedingJoinPoint joinPoint) throws Throwable {
        return super.intercept(joinPoint);
    }

    @Override
    public Exception convertCoreException(Exception exception) {
        if (exception instanceof DomibusServiceExtException) {
            return exception;
        }
        if (exception instanceof UserMessageValidatorSpiException) {
            final UserMessageExtException userMessageExtException = new UserMessageExtException(DomibusErrorCode.DOM_005, exception.getMessage(), exception);
            setEbms3ErrorCode((UserMessageValidatorSpiException) exception, userMessageExtException);
            return userMessageExtException;
        }
        return new UserMessageExtException(exception);
    }

    protected void setEbms3ErrorCode(UserMessageValidatorSpiException originalException, UserMessageExtException target) {
        //use the custom error code if it is present
        final Ebms3ErrorSpi ebms3ErrorCode = originalException.getEbms3ErrorCode();
        if (ebms3ErrorCode != null) {
            final Ebms3ErrorExt ebms3ErrorExt = convertFromEbms3ErrorSpi(ebms3ErrorCode);
            target.setEbmsError(ebms3ErrorExt);
        }
    }

    @Override
    public DomibusLogger getLogger() {
        return LOG;
    }

    protected Ebms3ErrorExt convertFromEbms3ErrorSpi(Ebms3ErrorSpi ebms3ErrorCode) {
        Ebms3ErrorExt ebms3ErrorExt = new Ebms3ErrorExt();
        ebms3ErrorExt.setErrorCode(ebms3ErrorCode.getErrorCode());
        ebms3ErrorExt.setErrorDetail(ebms3ErrorCode.getErrorDetail());
        ebms3ErrorExt.setCategory(ebms3ErrorCode.getCategory());
        ebms3ErrorExt.setOrigin(ebms3ErrorCode.getOrigin());
        ebms3ErrorExt.setSeverity(ebms3ErrorCode.getSeverity());
        ebms3ErrorExt.setShortDescription(ebms3ErrorCode.getShortDescription());
        return ebms3ErrorExt;
    }
}
