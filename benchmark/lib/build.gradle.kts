import com.apollographql.apollo3.compiler.APOLLO_VERSION

apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")
apply(plugin = "androidx.benchmark")
apply(plugin = "com.google.devtools.ksp")

dependencies {
  add("implementation", "com.apollographql.apollo3:apollo-runtime:$APOLLO_VERSION")
  add("implementation", "com.apollographql.apollo3:apollo-normalized-cache-sqlite:$APOLLO_VERSION")
  add("implementation", "com.apollographql.apollo3:apollo-normalized-cache:$APOLLO_VERSION")

  add("implementation", groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  add("ksp", groovy.util.Eval.x(project, "x.dep.moshiKsp"))

  add("androidTestImplementation", "androidx.benchmark:benchmark-junit4:1.1.0-rc02")
  add("androidTestImplementation", "androidx.test:core:1.4.0")
}

configure<com.android.build.gradle.LibraryExtension> {
  namespace = "com.apollographql.apollo3.benchmark"
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdk = groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString().toInt()
    targetSdk = groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString().toInt()
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

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
