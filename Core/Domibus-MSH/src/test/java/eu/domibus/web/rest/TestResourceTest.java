package eu.domibus.web.rest;

import eu.domibus.web.rest.ro.LoginRO;
import eu.domibus.web.rest.ro.UserRO;
import mockit.Tested;
import mockit.integration.junit5.JMockitExtension;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.mock.web.MockHttpServletRequest;

import javax.servlet.http.HttpServletRequest;

/**
 * @author Tiago Miguel
 * @since 3.3
 */
@ExtendWith(JMockitExtension.class)
public class TestResourceTest {

    @Tested
    TestResource testResource;

    @Test
    public void testTestGet() {
        // Given

        // When
        UserRO userRO = testResource.testGet();

        // Then
        Assertions.assertNotNull(userRO);
        Assertions.assertEquals("testGet", userRO.getUsername());
    }

    @Test
    public void testTestPost() {
        // Given
        LoginRO loginRO = new LoginRO();
        HttpServletRequest request = new MockHttpServletRequest("post", "http://testPost");

        // When
        UserRO userRO = testResource.testPost(loginRO, request);

        // Then
        Assertions.assertNotNull(userRO);
        Assertions.assertEquals("testPost", userRO.getUsername());
    }
}
