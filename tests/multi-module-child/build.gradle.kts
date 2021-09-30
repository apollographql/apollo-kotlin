plugins {
  id("com.apollographql.apollo3")
  id("org.jetbrains.kotlin.jvm")
}


dependencies {
  implementation("com.apollographql.apollo3:apollo-runtime")
  implementation(project(":multi-module-root"))
  apolloMetadata(project(":multi-module-root"))
  testImplementation(kotlin("test-junit"))
}

apollo {
  packageName.set("multimodule.child")
}