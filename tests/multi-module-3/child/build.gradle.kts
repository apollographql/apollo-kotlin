plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":multi-module-3-root"))
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("multimodule3") {
    packageName.set("multimodule3.child")
    flattenModels.set(false)
    dependsOn(project(":multi-module-3-root"))
  }
}
