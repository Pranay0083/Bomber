plugins {
    application
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

dependencies {
    implementation(project(":core"))
    implementation(libs.gdx.core)
    implementation(libs.gdx.backend.lwjgl3)
    runtimeOnly(variantOf(libs.gdx.platform) { classifier("natives-desktop") })
    runtimeOnly(libs.logback.classic)

    testImplementation(platform(libs.junit.bom))
    testImplementation(libs.junit.jupiter)
    testImplementation(libs.assertj.core)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}

application {
    mainClass = "com.blastarena.desktop.DesktopLauncher"
    // GLFW on macOS must run on the first thread.
    if (System.getProperty("os.name").lowercase().contains("mac")) {
        applicationDefaultJvmArgs = listOf("-XstartOnFirstThread")
    }
}
