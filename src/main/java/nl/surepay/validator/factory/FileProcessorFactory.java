package nl.surepay.validator.factory;

import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.processor.FileProcessor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Component
public class FileProcessorFactory {
    private final Map<String, FileProcessor> processors;

    public FileProcessorFactory(List<FileProcessor> processors) {
        this.processors = processors.stream().collect(Collectors.toMap(FileProcessor::supports, Function.identity()));
    }

    public FileProcessor getProcessor(String fileName) {
        var extension = getExtension(fileName);
        var processor = processors.get(extension);

        if (processor == null) {
            throw new StatusException(BAD_REQUEST, "validation.file.notSupported", new Object[]{extension}, "/body/file/extension");
        }

        return processor;
    }

    private String getExtension(String fileName) {
        var index = fileName.lastIndexOf('.');

        if (index < 0) {
            throw new StatusException(BAD_REQUEST, "validation.file.invalidExtension", "/body/file/extension");
        }

        var extension = fileName.substring(index + 1);

        if (StringUtils.isEmpty(extension)) {
            throw new StatusException(BAD_REQUEST, "validation.file.invalidExtension", "/body/file/extension");
        }

        return extension;
    }
}
