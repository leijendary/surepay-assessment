package nl.surepay.validator.service;

import jakarta.servlet.http.HttpServletResponse;
import nl.surepay.validator.entity.Batch;
import nl.surepay.validator.entity.Transaction;
import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.factory.FileProcessorFactory;
import nl.surepay.validator.model.Report;
import nl.surepay.validator.model.RowResult;
import nl.surepay.validator.model.RowValue;
import nl.surepay.validator.processor.JsonFileProcessor;
import nl.surepay.validator.repository.BatchRepository;
import nl.surepay.validator.repository.TransactionRepository;
import nl.surepay.validator.writer.JsonFileWriter;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.mock.mockito.SpyBean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.relational.core.conversion.DbActionExecutionException;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.UUID;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;

@ExtendWith(SpringExtension.class)
class UploadServiceTest {
    @MockBean
    BatchRepository batchRepository;

    @MockBean
    FileProcessorFactory fileProcessorFactory;

    @MockBean
    TransactionRepository transactionRepository;

    @Mock
    JsonFileProcessor jsonFileProcessor;

    @Mock
    JsonFileWriter jsonFileWriter;

    @Mock
    HttpServletResponse httpServletResponse;

    @SpyBean
    UploadService uploadService;

    @Test
    void upload_shouldCompleteBatch() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", resource.getFilename(), APPLICATION_JSON_VALUE, resource.getInputStream());
        var newBatch = new Batch();
        newBatch.setId(UUID.randomUUID());

        var completedBatch = new Batch();
        completedBatch.setId(newBatch.getId());
        completedBatch.setStatus(Batch.Status.COMPLETED);

        var rowValue = new RowValue(1, "IBAN", "Test transaction",
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("3"));
        var rowResult = new RowResult.Valid(rowValue);
        var transaction = Transaction.builder()
                .batchId(newBatch.getId())
                .reference(rowValue.reference())
                .accountNumber(rowValue.accountNumber())
                .description(rowValue.description())
                .startBalance(rowValue.startBalance())
                .mutation(rowValue.mutation())
                .endBalance(rowValue.endBalance())
                .build();

        when(fileProcessorFactory.getProcessor(file.getOriginalFilename())).thenReturn(jsonFileProcessor);
        when(batchRepository.save(new Batch())).thenReturn(newBatch);
        when(jsonFileProcessor.read(any(InputStream.class))).thenReturn(Stream.of(rowResult));

        uploadService.upload(file, httpServletResponse);

        verify(fileProcessorFactory).getProcessor(file.getOriginalFilename());
        verify(batchRepository).save(new Batch());
        verify(transactionRepository).save(transaction);
        verify(batchRepository).save(completedBatch);
        verifyNoInteractions(jsonFileWriter);
    }

    @Test
    void upload_shouldThrowError_whenFileIsEmpty() {
        var file = new MockMultipartFile("file", "records.json", APPLICATION_JSON_VALUE, new byte[0]);

        assertThrows(StatusException.class, () -> uploadService.upload(file, httpServletResponse));
    }

    @Test
    void upload_shouldThrowError_whenFileNameIsEmpty() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", null, APPLICATION_JSON_VALUE, resource.getInputStream());

        when(fileProcessorFactory.getProcessor("")).thenThrow(IllegalArgumentException.class);

        assertThrows(IllegalArgumentException.class, () -> uploadService.upload(file, httpServletResponse));
    }

    @Test
    void upload_shouldFailBatchAndThrowError_onException() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", resource.getFilename(), APPLICATION_JSON_VALUE, resource.getInputStream());
        var newBatch = new Batch();
        newBatch.setId(UUID.randomUUID());

        var failedBatch = new Batch();
        failedBatch.setId(newBatch.getId());
        failedBatch.setStatus(Batch.Status.FAILED);

        when(fileProcessorFactory.getProcessor(file.getOriginalFilename())).thenReturn(jsonFileProcessor);
        when(batchRepository.save(new Batch())).thenReturn(newBatch);
        when(jsonFileProcessor.read(any(InputStream.class))).thenThrow(IOException.class);

        assertThrows(RuntimeException.class, () -> uploadService.upload(file, httpServletResponse));

        verify(fileProcessorFactory).getProcessor(file.getOriginalFilename());
        verify(batchRepository).save(new Batch());
        verify(batchRepository).save(failedBatch);
        verifyNoMoreInteractions(batchRepository);
        verifyNoInteractions(httpServletResponse);
        verifyNoInteractions(transactionRepository);
    }

    @Test
    void upload_shouldWriteToResponse_whenRowResultIsInvalid() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", resource.getFilename(), APPLICATION_JSON_VALUE, resource.getInputStream());
        var newBatch = new Batch();
        newBatch.setId(UUID.randomUUID());

        var completedBatch = new Batch();
        completedBatch.setId(newBatch.getId());
        completedBatch.setStatus(Batch.Status.COMPLETED);

        var rowResult = new RowResult.Invalid("Reference is not a number", 1);
        var report = new Report(null, null, rowResult.error() + " at line " + rowResult.lineNumber());

        when(fileProcessorFactory.getProcessor(file.getOriginalFilename())).thenReturn(jsonFileProcessor);
        when(batchRepository.save(new Batch())).thenReturn(newBatch);
        when(jsonFileProcessor.read(any(InputStream.class))).thenReturn(Stream.of(rowResult));
        when(jsonFileProcessor.createWriter(newBatch.getId(), httpServletResponse)).thenReturn(jsonFileWriter);

        uploadService.upload(file, httpServletResponse);

        verify(fileProcessorFactory).getProcessor(file.getOriginalFilename());
        verify(batchRepository).save(new Batch());
        verifyNoInteractions(transactionRepository);
        verify(batchRepository).save(completedBatch);
        verify(jsonFileWriter).write(report);
    }

    @Test
    void upload_shouldWriteToResponse_whenValidationFailed() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", resource.getFilename(), APPLICATION_JSON_VALUE, resource.getInputStream());
        var newBatch = new Batch();
        newBatch.setId(UUID.randomUUID());

        var completedBatch = new Batch();
        completedBatch.setId(newBatch.getId());
        completedBatch.setStatus(Batch.Status.COMPLETED);

        var rowValue = new RowValue(1, "IBAN", "Test transaction",
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("4"));
        var rowResult = new RowResult.Valid(rowValue);
        var report = new Report(rowValue.reference(), rowValue.description(), "Ending balance did not match");
        var transaction = Transaction.builder()
                .batchId(newBatch.getId())
                .reference(rowValue.reference())
                .accountNumber(rowValue.accountNumber())
                .description(rowValue.description())
                .startBalance(rowValue.startBalance())
                .mutation(rowValue.mutation())
                .endBalance(rowValue.endBalance())
                .build();

        when(fileProcessorFactory.getProcessor(file.getOriginalFilename())).thenReturn(jsonFileProcessor);
        when(batchRepository.save(new Batch())).thenReturn(newBatch);
        when(jsonFileProcessor.read(any(InputStream.class))).thenReturn(Stream.of(rowResult));
        when(jsonFileProcessor.createWriter(newBatch.getId(), httpServletResponse)).thenReturn(jsonFileWriter);

        uploadService.upload(file, httpServletResponse);

        verify(fileProcessorFactory).getProcessor(file.getOriginalFilename());
        verify(batchRepository).save(new Batch());
        verify(transactionRepository).save(transaction);
        verify(batchRepository).save(completedBatch);
        verify(jsonFileWriter).write(report);
    }

    @Test
    void upload_shouldWriteToResponse_whenReferenceIsUnique() throws IOException {
        var resource = new ClassPathResource("input/records.json");
        var file = new MockMultipartFile("file", resource.getFilename(), APPLICATION_JSON_VALUE, resource.getInputStream());
        var newBatch = new Batch();
        newBatch.setId(UUID.randomUUID());

        var completedBatch = new Batch();
        completedBatch.setId(newBatch.getId());
        completedBatch.setStatus(Batch.Status.COMPLETED);

        var rowValue = new RowValue(1, "IBAN", "Test transaction",
                new BigDecimal("1"), new BigDecimal("2"), new BigDecimal("4"));
        var rowResult = new RowResult.Valid(rowValue);
        var report = new Report(rowValue.reference(), rowValue.description(), "Duplicate reference");
        var transaction = Transaction.builder()
                .batchId(newBatch.getId())
                .reference(rowValue.reference())
                .accountNumber(rowValue.accountNumber())
                .description(rowValue.description())
                .startBalance(rowValue.startBalance())
                .mutation(rowValue.mutation())
                .endBalance(rowValue.endBalance())
                .build();

        when(fileProcessorFactory.getProcessor(file.getOriginalFilename())).thenReturn(jsonFileProcessor);
        when(batchRepository.save(new Batch())).thenReturn(newBatch);
        when(jsonFileProcessor.read(any(InputStream.class))).thenReturn(Stream.of(rowResult));
        when(jsonFileProcessor.createWriter(newBatch.getId(), httpServletResponse)).thenReturn(jsonFileWriter);
        when(transactionRepository.save(transaction))
                .thenThrow(new DbActionExecutionException(() -> Object.class, new DuplicateKeyException("Not unique")));

        uploadService.upload(file, httpServletResponse);

        verify(fileProcessorFactory).getProcessor(file.getOriginalFilename());
        verify(batchRepository).save(new Batch());
        verify(transactionRepository).save(transaction);
        verify(batchRepository).save(completedBatch);
        verify(jsonFileWriter).write(report);
    }
}
