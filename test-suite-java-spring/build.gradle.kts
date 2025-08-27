plugins {
    id("io.micronaut.build.internal.openapi-test-java")
}

sourceSets {
    test {
        java {
            srcDirs += layout.buildDirectory.dir("/classes/java").get().asFile
        }
    }
}

dependencies {

    annotationProcessor(mnSpring.micronaut.spring.web.annotation)
    annotationProcessor(mnSpring.micronaut.spring.boot.annotation)
    annotationProcessor(mn.micronaut.inject.java)
    annotationProcessor(projects.micronautOpenapi)

    compileOnly(projects.micronautOpenapiAnnotations)

    implementation(platform("org.springframework.boot:spring-boot-dependencies:${mnSpring.versions.spring.boot.get()}"))
    implementation(libs.spring.boot.starter.web)
    implementation(libs.spring.boot.starter.validation)
    implementation(libs.spring.boot.starter.data.rest)
    implementation(libs.spring.boot.starter.actuator)
    implementation(libs.micrometer.registry.prometheus)

    testImplementation(libs.spring.boot.starter.test)
    testImplementation(mnTest.junit.jupiter.api)
    testImplementation(projects.micronautOpenapiCommon)

    testRuntimeOnly(mnLogging.logback.classic)
    testRuntimeOnly(mnTest.junit.platform.engine)
    testRuntimeOnly(mnTest.junit.platform.launcher)
}

tasks.withType(JavaCompile::class) {
    options.encoding = "UTF-8"
    options.isIncremental = true
    options.isFork = true
    options.compilerArgs = mutableListOf(
            "-parameters",
            "-Xlint:unchecked",
            "-Xlint:deprecation"
    )
    options.forkOptions.jvmArgs = mutableListOf("-Dapp.version=myVersion")
}

tasks.register("removeMnFiles") {
    doLast {
        delete(layout.buildDirectory.file("/classes/java/main/META-INF/micronaut"))
        delete(
                layout.buildDirectory.dir("/classes/java/main").get().asFileTree.matching {
                    include(
                            "**/*\$Definition*.class",
                            "**/*\$Intercepted*.class",
                            "**/*\$Introspection*.class",
                    )
                }.files
        )
    }
    dependsOn(tasks.classes)
}
tasks.jar {
    dependsOn(tasks.named("removeMnFiles"))
}
