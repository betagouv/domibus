package eu.domibus.common.model.configuration;

import eu.domibus.api.exceptions.DomibusCoreException;
import eu.domibus.api.pmode.PModeValidationException;
import eu.domibus.logging.DomibusLogger;
import eu.domibus.logging.DomibusLoggerFactory;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * @author Razvan Cretu
 * @since 5.1
 */
class ReceptionAwarenessProgressiveTest {

    private static final DomibusLogger LOG = DomibusLoggerFactory.getLogger(ReceptionAwarenessProgressiveTest.class);

    private ReceptionAwareness receptionAwareness;

    private static final List<Integer> NO_RETRY = Collections.emptyList();


    @BeforeEach
    public void setUp() {
        receptionAwareness = new ReceptionAwareness();
    }

    static Stream<Arguments> calculateRetryIntervals_for_progressive() {
        return Stream.of(

                Arguments.of("9;1;2;", Arrays.asList(1, 2, 4, 8)),
                Arguments.of("100;1;3;", Arrays.asList(1, 3, 9, 27, 81)),
                Arguments.of("100;2;3;", Arrays.asList(2, 6, 18, 54)),
                Arguments.of("100;20;3;", Arrays.asList(20, 60)),
                Arguments.of("100;3;2;", Arrays.asList(3, 6, 12, 24, 48, 96)),

                //extreme cases:
                Arguments.of("100000;1;2;", Arrays.asList(1, 2, 4, 8, 16, 32, 64, 128, 256, 512, 1024, 2048, 4096, 8192, 16384, 32768, 65536)),
                Arguments.of("10000000;1;10;", Arrays.asList(1, 10, 100, 1000, 10000, 100000, 1000000, 10000000)),

                // edge cases:
                Arguments.of("8;1;2;", Arrays.asList(1, 2, 4, 8)),
                Arguments.of("2;1;2;", Arrays.asList(1, 2)),
                Arguments.of("1;1;2;", Collections.singletonList(1))
        );
    }

    static Stream<Arguments> calculateRetryIntervals_for_progressive_PModeValidationException() {
        return Stream.of(

                Arguments.of("1;2;2;", NO_RETRY),
                Arguments.of("10;0;2;", NO_RETRY),
                Arguments.of("10;1;0;", NO_RETRY),

                //validation errors
                Arguments.of("100;400;3;", NO_RETRY),
                Arguments.of("10;11;3;", Collections.emptyList()),
                Arguments.of("1;10;1;", NO_RETRY),
                Arguments.of("1;1;1;", NO_RETRY)
        );
    }

    static Stream<Arguments> calculateRetryIntervals_for_progressive_DomibusCoreException() {
        return Stream.of(
                Arguments.of("1.5;1;1;", NO_RETRY),
                Arguments.of("-2;-3;1;", NO_RETRY)
        );
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void calculateRetryIntervals_for_progressive(String progressiveConfig,
                                                 List<Integer> expectedProgressiveIntervals) {

        receptionAwareness.retryXml = progressiveConfig + "PROGRESSIVE";
        receptionAwareness.init(null);

        LOG.info("Progressive retry intervals for ([{}], [{}], [{}]) are: [{}]",
                receptionAwareness.getInitialInterval(),
                receptionAwareness.getMultiplyingFactor(),
                receptionAwareness.getRetryTimeout(),
                expectedProgressiveIntervals);
        assertEquals(expectedProgressiveIntervals, receptionAwareness.getRetryIntervals());
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void calculateRetryIntervals_for_progressive_PModeValidationException(String progressiveConfig) {
        receptionAwareness.retryXml = progressiveConfig + "PROGRESSIVE";

        Assertions.assertThrows(PModeValidationException.class,
                () -> receptionAwareness.init(null));
    }

    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void calculateRetryIntervals_for_progressive_DomibusCoreException(String progressiveConfig) {
        receptionAwareness.retryXml = progressiveConfig + "PROGRESSIVE";

        Assertions.assertThrows(DomibusCoreException.class,
                () -> receptionAwareness.init(null));
    }

}
