plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("apollo.library")
}

dependencies {
  implementation(golatac.lib("androidx.espresso.idlingresource"))
  api(project(":apollo-runtime"))
}

android {
  compileSdk = golatac.version("android.sdkversion.compile").toInt()
  namespace = "com.apollographql.apollo3.android"

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
    targetSdk = golatac.version("android.sdkversion.target").toInt()
  }
}
