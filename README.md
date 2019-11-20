# Micronaut OpenAPI Configuration #

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.configuration/micronaut-openapi.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.configuration%22%20AND%20a:%22micronaut-openapi%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-openapi/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-openapi/actions)
[![Download](https://api.bintray.com/packages/micronaut/core-releases-local/openapi/images/download.svg)](https://bintray.com/micronaut/core-releases-local/openapi/_latestVersion)

This is a configuration for using OpenAPI in Micronaut applications.
  
## Documentation ##

See the [Documentation](https://micronaut-projects.github.io/micronaut-openapi/latest/guide/index.html) for more information.

[Snapshot Documentation](https://micronaut-projects.github.io/micronaut-openapi/snapshot/guide/index.html)

## Snapshots and Releases

Snaphots are automatically published to [JFrog OSS](https://oss.jfrog.org/artifactory/oss-snapshot-local/) using [Github Actions](https://github.com/micronaut-projects/micronaut-openapi/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to JCenter and Maven Central via [Github Actions](https://github.com/micronaut-projects/micronaut-aws/actions).

A release is performed with the following steps:

* [Edit the version](https://github.com/micronaut-projects/micronaut-openapi/edit/master/gradle.properties) specified by `projectVersion` in `gradle.properties` to a semantic, unreleased version. Example `1.0.0`
* [Create a new release](https://github.com/micronaut-projects/micronaut-openapi/releases/new). The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-openapi/actions?query=workflow%3ARelease) to check it passed successfully.
* Celebrate!
