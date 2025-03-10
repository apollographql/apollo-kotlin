plugins {
  id("org.jetbrains.kotlin.jvm")
  id("com.apollographql.apollo")
}

apolloTest()

dependencies {
  implementation(libs.apollo.runtime)
  implementation(project(":multi-module-2-root"))
  testImplementation(libs.kotlin.test.junit)
}

apollo {
  service("multimodule2") {
    packageName.set("multimodule2.child")
    generateApolloMetadata.set(true)
    flattenModels.set(false)
  }
}

dependencies {
  add("apolloMultimodule2", project(":multi-module-2-root"))
}
