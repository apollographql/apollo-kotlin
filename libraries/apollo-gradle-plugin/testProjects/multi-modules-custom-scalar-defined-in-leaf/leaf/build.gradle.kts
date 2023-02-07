plugins {
  id("org.jetbrains.kotlin.jvm")
  alias(libs.plugins.apollo)
}

dependencies {
  implementation(kotlin("stdlib"))
  implementation(libs.apollo.api)
  testImplementation(libs.kotlin.test.junit)

  implementation(project(":root"))
}

apollo {
  service("service") {
    packageName.set("com.library")
    mapScalar("Long", "java.lang.Long")
    dependsOn(project(":root"))
  }
}
