plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo")
  id("androidx.benchmark")
  id("com.google.devtools.ksp")
}

configure<com.android.build.gradle.LibraryExtension> {
  namespace = "com.apollographql.apollo.benchmark"
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
}

dependencies {

  implementation(libs.apollo.runtime)

  implementation(libs.moshi)
  ksp(libs.moshix.ksp)

  androidTestImplementation(libs.benchmark.junit4)
  androidTestImplementation(libs.androidx.test.core)
  androidTestImplementation(libs.apollo.mockserver)
  androidTestImplementation(libs.apollo.testingsupport)

  // Stable cache
  androidTestImplementation(libs.apollo.normalizedcache)
  androidTestImplementation(libs.apollo.normalizedcache.sqlite)

  // Incubating cache
  androidTestImplementation(libs.apollo.normalizedcache.sqlite.incubating.snapshot)
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

configure<com.apollographql.apollo.gradle.api.ApolloExtension> {
  service("benchmark") {
    srcDir("src/main/graphql/benchmark")
    packageName.set("com.apollographql.apollo.benchmark")
  }
  service("calendar-response") {
    srcDir("src/main/graphql/calendar")
    codegenModels.set("responseBased")
    packageName.set("com.apollographql.apollo.calendar.response")
  }
  service("calendar-operation") {
    srcDir("src/main/graphql/calendar")
    codegenModels.set("operationBased")
    packageName.set("com.apollographql.apollo.calendar.operation")
  }
}
