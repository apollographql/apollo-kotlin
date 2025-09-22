plugins {
  alias(libs.plugins.kotlin.jvm.latest)
  alias(libs.plugins.compat.patrouille)
}

group = "benchmark"

compatPatrouille {
  java(17)
  kotlin(embeddedKotlinVersion)
}

dependencies {
  compileOnly(gradleApi())
  implementation(libs.kotlin.plugin)
  implementation(libs.okio)
  implementation(libs.ksp)
  implementation(libs.android.plugin)
  implementation(libs.benchmark.gradle.plugin)
}
