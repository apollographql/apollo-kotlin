import org.jetbrains.kotlin.gradle.plugin.getKotlinPluginVersion

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

  // Pin the Kotlin stdlib to the version used by the embedded Kotlin plugin
  implementation("org.jetbrains.kotlin:kotlin-stdlib") {
    version {
      strictly(getKotlinPluginVersion())
    }
  }
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
  implementation(groovy.util.Eval.x(project, "x.dep.kotlinPluginWithoutVersion"))
  implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradlePublishPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.benManesVersions"))
  implementation(groovy.util.Eval.x(project, "x.dep.vespene"))
  implementation(groovy.util.Eval.x(project, "x.dep.gr8"))

  // We want the KSP plugin to use the version from the classpath and not force a newer version
  // of the Gradle plugin
  if (System.getProperty("idea.sync.active") == null) {
    implementation(groovy.util.Eval.x(project, "x.dep.kotlinPlugin"))
    runtimeOnly(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin_1_7_0"))
  } else {
    implementation(groovy.util.Eval.x(project, "x.dep.kotlinPluginDuringIdeaSync"))
    runtimeOnly(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin_1_6_10"))
  }

  implementation(groovy.util.Eval.x(project, "x.dep.dokka"))
  implementation(groovy.util.Eval.x(project, "x.dep.binaryCompatibilityValidator"))
  // XXX: This is only needed for tests. We could have different build logic for different
  // builds but this seems just overkill for now
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.allOpen"))
}

// This shuts down a warning in Kotlin 1.5.30:
// 'compileJava' task (current target is 11) and 'compileKotlin' task (current target is 1.8) jvm target compatibility should be set to the same Java version.
// I'm not sure
java {
  sourceCompatibility = JavaVersion.VERSION_1_8
  targetCompatibility = JavaVersion.VERSION_1_8
}

// Commented for now as we do have a warning related to gr8-plugin-0.4.jar: "Library has Kotlin runtime bundled into it"
//tasks.withType(org.jetbrains.kotlin.gradle.tasks.KotlinCompile::class.java) {
//  kotlinOptions {
//    allWarningsAsErrors = true
//  }
//}
