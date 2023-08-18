package eu.domibus.core.pmode.validation;

import eu.domibus.api.ebms3.MessageExchangePattern;
import eu.domibus.common.model.configuration.Binding;
import eu.domibus.common.model.configuration.Process;
import mockit.Expectations;
import mockit.Injectable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SuppressWarnings("ResultOfMethodCallIgnored")
class PModeValidationHelperImplTest {

    private PModeValidationHelperImpl pModeValidationHelper;

    @Injectable
    Process process;
    @Injectable
    Binding binding;

    @BeforeEach
    void setUp() {
        pModeValidationHelper = new PModeValidationHelperImpl();
    }

    @ParameterizedTest(name = "[{index}] ONE_WAY_PULL")
    @EnumSource(
            value = MessageExchangePattern.class,
            names = {"ONE_WAY_PULL"})
    void processIsPull_true() {
        new Expectations(){{
            process.getMepBinding();
            result = binding;

            binding.getValue();
            result = MessageExchangePattern.ONE_WAY_PULL.getUri();
        }};
        assertTrue(pModeValidationHelper.isPullProcess(process));
    }

    @ParameterizedTest(name = "[{index}] not ONE_WAY_PULL")
    @EnumSource(
            value = MessageExchangePattern.class,
            names = {"ONE_WAY_PULL"},
            mode = EnumSource.Mode.EXCLUDE)
    void processIsPull_false() {
        new Expectations(){{
            process.getMepBinding();
            result = binding;

            binding.getValue();
            result = MessageExchangePattern.ONE_WAY_PUSH.getUri();
        }};

        assertFalse(pModeValidationHelper.isPullProcess(process));
    }

    @Test
    void processIsPull_null() {
        new Expectations(){{
            process.getMepBinding();
            result = null;
        }};
        assertFalse(pModeValidationHelper.isPullProcess(process));
    }
}