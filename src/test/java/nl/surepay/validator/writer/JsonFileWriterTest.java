package nl.surepay.validator.writer;

import com.fasterxml.jackson.databind.ObjectMapper;
import nl.surepay.validator.model.Report;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.json.JsonTest;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

@JsonTest
class JsonFileWriterTest {
    @Autowired
    ObjectMapper objectMapper;

    @Test
    void write_shouldWriteReport() throws IOException {
        var writer = new StringWriter();
        var jsonFileWriter = new JsonFileWriter(objectMapper, writer);
        var reportNullReference = new Report(null, null, "Null reference and description");
        var reportWithReference = new Report(1L, "Description", "With reference and description");
        var expected = objectMapper.writeValueAsString(List.of(reportNullReference, reportWithReference));

        jsonFileWriter.write(reportNullReference);
        jsonFileWriter.write(reportWithReference);
        jsonFileWriter.close();

        assertEquals(expected, writer.toString());
    }

    @Test
    void close_shouldCloseResource() throws IOException {
        var writer = mock(Writer.class);
        var jsonFileWriter = new JsonFileWriter(objectMapper, writer);

        jsonFileWriter.close();

        verify(writer).flush();
        verify(writer).close();
    }
}
