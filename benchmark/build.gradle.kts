buildscript {
  apply(from = "../gradle/dependencies.gradle")

  repositories {
    mavenCentral()
    google()
    mavenLocal()
    maven {
      url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
    }
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.androidPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))

    classpath("com.apollographql.apollo3:apollo-gradle-plugin:${properties.get("apolloVersion")}")
    classpath("androidx.benchmark:benchmark-gradle-plugin:1.0.0")
  }
}

apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo3")
apply(plugin = "androidx.benchmark")
apply(plugin = "com.google.devtools.ksp")

repositories {
  mavenCentral()
  google()
  mavenLocal()
  maven {
    url = uri("https://s01.oss.sonatype.org/content/repositories/snapshots/")
  }
}

dependencies {
  add("implementation", "com.apollographql.apollo3:apollo-runtime:${properties.get("apolloVersion")}")
  add("implementation", "com.apollographql.apollo3:apollo-api:${properties.get("apolloVersion")}")
  add("implementation", "com.apollographql.apollo3:apollo-normalized-cache-sqlite:${properties.get("apolloVersion")}")
  add("implementation", "com.apollographql.apollo3:apollo-normalized-cache:${properties.get("apolloVersion")}")

  add("implementation", groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  add("ksp", groovy.util.Eval.x(project, "x.dep.moshiKsp"))

  add("androidTestImplementation", "androidx.benchmark:benchmark-junit4:1.0.0")
}

configure<com.android.build.gradle.LibraryExtension> {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

  useLibrary("android.test.base")
}

configure<com.apollographql.apollo3.gradle.api.ApolloExtension> {
  packageNamesFromFilePaths()
}
