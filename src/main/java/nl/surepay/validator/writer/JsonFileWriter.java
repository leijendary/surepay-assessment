package nl.surepay.validator.writer;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import nl.surepay.validator.model.Report;

import java.io.IOException;
import java.io.Writer;

public class JsonFileWriter implements FileWriter {
    private final JsonGenerator generator;

    public JsonFileWriter(ObjectMapper objectMapper, Writer writer) throws IOException {
        this.generator = objectMapper.createGenerator(writer).enable(JsonGenerator.Feature.AUTO_CLOSE_TARGET);
        this.generator.writeStartArray();
    }

    @Override
    public void write(Report report) throws IOException {
        this.generator.writeObject(report);
    }

    @Override
    public void close() throws IOException {
        this.generator.flush();
        this.generator.close();
    }
}
