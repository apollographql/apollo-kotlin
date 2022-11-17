import com.android.build.gradle.BaseExtension

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android.min)
  alias(libs.plugins.apollo)
}

configure<BaseExtension> {
  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkversion.min.get())
    targetSdkVersion(libs.versions.android.sdkversion.target.get())
  }
}

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  packageName.set("com.example")
}

