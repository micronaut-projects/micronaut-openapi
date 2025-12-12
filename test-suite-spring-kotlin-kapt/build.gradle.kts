import org.jetbrains.kotlin.gradle.internal.KaptTask

plugins {
    id("io.micronaut.build.internal.openapi-test-java")
    alias(mn.plugins.kotlin.jvm)
    alias(mn.plugins.kotlin.kapt)
    alias(mn.plugins.kotlin.allopen)
}

sourceSets {
    test {
        java {
            srcDirs += layout.buildDirectory.dir("/tmp/kapt3/classes/main").get().asFile
        }
    }
}

dependencies {

    kapt(mnSpring.micronaut.spring.web.annotation)
    kapt(mnSpring.micronaut.spring.boot.annotation)
    kapt(mn.micronaut.inject.java)
    kapt(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)

    implementation(platform("org.springframework.boot:spring-boot-dependencies:${mnSpring.versions.spring.boot.get()}"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.rest)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)
    implementation(mn.kotlin.stdlib.asProvider())
    implementation(mn.kotlin.reflect)

    testCompileOnly(projects.micronautOpenapiAnnotations)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(projects.micronautOpenapiCommon)

    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

java {
    sourceCompatibility = JavaVersion.VERSION_21
}

kotlin {
    jvmToolchain(17)
}

kapt {
    arguments {
        arg("micronaut.openapi.project.dir", "$projectDir")
        arg("micronaut.openapi.expand.app.version", "myVersion")
    }
}

tasks.register("removeMnFiles") {
    doLast {
        delete(layout.buildDirectory.dir("/tmp/kapt3/classes/main/META-INF/micronaut"))
        delete(
            layout.buildDirectory.dir("/tmp/kapt3/classes/main").get().asFileTree.matching {
                include(
                    "**/*\$Definition*.class",
                    "**/*\$Intercepted*.class",
                    "**/*\$Introspection*.class",
                )
            }.files
        )
    }
    dependsOn(tasks.named("kaptKotlin"))
}
tasks.compileKotlin {
    dependsOn(tasks.named("removeMnFiles"))
}

tasks.withType(KaptTask::class) {
    if (name == "kaptTestKotlin") {
        enabled = false
    }
}
