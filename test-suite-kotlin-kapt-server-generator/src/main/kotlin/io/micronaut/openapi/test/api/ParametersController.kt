package io.micronaut.openapi.test.api

import io.micronaut.data.model.Pageable
import io.micronaut.data.model.Sort
import io.micronaut.http.annotation.Controller
import io.micronaut.openapi.test.filter.MyFilter
import io.micronaut.openapi.test.model.ColorEnum
import io.micronaut.openapi.test.model.SendDatesResponse
import io.micronaut.openapi.test.model.SendPrimitivesResponse
import reactor.core.publisher.Mono
import java.math.BigDecimal
import java.time.LocalDate
import java.time.ZonedDateTime

@Controller
open class ParametersController : ParametersApi {

    override fun sendPrimitives(name: String, age: BigDecimal, height: Float, isPositive: Boolean): Mono<SendPrimitivesResponse> {
        return Mono.just(SendPrimitivesResponse(name, age, height, isPositive))
    }

    override fun sendValidatedPrimitives(name: String?, age: Int?, favoriteNumber: BigDecimal?, height: Double?): Mono<String> {
        return Mono.just("Success")
    }

    override fun sendDates(commitDate: LocalDate?, commitDateTime: ZonedDateTime?): Mono<SendDatesResponse> {
        return Mono.just(SendDatesResponse(commitDate, commitDateTime))
    }

    override fun sendParameterEnum(colorParam: ColorEnum?): Mono<ColorEnum> {
        return Mono.just(colorParam!!)
    }

    override fun getIgnoredHeader(): Mono<String> {
        return Mono.just("Success")
    }

    override fun sendIgnoredHeader(): Mono<String> {
        return Mono.just("Success")
    }

    override fun sendPageQuery(pageable: Pageable): Mono<String> {
        return Mono.just(
                "(page: " + pageable.number +
                        ", size: " + pageable.size +
                        ", sort: " + sortToString(pageable.sort) + ")"
        )
    }

    override fun sendMappedParameter(myFilter: MyFilter): Mono<String> {
        return Mono.just(myFilter.toString())
    }

    private fun sortToString(sort: Sort): String {
        return sort.orderBy.joinToString(" ") { order -> order.property + "(dir=" + order.direction + ")" }
    }
}
