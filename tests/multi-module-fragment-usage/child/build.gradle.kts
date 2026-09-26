plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":multi-module-fragment-usage-root"))
}

apollo {
  service("service") {
    packageName.set("multimodulefragmentusage.child")
    dependsOn(project(":multi-module-fragment-usage-root"))
  }
}
