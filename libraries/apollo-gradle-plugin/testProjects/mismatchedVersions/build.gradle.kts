plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.apollo)
}
dependencies {
    add("implementation","com.apollographql.apollo:apollo-runtime:1.4.5")
}

apollo {
    service("service") {
        packageNamesFromFilePaths()
    }
}