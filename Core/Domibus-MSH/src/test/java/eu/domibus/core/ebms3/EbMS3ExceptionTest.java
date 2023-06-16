package eu.domibus.core.ebms3;

import eu.domibus.common.ErrorCode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Created by muellers on 4/20/16.
 */
public class EbMS3ExceptionTest {

    private EbMS3Exception ebMS3Exception;
    private static final String ERROR_DETAIL_256CHARS = "OkgJvOG5Xp7rL1CzL5AXjdpgDGCYFIxXw43k6D87NA27CnnY3SKDX5FDGnU90IW6uNGMgxqi3nvbpMyIIxcuOLm9PP8cVytva0uyGiyiJHituKdj9bxnxYeRazfqLOz8HvfVfHxFF3JsXWwndiCgTUIdVzeDXnPt6tSB5NOEPdq6tbH7WScgY2kHl0VBhW8eGZu220D2MwSuFIFh6k2U2VzCd80eKz0bQlcOAQpDN2Pssj308uWULedijmPbvRoH";
    private static final String ERROR_DETAIL_254CHARS = "gJvOG5Xp7rL1CzL5AXjdpgDGCYFIxXw43k6D87NA27CnnY3SKDX5FDGnU90IW6uNGMgxqi3nvbpMyIIxcuOLm9PP8cVytva0uyGiyiJHituKdj9bxnxYeRazfqLOz8HvfVfHxFF3JsXWwndiCgTUIdVzeDXnPt6tSB5NOEPdq6tbH7WScgY2kHl0VBhW8eGZu220D2MwSuFIFh6k2U2VzCd80eKz0bQlcOAQpDN2Pssj308uWULedijmPbvRoH";

    @BeforeEach
    public void setup() {
        ebMS3Exception = EbMS3ExceptionBuilder.getInstance()
                .ebMS3ErrorCode(ErrorCode.EbMS3ErrorCode.EBMS_0001)
                .refToMessageId(UUID.randomUUID().toString())
                .build();
    }

    @Test
    public void getErrorDetail_CharacterLimitReached() {
        ebMS3Exception.setErrorDetail(ERROR_DETAIL_256CHARS);
        assertEquals(256, ERROR_DETAIL_256CHARS.length());
        assertEquals(255, ebMS3Exception.getErrorDetail().length());
    }

    @Test
    public void getErrorDetail_CharacterLimitNotReached() {
        ebMS3Exception.setErrorDetail(ERROR_DETAIL_254CHARS);
        assertEquals(254, ebMS3Exception.getErrorDetail().length());
    }

    @Test
    public void getErrorDetail_Empty() {
        ebMS3Exception.setErrorDetail("");
        assertEquals(0, ebMS3Exception.getErrorDetail().length());
    }

    @Test
    public void getErrorDetail_Null() {
        ebMS3Exception.setErrorDetail(null);
        assertNull(ebMS3Exception.getErrorDetail());
    }

    @Test
    public void getFaultInfo_ErrorDetail_CharacterLimitReached() {
        ebMS3Exception.setErrorDetail(ERROR_DETAIL_256CHARS);
        assertEquals(ebMS3Exception.getErrorDetail().length(), ebMS3Exception.getFaultInfoError().getErrorDetail().length());
    }

}
