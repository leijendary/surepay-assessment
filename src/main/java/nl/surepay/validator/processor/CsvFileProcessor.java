package nl.surepay.validator.processor;

import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import com.opencsv.exceptions.CsvValidationException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.model.RowValue;
import nl.surepay.validator.writer.CsvFileWriter;
import nl.surepay.validator.writer.FileWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;

@Component
@Slf4j
public class CsvFileProcessor implements FileProcessor {
    @Override
    public String supports() {
        return "csv";
    }

    @Override
    public Stream<RowResult> read(InputStream inputStream) {
        var reader = new InputStreamReader(inputStream);
        var csvReader = new CSVReaderBuilder(reader)
                // Skip header
                .withSkipLines(1)
                .build();
        var lineNumber = new AtomicLong();

        return Stream
                .generate(() -> readNextLine(csvReader, lineNumber))
                .takeWhile(Objects::nonNull)
                .onClose(() -> onClose(reader, csvReader));
    }

    @Override
    public FileWriter createWriter(UUID id, HttpServletResponse response) throws IOException {
        // Set the headers of the HTTP response.
        response.setContentType("text/csv");
        response.setHeader(CONTENT_DISPOSITION, "attachment; filename=upload-report-%s.csv".formatted(id));

        return new CsvFileWriter(response.getWriter());
    }

    private RowResult readNextLine(CSVReader csvReader, AtomicLong lineNumber) {
        var i = lineNumber.incrementAndGet();
        String[] line;
        RowValue rowValue;

        try {
            line = csvReader.readNext();

            if (line == null) {
                return null;
            }

            rowValue = RowValue.fromLine(line);
        } catch (IOException | CsvValidationException e) {
            return new RowResult.Invalid(e.getMessage(), i);
        }

        return new RowResult.Valid(rowValue);
    }

    private void onClose(Reader reader, CSVReader csvReader) {
        try {
            reader.close();
        } catch (IOException e) {
            log.warn("Failed to close Reader", e);
        }

        try {
            csvReader.close();
        } catch (IOException e) {
            log.warn("Failed to close CSVReader", e);
        }
    }
}
