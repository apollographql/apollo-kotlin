plugins {
  id("com.android.library")
  kotlin("android")
}

dependencies {
  implementation(libs.androidx.espresso.idlingResource)
  api(projects.apolloRuntime)
}

android {
  compileSdk = libs.versions.android.sdkVersion.compile.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkVersion.min.get().toInt()
    targetSdk = libs.versions.android.sdkVersion.target.get().toInt()
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
