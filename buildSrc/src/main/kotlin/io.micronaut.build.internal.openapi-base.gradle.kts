import org.gradle.api.tasks.testing.logging.TestExceptionFormat.FULL

repositories {
    mavenCentral()
}

tasks.withType(Test::class) {
    useJUnitPlatform()
    testLogging {
        exceptionFormat = FULL
    }
}
