package nl.surepay.validator.service;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import nl.surepay.validator.entity.Batch;
import nl.surepay.validator.entity.Transaction;
import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.factory.FileProcessorFactory;
import nl.surepay.validator.model.Report;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.model.RowValue;
import nl.surepay.validator.repository.BatchRepository;
import nl.surepay.validator.repository.TransactionRepository;
import nl.surepay.validator.writer.FileWriter;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Optional;
import java.util.UUID;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
@RequiredArgsConstructor
public class UploadService {
    private final BatchRepository batchRepository;
    private final FileProcessorFactory fileProcessorFactory;
    private final TransactionRepository transactionRepository;

    public void upload(MultipartFile file, HttpServletResponse response) {
        if (file.isEmpty()) {
            throw new StatusException(BAD_REQUEST, "validation.file.empty", "/body/file");
        }

        var fileName = Optional.ofNullable(file.getOriginalFilename())
                .orElseThrow(() -> new StatusException(BAD_REQUEST, "validation.file.name.empty", "/body/file/name"));
        var processor = fileProcessorFactory.getProcessor(fileName);
        // Create a new batch record where we have the ID.
        var batch = batchRepository.save(new Batch());

        try (var stream = processor.read(file.getInputStream());
             var writer = processor.createWriter(batch.getId(), response)) {

            for (var iterator = stream.iterator(); iterator.hasNext(); ) {
                var rowResult = iterator.next();

                processResult(batch.getId(), rowResult, writer);
            }
        } catch (IOException e) {
            batch.setStatus(Batch.Status.FAILED);

            batchRepository.save(batch);

            throw new RuntimeException(e);
        }

        batch.setStatus(Batch.Status.COMPLETED);

        batchRepository.save(batch);
    }

    private void processResult(UUID batchId, RowResult result, FileWriter writer) throws IOException {
        switch (result) {
            case RowResult.Valid valid -> processValid(batchId, valid.value(), writer);
            case RowResult.Invalid error -> processInvalid(error, writer);
        }
    }

    private void processValid(UUID batchId, RowValue value, FileWriter writer) throws IOException {
        var transaction = Transaction.builder()
                .batchId(batchId)
                .reference(value.reference())
                .accountNumber(value.accountNumber())
                .description(value.description())
                .startBalance(value.startBalance())
                .mutation(value.mutation())
                .endBalance(value.endBalance())
                .build();

        try {
            transactionRepository.save(transaction);
        } catch (DbActionExecutionException e) {
            var message = e.getMessage();

            if (e.getCause() instanceof DuplicateKeyException) {
                message = "Duplicate reference";
            }

            writeReport(value.reference(), value.description(), message, writer);
            return;
        }

        var expectedEndBalance = value.startBalance().add(value.mutation());

        if (expectedEndBalance.compareTo(value.endBalance()) != 0) {
            writeReport(value.reference(), value.description(), "Ending balance did not match", writer);
        }
    }

    private void processInvalid(RowResult.Invalid result, FileWriter writer) throws IOException {
        var message = "%s at line %d".formatted(result.error(), result.lineNumber());
        var report = new Report(null, null, message);

        writer.write(report);
    }

    private void writeReport(long reference, String description, String message, FileWriter writer) throws IOException {
        var report = new Report(reference, description, message);

        writer.write(report);
    }
}
