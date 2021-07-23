plugins {
  kotlin("jvm")
}

apply(from = "../gradle/dependencies.gradle")

repositories {
  mavenCentral()
  google()
  gradlePluginPortal()
}

group = "com.apollographql.apollo"

dependencies {
  implementation(gradleApi())
  implementation(groovy.util.Eval.x(project, "x.dep.okHttp.okHttp4"))
  implementation(groovy.util.Eval.x(project, "x.dep.moshi.moshi"))

  compileOnly(groovy.util.Eval.x(project, "x.dep.kotlin.reflect").toString()) {
    because("AGP pulls kotlin-reflect with an older version and that triggers a warning in the Kotlin compiler.")
  }

  // We add all the plugins to the classpath here so that they are loaded with proper conflict resolution
  // See https://github.com/gradle/gradle/issues/4741
  implementation(groovy.util.Eval.x(project, "x.dep.android.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleJapiCmpPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradleMetalavaPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.kotlin.coroutines"))
  implementation(groovy.util.Eval.x(project, "x.dep.sqldelight.plugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.gradlePublishPlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.benManesVersions"))
  implementation(groovy.util.Eval.x(project, "x.dep.vespene"))
  implementation(groovy.util.Eval.x(project, "x.dep.shadow"))
  implementation(groovy.util.Eval.x(project, "x.dep.kspGradlePlugin"))
  implementation(groovy.util.Eval.x(project, "x.dep.dokka"))
}
