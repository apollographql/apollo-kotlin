plugins {
  `embedded-kotlin`
  `java-gradle-plugin`
}

group = "com.apollographql.apollo.benchmark"

dependencies {
  compileOnly(gradleApi())
  implementation(libs.kotlin.plugin)
  implementation(libs.okio)
  implementation(libs.ksp)
  implementation(libs.android.plugin)
  implementation(libs.benchmark.gradle.plugin)
}


gradlePlugin {
  plugins {
    register("apollo.benchmark") {
      id = "apollo.benchmark"
      implementationClass = "BenchmarkPlugin"
    }
  }
}
