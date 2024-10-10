package nl.surepay.validator.processor;

import jakarta.servlet.http.HttpServletResponse;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.writer.CsvFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.UUID;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.mockito.Mockito.*;
import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@ExtendWith(SpringExtension.class)
class CsvFileProcessorTest {
    @SpyBean
    CsvFileProcessor processor;

    @Test
    void supports_shouldReturnCsv() {
        assertEquals("csv", processor.supports());
    }

    @Test
    void read_shouldStreamResults() throws IOException {
        var fileContent = new ClassPathResource("input/records.csv").getContentAsString(UTF_8);
        var newLineIndex = fileContent.indexOf('\n');
        // Expected result
        var expected = fileContent.substring(newLineIndex + 1).replace("+", "");

        // The actual result
        var stringBuilder = new StringBuilder();

        try (var inputStream = IOUtils.toInputStream(fileContent, UTF_8);
             var stream = processor.read(inputStream)) {
            stream.forEach(result -> {
                assertInstanceOf(RowResult.Valid.class, result);

                var valid = (RowResult.Valid) result;
                var value = valid.value();

                stringBuilder.append("%d,%s,%s,%s,%s,%s".formatted(
                        value.reference(), value.accountNumber(), value.description(), value.startBalance(),
                        value.mutation(), value.endBalance()));
                stringBuilder.append('\n');
            });
        }

        assertEquals(expected, stringBuilder.toString());
    }

    @Test
    void createWriter_shouldWriteToResponseAndCreateFileWriter() throws IOException {
        var id = UUID.randomUUID();
        var httpServletResponse = mock(HttpServletResponse.class);
        var printWriter = mock(PrintWriter.class);

        when(httpServletResponse.getWriter()).thenReturn(printWriter);

        try (var writer = processor.createWriter(id, httpServletResponse)) {
            assertInstanceOf(CsvFileWriter.class, writer);
        }

        verify(httpServletResponse).setContentType("text/csv");
        verify(httpServletResponse).setHeader(CONTENT_DISPOSITION, "attachment; filename=upload-report-%s.csv".formatted(id));
        verify(printWriter).write(anyString());
    }
}
