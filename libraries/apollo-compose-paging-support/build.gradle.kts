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
