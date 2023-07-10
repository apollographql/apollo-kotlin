import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.library")
}

dependencies {
  api(project(":apollo-compose-support"))
  api("androidx.paging:paging-compose:1.0.0-alpha18")
}

android {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()
  namespace = "com.apollographql.apollo3.compose.paging.support"

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.compose.min").toInt()
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = golatac.version("compose.compiler")
  }

  // TODO: compiling fails only with the debug variant currently, due to using a version of Kotlin non supported by Compose.
  // For now, disabling the debug variant works around the issue.
  variantFilter {
    ignore = name == "debug"
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
