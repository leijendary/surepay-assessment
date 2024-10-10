package nl.surepay.validator.processor;

import jakarta.servlet.http.HttpServletResponse;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.writer.FileWriter;

import java.io.IOException;
import java.io.InputStream;
import java.util.UUID;
import java.util.stream.Stream;

public interface FileProcessor {
    /**
     * Return the extension (without the dot) that the implementation supports.
     *
     * @return a file extension
     */
    String supports();

    /**
     * Read the contents if the {@param inputStream} and return a {@link Stream} instance that
     * generates a chunk of the buffer.
     *
     * @param inputStream which the stream will read from
     * @return instance of {@link Stream} that returns each chunk of the buffer.
     * @throws IOException if there are IO exceptions
     */
    Stream<RowResult> read(InputStream inputStream) throws IOException;

    /**
     * Creates an instance of the {@link FileWriter} based on the supported extension.
     *
     * @param id       unique ID to include as identifier. This ID could be written as a header value.
     * @param response the {@link HttpServletResponse} of the request to write the headers and content to.
     * @return instance of {@link FileWriter}
     * @throws IOException if there are IO exceptions
     */
    FileWriter createWriter(UUID id, HttpServletResponse response) throws IOException;
}
