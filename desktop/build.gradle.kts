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
    testRuntimeOnly(libs.logback.classic)
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

// One jar with the game and everything it needs: ./gradlew :desktop:dist, then java -jar on the result.
val dist = tasks.register<Jar>("dist") {
    description = "Builds a single runnable jar of the game."
    group = "distribution"
    archiveBaseName = "blast-arena"
    archiveVersion = project.version.toString()
    destinationDirectory = layout.buildDirectory.dir("dist")
    manifest {
        attributes("Main-Class" to application.mainClass.get())
    }
    from(sourceSets.main.get().output)
    dependsOn(configurations.runtimeClasspath)
    from({
        configurations.runtimeClasspath.get().filter { it.exists() }.map { if (it.isDirectory) it else zipTree(it) }
    })
    duplicatesStrategy = DuplicatesStrategy.EXCLUDE
    // Signature files from dependencies would make the merged jar fail verification.
    exclude("META-INF/*.SF", "META-INF/*.DSA", "META-INF/*.RSA", "META-INF/INDEX.LIST")
}
