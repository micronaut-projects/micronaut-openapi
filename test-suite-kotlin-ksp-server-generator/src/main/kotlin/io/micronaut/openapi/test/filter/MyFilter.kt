package io.micronaut.openapi.test.filter

import io.micronaut.core.annotation.Nullable
import java.util.regex.Pattern

/**
 * A type for specifying result filtering.
 *
 * This is used to demonstrate custom parameter mapping, as it is mapped from the
 * Filter header and bound with a custom binder.
 *
 * @param conditions The filtering conditions
 */
data class MyFilter(
        val conditions: List<Condition>
) {

    override fun toString(): String = conditions.joinToString( ",", transform = { it.toString() })

    /**
     * A filtering condition.
     *
     * @param propertyName the parameter to use for filtering
     * @param comparator the filtering comparator
     * @param value the value to compare with
     */
    data class Condition(
        val propertyName: String,
        val comparator: ConditionComparator,
        val value: Any,
    ) {

        override fun toString(): String {
            return propertyName + comparator + value
        }

        companion object {
            /**
             * Parse the condition from a string representation.
             *
             * @param string the string
             * @return the parsed condition
             */
            @JvmStatic
            fun parse(string: String): Condition {
                val matcher = CONDITION_PATTERN.matcher(string)
                if (!matcher.find()) {
                    throw ParseException("The filter condition must match '${CONDITION_REGEX}' but is '${string}'")
                }
                return Condition(
                        matcher.group(1),
                        ConditionComparator.parse(matcher.group(2)),
                        matcher.group(3)
                )
            }
        }
    }

    /**
     * An enum value for specifying how to compare in the condition.
     */
    enum class ConditionComparator(
        private val representation: String
    ) {

        EQUALS("="),
        GREATER_THAN(">"),
        LESS_THAN("<");

        override fun toString(): String {
            return representation
        }

        companion object {

            private val VALUE_MAPPING = entries.associateBy { it.representation }

            /**
             * Parse the condition comparator from string representation.
             *
             * @param representation the string
             * @return the comparator
             */
            @JvmStatic
            fun parse(representation: String): ConditionComparator {
                require(VALUE_MAPPING.containsKey(representation)) { ParseException("Condition comparator not supported: '$representation'") }
                return VALUE_MAPPING[representation]!!
            }
        }
    }

    /**
     * A custom exception for failed parsing
     */
    class ParseException(
        message: String
    ) : RuntimeException(message)

    companion object {
        private const val CONDITION_REGEX = "^(.+)([<>=])(.+)$"
        /**
         * An implementation with no filtering.
         */
        private val EMPTY = MyFilter(listOf())
        private val CONDITION_PATTERN = Pattern.compile(CONDITION_REGEX)

        /**
         * Parse the filter from a query parameter.
         *
         * @param value the string representation of filter.
         * @return the filter.
         */
        @JvmStatic
        fun parse(value: @Nullable String?): MyFilter {
            if (value == null) {
                return EMPTY
            }
            val conditions = value.split(",")
                    .map { Condition.parse(it) }
                    .toList()
            return MyFilter(conditions)
        }
    }
}
