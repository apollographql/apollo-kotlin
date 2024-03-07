plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.compose",
    androidOptions = AndroidOptions(withCompose = true)
)

dependencies {
  api(libs.compose.runtime)
  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}

// Uncomment when using a non supported version of Kotlin
// See https://developer.android.com/jetpack/androidx/releases/compose-kotlin
//tasks.withType(KotlinCompile::class.java).configureEach {
//  kotlinOptions {
//    freeCompilerArgs += listOf(
//        "-P",
//        "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.9.0"
//    )
//  }
//}
