plugins {
    alias(libs.plugins.kotlin.multiplatform)
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
