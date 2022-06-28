apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")

configure<com.android.build.gradle.AppExtension> {
  namespace = "com.apollographql.apollo3.emptyapp"
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdk = groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString().toInt()
    targetSdk = groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString().toInt()

    val debugSigningConfig = signingConfigs.getByName("debug").apply {
      // This is all public. This app is only
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