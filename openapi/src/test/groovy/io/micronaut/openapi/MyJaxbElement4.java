package io.micronaut.openapi;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import io.swagger.v3.oas.annotations.media.Schema;

public class MyJaxbElement4 {

    /**
     * Discount type.
     */
    @NotNull
    public DiscountTypeType type;
    /**
     * Discount data
     */
    @Schema(oneOf = {DiscountSizeOpenApi.class, DiscountFixedOpenApi.class, MultiplierSizeOpenApi.class})
    public Object value;

    /**
     * Discount type
     */
    public enum DiscountTypeType {

        DiscountSize,
        DiscountFixed,
        MultiplierSize,
    }

    /**
     * Discount size
     */
    public static class DiscountSizeOpenApi {

        /**
         * Value description
         */
        @Size(max = 3)
        @Pattern(regexp = "^[1-9][0-9]?$|^100$")
        @NotNull
        public String value;
        /**
         * Expiry description
         */
        @NotNull
        @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2})|0")
        public String expiry;
    }

    /**
     * Discount fixed
     */
    public static class DiscountFixedOpenApi {

        /**
         * Value description
         */
        @NotNull
        @Pattern(regexp = "^0*[1-9]\\d*$")
        public String value;
        /**
         * Expiry description
         */
        @NotNull
        @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2})|0")
        public String expiry;
    }

    /**
     * Multiplier size
     */
    public static class MultiplierSizeOpenApi {

        /**
         * Value description
         */
        @NotNull
        @Size(min = 3, max = 4)
        @Pattern(regexp = "0\\.\\d\\d?")
        public String value;
        /**
         * Expiry description
         */
        @NotNull
        @Pattern(regexp = "(\\d{4}-\\d{2}-\\d{2})|0")
        public String expiry;
    }
}
