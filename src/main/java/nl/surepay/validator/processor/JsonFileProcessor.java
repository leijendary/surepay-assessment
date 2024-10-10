package nl.surepay.validator.processor;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.model.RowValue;
import nl.surepay.validator.writer.FileWriter;
import nl.surepay.validator.writer.JsonFileWriter;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;
import java.util.stream.Stream;

import static org.springframework.http.HttpHeaders.CONTENT_DISPOSITION;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@Component
@RequiredArgsConstructor
@Slf4j
public class JsonFileProcessor implements FileProcessor {
    private final ObjectMapper objectMapper;

    @Override
    public String supports() {
        return "json";
    }

    @Override
    public Stream<RowResult> read(InputStream inputStream) throws IOException {
        var factory = objectMapper.getFactory();
        var parser = factory.createParser(inputStream);
        var firstToken = parser.nextToken();

        // Validate that the json contains an array.
        if (firstToken != JsonToken.START_ARRAY) {
            throw new StatusException(BAD_REQUEST, "validation.json.invalid", "/body/file");
        }

        var lineNumber = new AtomicLong();

        return Stream
                .generate(() -> readObject(parser, lineNumber))
                .takeWhile(Objects::nonNull)
                .onClose(() -> onClose(parser));
    }

    @Override
    public FileWriter createWriter(UUID id, HttpServletResponse response) throws IOException {
        // Set the headers of the HTTP response.
        response.setContentType(APPLICATION_JSON_VALUE);
        response.setHeader(CONTENT_DISPOSITION, "attachment; filename=upload-report-%s.json".formatted(id));

        return new JsonFileWriter(objectMapper, response.getWriter());
    }

    private RowResult readObject(JsonParser parser, AtomicLong lineNumber) {
        var i = lineNumber.incrementAndGet();
        RowValue rowValue;

        try {
            // Skip the first token since that is the START_OBJECT and return null if
            // this is not the start of the object.
            if (parser.nextToken() != JsonToken.START_OBJECT) {
                return null;
            }

            rowValue = RowValue.fromParser(parser);
        } catch (IOException e) {
            return new RowResult.Invalid(e.getMessage(), i);
        }

        return new RowResult.Valid(rowValue);
    }

    private void onClose(JsonParser parser) {
        try {
            parser.close();
        } catch (IOException e) {
            log.warn("Failed to close JsonParser", e);
        }
    }
}
