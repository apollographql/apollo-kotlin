import com.android.build.gradle.BaseExtension

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android.min)
  alias(libs.plugins.apollo)
}

android {
  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkversion.min.get())
    targetSdkVersion(libs.versions.android.sdkversion.target.get())
  }

  kotlinOptions {
    jvmTarget = "1.8"
  }
}

dependencies {
  add("implementation", libs.apollo.api)
}

apollo {
  service("service") {
    packageName.set("com.example")
  }
}

