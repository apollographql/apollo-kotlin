plugins {
  id("com.android.library")
  id("org.jetbrains.kotlin.android")
  id("com.apollographql.apollo3")
  id("androidx.benchmark")
  id("com.google.devtools.ksp")
}

val relocated = Attribute.of("relocated", Boolean::class.javaObjectType)
val artifactType = Attribute.of("artifactType", String::class.java)

dependencies {
  attributesSchema {
    attribute(relocated)
  }

  artifactTypes.getByName("jar") {
    attributes.attribute(relocated, false)
  }

  registerTransform(RelocateTransform::class) {
    from.attribute(relocated, false).attribute(artifactType, "jar") //.attribute(Category.CATEGORY_ATTRIBUTE, library)
    to.attribute(relocated, true).attribute(artifactType, "jar") //.attribute(Category.CATEGORY_ATTRIBUTE, library)

    parameters.relocations.set(mapOf(
        "com.apollographql.apollo3.cache.normalized" to "com.apollographql.apollo3.cache.normalized.incubating",
    ))
  }

  implementation("com.apollographql.apollo3:apollo-runtime")

  listOf(
      "com.apollographql.apollo3:apollo-normalized-cache-api",
      "com.apollographql.apollo3:apollo-normalized-cache-sqlite",
      "com.apollographql.apollo3:apollo-normalized-cache"
  ).forEach {
    implementation("$it-jvm")
    implementation("$it-incubating-jvm") {
      attributes {
        attribute(relocated, true)
      }
    }
  }
  implementation(groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  ksp(groovy.util.Eval.x(project, "x.dep.moshiKsp"))

  androidTestImplementation("androidx.benchmark:benchmark-junit4:1.1.0-rc02")
  androidTestImplementation("androidx.test:core:1.4.0")
}

configure<com.android.build.gradle.LibraryExtension> {
  namespace = "com.apollographql.apollo3.benchmark"
  compileSdk = groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt()

  defaultConfig {
    minSdk = groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString().toInt()
    targetSdk = groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString().toInt()
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

  @Suppress("UnstableApiUsage")
  useLibrary("android.test.base")
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
