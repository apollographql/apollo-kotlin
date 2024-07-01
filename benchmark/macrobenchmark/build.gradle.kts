import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE

plugins {
  id("com.android.test")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo")
}

android {
  namespace = "com.apollographql.apollo.benchmark.macro"
  compileSdk = libs.versions.android.sdkversion.compilebenchmark.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.benchmark.min.get().toInt()
    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
  }

  @Suppress("UnstableApiUsage")
  useLibrary("android.test.base")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  val debugSigningConfig = signingConfigs.getByName("debug").apply {
    keyAlias = "key"
    keyPassword = "apollo"
    storeFile = file("../app/keystore")
    storePassword = "apollo"
  }

  buildTypes {
    findByName("release")?.apply {
      signingConfig = debugSigningConfig
      isMinifyEnabled = true
    }
    create("benchmark") {
      signingConfig = debugSigningConfig
      isMinifyEnabled = false
      isDebuggable = true
    }
  }

  targetProjectPath = ":app"
  @Suppress("UnstableApiUsage")
  experimentalProperties["android.experimental.self-instrumenting"] = true
}

androidComponents {
  beforeVariants {
    it.enable = it.name == "benchmark"
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

dependencies {
  implementation(libs.androidx.benchmark.macro)
  implementation(libs.androidx.test.core)
  implementation(libs.androidx.test.rules)
  implementation(libs.androidx.test.runner)
  implementation(libs.androidx.test.uiautomator)
  implementation(libs.androidx.profileinstaller)
}