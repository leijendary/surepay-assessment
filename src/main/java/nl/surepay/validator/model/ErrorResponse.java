package nl.surepay.validator.model;

public record ErrorResponse(String code, String message, String pointer) {
}
