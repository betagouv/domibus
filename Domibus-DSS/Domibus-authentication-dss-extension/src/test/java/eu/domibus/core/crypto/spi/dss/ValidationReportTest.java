package eu.domibus.core.crypto.spi.dss;

import eu.europa.esig.dss.detailedreport.jaxb.XmlDetailedReport;
import eu.europa.esig.dss.validation.reports.CertificateReports;
import mockit.Expectations;
import mockit.Mocked;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.stream.StreamSource;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Thomas Dussart
 * @since 4.1
 */
@ExtendWith(JMockitExtension.class)
public class ValidationReportTest {


    @Test
    @Disabled("EDELIVERY-6896")
    public void isValidNoConfiguredConstraints(@Mocked CertificateReports certificateReports) throws JAXBException {
        final XmlDetailedReport detailedReport = getXmlDetailedReport();

        new Expectations() {{
            certificateReports.getDetailedReportJaxb();
            result = detailedReport;
        }};
        ValidationReport validationReport = new ValidationReport();
        Assertions.assertThrows(IllegalStateException.class, () -> validationReport.extractInvalidConstraints(certificateReports, new ArrayList<>()));
    }

    @Test
    public void isValidDetailReportCertificateIsNull(@Mocked CertificateReports certificateReports) {
        ValidationReport validationReport = new ValidationReport();
        final List<ConstraintInternal> constraints = new ArrayList<>();
        constraints.add(new ConstraintInternal("BBB_XCV_CCCBB", "OK"));
        constraints.add(new ConstraintInternal("BBB_XCV_ICTIVRSC", "OK"));
        new Expectations() {{
            certificateReports.getDetailedReportJaxb();
            result = new XmlDetailedReport();
        }};
        Assertions.assertFalse(validationReport.extractInvalidConstraints(certificateReports, constraints).isEmpty());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void isValidAnchorAndValidityDate(@Mocked CertificateReports certificateReports) throws JAXBException {
        final XmlDetailedReport detailedReport = getXmlDetailedReport();
        new Expectations() {{
            certificateReports.getDetailedReportJaxb();
            result = detailedReport;
        }};

        final List<ConstraintInternal> constraints = new ArrayList<>();
        constraints.add(new ConstraintInternal("BBB_XCV_CCCBB", "OK"));
        ValidationReport validationReport = new ValidationReport();
        Assertions.assertTrue(validationReport.extractInvalidConstraints(certificateReports, constraints).isEmpty());
    }

    @Test
    @Disabled("EDELIVERY-6896")
    public void isValidOneConstraintIsWrong(@Mocked CertificateReports certificateReports) throws JAXBException {

        final XmlDetailedReport detailedReport = getXmlDetailedReport();

        final ArrayList<ConstraintInternal> constraints = new ArrayList<>();
        constraints.add(new ConstraintInternal("BBB_XCV_CCCBB", "OK"));
        constraints.add(new ConstraintInternal("BBB_XCV_ICTIVRS", "OK"));
        constraints.add(new ConstraintInternal("QUAL_HAS_CAQC", "OK"));
        ValidationReport validationReport = new ValidationReport();
        new Expectations() {{
            certificateReports.getDetailedReportJaxb();
            result = detailedReport;
        }};
        Assertions.assertFalse(validationReport.extractInvalidConstraints(certificateReports, constraints).isEmpty());
    }

    protected XmlDetailedReport getXmlDetailedReport() throws JAXBException {
        InputStream xmlStream = getClass().getClassLoader().getResourceAsStream("Validation-report-sample.xml");
        JAXBContext jaxbContext = JAXBContext.newInstance(XmlDetailedReport.class);
        Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        JAXBElement<XmlDetailedReport> customer = (JAXBElement<XmlDetailedReport>) unmarshaller.unmarshal(new StreamSource(xmlStream), XmlDetailedReport.class);
        return customer.getValue();
    }


}
