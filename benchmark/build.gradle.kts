buildscript {
  apply(from = "../gradle/dependencies.gradle")

  repositories {
    google()
    mavenCentral()
    jcenter()
    repositories {
      maven {
        url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
      }
    }
    mavenLocal()
  }
  dependencies {
    classpath(groovy.util.Eval.x(project, "x.dep.android.plugin"))
    classpath(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
    classpath("com.apollographql.apollo:apollo-gradle-plugin:2.4.1")
    classpath("androidx.benchmark:benchmark-gradle-plugin:1.0.0")
  }
}

apply(plugin = "com.android.library")
apply(plugin = "org.jetbrains.kotlin.android")
apply(plugin = "com.apollographql.apollo")
apply(plugin = "androidx.benchmark")
apply(plugin = "org.jetbrains.kotlin.kapt")

repositories {
  google()
  mavenCentral()
  jcenter()
  repositories {
    maven {
      url = uri("https://oss.sonatype.org/content/repositories/snapshots/")
    }
  }
  mavenLocal()
}

dependencies {
  add("implementation", "com.apollographql.apollo:apollo-api:2.4.1")
  add("implementation", groovy.util.Eval.x(project, "x.dep.moshi.moshi"))
  add("kapt", groovy.util.Eval.x(project, "x.dep.moshi.kotlinCodegen"))

  add("androidTestImplementation", "androidx.benchmark:benchmark-junit4:1.0.0")
}

configure<com.android.build.gradle.LibraryExtension> {
  compileSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.compileSdkVersion").toString().toInt())

  defaultConfig {
    minSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.minSdkVersion").toString())
    targetSdkVersion(groovy.util.Eval.x(project, "x.androidConfig.targetSdkVersion").toString())
    testInstrumentationRunner = "androidx.benchmark.junit4.AndroidBenchmarkRunner"
  }

  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }

  useLibrary("android.test.base")
}

tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinCompile> {
  kotlinOptions {
    jvmTarget = JavaVersion.VERSION_1_8.toString()
  }
}

plugins.withType(org.gradle.api.plugins.JavaPlugin::class.java) {
  extensions.configure(JavaPluginExtension::class.java) {
    sourceCompatibility = JavaVersion.VERSION_1_8
    targetCompatibility = JavaVersion.VERSION_1_8
  }
}

configure<com.apollographql.apollo.gradle.api.ApolloExtension> {
}
