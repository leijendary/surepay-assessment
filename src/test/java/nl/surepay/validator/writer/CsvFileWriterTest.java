package nl.surepay.validator.writer;

import nl.surepay.validator.model.Report;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.Writer;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class CsvFileWriterTest {
    @Test
    void write_shouldWriteReport() throws IOException {
        var writer = mock(Writer.class);
        var csvFileWriter = new CsvFileWriter(writer);

        csvFileWriter.write(new Report(null, null, "Null reference and description"));
        csvFileWriter.write(new Report(1L, "Description", "With reference and description"));

        verify(writer).write("\"Reference\",\"Description\",\"Error Message\"\n");
        verify(writer).write("\"\",,\"Null reference and description\"\n");
        verify(writer).write("\"1\",\"Description\",\"With reference and description\"\n");
    }

    @Test
    void close_shouldCloseResource() throws IOException {
        var writer = mock(Writer.class);
        var csvFileWriter = new CsvFileWriter(writer);

        csvFileWriter.close();

        verify(writer).flush();
        verify(writer).close();
    }
}
