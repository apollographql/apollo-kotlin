plugins {
  kotlin("jvm")
}

apply(from = "../gradle/dependencies.gradle")

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

group = "com.apollographql.apollo3"

dependencies {
  compileOnly(groovy.util.Eval.x(project, "x.dep.gradleApi"))
  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))

  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.reflect").toString()) {
    because("AGP pulls kotlin-reflect with an older version and that triggers a warning in the Kotlin compiler.")
  }

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(groovy.util.Eval.x(project, "x.dep.android.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleJapiCmpPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleMetalavaPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradlePublishPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.benManesVersions"))
  implementation(groovy.util.Eval.x(project, "x.dep.vespene"))
  implementation(groovy.util.Eval.x(project, "x.dep.gr8"))
  implementation(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.dokka"))
  implementation(groovy.util.Eval.x(project, "x.dep.binaryCompatibilityValidator"))
}

// This shuts down a warning in Kotlin 1.5.30:
// 'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
// I'm not sure
java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
