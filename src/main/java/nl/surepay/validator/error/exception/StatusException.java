package nl.surepay.validator.error.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public class StatusException extends RuntimeException {
    private final HttpStatus status;
    private final String code;
    private final Object[] args;
    private final String pointer;

    public StatusException(HttpStatus status, String code, String pointer) {
        this(status, code, null, pointer);
    }

    public StatusException(HttpStatus status, String code, Object[] args, String pointer) {
        super("Status %s: %s at %s".formatted(status, code, pointer));

        this.status = status;
        this.code = code;
        this.args = args;
        this.pointer = pointer;
    }
}
