package nl.surepay.validator.writer;

import nl.surepay.validator.model.Report;

import java.io.Closeable;
import java.io.IOException;

public interface FileWriter extends Closeable {
    /**
     * Write the {@link Report} object to a file.
     *
     * @param report instance of {@link Report}
     * @throws IOException when there are errors in writing to the file
     */
    void write(Report report) throws IOException;
}
