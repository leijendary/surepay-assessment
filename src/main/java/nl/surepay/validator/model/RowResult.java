package nl.surepay.validator.model;

public sealed interface RowResult permits RowResult.Valid, RowResult.Invalid {
    record Valid(RowValue value) implements RowResult {
    }

    record Invalid(String error, long lineNumber) implements RowResult {
    }
}
