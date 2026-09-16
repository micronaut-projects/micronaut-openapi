package io.micronaut.configuration.openapi.docs.accessors

// tag::imports[]
import io.micronaut.core.annotation.AccessorsStyle
import io.micronaut.core.annotation.Introspected
// end::imports[]

// tag::clazz[]
@Introspected
@AccessorsStyle(readPrefixes = [""], writePrefixes = [""]) // <1>
class Person(private var name: String, private var debtValue: Int, private var totalGoals: Int) {

    fun name(): String { // <2>
        return name
    }

    fun debtValue(): Int {
        return debtValue
    }

    fun totalGoals(): Int {
        return totalGoals
    }

    fun name(name: String) { // <2>
        this.name = name
    }

    fun debtValue(debtValue: Int) {
        this.debtValue = debtValue
    }

    fun totalGoals(totalGoals: Int) {
        this.totalGoals = totalGoals
    }
}
// end::clazz[]
