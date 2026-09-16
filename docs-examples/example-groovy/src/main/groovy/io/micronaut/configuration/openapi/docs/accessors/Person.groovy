package io.micronaut.configuration.openapi.docs.accessors

// tag::imports[]
import io.micronaut.core.annotation.AccessorsStyle
import io.micronaut.core.annotation.Introspected
// end::imports[]

// tag::clazz[]
@Introspected
@AccessorsStyle(readPrefixes = "", writePrefixes = "") // <1>
class Person {

    private String name
    private Integer debtValue
    private Integer totalGoals

    Person(String name, Integer debtValue, Integer totalGoals) {
        this.name = name
        this.debtValue = debtValue
        this.totalGoals = totalGoals
    }

    String name() { // <2>
        return name
    }

    Integer debtValue() {
        return debtValue
    }

    Integer totalGoals() {
        return totalGoals
    }

    void name(String name) { // <2>
        this.name = name
    }

    void debtValue(Integer debtValue) {
        this.debtValue = debtValue
    }

    void totalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals
    }
}
// end::clazz[]
