import com.google.devtools.ksp.gradle.KspTask

plugins {
    id("io.micronaut.build.internal.openapi-test-java")
    alias(mn.plugins.kotlin.jvm)
    alias(mn.plugins.kotlin.allopen)
    alias(mn.plugins.ksp)
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

java {
    sourceCompatibility = JavaVersion.VERSION_25
}

kotlin {
    compilerOptions {
        jvmTarget = org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_25
    }
}

ksp {
    arg("micronaut.openapi.project.dir", "$projectDir")
    arg("micronaut.openapi.expand.app.version", "myVersion")
}

tasks.register("removeMnFiles") {
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

tasks.withType(KspTask::class) {
    if (name == "kspTestKotlin") {
        enabled = false
    }
}
