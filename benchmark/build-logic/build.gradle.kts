plugins {
  `embedded-kotlin`
}

group = "com.apollographql.apollo3.benchmark"

dependencies {
  compileOnly(gradleApi())
  implementation(libs.kotlin.plugin)
  implementation(libs.okio)
  implementation(libs.ksp)
  implementation(benchmarks.jar.relocator)
  if (true) {
    implementation(libs.apollo.plugin)
  } else {
    implementation("com.apollographql.apollo3:apollo-gradle-plugin:3.4.0")
  }
  implementation(benchmarks.benchmark.gradle.plugin)
  implementation(benchmarks.agp)
  implementation(benchmarks.asm)
}
