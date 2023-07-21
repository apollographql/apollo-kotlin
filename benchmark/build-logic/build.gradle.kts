plugins {
  `embedded-kotlin`
  `java-gradle-plugin`
}

group = "com.apollographql.apollo3.benchmark"

dependencies {
  compileOnly(gradleApi())
  implementation(libs.kotlin.plugin)
  implementation(libs.okio)
  implementation(libs.ksp)
  implementation(libs.android.plugin)

  implementation(libs.jar.relocator)
  implementation(libs.benchmark.gradle.plugin)
  implementation(libs.asm)
}


gradlePlugin {
  plugins {
    register("apollo.benchmark") {
      id = "apollo.benchmark"
      implementationClass = "BenchmarkPlugin"
    }
  }
}
