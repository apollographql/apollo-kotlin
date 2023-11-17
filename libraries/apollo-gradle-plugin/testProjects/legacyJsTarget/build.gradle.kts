plugins {
    // Using a version < 2.0 of the Kotlin plugin because LEGACY js target is no longer supported in 2.0+
    id("org.jetbrains.kotlin.multiplatform").version("1.9.20")
    alias(libs.plugins.apollo)
}

kotlin {
    js(LEGACY) {
        browser()
    }
}

apollo {
    service("service") {
        packageNamesFromFilePaths()
    }
}
