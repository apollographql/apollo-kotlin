plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":multi-module-1-root"))
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("service") {
    dependsOn(project(":multi-module-1-root"))
    packageName.set("multimodule1.child")
    flattenModels.set(false)
  }
}
