plugins {
    id("io.micronaut.build.internal.parent")
    alias(mn.plugins.kotlin.jvm) apply(false)
    alias(mn.plugins.kotlin.kapt) apply(false)
    alias(mn.plugins.kotlin.allopen) apply(false)
    alias(mn.plugins.ksp) apply(false)
}

repositories {
    mavenCentral()
}
