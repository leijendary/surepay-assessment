package nl.surepay.validator;

import nl.surepay.validator.controller.UploadController;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class SurepayApplicationTests {
    @Autowired
    UploadController uploadController;

    @Autowired
    MockMvc mockMvc;

    @Test
    void contextLoads() {
        assertNotNull(uploadController);
    }

    @Test
    void uploadCsvFile_shouldReturnCsvReport() throws Exception {
        var resource = new ClassPathResource("input/records.csv");
        var file = new MockMultipartFile("file", "records.csv", "text/csv", resource.getInputStream());

        mockMvc.perform(multipart("/api/v1/uploads").file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "text/csv"))
                .andExpect(header().string(
                        "Content-Disposition",
                        Matchers.matchesRegex("attachment; filename=upload-report-(.*).csv")))
                .andExpect(content().string("""
                        "Reference","Description","Error Message"
                        "112806","Book Peter de Vries","Duplicate reference"
                        "112806","Book Richard Tyson","Duplicate reference"
                        """));
    }

    @Test
    void uploadJsonFile_shouldReturnJsonReport() throws Exception {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", "records.json", APPLICATION_JSON_VALUE, resource.getInputStream());

        mockMvc.perform(multipart("/api/v1/uploads").file(file))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", APPLICATION_JSON_VALUE))
                .andExpect(header().string(
                        "Content-Disposition",
                        Matchers.matchesRegex("attachment; filename=upload-report-(.*).json")))
                .andExpect(content().string("[{\"reference\":167875,\"description\":\"Toy Greg Alysha\",\"errorMessage\":\"Ending balance did not match\"},{\"reference\":165102,\"description\":\"Book Shevaun Taylor\",\"errorMessage\":\"Ending balance did not match\"}]"));
    }
}
