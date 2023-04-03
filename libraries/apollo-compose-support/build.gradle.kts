import org.jetbrains.kotlin.gradle.dsl.KotlinCompile

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.library")
}

dependencies {
  api(golatac.lib("compose-runtime"))
  api(project(":apollo-runtime"))
  api(project(":apollo-normalized-cache"))
}

android {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.compose.min").toInt()
    targetSdk = golatac.version("android.sdkversion.target").toInt()
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = golatac.version("compose.compiler")
  }
}

// TODO: needed as long as we use a non supported version of Kotlin
// See https://developer.android.com/jetpack/androidx/releases/compose-kotlin
tasks.withType(KotlinCompile::class.java).configureEach {
  kotlinOptions {
    freeCompilerArgs += listOf(
        "-P",
        "plugin:androidx.compose.compiler.plugins.kotlin:suppressKotlinVersionCompatibilityCheck=1.8.20"
    )
  }
}
