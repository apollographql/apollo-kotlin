import org.gradle.kotlin.dsl.add
import org.gradle.kotlin.dsl.project

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
    packageName.set("multimodule1.child")
    flattenModels.set(false)
    generateApolloMetadata.set(true)
    alwaysGenerateTypesMatching.set(emptyList())
  }
}

dependencies {
  add("apolloService", project(":multi-module-1-root"))
}
