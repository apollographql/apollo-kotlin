plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.library")
}

dependencies {
  implementation(libs.androidx.espresso.idlingresource)
  api(project(":apollo-runtime"))
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()
  namespace = "com.apollographql.apollo3.idling.resource"

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
  }
}
