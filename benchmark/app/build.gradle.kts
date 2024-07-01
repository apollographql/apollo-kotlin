import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinJvmCompile

plugins {
  id("com.android.application")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo")
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "app.under.test"

  compileSdk = libs.versions.android.sdkversion.compilebenchmark.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.compose.min.get().toInt()
    targetSdk = libs.versions.android.sdkversion.target.get().toInt()

    val debugSigningConfig = signingConfigs.getByName("debug").apply {
      keyAlias = "key"
      keyPassword = "apollo"
      storeFile = file("keystore")
      storePassword = "apollo"
    }

    buildTypes {
      getByName("release") {
        signingConfig = debugSigningConfig
        isMinifyEnabled = true
      }
      create("benchmark") {
        initWith(getByName("release"))
        signingConfig = debugSigningConfig
        isMinifyEnabled = true
        isDebuggable = false
        applicationIdSuffix = ".benchmark"
        proguardFiles("rules.pro")
      }
    }
  }

  @Suppress("UnstableApiUsage")
  buildFeatures {
    compose = true
  }
}

dependencies {
  implementation("com.apollographql.apollo:apollo-runtime")
  implementation("com.apollographql.apollo:apollo-normalized-cache")
  implementation(libs.compose.runtime)
  implementation(libs.compose.ui)
  implementation(libs.androidx.profileinstaller)
  implementation(libs.androidx.activity)
}

tasks.withType<KotlinJvmCompile>().configureEach {
  compilerOptions {
    jvmTarget.set(JvmTarget.JVM_1_8)
  }
}

val generateQueries: Provider<Task> = tasks.register("generateQueries") {
  inputs.file("src/main/graphql/api/operations.graphql")
      .withPropertyName("inputGraphQLFile")
      .withPathSensitivity(PathSensitivity.RELATIVE)
  outputs.dir(layout.buildDirectory.dir("generated/graphql/api"))
      .withPropertyName("outputDir")

  doLast {
    val inputDocument = inputs.files.singleFile.readText()
    val outputDir = outputs.files.singleFile
    outputDir.deleteRecursively()
    outputDir.mkdirs()

    repeat(10) {
      outputDir.resolve("operation$it.graphql").writeText(
          inputDocument.replace("#SuffixPlaceholder", it.toString())
      )
    }
  }
}
apollo {
  service("api") {
    packageName.set("com.apollographql.sample")
    //generateCompiledField.set(false)
    addTypename.set("always")
    srcDir(generateQueries)
    schemaFiles.from(file("src/main/graphql/api/schema.graphqls"))
  }
}
