package eu.domibus.openapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import java.io.File;
import java.io.FileWriter;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The class contains one (test) method responsible for generating an open API document with springdoc.
 * At the moment, springdoc enables the generation of the open API document only at runtime. Therefore springdoc
 * maven plugin works only with spring-boot to start the application. The MockMvc is used to reduce complexity while
 * starting the REST API endpoints for the springdoc to generate OpenAPI.
 * (The swagger maven plugin does not support spring mvc annotations and it can not be used)
 *
 * @author Joze Rihtarsic
 * @since 5.0
 */
@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        OpenApiConfig.class,
})
public class OpenApiGenerator {
    public static String sourcePath = "./target/generated-sources";
    private MockMvc mockMvc;
    @Autowired
    private WebApplicationContext webAppContext;

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webAppContext)
                .build();
    }

    @Test
    public void generateJsonOpenApi() throws Exception {

        // retrieve openapi document
        MvcResult result = mockMvc.perform(get(OpenApiConfig.OPEN_API_DOCS_URL))
                .andExpect(status().is2xxSuccessful())
                .andReturn();
        String content = result.getResponse().getContentAsString();
        File target = new File(sourcePath);
        if (!target.exists()){
            target.mkdirs();
        }
        // store document to webapp
        try (FileWriter fileWriter = new FileWriter(new File(sourcePath, "openapi.json"))) {
            fileWriter.write(content);
            fileWriter.flush();
        }
    }

}
