# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `docs-examples/example-*-python`
that are disabled, reduced, or carry a workaround because the direct port of the Java example does not
compile or does not behave like the Java example yet. It is the bug-fixing task list for the Python compiler
(`micronaut-inject-python` / `micronaut-context-python`); every row references a `TODO(python)`
comment in the sources or the build.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in docs
  snippets. Standard Micronaut and library annotations are imported from their Java package
  (`micronaut.openapi.annotation`, `io.swagger.v3.oas.annotations`, `jakarta.validation.constraints`, ...).
- Do not add Java-style getters or setters to Python docs models. Prefer `@dataclass` models with
  idiomatic Python attributes; attribute names are used verbatim as schema property names, so models
  use the same camelCase names as the Java examples.
- Method docstrings are passed to the Javadoc parser of the OpenAPI visitor as they are, so the
  parameter and response descriptions use the Javadoc `@param` / `@return` tags (reST `:param:` fields
  are not translated).
- A Python test class is a `@MicronautTest` with `@Test` methods; the tests read the generated
  `META-INF/swagger/*.yml` documents through `ClassLoader.getSystemResourceAsStream` (the
  caller-sensitive `Thread.getContextClassLoader()` cannot be called from GraalPy).
- The main and test Python sources of a `docs-examples/example-*-python` project are compiled together
  into the test output (`compilePython` is disabled by the `openapi-example-python` convention),
  because two GraalPy virtual file systems on the same classpath shadow each other's generated shims.
- The `micronaut.openapi.project.dir` system property is passed to the Python compiler (the
  convention plugin does it) because the visitor cannot derive the project directory from the
  generated files of a Python compilation; without it `openapi.properties` and `application.yml` are
  not read.

## Active `@Disabled` Tests

None.

## Workarounds in the Sources

| Target | Reason |
| --- | --- |
| Every module importing `io.swagger.v3.oas.annotations.*` (`Application`, `controllers.annotations.HelloController`, `security.OrderController`, `schemas.*`, `generics.NamedResponseController`, `naming.*`, `resolution.*`, `config.Application`, `groups.Application`) | the Python compiler strips the `io.` prefix of imported Java packages and only restores it for `io.micronaut`: an explicit `from io.swagger.v3.oas.annotations import Operation` is recorded as `swagger.v3.oas.annotations.Operation` and silently ignored by the type element visitors, while a star import (`from io.swagger.v3.oas.annotations import *`) resolves the annotations correctly at compile time. At runtime the transformer only rewrites `io.micronaut.*` imports, so neither form is importable; the sources import the generated `swagger.*` shim packages in an `except ImportError` fallback (hidden from the guide with `indent=0`). |
| `generics.Response`, `dynamicrefs.Response`, `dynamicrefs.Page`, `customserializers.MyJaxbElement` | A generic `@dataclass` attribute typed with a `TypeVar` is generated as `python.T` (or `Object cannot be converted to T`) in the Java stub, and a generic class annotated with `@Introspected` fails with `ReflectClassElement does not support copy constructor`. The generic models are plain classes with annotated attributes and no `@Introspected`. |
| `dynamicrefs.LocalizedCategory` | A `@dataclass` extending another `@dataclass` calls a no-argument super constructor that the stub of the base class does not have; both categories are plain classes with attribute defaults. |
| `schemas.Pet` | An optional enum attribute (`type: PetType \| None`) is not seen as an enum by the visitor (the schema becomes an object with the constants as properties); the attribute is non-optional. |
| `customserializers.MyDto` | `@Introspected` on a dataclass with a `JAXBElement[XmlElement]` attribute fails (`ReflectClassElement does not support copy constructor`); the class is a plain dataclass. |

## `java.type` usages

The sources import Java classes the normal way (`from java.time import Duration`, `from reactor.core.publisher import Mono`,
`from jakarta.xml.bind import JAXBElement`, `from java.lang import ClassLoader`, `from micronaut.openapi import OpenApiUtils`).
`java.type(...)` is only kept where the import form does not work; every call carries a `# TODO(python)` comment.

| Target | Reason |
| --- | --- |
| `OpenApiSpec.py` (`example-python`, `example-config-python`, `example-groups-python`, `example-versions-python`): `io.swagger.v3.oas.models.OpenAPI` | The runtime transformer only rewrites `io.micronaut.*` imports, so `from io.swagger.v3.oas.models import OpenAPI` fails with `ModuleNotFoundError: No module named 'io.swagger'; 'io' is not a package` (there is no generated `swagger.*` shim for the model classes either, only for the annotations). |
| `SecuritySchemeTest.py`: `io.swagger.v3.oas.models.security.SecurityScheme$Type` | Same `io.` prefix limitation (nested enum of a swagger model class). |
| `versions.VersionedControllerTest.py`: `io.swagger.v3.oas.models.PathItem$HttpMethod` | Same `io.` prefix limitation (nested enum of a swagger model class). |

`SchemasTest.py` and `versions.VersionedControllerTest.py` call `getattr(schema, "get$ref")()`: this is not a keyword
alias workaround, `$` cannot appear in a Python identifier so the swagger `Schema.get$ref()` accessor has no alias.

## Reduced Ports

| Target | Difference |
| --- | --- |
| `accessors.Person` | Python classes expose attributes, so `@AccessorsStyle` and the accessor methods of the Java example have no equivalent; the class is an introspected dataclass. |
| `versions.UserDto` | Nested classes are not supported, `UserDto` is a top-level class in every language (the schema is therefore named `UserDto` instead of `VersionedController.UserDto`). |
