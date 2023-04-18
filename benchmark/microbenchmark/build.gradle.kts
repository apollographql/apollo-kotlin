import org.gradle.api.artifacts.type.ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE

plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo3")
  id("androidx.benchmark")
  id("com.google.devtools.ksp")
}

val relocated = Attribute.of("relocated", Boolean::class.javaObjectType)

dependencies {
  attributesSchema {
    attribute(relocated)
  }
  artifactTypes.named("jar").configure {
    attributes.attribute(relocated, false)
  }
  artifactTypes.create("aar") {
    attributes.attribute(relocated, false)
  }

  val relocations = mapOf(
      "com.apollographql.apollo3.cache.normalized" to "com.apollographql.apollo3.cache.normalized.incubating",
  )

  registerTransform(JarRelocateTransform::class) {
    from.attribute(relocated, false).attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar")
    to.attribute(relocated, true).attribute(ARTIFACT_TYPE_ATTRIBUTE, "jar")

    parameters.relocations.set(relocations)
  }
  registerTransform(AarRelocateTransform::class) {
    from.attribute(relocated, false).attribute(ARTIFACT_TYPE_ATTRIBUTE, "aar")
    to.attribute(relocated, true).attribute(ARTIFACT_TYPE_ATTRIBUTE, "aar")

    parameters.relocations.set(relocations)
    parameters.tmpDir.set(layout.buildDirectory.dir("aarTransforms"))
    parameters.random.set(0) //(Math.random() * 10000).toInt()) // uncomment for debug
  }

  implementation("com.apollographql.apollo3:apollo-runtime")

  mapOf(
      "com.apollographql.apollo3:apollo-normalized-cache-api" to "jvm",
      "com.apollographql.apollo3:apollo-normalized-cache-sqlite" to "android",
      "com.apollographql.apollo3:apollo-normalized-cache" to "jvm"
  ).forEach {
    val ga = it.key
    val platform = it.value
    implementation("$ga-$platform")
    /**
     * Because we want to test both artifacts and they contain the same symbols, relocate the incubating ones
     */
    implementation("$ga-incubating-$platform") {
      attributes {
        attribute(relocated, true)
      }
    }
  }
  implementation(libs.moshi)
  ksp(benchmarks.moshix.ksp)

  androidTestImplementation(benchmarks.benchmark.junit4)
  androidTestImplementation(benchmarks.androidx.test.core)
}

configure<com.android.build.gradle.LibraryExtension> {
  namespace = "com.apollographql.apollo3.benchmark"
  compileSdk = golatac.version("android.sdkversion.compile").toInt()

  defaultConfig {
    minSdk = golatac.version("android.sdkversion.min").toInt()
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

  @Suppress("UnstableApiUsage")
  useLibrary("android.test.base")

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
}

java {
  toolchain {
    languageVersion.set(JavaLanguageVersion.of(11))
  }
}

configure<com.apollographql.apollo3.gradle.api.ApolloExtension> {
  service("benchmark") {
    sourceFolder.set("benchmark")
    packageName.set("com.apollographql.apollo3.benchmark")
  }
  service("calendar-response") {
    sourceFolder.set("calendar")
    codegenModels.set("responseBased")
    packageName.set("com.apollographql.apollo3.calendar.response")
  }
  service("calendar-operation") {
    sourceFolder.set("calendar")
    codegenModels.set("operationBased")
    packageName.set("com.apollographql.apollo3.calendar.operation")
  }
}
