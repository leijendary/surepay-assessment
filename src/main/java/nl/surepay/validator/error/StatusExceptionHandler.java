package nl.surepay.validator.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.surepay.validator.error.exception.StatusException;
import nl.surepay.validator.model.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

@RestControllerAdvice
@Order(1)
@Slf4j
@RequiredArgsConstructor
public class StatusExceptionHandler {
    private final MessageSource messageSource;

    @ExceptionHandler(StatusException.class)
    public ResponseEntity<List<ErrorResponse>> catchStatusException(StatusException e) {
        log.debug("Got status exception", e);

        var message = messageSource.getMessage(e.getCode(), e.getArgs(), LocaleContextHolder.getLocale());
        var error = new ErrorResponse(e.getCode(), message, e.getPointer());

        return ResponseEntity.status(e.getStatus()).body(List.of(error));
    }
}
