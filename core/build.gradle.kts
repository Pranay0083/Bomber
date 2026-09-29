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
    // core depends on nothing but the JDK and SLF4J (Jackson arrives in Phase 8).
    // LibGDX must never be added here.
    api(libs.slf4j.api)

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
    useJUnitPlatform()
    // Load Mockito as an agent up front instead of letting it self-attach, which newer JDKs warn about.
    // -Xshare:off avoids a class-sharing notice caused by the agent adding classes to the boot path.
    jvmArgs("-javaagent:${mockitoAgent.asPath}", "-Xshare:off")
}
