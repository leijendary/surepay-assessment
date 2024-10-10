package nl.surepay.validator.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.cfg.JsonNodeFeature;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.servlet.http.HttpServletResponse;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.writer.JsonFileWriter;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@JsonTest
class JsonFileProcessorTest {
    @Autowired
    ObjectMapper objectMapper;

    @SpyBean
    JsonFileProcessor processor;

    @Test
    void supports_shouldReturnJson() {
        assertEquals("json", processor.supports());
    }

    @Test
    void read_shouldStreamResults() throws IOException {
        objectMapper.configure(JsonNodeFeature.STRIP_TRAILING_BIGDECIMAL_ZEROES, false);

        var fileContent = new ClassPathResource("input/records.json")
                .getContentAsString(UTF_8)
                .replace("+", "");
        var expected = objectMapper.readTree(fileContent);
        expected.forEach(jsonNode -> {
            var longValue = jsonNode.get("reference").asLong();

            ((ObjectNode) jsonNode).put("reference", longValue);
        });

        final var actual = objectMapper.createArrayNode();

        try (var inputStream = IOUtils.toInputStream(fileContent, UTF_8);
             var stream = processor.read(inputStream)) {
            stream.forEach(result -> {
                assertInstanceOf(RowResult.Valid.class, result);

                var valid = (RowResult.Valid) result;
                var value = valid.value();

                actual.add(objectMapper.valueToTree(value));
            });
        }

        assertEquals(expected.toPrettyString(), actual.toPrettyString());
    }

    @Test
    void createWriter_shouldWriteToResponseAndCreateFileWriter() throws IOException {
        var id = UUID.randomUUID();
        var httpServletResponse = mock(HttpServletResponse.class);
        var printWriter = mock(PrintWriter.class);

        when(httpServletResponse.getWriter()).thenReturn(printWriter);

        try (var writer = processor.createWriter(id, httpServletResponse)) {
            assertInstanceOf(JsonFileWriter.class, writer);
        }

        verify(httpServletResponse).setContentType(APPLICATION_JSON_VALUE);
        verify(httpServletResponse).setHeader(CONTENT_DISPOSITION, "attachment; filename=upload-report-%s.json".formatted(id));
        verify(printWriter, times(2)).write(any(char[].class), anyInt(), anyInt());
    }
}
