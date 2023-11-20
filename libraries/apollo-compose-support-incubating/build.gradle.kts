import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.compose"
)

dependencies {
  api(libs.compose.runtime)
  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.apollographql.apollo3.compose.support"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.compose.min.get().toInt()
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = libs.versions.compose.compiler.get()
  }
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
