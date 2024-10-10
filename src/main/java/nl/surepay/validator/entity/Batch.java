package nl.surepay.validator.entity;

import lombok.Data;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.time.Instant;
import java.util.UUID;

@Data
@Table
public class Batch {
    @Id
    private UUID id;

    private Status status = Status.RUNNING;

    @CreatedDate
    private Instant createdAt;

    public enum Status {
        RUNNING, FAILED, COMPLETED
    }
}
