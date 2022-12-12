plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.apollo)
}

apollo {
    service("service") {
        packageName.set("com.example")
    }
}
