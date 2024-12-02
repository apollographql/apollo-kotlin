plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
}

apolloTest()

dependencies {
  implementation(libs.apollo.normalizedcache.sqlite)
  androidTestImplementation(libs.kotlin.test)
  androidTestImplementation(libs.androidx.espresso.core)
}


android {
  namespace = "com.example"
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()

  defaultConfig {
    applicationId = "com.example.myapplication"
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
    targetSdk = libs.versions.android.sdkversion.compile.get().toInt()
    versionCode = 1
    versionName = "1.0"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

}