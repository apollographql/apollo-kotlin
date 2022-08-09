plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
}

dependencies {
  implementation(libs.androidx.espresso.idlingresource)
  api(projects.apolloRuntime)
}

android {
  compileSdk = libs.versions.android.sdkversion.compile.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.min.get().toInt()
    targetSdk = libs.versions.android.sdkversion.target.get().toInt()
  }
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
