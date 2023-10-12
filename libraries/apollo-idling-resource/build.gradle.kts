plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

apolloLibrary(
    javaModuleName = "com.apollographql.apollo3.idling.resource",
)

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
