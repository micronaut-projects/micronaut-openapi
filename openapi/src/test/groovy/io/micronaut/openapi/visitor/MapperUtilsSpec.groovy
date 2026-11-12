package io.micronaut.openapi.visitor

import io.micronaut.core.type.Argument
import io.micronaut.json.tree.JsonNode
import io.micronaut.openapi.mapper.OpenApiMapperType
import io.micronaut.openapi.mapper.OpenApiVersion
import io.swagger.v3.oas.models.info.Info
import io.swagger.v3.oas.models.media.Schema
import io.swagger.v3.oas.models.media.StringSchema
import spock.lang.Shared
import spock.lang.Specification

import java.nio.file.Files

class MapperUtilsSpec extends Specification {

    @Shared
    def json = Utils.getMapperProvider().getMapper(OpenApiMapperType.JSON, OpenApiVersion.of(false))
    @Shared
    def yaml = Utils.getMapperProvider().getMapper(OpenApiMapperType.YAML, OpenApiVersion.of(false))

    void "reads and writes values"() {
        given:
        def file = Files.createTempFile("openapi", ".yml")
        Files.writeString(file, "title: t\nversion: '1'\n")
        def writer = new StringWriter()

        when:
        Info info = MapperUtils.readValue(yaml, file, Info)
        MapperUtils.writeValue(json, writer, info)

        then:
        info.title == 't'
        writer.toString() == '{"title":"t","version":"1"}'
        MapperUtils.writeValueAsString(json, info) == writer.toString()
        MapperUtils.readValue(json, '["a"]', Argument.listOf(String)) == ['a']

        cleanup:
        Files.deleteIfExists(file)
    }

    void "converts through trees"() {
        when:
        JsonNode tree = MapperUtils.readTree(json, '{"title":"t","n":null}')
        Map<String, Object> map = MapperUtils.convertValue(json, new Info().title('t'), ConvertUtils.MAP_TYPE_REFERENCE)

        then:
        tree.get('title').stringValue == 't'
        MapperUtils.readTree(json, 'null').isNull()
        MapperUtils.treeToValue(json, tree, Info).title == 't'
        MapperUtils.convertValue(json, [title: 't'], Info).title == 't'
        map == [title: 't']
    }

    void "updates values and returns the result"() {
        given:
        def tree = JsonNode.from([description: 'd'])
        def stringSchema = new StringSchema()

        expect:
        MapperUtils.updateValue(json, stringSchema, tree).is(stringSchema)
        stringSchema.description == 'd'
        MapperUtils.updateValue(json, new Schema<>(), tree).description == 'd'
    }

    void "wraps mapping failures as UncheckedIOException"() {
        when:
        call()

        then:
        thrown(UncheckedIOException)

        where:
        call << [
            { MapperUtils.readValue(json, '{not json', Info) },
            { MapperUtils.readValue(yaml, Files.createTempFile("missing", ".yml").tap { Files.delete(it) }, Info) },
            { MapperUtils.treeToValue(json, JsonNode.createStringNode('x'), Integer) },
            { MapperUtils.valueToTree(json, new Failing()) },
            { MapperUtils.writeValueAsString(json, new Failing()) },
            { MapperUtils.writeValue(json, new StringWriter(), new Failing()) },
            { MapperUtils.updateValue(json, new Info(), JsonNode.from([title: [a: 1]])) },
        ]
    }

    void "reads tree values"() {
        given:
        def node = JsonNode.from([s: 'x', i: 1, o: [a: 1], n: null])

        expect:
        MapperUtils.stringValue(node.get('s')) == 'x'
        MapperUtils.stringValue(node.get('i')) == '1'
        MapperUtils.stringValue(node.get('o')) == null
        MapperUtils.stringValue(node.get('n')) == null
        MapperUtils.stringValue(null) == null
        MapperUtils.has(node, 's')
        !MapperUtils.has(node, 'missing')
        !MapperUtils.has(JsonNode.createStringNode('x'), 's')
    }

    void "copies objects with and without properties"() {
        given:
        def node = JsonNode.from([a: 1, b: 2])
        def string = JsonNode.createStringNode('x')

        expect:
        MapperUtils.without(node, 'a').entries()*.key == ['b']
        MapperUtils.without(string, 'a').is(string)
        MapperUtils.with(node, 'c', JsonNode.createStringNode('z')).entries()*.key == ['a', 'b', 'c']
        MapperUtils.with(node, 'a', JsonNode.createNumberNode(3)).get('a').intValue == 3
    }

    /**
     * A bean that cannot be serialized.
     */
    static class Failing {
        String getValue() {
            throw new IllegalStateException("boom")
        }
    }
}
