from micronaut.http.annotation import Controller, Get


@Controller("/old")
class OldApi:

    @Get
    def index(self) -> str:
        return "old"
