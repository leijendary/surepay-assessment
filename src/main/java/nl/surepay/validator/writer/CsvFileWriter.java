package nl.surepay.validator.writer;

import com.opencsv.CSVWriter;
import nl.surepay.validator.model.Report;

import java.io.IOException;
import java.io.Writer;
import java.util.Optional;

public class CsvFileWriter implements FileWriter {
    private static final String[] HEADERS = new String[]{
            "Reference",
            "Description",
            "Error Message",
    };

    private final CSVWriter writer;

    public CsvFileWriter(Writer writer) {
        this.writer = new CSVWriter(writer);
        this.writer.writeNext(HEADERS);
    }

    @Override
    public void write(Report report) {
        var line = new String[]{
                Optional.ofNullable(report.reference()).map(String::valueOf).orElse(""),
                report.description(),
                report.errorMessage()
        };

        this.writer.writeNext(line);
    }

    @Override
    public void close() throws IOException {
        this.writer.close();
    }
}
