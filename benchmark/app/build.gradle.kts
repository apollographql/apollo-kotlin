apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")

configure<com.android.build.gradle.AppExtension> {
  namespace = "com.apollographql.apollo3.emptyapp"
  compileSdkVersion(libs.versions.android.sdkVersion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkVersion.min.get())
    targetSdkVersion(libs.versions.android.sdkVersion.target.get())

    val debugSigningConfig = signingConfigs.getByName("debug").apply {
      // This is all public. This app is only an empty shell to make Firebase happy because it requires an 'app' APK.
      keyAlias = "key"
      keyPassword = "apollo"
      storeFile = file("keystore")
      storePassword = "apollo"
    }

    buildTypes {
      getByName("release") {
        signingConfig = debugSigningConfig
      }
    }
  }
}
