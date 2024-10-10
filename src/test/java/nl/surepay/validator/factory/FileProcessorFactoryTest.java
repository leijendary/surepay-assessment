package nl.surepay.validator.factory;

import jakarta.servlet.http.HttpServletResponse;
import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.processor.FileProcessor;
import nl.surepay.validator.writer.FileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(SpringExtension.class)
@Import(FileProcessorFactoryTest.Configuration.class)
class FileProcessorFactoryTest {
    @SpyBean(name = "csvFileProcessor")
    FileProcessor csvFileProcessor;

    @SpyBean(name = "jsonFileProcessor")
    FileProcessor jsonFileProcessor;

    @SpyBean
    FileProcessorFactory fileProcessorFactory;

    @Test
    void getProcessor_shouldReturnTheProcessor() {
        var csv = fileProcessorFactory.getProcessor("file 1.csv");
        var json = fileProcessorFactory.getProcessor("file 1.json");

        assertEquals(csvFileProcessor, csv);
        assertEquals(jsonFileProcessor, json);
    }

    @Test
    void getProcessor_shouldThrowError_whenInvalidFile() {
        assertThrows(StatusException.class, () -> fileProcessorFactory.getProcessor("file 1.txt"));
        assertThrows(StatusException.class, () -> fileProcessorFactory.getProcessor("file 1"));
        assertThrows(StatusException.class, () -> fileProcessorFactory.getProcessor("file 1."));
    }

    @TestConfiguration
    static class Configuration {
        @Bean("csvFileProcessor")
        FileProcessor csvFileProcessor() {
            return new FileProcessor() {
                @Override
                public String supports() {
                    return "csv";
                }

                @Override
                public Stream<RowResult> read(InputStream inputStream) throws IOException {
                    return Stream.empty();
                }

                @Override
                public FileWriter createWriter(UUID id, HttpServletResponse response) throws IOException {
                    return null;
                }
            };
        }

        @Bean("jsonFileProcessor")
        FileProcessor jsonFileProcessor() {
            return new FileProcessor() {
                @Override
                public String supports() {
                    return "json";
                }

                @Override
                public Stream<RowResult> read(InputStream inputStream) throws IOException {
                    return Stream.empty();
                }

                @Override
                public FileWriter createWriter(UUID id, HttpServletResponse response) throws IOException {
                    return null;
                }
            };
        }
    }
}
