buildscript {
  apply(from = "../gradle/dependencies.gradle")

  repositories {
    mavenCentral()
    google()
    maven {
      url = uri("../build/localMaven")
    }
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))

    val apolloVersion = properties["apolloVersion"]?.toString()
    if (apolloVersion.isNullOrBlank()) {
      classpath(groovy.util.Eval.x(project, "x.dep.apolloPlugin"))
    } else {
      classpath("com.apollographql.apollo3:apollo-gradle-plugin:${apolloVersion}")
    }
    classpath("androidx.benchmark:benchmark-gradle-plugin:1.1.0")
    classpath("com.android.tools.build:gradle:7.4.0-alpha05")
  }
}

apply(plugin = "com.android.application")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")
apply(plugin = "androidx.benchmark")
apply(plugin = "com.google.devtools.ksp")

repositories {
  mavenCentral()
  google()
  maven {
    url = uri("../build/localMaven")
  }
  maven {
    url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
}

dependencies {
  val apolloVersion = properties["apolloVersion"]?.toString()
  if (apolloVersion.isNullOrBlank()) {
    add("implementation", groovy.util.Eval.x(project, "x.dep.apolloRuntime"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.apolloNormalizedCacheSqlite"))
    add("implementation", groovy.util.Eval.x(project, "x.dep.apolloNormalizedCache"))
  } else {
    add("implementation", "com.apollographql.apollo3:apollo-runtime:${properties.get("apolloVersion")}")
    add("implementation", "com.apollographql.apollo3:apollo-normalized-cache-sqlite:${properties.get("apolloVersion")}")
    add("implementation", "com.apollographql.apollo3:apollo-normalized-cache:${properties.get("apolloVersion")}")
  }
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  add("ksp", groovy.util.Eval.x(project, "x.dep.moshiKsp"))

  add("androidTestImplementation", "androidx.benchmark:benchmark-junit4:1.1.0-rc02")
  add("androidTestImplementation", "androidx.test:core:1.4.0")
}

configure<com.android.build.gradle.AppExtension> {
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
