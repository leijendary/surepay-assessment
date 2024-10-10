package nl.surepay.validator.model;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;

import java.io.IOException;
import java.math.BigDecimal;

public record RowValue(
        long reference,
        String accountNumber,
        String description,
        BigDecimal startBalance,
        BigDecimal mutation,
        BigDecimal endBalance
) {
    /**
     * Create an instance of {@link RowValue} based on the {@param line} string array. This method will validate the
     * contents of the line based on the following:
     * <ol>
     *     <li>{@param line} has a size of 6 columns.</li>
     *     <li>Reference is a valid long value.</li>
     *     <li>Account number is not null.</li>
     *     <li>Description is not null.</li>
     *     <li>Start balance is a valid instance of {@link BigDecimal}.</li>
     *     <li>Mutation is a valid instance of {@link BigDecimal}.</li>
     *     <li>End balance is a valid instance of {@link BigDecimal}.</li>
     * </ol>
     *
     * @param line the csv line to parse.
     * @return an instance of {@link RowValue} with the contents from {@param line}
     * @throws IOException if any of the validations fail.
     */
    public static RowValue fromLine(String[] line) throws IOException {
        if (line.length != 6) {
            throw new IOException("Invalid number of columns");
        }

        var reference = toLong("Reference", line[0]);
        var accountNumber = line[1];

        if (accountNumber == null) {
            throw new IOException("Account number is missing");
        }

        var description = line[2];

        if (description == null) {
            throw new IOException("Description is missing");
        }

        var startBalance = toBigDecimal("Start balance", line[3]);
        var mutation = toBigDecimal("Mutation", line[4]);
        var endBalance = toBigDecimal("End balance", line[5]);

        return new RowValue(reference, accountNumber, description, startBalance, mutation, endBalance);
    }

    /**
     * Create an instance of {@link RowValue} based on the {@link JsonParser} content. This method will validate the
     * contents of the line based on the following:
     * <ol>
     *     <li>Reference is a valid long value.</li>
     *     <li>Start balance is a valid instance of {@link BigDecimal}.</li>
     *     <li>Mutation is a valid instance of {@link BigDecimal}.</li>
     *     <li>End balance is a valid instance of {@link BigDecimal}.</li>
     *     <li>All fields are not null.</li>
     * </ol>
     *
     * @param parser {@link JsonParser} to read the object from.
     * @return an instance of {@link RowValue} with the contents from {@param parser}
     * @throws IOException if any of the validations fail.
     */
    public static RowValue fromParser(JsonParser parser) throws IOException {
        long reference = 0;
        String accountNumber = null;
        String description = null;
        BigDecimal startBalance = null;
        BigDecimal mutation = null;
        BigDecimal endBalance = null;

        while (parser.nextToken() != JsonToken.END_OBJECT) {
            var fieldName = parser.currentName();
            // Go to the value
            parser.nextToken();
            // Now get the value
            var value = parser.getValueAsString();

            switch (fieldName) {
                case "reference" -> reference = toLong("Reference", value);
                case "accountNumber" -> accountNumber = value;
                case "description" -> description = value;
                case "startBalance" -> startBalance = toBigDecimal("Start balance", value);
                case "mutation" -> mutation = toBigDecimal("Mutation", value);
                case "endBalance" -> endBalance = toBigDecimal("End balance", value);
            }
        }

        if (reference == 0) {
            throw new IOException("Reference is missing");
        }

        if (accountNumber == null) {
            throw new IOException("Account number is missing");
        }

        if (description == null) {
            throw new IOException("Description is missing");
        }

        if (startBalance == null) {
            throw new IOException("Start balance is missing");
        }

        if (mutation == null) {
            throw new IOException("Mutation is missing");
        }

        if (endBalance == null) {
            throw new IOException("End balance is missing");
        }

        return new RowValue(reference, accountNumber, description, startBalance, mutation, endBalance);
    }

    private static long toLong(String name, String value) throws IOException {
        try {
            return Long.parseLong(value);
        } catch (NumberFormatException e) {
            throw new IOException("%s is not a valid number".formatted(name));
        }
    }

    private static BigDecimal toBigDecimal(String name, String value) throws IOException {
        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            // Error message can be improved when we know the reference and description.
            throw new IOException("%s is not a valid number".formatted(name));
        }
    }
}
