
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.apollo)
}

dependencies {
    implementation("com.apollographql.apollo:apollo-compiler")
}