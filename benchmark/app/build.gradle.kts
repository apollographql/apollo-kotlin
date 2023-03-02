apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")

configure<com.android.build.gradle.AppExtension> {
  namespace = "com.apollographql.apollo3.emptyapp"

  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
    targetSdk = libs.versions.android.sdkversion.target.get().toInt()

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
