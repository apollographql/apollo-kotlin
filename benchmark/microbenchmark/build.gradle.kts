import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo3")
  id("androidx.benchmark")
  id("com.google.devtools.ksp")
}

configure<com.android.build.gradle.LibraryExtension> {
  namespace = "com.apollographql.apollo3.benchmark"
  compileSdk = libs.versions.android.sdkversion.compilebenchmark.get().toInt()

  defaultConfig {
    minSdk = libs.versions.android.sdkversion.benchmark.min.get().toInt()
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

  useLibrary("android.test.base")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }

  flavorDimensions += "stability"
  productFlavors {
    create("incubating") {
      dimension = "stability"
    }
    create("stable") {
      dimension = "stability"
    }
  }
}

dependencies {

  implementation("com.apollographql.apollo3:apollo-runtime")

  listOf(
      "com.apollographql.apollo3:apollo-normalized-cache-api",
      "com.apollographql.apollo3:apollo-normalized-cache-sqlite",
      "com.apollographql.apollo3:apollo-normalized-cache"
  ).forEach {
    add("androidTestStableImplementation", it)
    add("androidTestIncubatingImplementation", "$it-incubating")
  }

  implementation(libs.moshi)
  ksp(libs.moshix.ksp)

  androidTestImplementation(libs.benchmark.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation("com.apollographql.apollo3:apollo-mockserver")
  androidTestImplementation("com.apollographql.apollo3:apollo-testing-support")
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

configure<com.apollographql.apollo3.gradle.api.ApolloExtension> {
  service("benchmark") {
    srcDir("src/main/graphql/benchmark")
    packageName.set("com.apollographql.apollo3.benchmark")
  }
  service("calendar-response") {
    srcDir("src/main/graphql/calendar")
    codegenModels.set("responseBased")
    packageName.set("com.apollographql.apollo3.calendar.response")
  }
  service("calendar-operation") {
    srcDir("src/main/graphql/calendar")
    codegenModels.set("operationBased")
    packageName.set("com.apollographql.apollo3.calendar.operation")
  }
}
