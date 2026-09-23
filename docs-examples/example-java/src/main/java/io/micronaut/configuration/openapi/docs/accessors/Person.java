package io.micronaut.configuration.openapi.docs.accessors;

// tag::imports[]
import io.micronaut.core.annotation.AccessorsStyle;
import io.micronaut.core.annotation.Introspected;
// end::imports[]

// tag::clazz[]
@Introspected
@AccessorsStyle(readPrefixes = "", writePrefixes = "") // <1>
class Person {

    private String name;
    private Integer debtValue;
    private Integer totalGoals;

    Person(String name, Integer debtValue, Integer totalGoals) {
        this.name = name;
        this.debtValue = debtValue;
        this.totalGoals = totalGoals;
    }

    public String name() { // <2>
        return name;
    }

    public Integer debtValue() {
        return debtValue;
    }

    public Integer totalGoals() {
        return totalGoals;
    }

    public void name(String name) { // <2>
        this.name = name;
    }

    public void debtValue(Integer debtValue) {
        this.debtValue = debtValue;
    }

    public void totalGoals(Integer totalGoals) {
        this.totalGoals = totalGoals;
    }
}
// end::clazz[]
