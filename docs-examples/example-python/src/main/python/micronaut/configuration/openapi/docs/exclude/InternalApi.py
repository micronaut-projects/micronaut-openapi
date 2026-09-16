from micronaut.http.annotation import Controller, Get


@Controller("/internal")
class InternalApi:

    @Get
    def index(self) -> str:
        return "internal"
