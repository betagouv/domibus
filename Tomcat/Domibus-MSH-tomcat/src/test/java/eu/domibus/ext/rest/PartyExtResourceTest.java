package eu.domibus.ext.rest;

import eu.domibus.ext.domain.PartyDTO;
import eu.domibus.ext.domain.PartyFilterRequestDTO;
import eu.domibus.ext.domain.archive.BatchDTO;
import eu.domibus.ext.exceptions.DomibusErrorCode;
import eu.domibus.ext.exceptions.PartyExtServiceException;
import eu.domibus.ext.rest.error.ExtExceptionHelper;
import eu.domibus.ext.services.PartyExtService;
import mockit.*;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

/**
 * @author Catalin Enache
 * @since 4.2
 */
@ExtendWith(JMockitExtension.class)
public class PartyExtResourceTest {

    @Tested
    PartyExtResource partyExtResource;

    @Injectable
    PartyExtService partyExtService;

    @Injectable
    ExtExceptionHelper extExceptionHelper;

    @Test
    public void test_listParties(final @Mocked PartyFilterRequestDTO partyFilterRequestDTO) {
        final String partyName = "domibus-blue";

        new Expectations() {{
            partyFilterRequestDTO.getPageStart();
            result = 1;

            partyFilterRequestDTO.getPageSize();
            result = 12;

            partyFilterRequestDTO.getName();
            result = partyName;
        }};

        //tested method
        partyExtResource.listParties(partyFilterRequestDTO);

        new FullVerifications(partyExtResource) {{
            String partyNameActual;
            int pageStartActual, pageSizeActual;
            partyExtService.getParties(partyNameActual = withCapture(),
                    anyString, anyString,
                    anyString, pageStartActual = withCapture(), pageSizeActual = withCapture());
            Assertions.assertEquals(partyName, partyNameActual);
            Assertions.assertEquals(1, pageStartActual);
            Assertions.assertEquals(12, pageSizeActual);
        }};


    }

    @Test
    public void test_createParty(final @Mocked PartyDTO partyDTO) {
        //tested method
        partyExtResource.createParty(partyDTO);

        new FullVerifications(partyExtService) {{
            partyExtService.createParty(partyDTO);
        }};
    }

    @Test
    public void test_createParty_Exception(final @Mocked PartyDTO partyDTO) {

        new Expectations() {{
            partyExtService.createParty(partyDTO);
            result = new PartyExtServiceException(DomibusErrorCode.DOM_001, "test");

        }};

        //tested method
        try {
            partyExtResource.createParty(partyDTO);
        } catch (Exception e) {
            Assertions.assertTrue(e instanceof PartyExtServiceException);
        }
    }

    @Test
    public void test_deleteParty() {
        final String partyName = "domibus-red";

        //tested method
        partyExtResource.deleteParty(partyName);

        new FullVerifications() {{
            String partyNameActual;
            partyExtService.deleteParty(partyNameActual = withCapture());
            Assertions.assertEquals(partyName, partyNameActual);
        }};
    }

    @Test
    public void test_getCertificateForParty() {
        final String partyName = "domibus-red";

        //tested method
        partyExtResource.getCertificateForParty(partyName);

        new FullVerifications() {{
            String partyNameActual;
            partyExtService.getPartyCertificateFromTruststore(partyNameActual = withCapture());
            Assertions.assertEquals(partyName, partyNameActual);
        }};
    }

    @Test
    public void test_getCertificateForParty_NotFound() {
        final String partyName = "domibus-red";

        new Expectations() {{
            partyExtService.getPartyCertificateFromTruststore(partyName);
            result = null;
        }};

        //tested method
        partyExtResource.getCertificateForParty(partyName);

        new FullVerifications() {{
        }};
    }

    @Test
    public void test_listProcesses(final @Mocked PartyFilterRequestDTO partyFilterRequestDTO) {
        final String partyName = "domibus-blue";

        new Expectations() {{
            partyFilterRequestDTO.getName();
            result = partyName;
        }};

        //tested method
        partyExtResource.listParties(partyFilterRequestDTO);

        new FullVerifications(partyExtService) {{
            String partyNameActual;
            partyExtService.getParties(partyNameActual = withCapture(), anyString,
                    anyString, anyString, anyInt, anyInt);
            Assertions.assertEquals(partyName, partyNameActual);
        }};
    }

    @Test
    public void test_updateParties(final @Mocked PartyDTO partyDTO) {

        //tested method
        partyExtResource.updateParty(partyDTO);

        new FullVerifications(partyExtService) {{
            PartyDTO partyDTOActual;
            partyExtService.updateParty(partyDTOActual = withCapture());
            Assertions.assertNotNull(partyDTOActual);
        }};
    }

    @Test
    public void test_handlePartyExtServiceException() {

        //tested method
        PartyExtServiceException e = new PartyExtServiceException(new Throwable());
        partyExtResource.handlePartyExtServiceException(e);

        new FullVerifications() {{
            extExceptionHelper.handleExtException(e);
        }};
    }
}
