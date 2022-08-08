plugins {
  `embedded-kotlin`
}

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

group = "com.apollographql.apollo3"

dependencies {
  compileOnly(libs.gradle.api)

  implementation(libs.okHttp)
  implementation(libs.moshi)
  implementation(libs.dokka.plugin)
  implementation(libs.dokka.base)

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(libs.android.plugin)
  implementation(libs.gradle.japicmp.plugin)
  implementation(libs.gradle.metalava.plugin)
  implementation(libs.vespene)

  // We want the KSP plugin to use the version from the classpath and not force a newer version
  // of the Gradle plugin
  if (System.getProperty("idea.sync.active") == null) {
    implementation(libs.kotlin.plugin)
    runtimeOnly(libs.ksp)
  } else {
    implementation(libs.kotlin.plugin.duringIdeaSync)
    runtimeOnly(libs.ksp.duringIdeaSync)
  }

  runtimeOnly(libs.sqldelight.plugin)
  runtimeOnly(libs.gradle.publish.plugin)
  runtimeOnly(libs.benManes.versions)
  runtimeOnly(libs.gr8)
  runtimeOnly(libs.binaryCompatibilityValidator)
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  runtimeOnly(libs.kotlin.allOpen)
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
