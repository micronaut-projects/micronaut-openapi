plugins {
    id("io.micronaut.build.internal.openapi-test-java")
    id("io.micronaut.build.internal.kotlin-ksp")
}

sourceSets {
    test {
        java {
            srcDirs += layout.buildDirectory.dir("/generated/ksp/main/resources").get().asFile
        }
    }
}

dependencies {

    ksp(mnSpring.micronaut.spring.web.annotation)
    ksp(mnSpring.micronaut.spring.boot.annotation)
    ksp(mn.micronaut.inject.kotlin)
    ksp(projects.micronautOpenapi)

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

ksp {
    arg("micronaut.openapi.project.dir", "$projectDir")
    arg("micronaut.openapi.expand.app.version", "myVersion")
}

tasks.register("removeMnFiles") {
    group = "build"
    description = "Removes generated Micronaut metadata from main KSP outputs."

    doLast {
        delete(layout.buildDirectory.dir("/generated/ksp/main/resources/META-INF/micronaut"))
        delete(
            layout.buildDirectory.dir("/generated/ksp/main/classes").get().asFileTree.matching {
                include(
                    "**/*\$Definition*.class",
                    "**/*\$Intercepted*.class",
                    "**/*\$Introspection*.class",
                )
            }.files
        )
    }
    dependsOn(tasks.named("kspKotlin"))
}
tasks.compileKotlin {
    dependsOn(tasks.named("removeMnFiles"))
}

tasks.register("removeMnTestFiles") {
    group = "build"
    description = "Removes generated Micronaut metadata from test KSP outputs."

    doLast {
        delete(layout.buildDirectory.dir("/generated/ksp/test/resources/META-INF/micronaut"))
        delete(
            layout.buildDirectory.dir("/generated/ksp/test/classes").get().asFileTree.matching {
                include(
                    "**/*\$Definition*.class",
                    "**/*\$Intercepted*.class",
                    "**/*\$Introspection*.class",
                )
            }.files
        )
    }
    dependsOn(tasks.named("kspTestKotlin"))
}
tasks.compileTestKotlin {
    dependsOn(tasks.named("removeMnTestFiles"))
}

afterEvaluate {
    tasks.named("kspTestKotlin") {
        enabled = false
    }
}
