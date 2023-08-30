
package eu.domibus.api.model;

import org.hibernate.HibernateException;
import org.hibernate.MappingException;
import org.hibernate.boot.model.relational.Database;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.service.ServiceRegistry;
import org.hibernate.type.Type;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.ArgumentMatchers;
import org.mockito.Mockito;

import java.io.Serializable;
import java.math.BigInteger;
import java.time.LocalDateTime;
import java.util.Properties;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;

class DomibusDatePrefixedSequenceIdGeneratorGeneratorTest {

    static Stream<Arguments> generateDomibus() {
        return Stream.of(
                Arguments.of("Base Integer sequence generator    ", Long.valueOf("210809150000000050"), LocalDateTime.parse("2021-08-09T15:15:30"), 50),
                Arguments.of("Base Long sequence generator       ", Long.valueOf("210809150000000050"), LocalDateTime.parse("2021-08-09T15:15:30"), 50L),
                Arguments.of("Base BigInteger sequence generator ", Long.valueOf("210809150000000050"), LocalDateTime.parse("2021-08-09T15:15:30"), BigInteger.valueOf(50)),
                Arguments.of("Base BigInteger change hour        ", Long.valueOf("210809170000000050"), LocalDateTime.parse("2021-08-09T17:15:30"), BigInteger.valueOf(50)),
                Arguments.of("Base BigInteger change sequence    ", Long.valueOf("210809150000013123"), LocalDateTime.parse("2021-08-09T15:15:30"), BigInteger.valueOf(13123)),
                Arguments.of("Base BigInteger change date        ", Long.valueOf("200101150000013123"), LocalDateTime.parse("2020-01-01T15:15:30"), BigInteger.valueOf(13123))
        );
    }

    @SuppressWarnings("unused")
    @ParameterizedTest(name = "[{index}] {0}")
    @MethodSource
    void generateDomibus(String testName, Long result, LocalDateTime currentDate, Serializable generatedSequenceObject) {
        // given
        Mockito.when(testInstance.generate(ArgumentMatchers.any(), ArgumentMatchers.any())).thenReturn(generatedSequenceObject);
        Mockito.when(testInstance.getCurrentDate()).thenReturn(currentDate);
        // when
        Serializable sequence = testInstance.generateDomibus(ArgumentMatchers.any(), ArgumentMatchers.any());
        //then
        assertEquals(result, sequence);
    }

    DomibusDatePrefixedSequenceIdGeneratorGenerator testInstance = Mockito.spy(new DomibusDatePrefixedSequenceIdGeneratorGenerator() {
        @Override
        public void configure(Type type, Properties properties, ServiceRegistry serviceRegistry) throws MappingException {
        }

        @Override
        public Object generatorKey() {
            return null;
        }

        @Override
        public void registerExportables(Database database) {
        }

        @Override
        public Serializable generate(SharedSessionContractImplementor sharedSessionContractImplementor, Object o) throws HibernateException {
            return null;
        }
    });

}
