plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.library")
}

dependencies {
  api(project(":apollo-compose-support"))
  api(project(":apollo-normalized-cache"))
}

android {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
    targetSdk = golatac.version("android.sdkversion.target").toInt()
  }

  buildFeatures {
    compose = true
  }

  composeOptions {
    kotlinCompilerExtensionVersion = golatac.version("compose.compiler")
  }
}
