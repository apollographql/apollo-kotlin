plugins {
  id("com.android.library")
}

apolloLibrary(
    namespace = "com.apollographql.apollo3.compose.paging",
    androidOptions = AndroidOptions(
        withCompose = true
    )
)

dependencies {
  api(project(":apollo-compose-support-incubating"))
  api(libs.androidx.paging.compose)
}

//android {
//  // TODO: compiling fails only with the debug variant currently, due to using a version of Kotlin non supported by Compose.
//  // For now, disabling the debug variant works around the issue.
//  variantFilter {
//    ignore = name == "debug"
//  }
//}

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
