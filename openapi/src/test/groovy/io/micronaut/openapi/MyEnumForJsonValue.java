package io.micronaut.openapi;

import io.swagger.v3.oas.annotations.media.Schema;

import com.fasterxml.jackson.annotation.JsonValue;

@Schema(type = "integer", format = "int32", defaultValue = "1")
public enum MyEnumForJsonValue {

    VAL1(1),
    VAL2(2),
    ;

    private final int code;

    MyEnumForJsonValue(int code) {
        this.code = code;
    }

    @JsonValue
    public int getCode() {
        return code;
    }
}
