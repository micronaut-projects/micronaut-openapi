from micronaut.http.annotation import Controller, Get

from .Person import Person


@Controller("/persons")
class PersonController:

    @Get("/{name}")
    def getPerson(self, name: str) -> Person:
        return Person(name, 1, 2)
