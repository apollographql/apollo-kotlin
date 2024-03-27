import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android.min)
  alias(libs.plugins.apollo)
}

android {
  compileSdkVersion(libs.versions.android.sdkversion.compile.get().toInt())
  namespace = "com.example"

  defaultConfig {
    minSdkVersion(libs.versions.android.sdkversion.min.get())
    targetSdkVersion(libs.versions.android.sdkversion.target.get())
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
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

java.toolchain {
  languageVersion.set(JavaLanguageVersion.of(11))
}

tasks.withType<KotlinCompile>().configureEach {
  kotlinOptions {
    // Required for Kotlin < 1.6.10
    // See https://kotlinlang.org/docs/whatsnew1620.html#compatibility-changes-in-the-xjvm-default-modes
    freeCompilerArgs += "-Xjvm-default=all"
    freeCompilerArgs += "-Xskip-prerelease-check"
  }
}
