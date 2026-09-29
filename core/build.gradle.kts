plugins {
    `java-library`
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

val mockitoAgent = configurations.create("mockitoAgent")

dependencies {
    // core depends on nothing but the JDK, SLF4J, and Jackson for level files.
    // LibGDX must never be added here.
    api(libs.slf4j.api)
    implementation(libs.jackson.databind)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.mockito.core)
    testImplementation(libs.mockito.junit)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
    testRuntimeOnly(libs.logback.classic)
    mockitoAgent(libs.mockito.core) { isTransitive = false }
}

tasks.test {
    useJUnitPlatform {
        excludeTags("soak")
    }
    // Load Mockito as an agent up front instead of letting it self-attach, which newer JDKs warn about.
    // -Xshare:off avoids a class-sharing notice caused by the agent adding classes to the boot path.
    jvmArgs("-javaagent:${mockitoAgent.asPath}", "-Xshare:off")
}

// The long bot-versus-bot soak: ./gradlew :core:soakTest
val soakTest = tasks.register<Test>("soakTest") {
    description = "Runs the long bot soak tests."
    group = "verification"
    testClassesDirs = sourceSets.test.get().output.classesDirs
    classpath = sourceSets.test.get().runtimeClasspath
    useJUnitPlatform {
        includeTags("soak")
    }
    jvmArgs("-javaagent:${mockitoAgent.asPath}", "-Xshare:off")
}
