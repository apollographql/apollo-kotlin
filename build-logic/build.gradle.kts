plugins {
  `embedded-kotlin`
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

  implementation(groovy.util.Eval.x(project, "x.dep.okHttpOkHttp"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshiMoshi"))
  implementation(groovy.util.Eval.x(project, "x.dep.dokka"))

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(groovy.util.Eval.x(project, "x.dep.androidPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleJapiCmpPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleMetalavaPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.vespene"))

  // We want the KSP plugin to use the version from the classpath and not force a newer version
  // of the Gradle plugin
  if (System.getProperty("idea.sync.active") == null) {
    implementation(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    runtimeOnly(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))
  } else {
    implementation(groovy.util.Eval.x(project, "x.dep.kotlinPluginDuringIdeaSync"))
    runtimeOnly(groovy.util.Eval.x(project, "x.dep.kspGradlePluginDuringIdeaSync"))
  }

  runtimeOnly(groovy.util.Eval.x(project, "x.dep.sqldelightPlugin"))
  runtimeOnly(groovy.util.Eval.x(project, "x.dep.gradlePublishPlugin"))
  runtimeOnly(groovy.util.Eval.x(project, "x.dep.benManesVersions"))
  runtimeOnly(groovy.util.Eval.x(project, "x.dep.gr8"))
  runtimeOnly(groovy.util.Eval.x(project, "x.dep.binaryCompatibilityValidator"))
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  runtimeOnly(groovy.util.Eval.x(project, "x.dep.kotlinAllOpen"))
}

// This shuts down a warning in Kotlin 1.5.30:
// 'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
java {
  toolchain.languageVersion.set(JavaLanguageVersion.of(8))
}

tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
  kotlinOptions {
    allWarningsAsErrors = true
  }
}
