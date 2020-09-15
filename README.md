# Micronaut OpenAPI #

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.openapi/micronaut-openapi.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.openapi%22%20AND%20a:%22micronaut-openapi%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-openapi/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-openapi/actions)
[![Download](https://api.bintray.com/packages/micronaut/core-releases-local/openapi/images/download.svg)](https://bintray.com/micronaut/core-releases-local/openapi/_latestVersion)

This project allows generating OpenAPI 3.x (Swagger) specifications for a Micronaut application at compilation time. By moving the generation of OpenAPI specs into the compiler this project allows Micronaut applications to save on memory and avoid the need for costly computation at runtime unlike most implementations of OpenAPI which rely on processing to occur on the server.
  
## Documentation ##

See the [Documentation](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/index.html) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-openapi/snapshot/guide/index.html) for the latest development version documentation.


## Examples

You can generate example applications at https://launch.micronaut.io by selecting the `Add Feature` button and the `openapi` feature then generate!

## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://oss.jfrog.org/artifactory/oss-snapshot-local/) using [Github Actions](https://github.com/micronaut-projects/micronaut-openapi/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-openapi/actions).

A release is performed with the following steps:

* [Edit the version](https://github.com/micronaut-projects/micronaut-openapi/edit/master/gradle.properties) specified by `projectVersion` in `gradle.properties` to a semantic, unreleased version. Example `1.0.0`
* [Create a new release](https://github.com/micronaut-projects/micronaut-openapi/releases/new). The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-openapi/actions?query=workflow%3ARelease) to check it passed successfully.
* Celebrate!
