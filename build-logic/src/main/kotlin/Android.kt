
import com.android.build.api.dsl.CommonExtension
import com.android.build.api.dsl.LibraryExtension
import com.android.build.api.dsl.LibraryVariantDimension
import org.gradle.api.Project

fun Project.configureAndroid(
    namespace: String,
    androidOptions: AndroidOptions
) {
  plugins.apply("com.android.library")

  extensions.findByName("android")?.apply {
    this as CommonExtension<*, *, *, *, *>

    compileSdk = getCatalogVersion("android.sdkversion.compile").toInt()
    this.namespace = namespace

    defaultConfig {
      minSdk = if (androidOptions.withCompose) {
        getCatalogVersion("android.sdkversion.compose.min").toInt()
      } else {
        getCatalogVersion("android.sdkversion.min").toInt()
      }
      testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"

      if (this is LibraryVariantDimension) {
        multiDexEnabled = true
      }
    }

    if (this is LibraryExtension) {
      @Suppress("UnstableApiUsage")
      testOptions.targetSdk = getCatalogVersion("android.sdkversion.target").toInt()
    }

    if (androidOptions.withCompose) {
      plugins.apply("org.jetbrains.kotlin.plugin.compose")
      buildFeatures {
        compose = true
      }
    }
  }
}
