plugins {
    id("io.micronaut.build.internal.openapi-base")
    id("io.micronaut.build.internal.module")
    id("org.sonatype.gradle.plugins.scan")
}
val ossIndexUsername = System.getenv("OSS_INDEX_USERNAME") ?: project.properties["ossIndexUsername"] as String?
val ossIndexPassword = System.getenv("OSS_INDEX_PASSWORD") ?: project.properties["ossIndexPassword"] as String?
if (ossIndexUsername != null && ossIndexPassword != null) {
    ossIndexAudit {
        username = ossIndexUsername
        password = ossIndexPassword
    }
}

repositories {
    mavenCentral()
    google()
}
