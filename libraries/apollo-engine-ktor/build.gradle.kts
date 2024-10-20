plugins {
  id("org.jetbrains.kotlin.multiplatform")
}

apolloLibrary(
    namespace = "com.apollographql.apollo.engine.ktor",
    withLinux = false,
    withWasm = false
)

