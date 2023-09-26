package eu.domibus.web.rest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Sets;
import eu.domibus.api.multitenancy.DomainTaskException;
import eu.domibus.api.pmode.PModeException;
import eu.domibus.api.property.DomibusPropertyProvider;
import eu.domibus.web.rest.error.ErrorHandlerService;
import eu.domibus.web.rest.error.GlobalExceptionHandlerAdvice;
import org.hibernate.HibernateException;
import org.hibernate.exception.SQLGrammarException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultMatcher;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.multipart.MultipartFile;

import javax.persistence.RollbackException;
import javax.validation.ConstraintViolation;
import javax.validation.ConstraintViolationException;
import javax.validation.Path;
import java.sql.SQLException;
import java.util.Collections;
import java.util.Iterator;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
public class GlobalExceptionHandlerAdviceTest {
    private final String exceptionMessage = "Lorem ipsum dolor sit amet";
    private MockMvc mockMvc;
    private MockMvc mockMvcPmode;

    @InjectMocks
    private GlobalExceptionHandlerAdvice unitUnderTest;

    @Mock
    private PluginUserResource pluginUserResource;

    @Mock
    private PModeResource pModeResource;

    @Mock
    DomibusPropertyProvider domibusPropertyProvider;

    @Spy
    private ErrorHandlerService errorHandlerService = new ErrorHandlerService(domibusPropertyProvider);

    @BeforeEach
    public void setup() {
        

        this.mockMvc = MockMvcBuilders.standaloneSetup(pluginUserResource)
                .setControllerAdvice(unitUnderTest)
                .build();

        this.mockMvcPmode = MockMvcBuilders.standaloneSetup(pModeResource)
                .setControllerAdvice(unitUnderTest)
                .build();
    }

    @Test
    public void testServerErrorHandler() throws Exception {
        Throwable thrown = new DomainTaskException(exceptionMessage);
        doThrow(thrown).when(pluginUserResource).updateUsers(anyList());
        String message = mockMvcResultContent(status().is5xxServerError());
        assertThat("",message, containsString("\"message\""));
        assertThat(message, containsString(exceptionMessage));

        thrown = new RollbackException(exceptionMessage);
        doThrow(thrown).when(pluginUserResource).updateUsers(anyList());
        message = mockMvcResultContent(status().is5xxServerError());
        assertThat(message, containsString("\"message\""));
        assertThat(message, containsString(exceptionMessage));

        thrown = new HibernateException(exceptionMessage);
        doThrow(thrown).when(pluginUserResource).updateUsers(anyList());
        message = mockMvcResultContent(status().is5xxServerError());
        assertThat(message, containsString("\"message\""));
        assertThat(message, containsString("Persistence exception occurred")); //HibernateException messages are now hidden from response and only logged (see EDELIVERY-9027)
    }

    @Test
    public void testServerErrorHandler_HibernateException_hides_causes() throws Exception {
        // test that all the messages from all causes (recursively) of a hibernate exception are not returned in the response ...
        // ... as per EDELIVERY-9027 and only a generic exception message is shown
        Throwable rootCause = new SQLGrammarException("SQL statement exposed", new SQLException("select query syntax error"));
        Throwable hibExc = new HibernateException("Hibernate exception message", rootCause);
        doThrow(hibExc).when(pluginUserResource).updateUsers(anyList());
        String message = mockMvcResultContent(status().is5xxServerError());
        Assertions.assertFalse(message.contains(hibExc.getMessage()));
        Assertions.assertFalse(message.contains(rootCause.getMessage()));
        assertThat(message, containsString("Persistence exception occurred")); //HibernateException messages are now hidden from response and only logged (see EDELIVERY-9027)
    }

    @Test
    public void testBadRequestHandler() throws Exception {
        Throwable thrown = new IllegalArgumentException(exceptionMessage);
        doThrow(thrown).when(pluginUserResource).updateUsers(anyList());
        String message = mockMvcResultContent(status().is4xxClientError());
        assertThat(message, containsString("\"message\""));
        assertThat(message, containsString(exceptionMessage));
    }

    @SuppressWarnings("DataFlowIssue")
    @Test
    public void shouldHandleMethodArgumentNotValidException() throws Exception {
        // given
        BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(Collections.singletonList(new FieldError("station", "name", exceptionMessage)));
        MethodParameter methodParameter = mock(MethodParameter.class);
        MethodArgumentNotValidException thrown = new MethodArgumentNotValidException(methodParameter, bindingResult);
        // when
        ResponseEntity<Object> restErrorResponse = unitUnderTest.handleMethodArgumentNotValid(thrown, null, null, null);
        String message = new ObjectMapper().writeValueAsString(restErrorResponse);
        // then
        assertThat(message, containsString(exceptionMessage));
    }

    @SuppressWarnings("unchecked")
    @Test
    public void testConstraintValidationException() throws Exception {
        String fieldName1 = "User name is required";
        String exceptionMessage1 = "User name is required";
        String generalMessage = "There are validation errors:";

        Path propertyPath = mock(Path.class);
        ConstraintViolation<?> constraintViolation = mock(ConstraintViolation.class);
        Iterator<Path.Node> iterator = mock(Iterator.class);
        Path.Node node = mock(Path.Node.class);

        when(constraintViolation.getMessage()).thenReturn(fieldName1);
        when(constraintViolation.getPropertyPath()).thenReturn(propertyPath);
        when(propertyPath.iterator()).thenReturn(iterator);
        when(iterator.hasNext()).thenReturn(false);
        when(iterator.next()).thenReturn(node);
        when(node.toString()).thenReturn(exceptionMessage1);

        ConstraintViolationException thrown = new ConstraintViolationException(generalMessage, Sets.newHashSet(constraintViolation));

        doThrow(thrown).when(pluginUserResource).updateUsers(anyList());
        String message = mockMvcResultContent(status().is4xxClientError());
        assertThat(message, containsString(generalMessage));
        assertThat(message, containsString(fieldName1));
        assertThat(message, containsString(exceptionMessage1));
    }

    @Test
    @Disabled
    public void testUploadPmodesEndpoint_throwing_HibernateException_sqlsHidden() throws Exception {
        // testing that even if not caught by the service layer and thrown by the endpoint rest method ...
        // ... a hibernate error message doesn't show explicit sql statements
        PModeException hibernateException = new PModeException(new HibernateException("Exception message containing SQL statements"));

        doThrow(hibernateException).when(pModeResource).uploadPMode(any(), any());
        MockHttpServletResponse response = mockMvcPmodeCall();
        Assertions.assertFalse(response.getContentAsString().contains(hibernateException.getMessage()));
        // instead, the hibernate exception message should contain only a generic message
        Assertions.assertTrue(response.getContentAsString().contains("Hibernate exception occured"));

    }

    private String mockMvcResultContent(ResultMatcher expectedStatus) throws Exception {
        String dummyPayload = "[]";
        MvcResult result = mockMvc.perform(put("/rest/plugin/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(dummyPayload)
        )
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(expectedStatus)
                .andReturn();

        return result.getResponse().getContentAsString();
    }

    private MockHttpServletResponse mockMvcPmodeCall() throws Exception {
        MultipartFile file = new MockMultipartFile("filename", new byte[]{1, 0, 1});
        MvcResult result = mockMvcPmode.perform(post("/rest/pmode")
                .content(file.getBytes()))
                .andReturn();
        return result.getResponse();
    }


}
