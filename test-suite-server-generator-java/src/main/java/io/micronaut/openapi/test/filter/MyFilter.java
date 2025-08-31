package io.micronaut.openapi.test.filter;

import io.micronaut.core.annotation.Nullable;

import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * A type for specifying result filtering.
 * <p>This is used to demonstrate custom parameter mapping, as it is mapped from the
 * Filter header and bound with a custom binder.</p>
 *
 * @param conditions The filtering conditions
 */
public record MyFilter (
    List<Condition> conditions
) {

    /**
     * An implementation with no filtering.
     */
    public static final MyFilter EMPTY = new MyFilter(List.of());

    private static final String CONDITION_REGEX = "^(.+)([<>=])(.+)$";
    private static final Pattern CONDITION_PATTERN = Pattern.compile(CONDITION_REGEX);

    /**
     * Parse the filter from a query parameter.
     *
     * @param value the string representation of filter.
     * @return the filter.
     */
    public static MyFilter parse(@Nullable String value) {
        if (value == null) {
            return EMPTY;
        }
        List<Condition> conditions = Arrays.stream(value.split(","))
            .map(Condition::parse)
            .toList();
        return new MyFilter(conditions);
    }

    @Override
    public String toString() {
        return conditions.stream()
            .map(Object::toString)
            .collect(Collectors.joining(","));
    }

    /**
     * A filtering condition.
     *
     * @param propertyName the parameter to use for filtering
     * @param comparator the filtering comparator
     * @param value the value to compare with
     */
    public record Condition(
        String propertyName,
        ConditionComparator comparator,
        Object value
    ) {
        /**
         * Parse the condition from a string representation.
         *
         * @param string the string
         * @return the parsed condition
         */
        public static Condition parse(String string) {
            Matcher matcher = CONDITION_PATTERN.matcher(string);
            if (!matcher.find()) {
                throw new ParseException("The filter condition must match '" + CONDITION_REGEX +
                    "' but is '" + string + "'");
            }
            return new Condition(
                matcher.group(1),
                ConditionComparator.parse(matcher.group(2)),
                matcher.group(3)
            );
        }

        @Override
        public String toString() {
            return propertyName + comparator + value;
        }
    }

    /**
     * An enum value for specifying how to compare in the condition.
     */
    public enum ConditionComparator {

        EQUALS("="),
        GREATER_THAN(">"),
        LESS_THAN("<");

        private final String representation;

        ConditionComparator(String representation) {
            this.representation = representation;
        }

        /**
         * Parse the condition comparator from string representation.
         *
         * @param string the string
         * @return the comparator
         */
        public static ConditionComparator parse(String string) {
            return Arrays.stream(values())
                .filter(v -> v.representation.equals(string))
                .findFirst()
                .orElseThrow(
                    () -> new ParseException("Condition comparator not supported: '" + string + "'")
                );
        }

        @Override
        public String toString() {
            return representation;
        }
    }

    /**
     * A custom exception for failed parsing
     */
    public static class ParseException extends RuntimeException {
        private ParseException(String message) {
            super(message);
        }
    }
}
