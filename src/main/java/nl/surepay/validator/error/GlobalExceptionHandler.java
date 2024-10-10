package nl.surepay.validator.error;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import nl.surepay.validator.model.ErrorResponse;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

import static org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR;

@RestControllerAdvice
@Order
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {
    private final Environment environment;
    private final MessageSource messageSource;

    @ExceptionHandler(Exception.class)
    @ResponseStatus(INTERNAL_SERVER_ERROR)
    public List<ErrorResponse> catchException(Exception e) {
        log.error("Global exception handler", e);

        var isProd = environment.acceptsProfiles(Profiles.of("prod"));
        var code = "error.internal";
        var message = isProd
                ? messageSource.getMessage(code, null, LocaleContextHolder.getLocale())
                : e.getMessage();
        var error = new ErrorResponse(code, message, "/server/internal");

        return List.of(error);
    }
}
